package actions;

import com.google.gson.Gson;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import dto.ImageDTO;
import dto.ResponseDTO;
import messages.MyMessageBundle;
import org.jetbrains.annotations.NotNull;
import requests.PostImageRequest;
import services.ImageService;

import java.net.http.HttpResponse;

public class AnalyzeCurrentFileAction extends BaseAnalysisAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || editor == null) {
            showNotification(project, MyMessageBundle.message("dialog.title.warning"),
                    MyMessageBundle.message("error.no.editor"), NotificationType.WARNING);
            return;
        }

        Document document = editor.getDocument();
        int lines = document.getLineCount();

        if (lines == 0) {
            showNotification(project, MyMessageBundle.message("dialog.title.warning"),
                    MyMessageBundle.message("error.empty.document"), NotificationType.WARNING);
            return;
        }

        int startOffset = document.getLineStartOffset(0);
        int endOffset = document.getLineEndOffset(lines - 1);
        if (lines > 127) {
            endOffset = document.getLineEndOffset(127);
        }

        final String capturedText = document.getText(new TextRange(startOffset, endOffset));
        final String relativePath = virtualFile != null ? virtualFile.getPath() : "CurrentEditorFile";

        ProgressManager.getInstance().run(new Task.Backgroundable(project, MyMessageBundle.message("action.send"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText(MyMessageBundle.message("process.image"));
                    String image = ImageService.createImage(capturedText);
                    String imageHash = ImageService.md5Hex(capturedText);

                    indicator.setText(MyMessageBundle.message("image.upload"));
                    HttpResponse<String> response = PostImageRequest.uploadImage(
                            new ImageDTO(project.getName(), imageHash, image)
                    );

                    if (response.statusCode() == 200) {
                        ResponseDTO responseDTO = new Gson().fromJson(response.body(), ResponseDTO.class);
                        String markdown = buildSingleFileMarkdown(project.getName(), relativePath, responseDTO);
                        String fileName = "AnalysisResult_" + responseDTO.hash().substring(0, Math.min(8, responseDTO.hash().length())) + ".md";
                        openMarkdownReport(project, fileName, markdown);
                    } else {
                        showNotification(project, MyMessageBundle.message("dialog.title.warning"),
                                "Error: " + response.statusCode() + " - " + response.body(), NotificationType.ERROR);
                    }
                } catch (Exception ex) {
                    showNotification(project, MyMessageBundle.message("dialog.title.warning"),
                            ex.getMessage() != null ? ex.getMessage() : MyMessageBundle.message("error.generic"),
                            NotificationType.ERROR);
                }
            }
        });
    }
}
