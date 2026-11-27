package dao;

import vo.ItemEstoqueVO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class EstoqueDoadorDAO {
    private final AdaptadorSqlite sqlite;

    public EstoqueDoadorDAO(AdaptadorSqlite sqlite) {
        this.sqlite = sqlite;
    }

    public boolean adicionar(String emailDoador, ItemEstoqueVO item) throws IOException {
        String sql = """
                INSERT INTO estoque_doador (doador_id, material, quantidade)
                SELECT id, %s, %s FROM doadores WHERE email = %s
                ON CONFLICT(doador_id, material) DO UPDATE
                SET quantidade = estoque_doador.quantidade + excluded.quantidade
                WHERE estoque_doador.quantidade + excluded.quantidade <= 2147483647;
                SELECT changes();
                """.formatted(AdaptadorSqlite.literalTexto(item.material()), item.quantidade(),
                AdaptadorSqlite.literalTexto(emailDoador));
        return "1".equals(sqlite.executar(sql));
    }

    public List<ItemEstoqueVO> listar(String emailDoador) throws IOException {
        String sql = """
                SELECT hex(estoque.material), estoque.quantidade
                FROM estoque_doador estoque
                JOIN doadores doador ON doador.id = estoque.doador_id
                WHERE doador.email = %s
                ORDER BY estoque.material;
                """.formatted(AdaptadorSqlite.literalTexto(emailDoador));
        List<ItemEstoqueVO> itens = new ArrayList<>();
        for (String linha : sqlite.executar(sql).lines().toList()) {
            String[] campos = linha.split("\\|", 2);
            itens.add(new ItemEstoqueVO(AdaptadorSqlite.decodificarTexto(campos[0]), Integer.parseInt(campos[1])));
        }
        return List.copyOf(itens);
    }
}
