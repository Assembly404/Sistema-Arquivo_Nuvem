package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.NodeVisibility;
import com.assemblyproject.cloudsystem.entity.Role;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.sun.net.httpserver.HttpServer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.json.BasicJsonParser;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class NodeHttpServer {

    private final StorageNodeRepository nodeRepository;
    private final UserRepository userRepository;
    private final StorageNodeService nodeStorageService;
    private final ContainerService containerService;
    private final NodeCommunicationService communicationService;
    private final RestTemplate restTemplate;

    @Value("${cloud.node.server-port:8081}")
    private int port;

    @Value("${cloud.node.server-address:0.0.0.0}")
    private String address;

    @Value("${cloud.node.auto-register:false}")
    private boolean autoRegister;

    @Value("${cloud.node.server-url:}")
    private String serverUrl;

    private HttpServer server;
    private volatile Long selfNodeId;

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(address, port), 0);
        server.createContext("/nodes/health", this::handleHealth);
        server.createContext("/nodes/replicas/", this::handleReplica);
        server.createContext("/nodes/register", this::handleRegister);
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("StorageNode HTTP server started on " + address + ":" + port);

        if (autoRegister && serverUrl != null && !serverUrl.isEmpty()) {
            registerWithServer();
        }
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    private void registerWithServer() {
        try {
            String url = serverUrl + "/nodes/register";
            String advertised = advertisedAddress();
            Map<String, Object> body = Map.of(
                    "name", "Node-" + port,
                    "totalSpaceBytes", 107374182400L,
                    "visibility", "PUBLIC",
                    "address", advertised
            );
            Map<?, ?> response = restTemplate.postForEntity(url, body, Map.class).getBody();
            if (response != null && response.get("nodeID") instanceof Number nodeId) {
                this.selfNodeId = nodeId.longValue();
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

    private void handleHealth(com.sun.net.httpserver.HttpExchange exch) throws IOException {
        sendJson(exch, 200, "{\"status\":\"OK\"}");
    }

    private void handleReplica(com.sun.net.httpserver.HttpExchange exch) throws IOException {
        String path = exch.getRequestURI().getPath();
        String fileIdStr = path.substring(path.lastIndexOf('/') + 1);
        Long fileId;
        try {
            fileId = Long.parseLong(fileIdStr);
        } catch (NumberFormatException e) {
            sendJson(exch, 400, "{\"error\":\"Invalid file id\"}");
            return;
        }

        if ("GET".equals(exch.getRequestMethod())) {
            serveReplica(exch, fileId);
        } else if ("POST".equals(exch.getRequestMethod())) {
            uploadReplica(exch, fileId);
        } else {
            exch.sendResponseHeaders(405, -1);
        }
    }

    private void handleRegister(com.sun.net.httpserver.HttpExchange exch) throws IOException {
        if (!"POST".equals(exch.getRequestMethod())) {
            exch.sendResponseHeaders(405, -1);
            return;
        }
        byte[] body = exch.getRequestBody().readAllBytes();
        Map<String, Object> request;
        try {
            request = new BasicJsonParser().parseMap(new String(body));
        } catch (Exception e) {
            sendJson(exch, 400, "{\"error\":\"Invalid JSON\"}");
            return;
        }
        Object nameValue = request.get("name");
        if (nameValue == null || nameValue.toString().isBlank()) {
            sendJson(exch, 400, "{\"error\":\"Missing name\"}");
            return;
        }

        User owner = userRepository.findFirstByRole(Role.ADMIN)
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
        if (owner == null) {
            sendJson(exch, 409, "{\"error\":\"No owner available\"}");
            return;
        }

        NodeVisibility visibility = NodeVisibility.PERSONAL;
        Object visibilityValue = request.get("visibility");
        if (visibilityValue != null && !visibilityValue.toString().isBlank()) {
            try {
                visibility = NodeVisibility.valueOf(visibilityValue.toString().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                visibility = NodeVisibility.PERSONAL;
            }
        }

        StorageNode node = StorageNode.builder()
                .owner(owner)
                .name(nameValue.toString())
                .totalSpaceBytes(parseLong(request.get("totalSpaceBytes"), 0L))
                .status(NodeStatus.ONLINE)
                .visibility(visibility)
                .lastHeartbeat(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        Object addressValue = request.get("address");
        if (addressValue != null) node.setSessionId(addressValue.toString());
        StorageNode saved = nodeRepository.save(node);

        sendJson(exch, 200, "{\"nodeID\":" + saved.getNodeID() + "}");
    }

    private long parseLong(Object value, long fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void serveReplica(com.sun.net.httpserver.HttpExchange exch, Long fileId) throws IOException {
        StorageNode node = resolveSelfNode();
        if (node == null) {
            sendJson(exch, 404, "{\"error\":\"Node not registered\"}");
            return;
        }

        byte[] data;
        try {
            data = nodeStorageService.readReplica(node, fileId);
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

    private void uploadReplica(com.sun.net.httpserver.HttpExchange exch, Long fileId) throws IOException {
        StorageNode node = resolveSelfNode();
        if (node == null) {
            sendJson(exch, 404, "{\"error\":\"Node not registered\"}");
            return;
        }

        byte[] data = exch.getRequestBody().readAllBytes();
        nodeStorageService.saveReplica(node, fileId, data);
        sendJson(exch, 200, "{\"status\":\"OK\"}");
    }

    /**
     * Identifica o nó correspondente a este processo: pelo ID obtido no registo
     * ou, em fallback, pela porta anunciada no sessionId.
     */
    private StorageNode resolveSelfNode() {
        if (selfNodeId != null) {
            Optional<StorageNode> byId = nodeRepository.findById(selfNodeId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        String suffix = ":" + port;
        return nodeRepository.findAll().stream()
                .filter(n -> n.getSessionId() != null && n.getSessionId().endsWith(suffix))
                .findFirst()
                .orElse(null);
    }

    private void sendJson(com.sun.net.httpserver.HttpExchange exch, int status, String json) throws IOException {
        exch.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes();
        exch.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exch.getResponseBody()) {
            os.write(bytes);
        }
    }
}
