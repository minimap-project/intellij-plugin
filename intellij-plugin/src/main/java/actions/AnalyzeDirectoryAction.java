package actions;

import com.google.gson.Gson;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import dto.ImageDTO;
import dto.ResponseDTO;
import messages.MyMessageBundle;
import org.jetbrains.annotations.NotNull;
import requests.PostImageRequest;
import services.ImageService;
import services.ProjectScannerService;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AnalyzeDirectoryAction extends BaseAnalysisAction {

    private static final int CONCURRENCY = 4;

    public record FileResultItem(
            String relativePath,
            boolean success,
            ResponseDTO response,
            String errorMessage
    ) {}

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("Selecione um Único Diretório para Análise Individual")
                .withDescription("Apenas arquivos elegíveis deste diretório e subdiretórios serão analisados");

        VirtualFile chosenFolder = FileChooser.chooseFile(descriptor, project, null);
        if (chosenFolder == null) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(
                project,
                "MinimapSignatures: Análise Individual de Diretório",
                true
        ) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Buscando arquivos elegíveis no diretório...");

                List<VirtualFile> eligibleFiles = ProjectScannerService.findEligibleFiles(chosenFolder);

                if (eligibleFiles.isEmpty()) {
                    showNotification(
                            project,
                            MyMessageBundle.message("dialog.title.warning"),
                            "Nenhum arquivo elegível encontrado no diretório: " + chosenFolder.getPath(),
                            NotificationType.WARNING
                    );
                    return;
                }

                int total = eligibleFiles.size();
                AtomicInteger processed = new AtomicInteger(0);
                AtomicInteger failedCount = new AtomicInteger(0);
                List<FileResultItem> results = Collections.synchronizedList(new ArrayList<>());

                ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);

                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (VirtualFile file : eligibleFiles) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        if (indicator.isCanceled()) {
                            return;
                        }

                        String relPath = VfsUtilCore.getRelativePath(file, chosenFolder);
                        if (relPath == null) {
                            relPath = file.getName();
                        }

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

                            String image = ImageService.createImage(content);
                            String imageHash = ImageService.md5Hex(relPath + ":" + content);

                            HttpResponse<String> response = PostImageRequest.uploadImage(
                                    new ImageDTO(project.getName(), imageHash, image)
                            );

                            if (response.statusCode() == 200) {
                                ResponseDTO responseDTO = new Gson().fromJson(response.body(), ResponseDTO.class);
                                results.add(new FileResultItem(relPath, true, responseDTO, null));
                            } else {
                                failedCount.incrementAndGet();
                                results.add(new FileResultItem(
                                        relPath,
                                        false,
                                        null,
                                        "HTTP " + response.statusCode() + ": " + response.body()
                                ));
                            }
                        } catch (Exception ex) {
                            failedCount.incrementAndGet();
                            String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                            results.add(new FileResultItem(relPath, false, null, msg));
                        } finally {
                            int current = processed.incrementAndGet();
                            indicator.setFraction((double) current / total);
                            indicator.setText(String.format("Processando: %d/%d (%d falhas)", current, total, failedCount.get()));
                        }
                    }, executor);

                    futures.add(future);
                }

                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                } finally {
                    executor.shutdown();
                }

                // Ordena resultados por caminho relativo
                results.sort((a, b) -> a.relativePath().compareToIgnoreCase(b.relativePath()));

                int successes = results.size() - failedCount.get();
                String markdown = buildDirectoryMarkdown(
                        project.getName(),
                        chosenFolder.getPath(),
                        total,
                        successes,
                        failedCount.get(),
                        results
                    );

                String reportFileName = "DirectoryAnalysis_" + chosenFolder.getName() + ".md";
                openMarkdownReport(project, reportFileName, markdown);

                showNotification(
                        project,
                        "MinimapSignatures",
                        String.format("Análise concluída: %d processados com sucesso, %d falhas.", successes, failedCount.get()),
                        failedCount.get() == 0 ? NotificationType.INFORMATION : NotificationType.WARNING
                );
            }
        });
    }

    private String buildDirectoryMarkdown(
            String projectName,
            String directoryPath,
            int totalFound,
            int successCount,
            int failedCount,
            List<FileResultItem> results
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("# MinimapSignatures — Análise Individual de Diretório\n\n");
        sb.append("## Resumo da Análise\n");
        sb.append("- **Diretório Analisado:** `").append(directoryPath).append("`\n");
        sb.append("- **Projeto:** `").append(projectName).append("`\n");
        sb.append("- **Total de Arquivos Encontrados:** ").append(totalFound).append("\n");
        sb.append("- **Processados com Sucesso:** ").append(successCount).append("\n");
        sb.append("- **Falhas:** ").append(failedCount).append("\n\n");

        sb.append("## Resultados por Arquivo\n\n");
        int index = 1;
        for (FileResultItem item : results) {
            sb.append("### ").append(index++).append(". `").append(item.relativePath()).append("`\n");
            if (item.success() && item.response() != null) {
                sb.append("- **Status:** Sucesso\n");
                sb.append("- **Hash:** `").append(item.response().hash()).append("`\n\n");
                appendPredictionsMarkdown(sb, item.response());
            } else {
                sb.append("- **Status:** Falha\n");
                sb.append("- **Erro:** `").append(item.errorMessage()).append("`\n\n");
            }
            sb.append("---\n\n");
        }

        sb.append("*Para realizar uma nova análise em outro diretório, execute novamente 'MinimapSignatures: Análise Individual de Diretório'.*\n");
        return sb.toString();
    }
}
