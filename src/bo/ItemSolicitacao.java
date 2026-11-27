package bo;

public final class ItemSolicitacao {
    private final String material;
    private final int quantidade;

    public ItemSolicitacao(String material, int quantidade) {
        if (material == null || material.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do material.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        this.material = material;
        this.quantidade = quantidade;
    }

    public String material() {
        return material;
    }

    public int quantidade() {
        return quantidade;
    }
}
