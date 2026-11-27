package bo;

public final class ExcecaoSolicitacaoVazia extends IllegalStateException {
    public ExcecaoSolicitacaoVazia() {
        super("Adicione pelo menos um material antes de confirmar a solicitação.");
    }
}
