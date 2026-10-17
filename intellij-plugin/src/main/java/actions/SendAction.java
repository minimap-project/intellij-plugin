package actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Ação principal de envio / análise do plugin, mantida para compatibilidade retroativa.
 * Solicita explicitamente ao usuário qual estratégia deseja utilizar.
 */
public class SendAction extends ChooseStrategyAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        super.actionPerformed(e);
    }
}