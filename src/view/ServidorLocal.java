package view;

import bo.ContaDoadorBO;
import bo.EstoqueDoadorBO;
import bo.ItemSolicitacao;
import bo.ProtecaoSenhaPbkdf2;
import dao.AdaptadorSqlite;
import dao.DoadorSqliteDAO;
import dao.EstoqueDoadorDAO;
import bo.Solicitacao;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ServidorLocal {
    private final String modeloHtml;
    private Solicitacao solicitacao = new Solicitacao();

    private ServidorLocal(String modeloHtml) {
        this.modeloHtml = modeloHtml;
    }

    public static void main(String[] argumentos) throws IOException {
        String modelo = Files.readString(Path.of("src", "view", "solicitacao.html"));
        ServidorLocal aplicacao = new ServidorLocal(modelo);
        AdaptadorSqlite sqlite = new AdaptadorSqlite(prepararBanco());
        try {
            sqlite.executar(Files.readString(Path.of("banco", "esquema.sql"), StandardCharsets.UTF_8));
        } catch (IOException excecao) {
            throw new IOException("Confira se o sqlite3 está no PATH e se o banco local está acessível.", excecao);
        }
        ContaDoadorWeb contas = new ContaDoadorWeb(
                new ContaDoadorBO(new DoadorSqliteDAO(sqlite), new ProtecaoSenhaPbkdf2()),
                new EstoqueDoadorBO(new EstoqueDoadorDAO(sqlite)));
        int porta = Integer.getInteger("conectadoacoes.porta", 8080);
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        for (String rota : List.of("/cadastro", "/entrar", "/area-doador", "/sair", "/conta.css")) {
            servidor.createContext(rota, requisicao -> {
                if (enderecoLocal(requisicao)) {
                    contas.atender(requisicao);
                } else {
                    try (requisicao) {
                        requisicao.sendResponseHeaders(403, -1);
                    }
                }
            });
        }
        servidor.createContext("/", aplicacao::atender);
        servidor.start();
        System.out.println("Abra http://localhost:" + servidor.getAddress().getPort()
                + " no navegador. Ctrl+C encerra o servidor.");
    }

    private static Path prepararBanco() throws IOException {
        Path pasta = Files.createDirectories(Path.of("dados-locais"));
        Path banco = pasta.resolve("conecta-doacoes.sqlite");
        if (Files.notExists(banco)) {
            Path copia = Files.createTempFile(pasta, "banco-inicial-", ".sqlite");
            try {
                Files.copy(Path.of("banco", "banco-inicial.sqlite"), copia,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Files.move(copia, banco);
            } catch (FileAlreadyExistsException excecao) {
                // Outra instância já colocou o banco inicial no lugar.
            } finally {
                Files.deleteIfExists(copia);
            }
        }
        return banco;
    }

    private static boolean enderecoLocal(HttpExchange requisicao) {
        String endereco = requisicao.getRequestHeaders().getFirst("Host");
        int porta = requisicao.getLocalAddress().getPort();
        return ("localhost:" + porta).equalsIgnoreCase(endereco)
                || ("127.0.0.1:" + porta).equals(endereco);
    }

    private void atender(HttpExchange requisicao) throws IOException {
        try (requisicao) {
            if (!"/".equals(requisicao.getRequestURI().getPath())) {
                requisicao.sendResponseHeaders(404, -1);
                return;
            }
            switch (requisicao.getRequestMethod()) {
                case "GET" -> exibir(requisicao, 200, "");
                case "POST" -> receberFormulario(requisicao);
                default -> {
                    requisicao.getResponseHeaders().set("Allow", "GET, POST");
                    requisicao.sendResponseHeaders(405, -1);
                }
            }
        }
    }

    private void receberFormulario(HttpExchange requisicao) throws IOException {
        Map<String, String> campos;
        try {
            campos = lerCampos(requisicao);
        } catch (IllegalArgumentException excecao) {
            exibir(requisicao, 400, "Não foi possível ler o formulário.");
            return;
        }

        try {
            switch (campos.getOrDefault("acao", "")) {
                case "adicionar" -> {
                    int quantidade = Integer.parseInt(campos.getOrDefault("quantidade", ""));
                    solicitacao.adicionarItem(campos.get("material"), quantidade);
                }
                case "confirmar" -> solicitacao.confirmar();
                case "reiniciar" -> solicitacao = new Solicitacao();
                default -> throw new IllegalArgumentException("Ação inválida.");
            }
        } catch (NumberFormatException excecao) {
            exibir(requisicao, 400, "Informe uma quantidade inteira válida.");
            return;
        } catch (IllegalArgumentException | IllegalStateException excecao) {
            exibir(requisicao, 400, excecao.getMessage());
            return;
        }

        redirecionar(requisicao, "/");
    }

    static Map<String, String> lerCampos(HttpExchange requisicao) throws IOException {
        byte[] dados = requisicao.getRequestBody().readNBytes(8193);
        if (dados.length > 8192) {
            throw new IllegalArgumentException("Formulário muito grande.");
        }

        Map<String, String> campos = new HashMap<>();
        String formulario = new String(dados, StandardCharsets.UTF_8);
        for (String campo : formulario.split("&")) {
            String[] par = campo.split("=", 2);
            if (par.length == 2) {
                campos.put(URLDecoder.decode(par[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(par[1], StandardCharsets.UTF_8));
            }
        }
        return campos;
    }

    private void exibir(HttpExchange requisicao, int codigo, String erro) throws IOException {
        StringBuilder itens = new StringBuilder();
        for (ItemSolicitacao item : solicitacao.itens()) {
            itens.append("<li><span>").append(escaparHtml(item.material()))
                    .append("</span><strong>").append(item.quantidade())
                    .append(" un.</strong></li>");
        }

        boolean confirmada = solicitacao.estaConfirmada();
        String html = modeloHtml
                .replace("{{edicao}}", confirmada ? "hidden" : "")
                .replace("{{confirmada}}", confirmada ? "" : "hidden")
                .replace("{{erro-visivel}}", erro.isEmpty() ? "hidden" : "")
                .replace("{{erro}}", escaparHtml(erro))
                .replace("{{lista-vazia}}", itens.length() == 0 ? "" : "hidden")
                .replace("{{materiais}}", opcoesMateriais())
                .replace("{{itens}}", itens);

        responder(requisicao, codigo, "text/html", html);
    }

    static void redirecionar(HttpExchange requisicao, String destino) throws IOException {
        requisicao.getResponseHeaders().set("Location", destino);
        requisicao.getResponseHeaders().set("Cache-Control", "no-store");
        requisicao.sendResponseHeaders(303, -1);
    }

    static void responder(HttpExchange requisicao, int codigo, String tipo, String conteudo) throws IOException {
        byte[] resposta = conteudo.getBytes(StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", tipo + "; charset=UTF-8");
        requisicao.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        requisicao.getResponseHeaders().set("Content-Security-Policy", "frame-ancestors 'none'; form-action 'self'");
        requisicao.getResponseHeaders().set("Cache-Control", "no-store");
        requisicao.sendResponseHeaders(codigo, resposta.length);
        requisicao.getResponseBody().write(resposta);
    }

    static String opcoesMateriais() {
        return EstoqueDoadorBO.MATERIAIS.stream().map(ServidorLocal::escaparHtml)
                .map(material -> "<option>" + material + "</option>").collect(Collectors.joining());
    }

    static String escaparHtml(String valor) {
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
