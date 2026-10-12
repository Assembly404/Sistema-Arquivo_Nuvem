package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP local que expõe as réplicas de um nó de armazenamento.
 * <p>
 * No servidor principal, representa a nuvem do sistema. Num agente de
 * dispositivo pessoal, representa o próprio dispositivo. O acesso às réplicas
 * exige o token de vinculação do nó (RF17.4), eliminando o acesso anónimo que
 * permitia ler/escrever réplicas arbitrárias.
 */
@Component
@RequiredArgsConstructor
public class NodeHttpServer {

    private final StorageNodeRepository nodeRepository;
    private final StorageNodeService nodeStorageService;
    private final SystemNodeService systemNodeService;
    private final NodeSecretService nodeSecretService;
    private final RestTemplate restTemplate;

    @Value("${cloud.node.server-port:8081}")
    private int port;

    @Value("${cloud.node.server-address:0.0.0.0}")
    private String address;

    @Value("${cloud.node.auto-register:false}")
    private boolean autoRegister;

    @Value("${cloud.node.server-url:}")
    private String serverUrl;

    @Value("${cloud.node.pairing-code:}")
    private String pairingCode;

    @Value("${cloud.node.device-token:}")
    private String configuredDeviceToken;

    @Value("${cloud.node.node-id:0}")
    private long configuredNodeId;

    @Value("${cloud.node.quota-bytes:0}")
    private long configuredQuotaBytes;

    @Value("${cloud.node.storage-dir:./node-storage}")
    private String configuredStorageDir;

