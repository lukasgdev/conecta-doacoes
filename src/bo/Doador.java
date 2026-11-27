package bo;

public final class Doador extends Usuario {
    private final String endereco;
    private final String hashSenha;

    public Doador(String nomeCompleto, String email, String telefone, String endereco, String hashSenha) {
        super(nomeCompleto, email, telefone);
        this.endereco = endereco;
        this.hashSenha = hashSenha;
    }

    public String endereco() {
        return endereco;
    }

    public String hashSenha() {
        return hashSenha;
    }
}
