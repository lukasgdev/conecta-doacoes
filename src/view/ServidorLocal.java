package view;

import bo.ItemSolicitacao;
import bo.Solicitacao;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ServidorLocal {
    private final String modeloHtml;
    private Solicitacao solicitacao = new Solicitacao();

    private ServidorLocal(String modeloHtml) {
        this.modeloHtml = modeloHtml;
    }

    public static void main(String[] args) throws IOException {
        String modelo = Files.readString(Path.of("src", "view", "solicitacao.html"));
        ServidorLocal aplicacao = new ServidorLocal(modelo);
        HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
        servidor.createContext("/", aplicacao::atender);
        servidor.start();
        System.out.println("Abra http://localhost:8080 no navegador. Ctrl+C encerra o servidor.");
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

        requisicao.getResponseHeaders().set("Location", "/");
        requisicao.sendResponseHeaders(303, -1);
    }

    private static Map<String, String> lerCampos(HttpExchange requisicao) throws IOException {
        byte[] dados = requisicao.getRequestBody().readNBytes(4097);
        if (dados.length > 4096) {
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
        for (ItemSolicitacao item : solicitacao.getItens()) {
            itens.append("<li><span>").append(escaparHtml(item.getMaterial()))
                    .append("</span><strong>").append(item.getQuantidade())
                    .append(" un.</strong></li>");
        }

        boolean confirmada = solicitacao.isConfirmada();
        String html = modeloHtml
                .replace("{{edicao}}", confirmada ? "hidden" : "")
                .replace("{{confirmada}}", confirmada ? "" : "hidden")
                .replace("{{erro-visivel}}", erro.isEmpty() ? "hidden" : "")
                .replace("{{erro}}", escaparHtml(erro))
                .replace("{{lista-vazia}}", itens.length() == 0 ? "" : "hidden")
                .replace("{{itens}}", itens);

        byte[] resposta = html.getBytes(StandardCharsets.UTF_8);
        requisicao.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        requisicao.getResponseHeaders().set("Cache-Control", "no-store");
        requisicao.sendResponseHeaders(codigo, resposta.length);
        requisicao.getResponseBody().write(resposta);
    }

    private static String escaparHtml(String valor) {
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
