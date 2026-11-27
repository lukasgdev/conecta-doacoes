package bo;

import dao.DoadorDAO;
import vo.AcessoVO;
import vo.CadastroDoadorVO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ContaDoadorBO {
    private final DoadorDAO doadores;
    private final ProtecaoSenha protecaoSenha;
    private final String hashSemConta;

    public ContaDoadorBO(DoadorDAO doadores, ProtecaoSenha protecaoSenha) {
        this.doadores = doadores;
        this.protecaoSenha = protecaoSenha;
        this.hashSemConta = protecaoSenha.gerarHash("conta inexistente");
    }

    public void cadastrar(CadastroDoadorVO dados) throws IOException {
        String nome = dados.nomeCompleto().strip();
        String email = normalizarEmail(dados.email());
        String telefone = dados.telefone().strip();
        String endereco = dados.endereco().strip();
        String senha = dados.senha();

        List<String> erros = new ArrayList<>();
        if (nome.isEmpty() || nome.length() > 150) {
            erros.add("Informe o nome completo com até 150 caracteres.");
        }
        if (!emailValido(email)) {
            erros.add("Informe um e-mail válido com até 254 caracteres.");
        }
        String digitos = telefone.replaceAll("[^0-9]", "");
        if (!telefone.matches("[0-9+() .-]{10,30}") || digitos.length() < 10 || digitos.length() > 15) {
            erros.add("Informe um telefone com DDD e de 10 a 15 dígitos.");
        }
        if (endereco.isEmpty() || endereco.length() > 300) {
            erros.add("Informe o endereço com até 300 caracteres.");
        }
        if (senha.isBlank() || senha.length() < 8 || senha.length() > 128) {
            erros.add("Escolha uma senha de 8 a 128 caracteres.");
        }
        if (!erros.isEmpty()) {
            throw new ExcecaoCadastroInvalido(erros);
        }

        Doador doador = new Doador(nome, email, telefone, endereco, protecaoSenha.gerarHash(senha));
        if (!doadores.inserir(doador)) {
            throw new ExcecaoEmailJaCadastrado();
        }
    }

    public Optional<Doador> autenticar(AcessoVO dados) throws IOException {
        String email = normalizarEmail(dados.email());
        if (!emailValido(email) || dados.senha().isEmpty() || dados.senha().length() > 128) {
            return Optional.empty();
        }

        Optional<Doador> doador = doadores.buscarPorEmail(email);
        String hash = doador.map(Doador::hashSenha).orElse(hashSemConta);
        return protecaoSenha.conferir(dados.senha(), hash) ? doador : Optional.empty();
    }

    private String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private boolean emailValido(String email) {
        return email.length() <= 254 && email.matches("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+");
    }
}
