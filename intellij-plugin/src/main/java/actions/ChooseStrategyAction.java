package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class ChooseStrategyAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        String[] options = {
                "Caminho 0: Analisar Arquivo Atual",
                "Caminho A: Análise Individual de Diretório",
                "Caminho B: Análise por Minimap Médio do Projeto"
        };

        int choice = Messages.showChooseDialog(
                project,
                "Selecione a estratégia de análise MinimapSignatures:",
                "MinimapSignatures — Selecionar Modo de Análise",
                Messages.getQuestionIcon(),
                options,
                options[0]
        );

        switch (choice) {
            case 0 -> new AnalyzeCurrentFileAction().actionPerformed(e);
            case 1 -> new AnalyzeDirectoryAction().actionPerformed(e);
            case 2 -> new AnalyzeProjectAverageAction().actionPerformed(e);
            default -> {}
        }
    }
}
