package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.NodeVisibility;
import com.assemblyproject.cloudsystem.entity.Role;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.dto.request.RegisterNodeRequest;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.HeartbeatMonitorService;
import com.assemblyproject.cloudsystem.service.StorageNodeService;
import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/nodes")
public class NodeController {

    private final StorageNodeRepository nodeRepository;
    private final StorageNodeService nodeStorageService;
    private final HeartbeatMonitorService heartbeatMonitorService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerNode(@Valid @RequestBody RegisterNodeRequest request,
                                          @AuthenticationPrincipal JWTUserData principal) {
        User owner = resolveUser(principal);
        if (owner == null) {
            owner = userRepository.findFirstByRole(Role.ADMIN).orElse(null);
        }
        if (owner == null) {
            return ResponseEntity.status(409).body(Map.of("message", "Nenhum utilizador disponível para ser dono do nó."));
        }

        NodeVisibility visibility = parseVisibility(request.visibility());

        StorageNode node = nodeRepository.findAllByOwner(owner).stream().findFirst().orElse(null);
        if (node != null) {
            node.setName(request.name());
            if (request.totalSpaceBytes() != null) node.setTotalSpaceBytes(request.totalSpaceBytes());
            if (visibility != null) node.setVisibility(visibility);
            if (request.address() != null) node.setSessionId(request.address());
            node.setStatus(NodeStatus.ONLINE);
            node.setLastHeartbeat(LocalDateTime.now());
            return ResponseEntity.ok(nodeRepository.save(node));
        }

        node = StorageNode.builder()
                .owner(owner)
                .name(request.name())
                .totalSpaceBytes(request.totalSpaceBytes() != null ? request.totalSpaceBytes() : 0L)
                .status(NodeStatus.ONLINE)
                .visibility(visibility != null ? visibility : NodeVisibility.PERSONAL)
                .lastHeartbeat(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        if (request.address() != null) node.setSessionId(request.address());
        return ResponseEntity.ok(nodeRepository.save(node));
    }

    @GetMapping
    public ResponseEntity<?> listNodes(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Não autenticado."));
        }
        return ResponseEntity.ok(nodeRepository.findAllByOwner(user));
    }

    @PostMapping("/{nodeId}/visibility")
    public ResponseEntity<?> setVisibility(@PathVariable Long nodeId,
                                           @RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        StorageNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) return notFound();
        if (!isOwner(node, user)) return forbidden();
        NodeVisibility visibility = parseVisibility(body.get("visibility"));
        if (visibility == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Visibilidade inválida."));
        }
        node.setVisibility(visibility);
        return ResponseEntity.ok(nodeRepository.save(node));
    }

    @PostMapping("/{nodeId}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable Long nodeId) {
        StorageNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) {
            return ResponseEntity.notFound().build();
        }
        node.setStatus(NodeStatus.ONLINE);
        node.setLastHeartbeat(LocalDateTime.now());
        nodeRepository.save(node);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{nodeId}/storage-info")
    public ResponseEntity<?> storageInfo(@PathVariable Long nodeId,
                                         @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        StorageNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) return notFound();
        if (!nodeStorageService.canAccess(node, user)) return forbidden();
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
            return ResponseEntity.status(500).body(Map.of("message", "Não foi possível ler o armazenamento do nó."));
        }
    }

    @PostMapping("/{nodeId}/quota")
    public ResponseEntity<?> setQuota(@PathVariable Long nodeId,
                                      @RequestBody Map<String, Long> body,
                                      @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        StorageNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) return notFound();
        if (!isOwner(node, user)) return forbidden();
        Long quota = body.get("quotaBytes");
        if (quota != null && quota < node.getUsedSpaceBytes()) {
            return ResponseEntity.badRequest().body(Map.of("message", "A quota não pode ser inferior ao espaço usado."));
        }
        if (quota != null) node.setTotalSpaceBytes(quota);
        return ResponseEntity.ok(nodeRepository.save(node));
    }

    @GetMapping("/{nodeId}/quota")
    public ResponseEntity<?> getQuota(@PathVariable Long nodeId,
                                      @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        StorageNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) return notFound();
        if (!nodeStorageService.canAccess(node, user)) return forbidden();
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
                                         @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        StorageNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) return notFound();
        if (!isOwner(node, user)) return forbidden();

        Long grantedUserId;
        try {
            grantedUserId = Long.valueOf(body.get("userID"));
        } catch (NumberFormatException | NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "userID inválido."));
        }
        User grantedUser = userRepository.findById(grantedUserId).orElse(null);
        if (grantedUser == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Utilizador não encontrado."));
        }
        nodeStorageService.grantAccess(node, grantedUser, null);
        return ResponseEntity.ok(Map.of("granted", true));
    }

    @DeleteMapping("/{nodeId}/access/{userID}")
    public ResponseEntity<?> revokeAccess(@PathVariable Long nodeId, @PathVariable Long userID,
                                          @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        StorageNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) return notFound();
        if (!isOwner(node, user)) return forbidden();
        User grantedUser = userRepository.findById(userID).orElse(null);
        if (grantedUser == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Utilizador não encontrado."));
        }
        nodeStorageService.revokeAccess(node, grantedUser);
        return ResponseEntity.ok(Map.of("revoked", true));
    }

    @GetMapping("/{nodeId}/access")
    public ResponseEntity<?> listAccess(@PathVariable Long nodeId,
                                        @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        StorageNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) return notFound();
        if (!isOwner(node, user)) return forbidden();

        List<Map<String, Object>> granted = new ArrayList<>();
        nodeStorageService.getGrantedUsers(node).forEach(access -> granted.add(Map.of(
                "userID", access.getGrantedUser().getUserID(),
                "name", access.getGrantedUser().getName(),
                "email", access.getGrantedUser().getEmail(),
                "grantedAt", access.getGrantedAt() != null ? access.getGrantedAt().toString() : "",
                "expiresAt", access.getExpiresAt() != null ? access.getExpiresAt().toString() : ""
        )));
        return ResponseEntity.ok(granted);
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
    public ResponseEntity<List<StorageNode>> listOffline() {
        return ResponseEntity.ok(heartbeatMonitorService.getOfflineNodes());
    }

    @GetMapping("/degraded")
    public ResponseEntity<List<StorageNode>> listDegraded() {
        return ResponseEntity.ok(heartbeatMonitorService.getDegradedNodes());
    }

    private boolean isOwner(StorageNode node, User user) {
        return node.getOwner() != null && node.getOwner().getUserID().equals(user.getUserID());
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("message", "Não autenticado."));
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Sem permissão."));
    }

    private ResponseEntity<Map<String, String>> notFound() {
        return ResponseEntity.status(404).body(Map.of("message", "Nó não encontrado."));
    }

    private NodeVisibility parseVisibility(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return NodeVisibility.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Resolve o utilizador autenticado (principal JWT) a partir do banco de dados.
     */
    private User resolveUser(JWTUserData principal) {
        if (principal == null || principal.userID() == null) {
            return null;
        }
        return userRepository.findById(principal.userID()).orElse(null);
    }
}
