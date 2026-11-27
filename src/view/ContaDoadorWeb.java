package view;

import bo.ExcecaoCadastroInvalido;
import bo.ContaDoadorBO;
import bo.Doador;
import bo.EstoqueDoadorBO;
import bo.ExcecaoEstoqueInvalido;
import bo.ExcecaoEmailJaCadastrado;
import com.sun.net.httpserver.HttpExchange;
import vo.AcessoVO;
import vo.CadastroDoadorVO;
import vo.ItemEstoqueVO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class ContaDoadorWeb {
    private static final int DURACAO_SESSAO = 1_800;
    private static final Pattern MARCADOR = Pattern.compile("\\{\\{([a-z-]+)}}");
    private final ContaDoadorBO contas;
    private final EstoqueDoadorBO estoque;
    private final Map<String, String> paginas;
    private final Map<String, Sessao> sessoes = new HashMap<>();

    ContaDoadorWeb(ContaDoadorBO contas, EstoqueDoadorBO estoque) throws IOException {
        this.contas = contas;
        this.estoque = estoque;
        this.paginas = Map.of(
                "/entrar", lerArquivo("entrar.html"),
                "/cadastro", lerArquivo("cadastro.html"),
                "/area-doador", lerArquivo("area-doador.html"),
                "/conta.css", lerArquivo("conta.css"));
    }

    void atender(HttpExchange requisicao) throws IOException {
        try (requisicao) {
            String caminho = requisicao.getRequestURI().getPath();
            String metodo = requisicao.getRequestMethod();

            if (!paginas.containsKey(caminho) && !"/sair".equals(caminho)) {
                requisicao.sendResponseHeaders(404, -1);
                return;
            }
            boolean aceitaPost = List.of("/cadastro", "/entrar", "/area-doador", "/sair").contains(caminho);
            boolean aceitaGet = !"/sair".equals(caminho);
            if ("GET".equals(metodo) && aceitaGet) {
                abrirPagina(requisicao, caminho);
            } else if ("POST".equals(metodo) && aceitaPost) {
                enviarFormulario(requisicao, caminho);
            } else {
                requisicao.getResponseHeaders().set("Allow", aceitaGet ? (aceitaPost ? "GET, POST" : "GET") : "POST");
                requisicao.sendResponseHeaders(405, -1);
            }
        }
    }

    private void abrirPagina(HttpExchange requisicao, String caminho) throws IOException {
        if ("/conta.css".equals(caminho)) {
            ServidorLocal.responder(requisicao, 200, "text/css", paginas.get(caminho));
            return;
        }
        Sessao sessao = obterSessao(requisicao);
        if ("/area-doador".equals(caminho)) {
            if (sessao == null) {
                ServidorLocal.redirecionar(requisicao, "/entrar");
                return;
            }
            String aviso = "estoque=atualizado".equals(requisicao.getRequestURI().getRawQuery())
                    ? "Material adicionado ao estoque." : "";
            exibirEstoque(requisicao, sessao, 200, "", aviso);
            return;
        }
        if (sessao != null) {
            ServidorLocal.redirecionar(requisicao, "/area-doador");
            return;
        }
        String aviso = "";
        if ("/entrar".equals(caminho) && "cadastro=concluido".equals(requisicao.getRequestURI().getRawQuery())) {
            aviso = "Conta criada. Entre com seu e-mail e sua senha.";
        }
        exibir(requisicao, caminho, 200, Map.of("aviso", aviso));
    }

    private void enviarFormulario(HttpExchange requisicao, String caminho) throws IOException {
        Map<String, String> campos;
        try {
            campos = ServidorLocal.lerCampos(requisicao);
        } catch (IllegalArgumentException excecao) {
            erroFormulario(requisicao, 400, "Não foi possível ler o formulário.");
            return;
        }

        String token = lerCookie(requisicao, "formulario");
        if (!token.matches("[0-9a-f-]{36}") || !token.equals(campos.get("token"))) {
            erroFormulario(requisicao, 403, "Reabra a página e envie o formulário novamente.");
            return;
        }

        if ("/sair".equals(caminho)) {
            sessoes.remove(lerCookie(requisicao, "sessao"));
            definirCookie(requisicao, "sessao", "", 0);
            ServidorLocal.redirecionar(requisicao, "/entrar");
            return;
        }
        Sessao sessao = obterSessao(requisicao);
        if ("/area-doador".equals(caminho)) {
            if (sessao == null) {
                ServidorLocal.redirecionar(requisicao, "/entrar");
            } else {
                adicionarMaterial(requisicao, sessao, campos);
            }
            return;
        }
        if (sessao != null) {
            ServidorLocal.redirecionar(requisicao, "/area-doador");
            return;
        }

        try {
            if ("/cadastro".equals(caminho)) {
                cadastrar(requisicao, campos);
            } else {
                entrar(requisicao, campos);
            }
        } catch (IOException excecao) {
            erroFormulario(requisicao, 503, "O banco está indisponível. Tente novamente.");
        }
    }

    private void adicionarMaterial(HttpExchange requisicao, Sessao sessao, Map<String, String> campos)
            throws IOException {
        try {
            int quantidade = Integer.parseInt(campos.getOrDefault("quantidade", ""));
            estoque.adicionar(sessao.email(), new ItemEstoqueVO(campos.getOrDefault("material", ""), quantidade));
        } catch (NumberFormatException excecao) {
            exibirEstoque(requisicao, sessao, 400, "Informe uma quantidade inteira válida.", "");
            return;
        } catch (ExcecaoEstoqueInvalido excecao) {
            exibirEstoque(requisicao, sessao, 400, excecao.getMessage(), "");
            return;
        } catch (IOException excecao) {
            erroFormulario(requisicao, 503, "O banco está indisponível. Tente novamente.");
            return;
        }
        ServidorLocal.redirecionar(requisicao, "/area-doador?estoque=atualizado");
    }

    private void exibirEstoque(HttpExchange requisicao, Sessao sessao, int codigo, String erro, String aviso)
            throws IOException {
        List<ItemEstoqueVO> itens;
        try {
            itens = estoque.listar(sessao.email());
        } catch (IOException excecao) {
            erroFormulario(requisicao, 503, "Não foi possível consultar o estoque. Tente novamente.");
            return;
        }
        String linhas = itens.stream().map(item -> "<li>" + ServidorLocal.escaparHtml(item.material())
                + " <strong>" + item.quantidade() + " un.</strong></li>").collect(Collectors.joining());
        exibir(requisicao, "/area-doador", codigo, Map.of(
                "nome", ServidorLocal.escaparHtml(sessao.nomeCompleto()),
                "erro", ServidorLocal.escaparHtml(erro), "aviso", aviso,
                "materiais", ServidorLocal.opcoesMateriais(), "itens", linhas,
                "estoque-vazio", itens.isEmpty() ? "" : "hidden"));
    }

    private void cadastrar(HttpExchange requisicao, Map<String, String> campos) throws IOException {
        CadastroDoadorVO cadastro = new CadastroDoadorVO(campos.getOrDefault("nome", ""),
                campos.getOrDefault("email", ""), campos.getOrDefault("senha", ""),
                campos.getOrDefault("telefone", ""), campos.getOrDefault("endereco", ""));
        try {
            contas.cadastrar(cadastro);
        } catch (ExcecaoCadastroInvalido excecao) {
            exibirCadastroComErros(requisicao, cadastro, 400, excecao.erros());
            return;
        } catch (ExcecaoEmailJaCadastrado excecao) {
            exibirCadastroComErros(requisicao, cadastro, 409, List.of(excecao.getMessage()));
            return;
        }
        ServidorLocal.redirecionar(requisicao, "/entrar?cadastro=concluido");
    }

    private void exibirCadastroComErros(HttpExchange requisicao, CadastroDoadorVO cadastro,
                                       int codigo, List<String> erros) throws IOException {
        String lista = erros.stream().map(ServidorLocal::escaparHtml)
                .map(erro -> "<li>" + erro + "</li>").collect(Collectors.joining());
        exibir(requisicao, "/cadastro", codigo, Map.of(
                "erros", lista,
                "nome", ServidorLocal.escaparHtml(cadastro.nomeCompleto()),
                "email", ServidorLocal.escaparHtml(cadastro.email()),
                "telefone", ServidorLocal.escaparHtml(cadastro.telefone()),
                "endereco", ServidorLocal.escaparHtml(cadastro.endereco())));
    }

    private void entrar(HttpExchange requisicao, Map<String, String> campos) throws IOException {
        AcessoVO acesso = new AcessoVO(campos.getOrDefault("email", ""), campos.getOrDefault("senha", ""));
        Optional<Doador> doador = contas.autenticar(acesso);
        if (doador.isEmpty()) {
            exibir(requisicao, "/entrar", 401, Map.of(
                    "erro", "E-mail ou senha incorretos.",
                    "email", ServidorLocal.escaparHtml(acesso.email())));
            return;
        }
        Instant agora = Instant.now();
        sessoes.values().removeIf(sessao -> !sessao.expiraEm().isAfter(agora));
        String identificador = UUID.randomUUID().toString();
        sessoes.put(identificador, new Sessao(doador.get().email(), doador.get().nomeCompleto(),
                agora.plusSeconds(DURACAO_SESSAO)));
        definirCookie(requisicao, "sessao", identificador, DURACAO_SESSAO);
        ServidorLocal.redirecionar(requisicao, "/area-doador");
    }

    private Sessao obterSessao(HttpExchange requisicao) {
        String identificador = lerCookie(requisicao, "sessao");
        Sessao sessao = sessoes.get(identificador);
        if (sessao != null && !sessao.expiraEm().isAfter(Instant.now())) {
            sessoes.remove(identificador);
            return null;
        }
        return sessao;
    }

    private void exibir(HttpExchange requisicao, String caminho, int codigo,
                        Map<String, String> valores) throws IOException {
        Map<String, String> campos = new HashMap<>(valores);
        String token = lerCookie(requisicao, "formulario");
        if (!token.matches("[0-9a-f-]{36}")) {
            token = UUID.randomUUID().toString();
            definirCookie(requisicao, "formulario", token, DURACAO_SESSAO);
        }
        campos.put("token", token);
        for (String campo : List.of("erro", "erros", "aviso")) {
            campos.put(campo + "-visivel", valores.getOrDefault(campo, "").isEmpty() ? "hidden" : "");
        }
        String html = MARCADOR.matcher(paginas.get(caminho))
                .replaceAll(marcador -> Matcher.quoteReplacement(campos.getOrDefault(marcador.group(1), "")));
        ServidorLocal.responder(requisicao, codigo, "text/html", html);
    }

    private String lerCookie(HttpExchange requisicao, String nome) {
        for (String linha : requisicao.getRequestHeaders().getOrDefault("Cookie", List.of())) {
            for (String cookie : linha.split(";")) {
                String par = cookie.strip();
                if (par.startsWith(nome + "=")) {
                    return par.substring(nome.length() + 1);
                }
            }
        }
        return "";
    }

    private void definirCookie(HttpExchange requisicao, String nome, String valor, int duracao) {
        requisicao.getResponseHeaders().add("Set-Cookie", nome + "=" + valor
                + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + duracao);
    }

    private void erroFormulario(HttpExchange requisicao, int codigo, String mensagem) throws IOException {
        ServidorLocal.responder(requisicao, codigo, "text/html", """
                <!doctype html>
                <html lang="pt-BR">
                <head><meta charset="utf-8"><title>Não foi possível continuar</title></head>
                <body><main><h1>Não foi possível continuar</h1><p>%s</p>
                <a href="/entrar">Voltar ao acesso</a></main></body>
                </html>
                """.formatted(mensagem));
    }

    private String lerArquivo(String nome) throws IOException {
        return Files.readString(Path.of("src", "view", nome), StandardCharsets.UTF_8);
    }

    private record Sessao(String email, String nomeCompleto, Instant expiraEm) {
    }
}
