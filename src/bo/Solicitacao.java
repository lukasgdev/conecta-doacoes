package bo;

import java.util.ArrayList;
import java.util.List;

public final class Solicitacao {
    private final List<ItemSolicitacao> itens;
    private boolean confirmada;

    public Solicitacao() {
        itens = new ArrayList<>();
    }

    public void adicionarItem(String material, int quantidade) {
        if (confirmada) {
            throw new IllegalStateException("A solicitação já foi confirmada.");
        }
        itens.add(new ItemSolicitacao(material, quantidade));
    }

    public void confirmar() {
        if (itens.isEmpty()) {
            throw new ExcecaoSolicitacaoVazia();
        }
        confirmada = true;
    }

    public boolean estaConfirmada() {
        return confirmada;
    }

    public List<ItemSolicitacao> itens() {
        return List.copyOf(itens);
    }
}
