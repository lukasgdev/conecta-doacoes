package bo;

public abstract class Usuario {
    private final String nomeCompleto;
    private final String email;
    private final String telefone;

    protected Usuario(String nomeCompleto, String email, String telefone) {
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.telefone = telefone;
    }

    public String nomeCompleto() {
        return nomeCompleto;
    }

    public String email() {
        return email;
    }

    public String telefone() {
        return telefone;
    }
}
