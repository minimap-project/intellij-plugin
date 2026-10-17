package actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import dto.ResponseDTO;

import javax.swing.*;

public abstract class BaseAnalysisAction extends AnAction {

    public static void showNotification(Project project, String title, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("SourceCodeMinimaps.Notifications")
                .createNotification(title, content, type)
                .notify(project);
    }

    public static void openMarkdownReport(Project project, String fileName, String markdownContent) {
        LightVirtualFile virtualFile = new LightVirtualFile(
                fileName,
                FileTypeManager.getInstance().getFileTypeByExtension("md"),
                markdownContent
        );

        ApplicationManager.getApplication().invokeLater(() -> {
            FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
            EditorWindow currentWindow = fileEditorManager.getCurrentWindow();

            if (currentWindow != null) {
                currentWindow.split(SwingConstants.VERTICAL, true, virtualFile, true);
            } else {
                fileEditorManager.openFile(virtualFile, true);
            }
        });
    }

    public static String buildSingleFileMarkdown(String projectName, String relativePath, ResponseDTO response) {
        StringBuilder sb = new StringBuilder();
        sb.append("# MinimapSignatures — Análise do Arquivo\n\n");
        sb.append("## Metadados\n");
        sb.append("- **Projeto:** `").append(projectName).append("`\n");
        sb.append("- **Arquivo:** `").append(relativePath).append("`\n");
        sb.append("- **Hash:** `").append(response.hash()).append("`\n\n");

        appendPredictionsMarkdown(sb, response);
        sb.append("\n---\n*Gerado por MinimapSignatures*");
        return sb.toString();
    }

    public static void appendPredictionsMarkdown(StringBuilder sb, ResponseDTO response) {
        sb.append("## Previsões do Modelo\n\n");
        if (response.predict() != null && !response.predict().isEmpty()) {
            for (ResponseDTO.TargetPrediction targetPred : response.predict()) {
                sb.append("### ").append(targetPred.target().toUpperCase()).append("\n");
                sb.append("| Classe | Confiança |\n");
                sb.append("| :--- | :--- |\n");
                for (ResponseDTO.Prediction pred : targetPred.predictions()) {
                    sb.append("| `").append(pred.className()).append("` | ")
                            .append(String.format("%.2f%%", pred.confidence() * 100))
                            .append(" |\n");
                }
                sb.append("\n");
            }
        } else {
            sb.append("Nenhuma previsão disponível.\n\n");
        }
    }
}
