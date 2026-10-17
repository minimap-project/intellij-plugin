package actions;

import com.google.gson.Gson;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import dto.ImageDTO;
import dto.ResponseDTO;
import messages.MyMessageBundle;
import org.jetbrains.annotations.NotNull;
import requests.PostImageRequest;
import services.ImageService;
import services.ProjectScannerService;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class AnalyzeProjectAverageAction extends BaseAnalysisAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        VirtualFile projectBaseDir = getProjectBaseDir(project);
        if (projectBaseDir == null) {
            showNotification(project, MyMessageBundle.message("dialog.title.warning"),
                    "Não foi possível determinar o diretório raiz do projeto.", NotificationType.WARNING);
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(
                project,
                "MinimapSignatures: Análise por Minimap Médio",
                false
        ) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Varrendo arquivos elegíveis do projeto...");

                List<VirtualFile> eligibleFiles = ProjectScannerService.findEligibleFiles(projectBaseDir);
                if (eligibleFiles.isEmpty()) {
                    showNotification(
                            project,
                            MyMessageBundle.message("dialog.title.warning"),
                            "Nenhum arquivo de código elegível encontrado no projeto " + project.getName(),
                            NotificationType.WARNING
                    );
                    return;
                }

                // Agrupamento por extensão
                Map<String, List<VirtualFile>> filesByExt = ProjectScannerService.groupByExtension(eligibleFiles);

                // Determinar a extensão predominante
                String predominantExt = "";
                List<VirtualFile> predominantFiles = null;
                for (Map.Entry<String, List<VirtualFile>> entry : filesByExt.entrySet()) {
                    if (predominantFiles == null || entry.getValue().size() > predominantFiles.size()) {
                        predominantExt = entry.getKey();
                        predominantFiles = entry.getValue();
                    }
                }

                if (predominantFiles == null || predominantFiles.isEmpty()) {
                    showNotification(
                            project,
                            MyMessageBundle.message("dialog.title.warning"),
                            "Nenhuma extensão predominante encontrada no projeto.",
                            NotificationType.WARNING
                    );
                    return;
                }

                indicator.setText(String.format("Calculando minimap médio para %d arquivos (%s)...",
                        predominantFiles.size(), predominantExt));

                long[][] sumPixels = new long[ImageService.IMAGE_SIZE][ImageService.IMAGE_SIZE];
                int validCount = 0;
                int readFailures = 0;

                int totalGroup = predominantFiles.size();
                for (int i = 0; i < totalGroup; i++) {
                    VirtualFile file = predominantFiles.get(i);
                    indicator.setFraction((double) (i + 1) / totalGroup);

                    try {
                        String content = ApplicationManager.getApplication().runReadAction(
                                (com.intellij.openapi.util.Computable<String>) () -> {
                                    try {
                                        return VfsUtilCore.loadText(file);
                                    } catch (Exception ex) {
                                        throw new RuntimeException(ex);
                                    }
                                }
                        );

                        int[][] intensities = ImageService.getPixelIntensities(content);
                        for (int r = 0; r < ImageService.IMAGE_SIZE; r++) {
                            for (int c = 0; c < ImageService.IMAGE_SIZE; c++) {
                                sumPixels[r][c] += intensities[r][c];
                            }
                        }
                        validCount++;
                    } catch (Exception ex) {
                        readFailures++;
                    }
                }

                if (validCount == 0) {
                    showNotification(
                            project,
                            MyMessageBundle.message("dialog.title.warning"),
                            "Falha ao ler os arquivos da extensão " + predominantExt,
                            NotificationType.ERROR
                    );
                    return;
                }

                indicator.setText("Construindo imagem média e enviando para inferência...");
                BufferedImage avgImage = ImageService.createAverageImage(sumPixels, validCount);

                try {
                    String base64Image = ImageService.convertBase64(avgImage);
                    String avgHash = ImageService.md5Hex("average:" + project.getName() + ":" + predominantExt + ":" + validCount);

                    HttpResponse<String> response = PostImageRequest.uploadImage(
                            new ImageDTO(project.getName(), avgHash, base64Image)
                    );

                    if (response.statusCode() == 200) {
                        ResponseDTO responseDTO = new Gson().fromJson(response.body(), ResponseDTO.class);
                        String markdown = buildAverageMarkdown(
                                project.getName(),
                                projectBaseDir.getPath(),
                                predominantExt,
                                validCount,
                                eligibleFiles.size(),
                                readFailures,
                                avgHash,
                                responseDTO
                        );

                        String reportFileName = "AverageMinimap_" + project.getName() + "_" + predominantExt.replace(".", "") + ".md";
                        openMarkdownReport(project, reportFileName, markdown);

                        showNotification(
                                project,
                                "MinimapSignatures",
                                String.format("Minimap médio analisado com sucesso (%d arquivos agregados).", validCount),
                                NotificationType.INFORMATION
                        );
                    } else {
                        showNotification(
                                project,
                                MyMessageBundle.message("dialog.title.warning"),
                                "Erro na API: " + response.statusCode() + " - " + response.body(),
                                NotificationType.ERROR
                        );
                    }
                } catch (Exception ex) {
                    showNotification(
                            project,
                            MyMessageBundle.message("dialog.title.warning"),
                            "Erro ao enviar minimap médio: " + ex.getMessage(),
                            NotificationType.ERROR
                    );
                }
            }
        });
    }

    private VirtualFile getProjectBaseDir(Project project) {
        String basePath = project.getBasePath();
        if (basePath != null) {
            VirtualFile dir = VfsUtil.findFileByIoFile(new File(basePath), true);
            if (dir != null && dir.isValid()) {
                return dir;
            }
        }
        return null;
    }

    private String buildAverageMarkdown(
            String projectName,
            String projectPath,
            String predominantExt,
            int filesAggregated,
            int totalEligibleInProject,
            int readFailures,
            String avgHash,
            ResponseDTO responseDTO
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# MinimapSignatures — Análise por Minimap Médio do Projeto\n\n");
        sb.append("## Metadados\n");
        sb.append("- **Projeto:** `").append(projectName).append("`\n");
        sb.append("- **Raiz do Projeto:** `").append(projectPath).append("`\n");
        sb.append("- **Extensão Predominante:** `").append(predominantExt).append("`\n");
        sb.append("- **Arquivos Agregados no Minimap Médio:** ").append(filesAggregated).append("\n");
        sb.append("- **Total de Arquivos Elegíveis no Projeto:** ").append(totalEligibleInProject).append("\n");
        if (readFailures > 0) {
            sb.append("- **Falhas de Leitura:** ").append(readFailures).append("\n");
        }
        sb.append("- **MD5 do Minimap Médio:** `").append(avgHash).append("`\n");
        sb.append("- **Hash Retornado pela API:** `").append(responseDTO.hash()).append("`\n\n");

        appendPredictionsMarkdown(sb, responseDTO);
        sb.append("\n---\n*Gerado por MinimapSignatures via agregação de pixels do minimap*");
        return sb.toString();
    }
}
