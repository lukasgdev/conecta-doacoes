package bo;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class ProtecaoSenhaPbkdf2 implements ProtecaoSenha {
    private static final int ITERACOES = 600_000;
    private static final HexFormat HEXADECIMAL = HexFormat.of();
    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    public String gerarHash(String senha) {
        byte[] sal = new byte[16];
        aleatorio.nextBytes(sal);
        return HEXADECIMAL.formatHex(sal) + ":" + HEXADECIMAL.formatHex(derivar(senha, sal));
    }

    @Override
    public boolean conferir(String senha, String hash) {
        String[] partes = hash.split(":", 2);
        byte[] sal = HEXADECIMAL.parseHex(partes[0]);
        byte[] esperado = HEXADECIMAL.parseHex(partes[1]);
        return MessageDigest.isEqual(esperado, derivar(senha, sal));
    }

    private byte[] derivar(String senha, byte[] sal) {
        PBEKeySpec parametros = new PBEKeySpec(senha.toCharArray(), sal, ITERACOES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(parametros).getEncoded();
        } catch (GeneralSecurityException excecao) {
            throw new IllegalStateException("Não foi possível proteger a senha.", excecao);
        } finally {
            parametros.clearPassword();
        }
    }
}
