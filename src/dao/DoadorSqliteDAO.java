package dao;

import bo.Doador;
import java.io.IOException;
import java.util.Optional;

public final class DoadorSqliteDAO implements DoadorDAO {
    private final AdaptadorSqlite sqlite;

    public DoadorSqliteDAO(AdaptadorSqlite sqlite) {
        this.sqlite = sqlite;
    }

    @Override
    public boolean inserir(Doador doador) throws IOException {
        String sql = """
                INSERT INTO doadores (nome_completo, email, telefone, endereco, hash_senha)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT(email) DO NOTHING;
                SELECT changes();
                """.formatted(AdaptadorSqlite.literalTexto(doador.nomeCompleto()),
                AdaptadorSqlite.literalTexto(doador.email()), AdaptadorSqlite.literalTexto(doador.telefone()),
                AdaptadorSqlite.literalTexto(doador.endereco()), AdaptadorSqlite.literalTexto(doador.hashSenha()));
        return "1".equals(sqlite.executar(sql));
    }

    @Override
    public Optional<Doador> buscarPorEmail(String email) throws IOException {
        String sql = """
                SELECT hex(nome_completo), hex(email), hex(telefone), hex(endereco), hex(hash_senha)
                FROM doadores WHERE email = %s;
                """.formatted(AdaptadorSqlite.literalTexto(email));
        String linha = sqlite.executar(sql);
        if (linha.isEmpty()) {
            return Optional.empty();
        }
        String[] campos = linha.split("\\|", -1);
        return Optional.of(new Doador(AdaptadorSqlite.decodificarTexto(campos[0]), AdaptadorSqlite.decodificarTexto(campos[1]), AdaptadorSqlite.decodificarTexto(campos[2]),
                AdaptadorSqlite.decodificarTexto(campos[3]), AdaptadorSqlite.decodificarTexto(campos[4])));
    }
}
