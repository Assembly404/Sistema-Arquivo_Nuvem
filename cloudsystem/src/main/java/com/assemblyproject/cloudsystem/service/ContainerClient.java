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

    public static final String NODE_TOKEN_HEADER = "X-Node-Token";

    private final RestTemplate restTemplate;
    private final StorageNodeService nodeService;
    private final NodeSecretService nodeSecretService;

    @Value("${cloud.node.connect-timeout:5000}")
    private int connectTimeout;

    public String uploadReplica(StorageNode node, Long fileID, byte[] containerData) throws IOException {
        // A nuvem do sistema vive no próprio servidor: gravação local direta,
        // sem chamada HTTP (evita o servidor chamar-se a si próprio).
        if (node.isSystem()) {
            nodeService.saveReplica(node, fileID, containerData);
            return "OK";
        }
        String url = nodeUrl(node) + "/nodes/replicas/" + fileID;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(containerData.length);
        applyNodeToken(headers, node);
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
        if (node.isSystem()) {
            return nodeService.readReplica(node, fileID);
        }
        String url = nodeUrl(node) + "/nodes/replicas/" + fileID;
        HttpHeaders headers = new HttpHeaders();
        applyNodeToken(headers, node);
        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), byte[].class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
                return resp.getBody();
            }
            throw new IOException("Node returned no data");
        } catch (RestClientException e) {
            throw new IOException("Failed to download replica from node " + node.getNodeID(), e);
        }
    }

    /**
     * Apaga uma réplica: localmente na nuvem do sistema, ou via HTTP no agente
     * do dispositivo pessoal. Sem isto as réplicas de um dispositivo ficariam
     * órfãs em disco depois de o ficheiro ser movido/apagado.
     */
    public void deleteReplica(StorageNode node, Long fileID) throws IOException {
        if (node.isSystem()) {
            nodeService.deleteReplica(node, fileID);
            return;
        }
        String url = nodeUrl(node) + "/nodes/replicas/" + fileID;
        HttpHeaders headers = new HttpHeaders();
        applyNodeToken(headers, node);
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (RestClientException e) {
            throw new IOException("Failed to delete replica on node " + node.getNodeID(), e);
        }
    }

    public boolean isNodeOnline(StorageNode node) {
        if (node.isSystem()) {
            return true;
        }
        if (!node.isEnabled()) {
            return false;
        }
        if (node.getSessionId() == null || node.getSessionId().isBlank()) {
            return false;
        }
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

    private void applyNodeToken(HttpHeaders headers, StorageNode node) {
        String token = nodeSecretService.decrypt(node.getDeviceTokenEncrypted());
        if (token != null && !token.isBlank()) {
            headers.set(NODE_TOKEN_HEADER, token);
        }
    }

    private String nodeUrl(StorageNode node) {
        return "http://" + node.getSessionId();
    }
}
