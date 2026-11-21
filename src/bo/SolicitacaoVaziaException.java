package bo;

public final class SolicitacaoVaziaException extends IllegalStateException {
    public SolicitacaoVaziaException() {
        super("Adicione pelo menos um material antes de confirmar a solicitação.");
    }
}
