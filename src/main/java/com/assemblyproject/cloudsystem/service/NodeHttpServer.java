package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import com.sun.net.httpserver.HttpServer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class NodeHttpServer {

    private final StorageNodeRepository nodeRepository;
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
            Map<String, Object> body = Map.of(
                    "name", "Node-" + port,
                    "totalSpaceBytes", 107374182400L,
                    "visibility", "PUBLIC",
                    "address", InetSocketAddress.createUnresolved(address, port).getHostString() + ":" + port
            );
            restTemplate.postForEntity(url, body, Map.class);
            System.out.println("Node registered with server at " + serverUrl);
        } catch (Exception e) {
            System.out.println("Failed to register node: " + e.getMessage());
        }
    }

    private void handleHealth(com.sun.net.httpserver.HttpExchange exch) throws IOException {
        String resp = "{\"status\":\"OK\"}";
        sendJson(exch, 200, resp);
    }

    private void handleReplica(com.sun.net.httpserver.HttpExchange exch) throws IOException {
        String path = exch.getRequestURI().getPath();
        String fileIdStr = path.substring(path.lastIndexOf('/') + 1);
        Long fileId = Long.parseLong(fileIdStr);

        if ("GET".equals(exch.getRequestMethod())) {
            serveReplica(exch, fileId);
        } else if ("POST".equals(exch.getRequestMethod())) {
            uploadReplica(exch, fileId);
        } else {
            exch.sendResponseHeaders(405, -1);
        }
    }

    private void handleRegister(com.sun.net.httpserver.HttpExchange exch) throws IOException {
        if ("POST".equals(exch.getRequestMethod())) {
            byte[] body = exch.getRequestBody().readAllBytes();
            Map<String, Object> request = parseJson(body);
            if (request == null) {
                sendJson(exch, 400, "{\"error\":\"Invalid JSON\"}");
                return;
            }

            String name = (String) request.get("name");
            Long totalSpaceBytes = ((Number) request.get("totalSpaceBytes")).longValue();
            String visibility = (String) request.get("visibility");
            String addr = (String) request.get("address");

            StorageNode node = StorageNode.builder()
                    .name(name)
                    .totalSpaceBytes(totalSpaceBytes)
                    .status(NodeStatus.ONLINE)
                    .visibility(com.assemblyproject.cloudsystem.entity.NodeVisibility.valueOf(visibility))
                    .lastHeartbeat(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
            if (addr != null) node.setSessionId(addr);
            StorageNode saved = nodeRepository.save(node);

            String resp = "{\"nodeID\":" + saved.getNodeID() + "}";
            sendJson(exch, 200, resp);
        } else {
            exch.sendResponseHeaders(405, -1);
        }
    }

    private Map<String, Object> parseJson(byte[] data) {
        try {
            String json = new String(data);
            // Simple JSON parser for basic objects
            Map<String, Object> map = new java.util.HashMap<>();
            json = json.trim().replaceAll("\\{", "").replaceAll("\\}", "");
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim().replaceAll("\"", "");
                    String value = kv[1].trim().replaceAll("\"", "");
                    map.put(key, value);
                }
            }
            return map;
        } catch (Exception e) {
            return null;
        }
    }

    private void serveReplica(com.sun.net.httpserver.HttpExchange exch, Long fileId) throws IOException {
        String nodeAddr = exch.getRemoteAddress().getAddress().getHostAddress() + ":" + exch.getRemoteAddress().getPort();
        StorageNode node = nodeRepository.findBySessionId(nodeAddr)
                .orElseThrow(() -> new RuntimeException("Node not registered"));

        byte[] data = nodeStorageService.readReplica(node, fileId);
        exch.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exch.sendResponseHeaders(200, data.length);
        try (OutputStream os = exch.getResponseBody()) {
            os.write(data);
        }
    }

    private void uploadReplica(com.sun.net.httpserver.HttpExchange exch, Long fileId) throws IOException {
        String nodeAddr = exch.getRemoteAddress().getAddress().getHostAddress() + ":" + exch.getRemoteAddress().getPort();
        StorageNode node = nodeRepository.findBySessionId(nodeAddr)
                .orElseThrow(() -> new RuntimeException("Node not registered"));

        byte[] data = exch.getRequestBody().readAllBytes();
        nodeStorageService.saveReplica(node, fileId, data);

        String resp = "{\"status\":\"OK\"}";
        sendJson(exch, 200, resp);
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
