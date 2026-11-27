package dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Locale;

public final class AdaptadorSqlite {
    private static final HexFormat HEXADECIMAL = HexFormat.of();
    private final Path banco;

    public AdaptadorSqlite(Path banco) {
        this.banco = banco.toAbsolutePath();
    }

    public String executar(String sql) throws IOException {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
        Process processo = new ProcessBuilder("sqlite3", "-batch", "-bail", "-list", "-noheader",
                "-init", windows ? "NUL" : "/dev/null", banco.toString())
                .redirectErrorStream(true)
                .start();

        try {
            try (var entrada = processo.getOutputStream()) {
                entrada.write((".timeout 3000\nPRAGMA foreign_keys = ON;\n" + sql + "\n").getBytes(StandardCharsets.UTF_8));
            }
            String saida;
            try (var resposta = processo.getInputStream()) {
                saida = new String(resposta.readAllBytes(), StandardCharsets.UTF_8).strip();
            }
            if (processo.waitFor() != 0) {
                throw new IOException("O SQLite não conseguiu executar a operação.");
            }
            return saida;
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new IOException("A operação do SQLite foi interrompida.", excecao);
        } finally {
            processo.destroy();
        }
    }
    static String literalTexto(String valor) {
        // O literal contém apenas dígitos hexadecimais, inclusive quando o texto tem aspas.
        return "CAST(X'" + HEXADECIMAL.formatHex(valor.getBytes(StandardCharsets.UTF_8)) + "' AS TEXT)";
    }

    static String decodificarTexto(String valor) {
        return new String(HEXADECIMAL.parseHex(valor), StandardCharsets.UTF_8);
    }
}
