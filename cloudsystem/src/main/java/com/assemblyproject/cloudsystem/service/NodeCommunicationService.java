package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.StorageNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NodeCommunicationService {

    private final ContainerClient containerClient;
    private final StorageNodeService nodeStorageService;
    private final RestTemplate restTemplate;

    @Value("${cloud.node.server-url:}")
    private String serverUrl;

    private final ConcurrentHashMap<String, Boolean> nodeStatus = new ConcurrentHashMap<>();

    public boolean sendHeartbeat(StorageNode node) {
        String url = "http://" + node.getSessionId() + "/nodes/heartbeat";
        try {
            restTemplate.postForEntity(url, null, Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean registerNode(StorageNode node) {
        String url = serverUrl + "/nodes/register";
        try {
            Map<String, Object> body = Map.of(
                    "name", node.getName(),
                    "totalSpaceBytes", node.getTotalSpaceBytes(),
                    "visibility", node.getVisibility().name(),
                    "address", node.getSessionId()
            );
            restTemplate.postForEntity(url, body, Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean uploadReplica(StorageNode node, Long fileID, byte[] containerData) {
        try {
            containerClient.uploadReplica(node, fileID, containerData);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public byte[] downloadReplica(StorageNode node, Long fileID) throws IOException {
        return containerClient.downloadReplica(node, fileID);
    }

    public boolean isNodeOnline(StorageNode node) {
        String url = "http://" + node.getSessionId() + "/nodes/health";
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            return resp.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}
