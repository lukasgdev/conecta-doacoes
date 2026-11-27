package bo;

public final class ExcecaoEstoqueInvalido extends IllegalArgumentException {
    public ExcecaoEstoqueInvalido(String mensagem) {
        super(mensagem);
    }
}