    private HttpServer server;
    private volatile Long selfNodeId;
    private volatile String selfDeviceToken;

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        // Um agente já vinculado conhece o id do seu dispositivo pela configuração.
        if (configuredNodeId > 0) {
            this.selfNodeId = configuredNodeId;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(address, port), 0);
        } catch (IOException e) {
            // Porta ocupada (ex.: reinício do devtools): não derruba a aplicação;
            // o servidor HTTP local do nó fica indisponível nesta execução.
            System.out.println("StorageNode HTTP server could not bind " + address + ":" + port
                    + " (" + e.getMessage() + "); a continuar sem servidor local de nó.");
            return;
        }
        server.createContext("/nodes/health", this::handleHealth);
        server.createContext("/nodes/replicas/", this::handleReplica);
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("StorageNode HTTP server started on " + address + ":" + port);

        if (autoRegister && selfNodeId == null && serverUrl != null && !serverUrl.isEmpty()) {
            registerWithServer();
        }
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void registerWithServer() {
        try {
            String url = serverUrl + "/nodes/register";
            String advertised = advertisedAddress();
            long freeBytes = 0L;
            try {
                freeBytes = new java.io.File(configuredStorageDir).getFreeSpace();
            } catch (Exception ignored) {
                // sem informação de espaço livre
            }
            long quota = configuredQuotaBytes > 0 ? configuredQuotaBytes
                    : (freeBytes > 0 ? freeBytes : 0L);
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("name", "Dispositivo-" + advertised);
            body.put("totalSpaceBytes", quota);
            body.put("reportedFreeBytes", freeBytes > 0 ? freeBytes : null);
            body.put("localPath", configuredStorageDir);
            body.put("visibility", "PERSONAL");
            body.put("address", advertised);
            body.put("operatingSystem", System.getProperty("os.name"));
            body.put("agentVersion", "1.0");
            if (pairingCode != null && !pairingCode.isBlank()) {
                body.put("pairingCode", pairingCode);
            }
            Map<?, ?> response = restTemplate.postForEntity(url, body, Map.class).getBody();
            if (response != null) {
                if (response.get("nodeID") instanceof Number nodeId) {
                    this.selfNodeId = nodeId.longValue();
                }
                if (response.get("deviceToken") instanceof String token) {
                    this.selfDeviceToken = token;
                }
            }
            System.out.println("Node registered with server at " + serverUrl + " as " + advertised);
        } catch (Exception e) {
            System.out.println("Failed to register node: " + e.getMessage());
        }
    }

    private String advertisedAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress() + ":" + port;
        } catch (Exception e) {
            return InetSocketAddress.createUnresolved(address, port).getHostString() + ":" + port;
        }
    }

    private void handleHealth(HttpExchange exch) throws IOException {
        sendJson(exch, 200, "{\"status\":\"OK\"}");
    }

    private void handleReplica(HttpExchange exch) throws IOException {
        StorageNode self = resolveSelfNode();
        if (self == null) {
            sendJson(exch, 404, "{\"error\":\"Node not registered\"}");
            return;
        }
        if (!isAuthorized(exch, self)) {
            sendJson(exch, 401, "{\"error\":\"Unauthorized\"}");
            return;
        }

        String path = exch.getRequestURI().getPath();
        String fileIdStr = path.substring(path.lastIndexOf('/') + 1);
        Long fileId;
        try {
            fileId = Long.parseLong(fileIdStr);
        } catch (NumberFormatException e) {
            sendJson(exch, 400, "{\"error\":\"Invalid file id\"}");
            return;
        }

        switch (exch.getRequestMethod()) {
            case "GET" -> serveReplica(exch, self, fileId);
            case "POST" -> uploadReplica(exch, self, fileId);
            case "DELETE" -> deleteReplica(exch, self, fileId);
            default -> exch.sendResponseHeaders(405, -1);
        }
    }

    private boolean isAuthorized(HttpExchange exch, StorageNode self) {
        // Nó de sistema: não aceita escrita remota (é servido localmente).
        if (self.isSystem()) {
            return false;
        }
        String provided = header(exch, ContainerClient.NODE_TOKEN_HEADER);
        if (provided == null || provided.isBlank()) {
            return false;
        }
        if (configuredDeviceToken != null && !configuredDeviceToken.isBlank()) {
            return java.security.MessageDigest.isEqual(
                    configuredDeviceToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    provided.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return nodeSecretService.matches(self.getDeviceTokenEncrypted(), provided);
    }

    private String header(HttpExchange exch, String name) {
        return exch.getRequestHeaders().getFirst(name);
    }

    private void serveReplica(HttpExchange exch, StorageNode self, Long fileId) throws IOException {
        byte[] data;
        try {
            data = nodeStorageService.readReplica(self, fileId);
        } catch (IOException e) {
            exch.sendResponseHeaders(404, -1);
            return;
        }
        exch.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exch.sendResponseHeaders(200, data.length);
        try (OutputStream os = exch.getResponseBody()) {
            os.write(data);
        }
    }

    private void uploadReplica(HttpExchange exch, StorageNode self, Long fileId) throws IOException {
        byte[] data = exch.getRequestBody().readAllBytes();
        nodeStorageService.saveReplica(self, fileId, data);
        nodeStorageService.refreshUsedSpace(self);
        nodeRepository.save(self);
        sendJson(exch, 200, "{\"status\":\"OK\"}");
    }

    private void deleteReplica(HttpExchange exch, StorageNode self, Long fileId) throws IOException {
        nodeStorageService.deleteReplica(self, fileId);
        nodeStorageService.refreshUsedSpace(self);
        nodeRepository.save(self);
        sendJson(exch, 200, "{\"status\":\"OK\"}");
    }

    /**
     * Identifica o nó correspondente a este processo: um dispositivo registado
     * (por id) ou, por omissão, o nó de sistema (nuvem oficial).
     */
    private StorageNode resolveSelfNode() {
        if (selfNodeId != null) {
            Optional<StorageNode> byId = nodeRepository.findById(selfNodeId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        return systemNodeService.getOrCreateSystemNode();
    }

    private void sendJson(HttpExchange exch, int status, String json) throws IOException {
        exch.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes();
        exch.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exch.getResponseBody()) {
            os.write(bytes);
        }
    }
}
