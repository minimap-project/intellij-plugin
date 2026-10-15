package actions;

import com.google.gson.Gson;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.testFramework.LightVirtualFile;
import dto.ImageDTO;
import messages.MyMessageBundle;
import org.jetbrains.annotations.NotNull;
import dto.ResponseDTO;
import requests.PostImageRequest;
import services.ImageService;
import java.net.http.HttpResponse;
import javax.swing.*;

public class SendAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (project == null || editor == null) {
            showNotification(project, MyMessageBundle.message("dialog.title.warning"), MyMessageBundle.message("error.no.editor"), NotificationType.WARNING);
            return;
        }

        Document document = editor.getDocument();
        int lines = document.getLineCount();

        if (lines == 0) {
            showNotification(project, MyMessageBundle.message("dialog.title.warning"), MyMessageBundle.message("error.empty.document"), NotificationType.WARNING);
            return;
        }

        int startOffset = document.getLineStartOffset(0);
        int endOffset = document.getLineEndOffset(lines - 1);
        if (lines > 127) {
            endOffset = document.getLineEndOffset(127);
        }

        final String capturedText = document.getText(new TextRange(startOffset, endOffset));

        ProgressManager.getInstance().run(new Task.Backgroundable(project, MyMessageBundle.message("action.send"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText(MyMessageBundle.message("process.image"));
                    String image = ImageService.createImage(capturedText);
                    String imageHash = ImageService.createImageHash(capturedText);

                    indicator.setText(MyMessageBundle.message("image.upload"));
                    HttpResponse<String> response = PostImageRequest.uploadImage(new ImageDTO(project.getName(), imageHash, image));

                    if (response.statusCode() == 200) {
//                        System.out.println(response.body());
                        ResponseDTO responseDTO = new Gson().fromJson(response.body(), ResponseDTO.class);
                        openAnalysisResult(project, responseDTO);
                    } else {
                        showNotification(project, MyMessageBundle.message("dialog.title.warning"), "Error: " + response.statusCode() + " - " + response.body(), NotificationType.ERROR);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    // showNotification(project, MyMessageBundle.message("dialog.title.warning"), ex.getMessage() != null ? ex.getMessage() : MyMessageBundle.message("error.generic"), NotificationType.ERROR);
                    showNotification(project, MyMessageBundle.message("dialog.title.warning"), ex.getMessage() , NotificationType.ERROR);
                }
            }
        });
    }
    private void showNotification(Project project, String title, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("SourceCodeMinimaps.Notifications")
                .createNotification(title, content, type)
                .notify(project);
    }

    private void openAnalysisResult(Project project, ResponseDTO response) {
        StringBuilder markdownContent = new StringBuilder();
        markdownContent.append("# Source Code Analysis Result\n\n");
        markdownContent.append("## Metadata\n");
        markdownContent.append("- **Hash:** `").append(response.hash()).append("`\n\n");

        markdownContent.append("## Predictions\n");
        if (response.predict() != null && !response.predict().isEmpty()) {
            for (ResponseDTO.TargetPrediction targetPred : response.predict()) {
                markdownContent.append("### ").append(targetPred.target().toUpperCase()).append("\n");
                markdownContent.append("| Class | Confidence |\n");
                markdownContent.append("| :--- | :--- |\n");
                for (ResponseDTO.Prediction pred : targetPred.predictions()) {
                    markdownContent.append("| `").append(pred.className()).append("` | ").append(String.format("%.2f%%", pred.confidence() * 100)).append(" |\n");
                }
                markdownContent.append("\n");
            }
        } else {
            markdownContent.append("No prediction data available.\n");
        }

        markdownContent.append("\n---\n*Generated by SourceCodeMinimaps*");

        String fileName = "AnalysisResult_" + response.hash().substring(0, Math.min(8, response.hash().length())) + ".md";
        LightVirtualFile virtualFile = new LightVirtualFile(fileName, FileTypeManager.getInstance().getFileTypeByExtension("md"), markdownContent.toString());

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
}