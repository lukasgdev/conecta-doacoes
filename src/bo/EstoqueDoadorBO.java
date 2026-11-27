package bo;

import dao.EstoqueDoadorDAO;
import vo.ItemEstoqueVO;

import java.io.IOException;
import java.util.List;

public final class EstoqueDoadorBO {
    public static final List<String> MATERIAIS = List.of("Caderno", "Lápis", "Borracha", "Régua", "Caneta");
    private final EstoqueDoadorDAO estoque;

    public EstoqueDoadorBO(EstoqueDoadorDAO estoque) {
        this.estoque = estoque;
    }

    public List<ItemEstoqueVO> listar(String emailDoador) throws IOException {
        return estoque.listar(emailDoador);
    }

    public void adicionar(String emailDoador, ItemEstoqueVO item) throws IOException {
        if (!MATERIAIS.contains(item.material())) {
            throw new ExcecaoEstoqueInvalido("Selecione um material da lista.");
        }
        if (item.quantidade() <= 0) {
            throw new ExcecaoEstoqueInvalido("Informe uma quantidade maior que zero.");
        }
        if (!estoque.adicionar(emailDoador, item)) {
            throw new ExcecaoEstoqueInvalido("Não foi possível adicionar essa quantidade ao estoque da conta.");
        }
    }
}
