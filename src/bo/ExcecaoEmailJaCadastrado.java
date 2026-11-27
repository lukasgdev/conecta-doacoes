package bo;

public final class ExcecaoEmailJaCadastrado extends IllegalArgumentException {
    public ExcecaoEmailJaCadastrado() {
        super("Já existe uma conta com esse e-mail.");
    }
}
