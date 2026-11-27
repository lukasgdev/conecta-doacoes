package bo;

public interface ProtecaoSenha {
    String gerarHash(String senha);

    boolean conferir(String senha, String hash);
}
