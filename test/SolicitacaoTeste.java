import bo.ItemSolicitacao;
import bo.Solicitacao;
import bo.SolicitacaoVaziaException;
import java.util.List;
import java.util.Objects;

public final class SolicitacaoTeste {
    public static void main(String[] args) {
        confirmaSolicitacaoComDoisMateriais();
        rejeitaItemInvalido();
        rejeitaConfirmacaoSemItens();
        impedeInclusaoAposConfirmacao();
        protegeListaDeItens();
        System.out.println("5 testes de solicitacao passaram.");
    }

    private static void confirmaSolicitacaoComDoisMateriais() {
        Solicitacao solicitacao = new Solicitacao();
        solicitacao.adicionarItem("Caderno", 2);
        solicitacao.adicionarItem("Lápis", 4);
        solicitacao.confirmar();

        List<ItemSolicitacao> itens = solicitacao.getItens();
        verificarIgual(2, itens.size());
        verificarIgual("Caderno", itens.get(0).getMaterial());
        verificarIgual(2, itens.get(0).getQuantidade());
        verificarIgual("Lápis", itens.get(1).getMaterial());
        verificarIgual(4, itens.get(1).getQuantidade());
        verificarIgual(true, solicitacao.isConfirmada());
    }

    private static void rejeitaItemInvalido() {
        Solicitacao solicitacao = new Solicitacao();
        verificarExcecao(IllegalArgumentException.class, () -> solicitacao.adicionarItem("Caderno", 0));
        verificarExcecao(IllegalArgumentException.class, () -> solicitacao.adicionarItem("Caderno", -1));
        verificarExcecao(IllegalArgumentException.class, () -> solicitacao.adicionarItem(" ", 1));
        verificarIgual(0, solicitacao.getItens().size());
    }

    private static void rejeitaConfirmacaoSemItens() {
        Solicitacao solicitacao = new Solicitacao();
        verificarExcecao(SolicitacaoVaziaException.class, solicitacao::confirmar);
        verificarIgual(false, solicitacao.isConfirmada());

        solicitacao.adicionarItem("Borracha", 1);
        solicitacao.confirmar();
        verificarIgual(true, solicitacao.isConfirmada());
    }

    private static void impedeInclusaoAposConfirmacao() {
        Solicitacao solicitacao = new Solicitacao();
        solicitacao.adicionarItem("Caderno", 2);
        solicitacao.confirmar();

        verificarExcecao(IllegalStateException.class, () -> solicitacao.adicionarItem("Régua", 1));
        verificarIgual(1, solicitacao.getItens().size());
    }

    private static void protegeListaDeItens() {
        Solicitacao solicitacao = new Solicitacao();
        solicitacao.adicionarItem("Caderno", 2);

        verificarExcecao(UnsupportedOperationException.class, () -> solicitacao.getItens().clear());
        verificarIgual(1, solicitacao.getItens().size());
    }

    private static void verificarIgual(Object esperado, Object atual) {
        if (!Objects.equals(esperado, atual)) {
            throw new AssertionError("Esperado: " + esperado + ". Obtido: " + atual + ".");
        }
    }

    private static void verificarExcecao(Class<? extends RuntimeException> tipo, Runnable operacao) {
        try {
            operacao.run();
        } catch (RuntimeException excecao) {
            if (tipo.isInstance(excecao)) {
                return;
            }
            throw excecao;
        }
        throw new AssertionError("Esperava a exceção " + tipo.getSimpleName() + ".");
    }
}
