package bo;

import java.util.List;

public final class ExcecaoCadastroInvalido extends IllegalArgumentException {
    private final List<String> erros;

    public ExcecaoCadastroInvalido(List<String> erros) {
        super(String.join(" ", erros));
        this.erros = List.copyOf(erros);
    }

    public List<String> erros() {
        return erros;
    }
}
