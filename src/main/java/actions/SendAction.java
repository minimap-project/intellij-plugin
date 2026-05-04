package actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import dto.ImageDTO;
import messages.MyMessageBundle;
import org.jetbrains.annotations.NotNull;
import dto.ResponseDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import requests.PostImageRequest;
import services.ImageService;
import java.net.http.HttpResponse;

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
                        ResponseDTO responseDTO = parseResponse(response.body());
                        String content = String.format(
                                "Type: %s\nProject: %s\nAuthor: %s\nQuality: %s",
                                responseDTO.predict().get(0),
                                responseDTO.predict().get(1),
                                responseDTO.predict().get(2),
                                responseDTO.predict().get(3)
                        );
                        showNotification(project, MyMessageBundle.message("dialog.title.success"), content, NotificationType.INFORMATION);
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

    private ResponseDTO parseResponse(String json) {
        String hash = "";
        Pattern hashPattern = Pattern.compile("\"hash\"\\s*:\\s*\"([^\"]*)\"");
        Matcher hashMatcher = hashPattern.matcher(json);
        if (hashMatcher.find()) {
            hash = hashMatcher.group(1);
        }

        List<String> predict = new ArrayList<>();
        Pattern predictPattern = Pattern.compile("\"predict\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher predictMatcher = predictPattern.matcher(json);
        if (predictMatcher.find()) {
            String arrayContent = predictMatcher.group(1);
            Pattern itemPattern = Pattern.compile("\"([^\"]*)\"");
            Matcher itemMatcher = itemPattern.matcher(arrayContent);
            while (itemMatcher.find()) {
                predict.add(itemMatcher.group(1));
            }
        }

        return new ResponseDTO(hash, predict);
    }
}