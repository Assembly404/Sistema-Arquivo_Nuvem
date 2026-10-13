package com.assemblyproject.cloudsystem.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP standalone para o agente de armazenamento (nó pessoal).
 * <p>
 * Ao contrário do {@link NodeHttpServer} (que corre no servidor principal e
 * depende de JPA), esta versão é auto-contida: guarda apenas o nodeId e o
 * deviceToken em memória e no disco, e serve as réplicas directamente do
 * directório local configurado. Usa apenas a biblioteca standard do Java
 * (java.net.http), sem Spring Web nem Jackson.
 */
@Component
@Profile("node")
public class StandaloneNodeServer {

    public static final String NODE_TOKEN_HEADER = "X-Node-Token";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${cloud.node.server-url:http://localhost:5000}")
    private String serverUrl;

    @Value("${cloud.node.server-port:8081}")
    private int port;

    @Value("${cloud.node.server-address:0.0.0.0}")
    private String address;

    @Value("${cloud.node.auto-register:false}")
    private boolean autoRegister;

    @Value("${cloud.node.pairing-code:}")
    private String pairingCode;

    @Value("${cloud.node.pairing-code-file:}")
    private String pairingCodeFile;

    @Value("${cloud.node.device-token:}")
    private String deviceToken;

    @Value("${cloud.node.node-id:0}")
    private long nodeId;

    @Value("${cloud.node.quota-bytes:0}")
    private long quotaBytes;

    @Value("${cloud.node.storage-dir:./node-storage}")
    private String storageDir;

    /**
     * Endereço público pelo qual o servidor central alcança este nó. Necessário
     * quando o computador está atrás de NAT (ex.: servidor na nuvem + PC em
     * casa): define aqui o URL do túnel (cloudflared/ngrok) ou o IP:porta
     * público. Vazio → anuncia o IP local e a porta (funciona em LAN).
     */
    @Value("${cloud.node.public-url:}")
    private String publicUrl;

    /** Nome amigável apresentado na lista de dispositivos. */
    @Value("${cloud.node.name:}")
    private String configuredName;

    private HttpServer server;
    private volatile String activeDeviceToken;
    private final Path stateFile;

    public StandaloneNodeServer() {
        this.stateFile = Paths.get(System.getProperty("user.home"), ".cloudsystem-node");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        loadState();

        try {
            server = HttpServer.create(new InetSocketAddress(address, port), 0);
        } catch (IOException e) {
            System.err.println("Não foi possível iniciar o servidor na porta " + port + ": " + e.getMessage());
            System.err.println("Fecha outra aplicação que esteja a usar a porta e tenta novamente.");
            throw e;
        }

        server.createContext("/nodes/health", this::handleHealth);
        server.createContext("/nodes/replicas/", this::handleReplica);
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("==============================================");
        System.out.println("  CloudSystem Node Agent iniciado!");
        System.out.println("==============================================");
        System.out.println("  Pasta local:    " + Paths.get(storageDir).toAbsolutePath());
        System.out.println("  Servidor:       " + serverUrl);
        System.out.println("  Porta local:    " + port);
        System.out.println("  Health local:   http://localhost:" + port + "/nodes/health");
        if (nodeId > 0) {
            System.out.println("  Node ID:        " + nodeId);
        }
        if (publicUrl != null && !publicUrl.isBlank()) {
            System.out.println("  public-url:     " + publicUrl);
        } else {
            System.out.println("  public-url:     (nao definido - anuncia IP local)");
        }

        if (autoRegister && nodeId == 0) {
            System.out.println();
            System.out.println("A registar no servidor com o codigo de pareamento...");
            registerWithServer();
        }
        if (nodeId > 0) {
            announceAddress();
            System.out.println("  Estado:         REGISTADO");
            System.out.println("  Endereco:       " + advertisedAddress());
            if (publicUrl == null || publicUrl.isBlank()) {
                System.out.println();
                System.out.println("  AVISO: sem public-url, o servidor central so consegue aceder a");
                System.out.println("         este dispositivo se estiver na mesma rede (LAN). Se o");
                System.out.println("         servidor estiver na nuvem e este PC atras de NAT, expoe");
                System.out.println("         o agente com um tunel e define cloud.node.public-url:");
                System.out.println("           cloudflared tunnel --url http://localhost:" + port);
            }
        } else {
            System.out.println("  Estado:         AGUARDANDO REGISTO");
            System.out.println("  Usa um código de pareamento do site para registar.");
        }
        System.out.println("==============================================");

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private void loadState() throws IOException {
        if (nodeId > 0 && deviceToken != null && !deviceToken.isBlank()) {
            this.activeDeviceToken = deviceToken;
            return;
        }
        if (!Files.exists(stateFile)) {
            return;
        }
        for (String line : Files.readAllLines(stateFile)) {
            line = line.trim();
            if (line.startsWith("nodeId=")) {
                nodeId = Long.parseLong(line.substring("nodeId=".length()));
            } else if (line.startsWith("deviceToken=")) {
                this.activeDeviceToken = line.substring("deviceToken=".length());
            }
        }
    }

    private void saveState() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("nodeId=").append(nodeId).append('\n');
        sb.append("deviceToken=").append(activeDeviceToken != null ? activeDeviceToken : "").append('\n');
        Files.write(stateFile, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void registerWithServer() {
        String code = pairingCode;
        if ((code == null || code.isBlank()) && pairingCodeFile != null && !pairingCodeFile.isBlank()) {
            try {
                Path p = Paths.get(pairingCodeFile);
                if (Files.exists(p)) {
                    code = Files.readString(p).trim();
                }
            } catch (IOException ignored) {
            }
        }

        if (code == null || code.isBlank()) {
            System.out.println("Auto-registo activado mas não foi fornecido código de pareamento.");
            System.out.println("Define cloud.node.pairing-code=CODIGO no node.properties.");
            return;
        }

        try {
            String url = serverUrl + "/nodes/register";
            File dir = new File(storageDir);
            long freeBytes = dir.getUsableSpace();
            String json = buildRegistrationJson(code.trim().toUpperCase(), freeBytes, dir);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map<String, Object> parsed = SimpleJson.parseObject(response.body());
                Object id = parsed.get("nodeID");
                Object token = parsed.get("deviceToken");
                if (id instanceof Number n) {
                    this.nodeId = n.longValue();
                }
                if (token instanceof String s) {
                    this.activeDeviceToken = s;
                }
                saveState();
                System.out.println("Registado com sucesso! Node ID: " + nodeId);
            } else {
                System.out.println("Falha ao registar (" + response.statusCode() + "): " + response.body());
                System.out.println("Verifica se o código de pareamento é válido e não expirou.");
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Falha ao registar: " + e.getMessage());
            System.out.println("Verifica se o servidor está acessível: " + serverUrl);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String buildRegistrationJson(String code, long freeBytes, File dir) {
        String name = configuredName != null && !configuredName.isBlank()
                ? configuredName.trim()
                : "Dispositivo-" + System.getProperty("user.name", "desconhecido");
        String os = System.getProperty("os.name", "unknown");
        long total = quotaBytes > 0 ? quotaBytes : freeBytes;
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("totalSpaceBytes", total);
        body.put("reportedFreeBytes", freeBytes);
        body.put("localPath", dir.getAbsolutePath());
        body.put("address", advertisedAddress());
        body.put("visibility", "PRIVATE");
        body.put("operatingSystem", os);
        body.put("agentVersion", "1.0");
        body.put("pairingCode", code);
        return SimpleJson.toJson(body);
    }

    /**
     * Endereço pelo qual o servidor central deve contactar este nó. Usa
     * {@code cloud.node.public-url} quando definido (túneis / IP público);
     * caso contrário anuncia o IP local e a porta (LAN/dev).
     */
    private String advertisedAddress() {
        if (publicUrl != null && !publicUrl.isBlank()) {
            String value = publicUrl.trim();
            while (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            return value;
        }
        String host;
        try {
            host = localRoutableAddress();
        } catch (Exception e) {
            host = "127.0.0.1";
        }
        return "http://" + host + ":" + port;
    }

    /**
     * Descobre um IP local não-loopback (preferindo IPv4), para que um servidor
     * na mesma rede consiga alcançar este nó. Evita anunciar 127.0.1.1 (comum em
     * Linux) ou endereços de link-local. Cai para 127.0.0.1 se nada melhor.
     */
    private String localRoutableAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> ifaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            String fallback = null;
            while (ifaces != null && ifaces.hasMoreElements()) {
                java.net.NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) {
                    continue;
                }
                java.util.Enumeration<java.net.InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
                        continue;
                    }
                    if (addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                    if (fallback == null) {
                        fallback = addr.getHostAddress();
                    }
                }
            }
            if (fallback != null) {
                return fallback;
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /**
     * Informa o servidor do endereço actual do nó. Útil quando o nó já estava
     * registado (ex.: sem endereço) ou quando o URL de um túnel muda entre
     * arranques. Falha silenciosamente se o servidor não responder.
     */
    private void announceAddress() {
        if (nodeId <= 0 || activeDeviceToken == null || activeDeviceToken.isBlank()) {
            return;
        }
        String endpoint = advertisedAddress();
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("address", endpoint);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/nodes/" + nodeId + "/address"))
                    .header("Content-Type", "application/json")
                    .header(NODE_TOKEN_HEADER, activeDeviceToken)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(SimpleJson.toJson(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("  Endereco anunciado ao servidor: " + endpoint);
            } else {
                System.out.println("  Nao foi possivel anunciar o endereco (" + response.statusCode() + ").");
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("  Nao foi possivel anunciar o endereco: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handleHealth(HttpExchange exch) throws IOException {
        sendJson(exch, 200, "{\"status\":\"OK\"}");
    }

    private void handleReplica(HttpExchange exch) throws IOException {
        if (nodeId == 0) {
            sendJson(exch, 503, "{\"error\":\"Node not registered yet. Provide a pairing code.\"}");
            return;
        }
        if (!isAuthorized(exch)) {
            sendJson(exch, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        String path = exch.getRequestURI().getPath();
        String fileIdStr = path.substring(path.lastIndexOf('/') + 1);
        if (!fileIdStr.matches("\\d+")) {
            sendJson(exch, 400, "{\"error\":\"Invalid file id\"}");
            return;
        }

        Path dir = Paths.get(storageDir);
        Path replica = dir.resolve(fileIdStr + ".dbs");

        switch (exch.getRequestMethod()) {
            case "GET" -> {
                if (!Files.exists(replica)) {
                    exch.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] data = Files.readAllBytes(replica);
                exch.getResponseHeaders().set("Content-Type", "application/octet-stream");
                exch.sendResponseHeaders(200, data.length);
                try (OutputStream os = exch.getResponseBody()) {
                    os.write(data);
                }
            }
            case "POST" -> {
                byte[] data = exch.getRequestBody().readAllBytes();
                Files.createDirectories(dir);
                Files.write(replica, data);
                sendJson(exch, 200, "{\"status\":\"OK\"}");
            }
            case "DELETE" -> {
                Files.deleteIfExists(replica);
                sendJson(exch, 200, "{\"status\":\"OK\"}");
            }
            default -> exch.sendResponseHeaders(405, -1);
        }
    }

    private boolean isAuthorized(HttpExchange exch) {
        String provided = exch.getRequestHeaders().getFirst(NODE_TOKEN_HEADER);
        if (provided == null || provided.isBlank() || activeDeviceToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                activeDeviceToken.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    private void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void sendJson(HttpExchange exch, int status, String json) throws IOException {
        exch.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exch.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exch.getResponseBody()) {
            os.write(bytes);
        }
    }
}
