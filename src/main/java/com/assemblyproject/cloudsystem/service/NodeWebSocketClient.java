package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.StorageNode;
import lombok.RequiredArgsConstructor;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class NodeWebSocketClient {

    private final StorageNodeService nodeStorageService;

    @Value("${cloud.node.server-url:}")
    private String serverUrl;

    private WebSocketClient webSocketClient;
    private final ConcurrentHashMap<String, byte[]> pendingDownloads = new ConcurrentHashMap<>();

    public void connect(StorageNode node) {
        String url = "ws://" + node.getSessionId() + "/ws/nodes";
        webSocketClient = new WebSocketClient(URI.create(url)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("WebSocket connected to node: " + node.getNodeID());
            }

            @Override
            public void onMessage(String message) {
                handleMessage(node, message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("WebSocket closed: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("WebSocket error: " + ex.getMessage());
            }
        };
        webSocketClient.connect();
    }

    public void sendCommand(String command, byte[] data) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            String message = command + ":" + java.util.Base64.getEncoder().encodeToString(data);
            webSocketClient.send(message);
        }
    }

    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    private void handleMessage(StorageNode node, String message) {
        String[] parts = message.split(":", 2);
        String command = parts[0];
        byte[] data = parts.length > 1 ? java.util.Base64.getDecoder().decode(parts[1]) : new byte[0];

        switch (command) {
            case "UPLOAD":
                try {
                    nodeStorageService.saveReplica(node, System.currentTimeMillis(), data);
                } catch (IOException e) {
                    System.out.println("Failed to save replica: " + e.getMessage());
                }
                break;
            case "DOWNLOAD":
                break;
            case "HEARTBEAT":
                break;
            case "DELETE":
                try {
                    nodeStorageService.deleteReplica(node, System.currentTimeMillis());
                } catch (IOException e) {
                    System.out.println("Failed to delete replica: " + e.getMessage());
                }
                break;
        }
    }
}
