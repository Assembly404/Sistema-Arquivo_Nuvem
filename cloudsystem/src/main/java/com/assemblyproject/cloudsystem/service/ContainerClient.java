package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.StorageNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ContainerClient {

    private final RestTemplate restTemplate;
    private final StorageNodeService nodeService;

    @Value("${cloud.node.connect-timeout:5000}")
    private int connectTimeout;

    public String uploadReplica(StorageNode node, Long fileID, byte[] containerData) throws IOException {
        String url = nodeUrl(node) + "/nodes/replicas/" + fileID;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(containerData.length);
        HttpEntity<byte[]> entity = new HttpEntity<>(containerData, headers);
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                return "OK";
            }
            throw new IOException("Node rejected replica: " + resp.getStatusCode());
        } catch (RestClientException e) {
            throw new IOException("Failed to upload replica to node " + node.getNodeID(), e);
        }
    }

    public byte[] downloadReplica(StorageNode node, Long fileID) throws IOException {
        String url = nodeUrl(node) + "/nodes/replicas/" + fileID;
        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                return resp.getBody();
            }
            throw new IOException("Node returned no data");
        } catch (RestClientException e) {
            throw new IOException("Failed to download replica from node " + node.getNodeID(), e);
        }
    }

    public boolean isNodeOnline(StorageNode node) {
        String url = nodeUrl(node) + "/nodes/health";
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            return resp.getStatusCode() == HttpStatus.OK;
        } catch (RestClientException e) {
            return false;
        }
    }

    public String registerNode(StorageNode node) throws IOException {
        String url = "http://" + node.getSessionId() + "/nodes/register";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("nodeID", node.getNodeID());
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, body, Map.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                return "OK";
            }
            throw new IOException("Node rejected registration");
        } catch (RestClientException e) {
            throw new IOException("Failed to register node", e);
        }
    }

    private String nodeUrl(StorageNode node) {
        return "http://" + node.getSessionId();
    }
}
