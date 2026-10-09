package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.NodeVisibility;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.HeartbeatMonitorService;
import com.assemblyproject.cloudsystem.service.StorageNodeService;
import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/nodes")
public class NodeController {

    private final StorageNodeRepository nodeRepository;
    private final StorageNodeService nodeStorageService;
    private final HeartbeatMonitorService heartbeatMonitorService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<StorageNode> registerNode(@RequestParam("name") String name,
                                                    @RequestParam(value = "totalSpaceBytes", required = false) Long totalSpaceBytes,
                                                    @RequestParam(value = "visibility", required = false) NodeVisibility visibility,
                                                    @RequestParam(value = "address", required = false) String address,
                                                    @AuthenticationPrincipal User principal) {
        Optional<StorageNode> existing = nodeRepository.findAllByOwner(principal).stream().findFirst();
        if (existing.isPresent()) {
            StorageNode node = existing.get();
            node.setName(name);
            node.setTotalSpaceBytes(totalSpaceBytes != null ? totalSpaceBytes : node.getTotalSpaceBytes());
            node.setVisibility(visibility != null ? visibility : node.getVisibility());
            if (address != null) node.setSessionId(address);
            node.setStatus(NodeStatus.ONLINE);
            node.setLastHeartbeat(LocalDateTime.now());
            return ResponseEntity.ok(nodeRepository.save(node));
        }

        StorageNode node = StorageNode.builder()
                .owner(principal)
                .name(name)
                .totalSpaceBytes(totalSpaceBytes != null ? totalSpaceBytes : 0L)
                .status(NodeStatus.ONLINE)
                .visibility(visibility != null ? visibility : NodeVisibility.PERSONAL)
                .lastHeartbeat(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        if (address != null) node.setSessionId(address);
        return ResponseEntity.ok(nodeRepository.save(node));
    }

    @GetMapping
    public ResponseEntity<List<StorageNode>> listNodes(@AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(nodeRepository.findAllByOwner(principal));
    }

    @PostMapping("/{nodeId}/visibility")
    public ResponseEntity<StorageNode> setVisibility(@PathVariable Long nodeId,
                                                     @RequestBody Map<String, String> body,
                                                     @AuthenticationPrincipal User principal) {
        StorageNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
        if (!node.getOwner().getUserID().equals(principal.getUserID())) {
            return ResponseEntity.status(403).build();
        }
        node.setVisibility(NodeVisibility.valueOf(body.get("visibility")));
        return ResponseEntity.ok(nodeRepository.save(node));
    }

    @PostMapping("/{nodeId}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable Long nodeId) {
        Optional<StorageNode> optional = nodeRepository.findById(nodeId);
        if (optional.isPresent()) {
            StorageNode node = optional.get();
            node.setStatus(NodeStatus.ONLINE);
            node.setLastHeartbeat(LocalDateTime.now());
            nodeRepository.save(node);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{nodeId}/storage-info")
    public ResponseEntity<Map<String, Object>> storageInfo(@PathVariable Long nodeId,
                                                           @AuthenticationPrincipal User principal) {
        StorageNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
        if (!nodeStorageService.canAccess(node, principal)) {
            return ResponseEntity.status(403).build();
        }
        try {
            long used = nodeStorageService.getStoredBytes(node);
            long free = node.getTotalSpaceBytes() - used;
            return ResponseEntity.ok(Map.of(
                    "nodeID", node.getNodeID(),
                    "usedBytes", used,
                    "freeBytes", free,
                    "totalBytes", node.getTotalSpaceBytes()
            ));
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/{nodeId}/quota")
    public ResponseEntity<StorageNode> setQuota(@PathVariable Long nodeId,
                                                @RequestBody Map<String, Long> body,
                                                @AuthenticationPrincipal User principal) {
        StorageNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
        if (!node.getOwner().getUserID().equals(principal.getUserID())) {
            return ResponseEntity.status(403).build();
        }
        Long quota = body.get("quotaBytes");
        if (quota != null && quota < node.getUsedSpaceBytes()) {
            return ResponseEntity.badRequest().body(null);
        }
        node.setTotalSpaceBytes(quota != null ? quota : node.getTotalSpaceBytes());
        return ResponseEntity.ok(nodeRepository.save(node));
    }

    @GetMapping("/{nodeId}/quota")
    public ResponseEntity<Map<String, Object>> getQuota(@PathVariable Long nodeId,
                                                        @AuthenticationPrincipal User principal) {
        StorageNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
        if (!nodeStorageService.canAccess(node, principal)) {
            return ResponseEntity.status(403).build();
        }
        long used = node.getUsedSpaceBytes();
        long total = node.getTotalSpaceBytes();
        long free = total - used;
        int usagePercent = total > 0 ? (int) ((used * 100) / total) : 0;
        String status = usagePercent >= 90 ? "CRITICAL" : usagePercent >= 70 ? "WARNING" : "OK";
        return ResponseEntity.ok(Map.of(
                "nodeID", node.getNodeID(),
                "quotaBytes", total,
                "usedBytes", used,
                "freeBytes", free,
                "usagePercent", usagePercent,
                "status", status
        ));
    }

    @PostMapping("/{nodeId}/access")
    public ResponseEntity<?> grantAccess(@PathVariable Long nodeId,
                                         @RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal User principal) {
        StorageNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
        if (!node.getOwner().getUserID().equals(principal.getUserID())) {
            return ResponseEntity.status(403).build();
        }
        Long grantedUserId = Long.valueOf(body.get("userID"));
        User grantedUser = new User();
        grantedUser.setUserID(grantedUserId);
        nodeStorageService.grantAccess(node, grantedUser, null);
        return ResponseEntity.ok(Map.of("granted", true));
    }

    @DeleteMapping("/{nodeId}/access/{userID}")
    public ResponseEntity<?> revokeAccess(@PathVariable Long nodeId, @PathVariable Long userID,
                                          @AuthenticationPrincipal User principal) {
        StorageNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
        if (!node.getOwner().getUserID().equals(principal.getUserID())) {
            return ResponseEntity.status(403).build();
        }
        User grantedUser = new User();
        grantedUser.setUserID(userID);
        nodeStorageService.revokeAccess(node, grantedUser);
        return ResponseEntity.ok(Map.of("revoked", true));
    }

    @GetMapping("/{nodeId}/access")
    public ResponseEntity<?> listAccess(@PathVariable Long nodeId,
                                        @AuthenticationPrincipal User principal) {
        StorageNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node not found"));
        if (!node.getOwner().getUserID().equals(principal.getUserID())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(nodeStorageService.getGrantedUsers(node));
    }

    @GetMapping("/jar")
    public ResponseEntity<byte[]> downloadNodeJar() {
        try {
            Path jarPath = Paths.get("cloudsystem-node.jar");
            if (!Files.exists(jarPath)) {
                return ResponseEntity.status(404).body(new byte[0]);
            }
            byte[] jarBytes = Files.readAllBytes(jarPath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cloudsystem-node.jar\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(jarBytes);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new byte[0]);
        }
    }

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> forceCheck() {
        heartbeatMonitorService.checkAllNodes();
        long offline = heartbeatMonitorService.getOfflineNodes().size();
        long degraded = heartbeatMonitorService.getDegradedNodes().size();
        return ResponseEntity.ok(Map.of(
                "checkedAt", LocalDateTime.now(),
                "offlineNodes", offline,
                "degradedNodes", degraded
        ));
    }

    @GetMapping("/offline")
    public ResponseEntity<List<com.assemblyproject.cloudsystem.entity.StorageNode>> listOffline() {
        return ResponseEntity.ok(heartbeatMonitorService.getOfflineNodes());
    }

    @GetMapping("/degraded")
    public ResponseEntity<List<com.assemblyproject.cloudsystem.entity.StorageNode>> listDegraded() {
        return ResponseEntity.ok(heartbeatMonitorService.getDegradedNodes());
    }

    /**
     * Resolve o utilizador autenticado (principal JWT) a partir do banco de dados.
     */
    private User currentUser(@AuthenticationPrincipal JWTUserData principal) {
        if (principal == null || principal.userID() == null) {
            return null;
        }
        return userRepository.findById(principal.userID()).orElse(null);
    }
}