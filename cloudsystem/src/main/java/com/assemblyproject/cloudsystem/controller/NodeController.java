package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.entity.FileReplica;
import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.NodeVisibility;
import com.assemblyproject.cloudsystem.entity.ReplicaStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.dto.request.RegisterNodeRequest;
import com.assemblyproject.cloudsystem.dto.response.StorageNodeResponse;
import com.assemblyproject.cloudsystem.repository.FileReplicaRepository;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.HeartbeatMonitorService;
import com.assemblyproject.cloudsystem.service.NodeManagementService;
import com.assemblyproject.cloudsystem.service.PairingCodeService;
import com.assemblyproject.cloudsystem.service.StorageNodeService;
import com.assemblyproject.cloudsystem.service.SystemNodeService;
import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/nodes")
public class NodeController {

    private final StorageNodeRepository nodeRepository;
    private final StorageNodeService nodeStorageService;
    private final HeartbeatMonitorService heartbeatMonitorService;
    private final NodeManagementService nodeManagementService;
    private final SystemNodeService systemNodeService;
    private final PairingCodeService pairingCodeService;
    private final FileReplicaRepository fileReplicaRepository;
    private final UserRepository userRepository;

    /** Caminho opcional para o jar do agente standalone (default: procura em target/). */
    @Value("${cloud.system.node-agent-jar:}")
    private String nodeAgentJarPath;

    // --- Vinculação segura de dispositivos (RF17.3/RF17.4) ---

    /**
     * Gera um código de pareamento de curta duração que o agente local usa no
     * registo, vinculando o dispositivo à conta do utilizador autenticado.
     */
    @PostMapping("/pairing-code")
    public ResponseEntity<?> createPairingCode(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        String code = pairingCodeService.create(user);
        return ResponseEntity.ok(Map.of(
                "code", code,
                "expiresInMinutes", pairingCodeService.ttlMinutes()));
    }

    // --- Registo de dispositivos (RF17) ---

    @PostMapping("/register")
    public ResponseEntity<?> registerNode(@Valid @RequestBody RegisterNodeRequest request,
                                          @AuthenticationPrincipal JWTUserData principal) {
        User owner = resolveUser(principal);
        if (owner == null && request.pairingCode() != null && !request.pairingCode().isBlank()) {
            Long userId = pairingCodeService.consume(request.pairingCode());
            owner = userRepository.findById(userId).orElse(null);
        }
        if (owner == null) {
            return ResponseEntity.status(401).body(Map.of("message",
                    "É necessário um código de pareamento válido ou sessão autenticada para registar um dispositivo."));
        }

        NodeManagementService.RegisteredNode registered = nodeManagementService.registerDevice(owner, request);
        StorageNode node = registered.node();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nodeID", node.getNodeID());
        body.put("name", node.getName());
        body.put("type", node.getType().name());
        body.put("status", node.getStatus().name());
        body.put("deviceToken", registered.deviceToken());
        return ResponseEntity.ok(body);
    }

    // --- Listagem de dispositivos e da nuvem do sistema (RF17.6/RF18/RF19) ---

    @GetMapping
    public ResponseEntity<?> listNodes(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Não autenticado."));
        }
        return ResponseEntity.ok(nodeRepository.findAllByOwnerAndTypeOrderByCreatedAtDesc(user,
                        com.assemblyproject.cloudsystem.entity.NodeType.DEVICE).stream()
                .map(StorageNodeResponse::from)
                .toList());
    }

    @GetMapping("/system")
    public ResponseEntity<?> systemNode(@AuthenticationPrincipal JWTUserData principal) {
        if (resolveUser(principal) == null) return unauthorized();
        return ResponseEntity.ok(StorageNodeResponse.from(systemNodeService.getOrCreateSystemNode()));
    }

    @PostMapping("/{nodeId}/enable")
    public ResponseEntity<?> enableNode(@PathVariable Long nodeId,
                                        @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        try {
            return ResponseEntity.ok(StorageNodeResponse.from(nodeManagementService.setEnabled(user, nodeId, true)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{nodeId}/disable")
    public ResponseEntity<?> disableNode(@PathVariable Long nodeId,
                                         @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        try {
            return ResponseEntity.ok(StorageNodeResponse.from(nodeManagementService.setEnabled(user, nodeId, false)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{nodeId}")
    public ResponseEntity<?> removeNode(@PathVariable Long nodeId,
                                        @RequestParam(value = "migrate", defaultValue = "false") boolean migrate,
                                        @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        try {
            nodeManagementService.removeDevice(user, nodeId, migrate);
            return ResponseEntity.ok(Map.of("removed", true, "migrated", migrate));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{nodeId}/migrate")
    public ResponseEntity<?> migrateNode(@PathVariable Long nodeId,
                                         @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        try {
            StorageNode node = nodeManagementService.requireOwnedDevice(user, nodeId);
            int migrated = nodeManagementService.migrateReplicas(node);
            return ResponseEntity.ok(Map.of("migrated", migrated));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }

    // --- Sincronização (RF28) ---

    @GetMapping("/{nodeId}/sync-status")
    public ResponseEntity<?> syncStatus(@PathVariable Long nodeId,
                                        @AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return unauthorized();
        StorageNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) return notFound();
        if (!node.isSystem() && !nodeStorageService.canAccess(node, user)) return forbidden();

        List<FileReplica> replicas = fileReplicaRepository.findAllByNode(node);
        long synced = replicas.stream().filter(r -> r.getStatus() == ReplicaStatus.SYNCED).count();
        long pending = replicas.stream().filter(r -> r.getStatus() == ReplicaStatus.PENDING).count();
        long failed = replicas.stream().filter(r -> r.getStatus() == ReplicaStatus.FAILED).count();

        List<Map<String, Object>> items = new ArrayList<>();
        for (FileReplica replica : replicas) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fileID", replica.getFile() != null ? replica.getFile().getFileID() : null);
            item.put("fileName", replica.getFile() != null ? replica.getFile().getOriginalName() : null);
            item.put("status", replica.getStatus() != null ? replica.getStatus().name() : null);
            item.put("updatedAt", replica.getUpdatedAt() != null ? replica.getUpdatedAt().toString() : null);
            items.add(item);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nodeID", node.getNodeID());
        body.put("nodeName", node.getName());
        body.put("online", node.getStatus() == NodeStatus.ONLINE);
        body.put("total", replicas.size());
        body.put("synced", synced);
        body.put("pending", pending);
        body.put("failed", failed);
        body.put("overall", failed > 0 ? "ERROR" : pending > 0 ? "PENDING" : "SYNCED");
        body.put("items", items);
        return ResponseEntity.ok(body);
    }

    // --- Configuração e consulta de espaço (RF19) ---

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
        return ResponseEntity.ok(StorageNodeResponse.from(nodeRepository.save(node)));
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

    /**
     * O agente local anuncia o seu endereço público (IP:porta ou URL de túnel)
     * para que o servidor central o consiga alcançar. Autenticado pelo token do
     * dispositivo ({@code X-Node-Token}), não pela sessão do utilizador.
     */
    @PostMapping("/{nodeId}/address")
    public ResponseEntity<?> updateAddress(@PathVariable Long nodeId,
                                           @RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-Node-Token", required = false) String deviceToken) {
        try {
            StorageNode node = nodeManagementService.updateAddress(nodeId, deviceToken, body.get("address"));
            return ResponseEntity.ok(Map.of(
                    "nodeID", node.getNodeID(),
                    "address", node.getSessionId() == null ? "" : node.getSessionId()));
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
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
        if (node.isSystem()) return forbidden();
        if (!isOwner(node, user)) return forbidden();
        Long quota = body.get("quotaBytes");
        if (quota != null && quota < node.getUsedSpaceBytes()) {
            return ResponseEntity.badRequest().body(Map.of("message", "A quota não pode ser inferior ao espaço usado."));
        }
        if (quota != null) node.setTotalSpaceBytes(quota);
        return ResponseEntity.ok(StorageNodeResponse.from(nodeRepository.save(node)));
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

    // --- Partilha de um dispositivo com outros utilizadores ---

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
            Path jarPath = resolveNodeAgentJar();
            if (jarPath == null) {
                return ResponseEntity.status(404).body(new byte[0]);
            }
            byte[] jarBytes = Files.readAllBytes(jarPath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cloudsystem-node.jar\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(jarBytes.length)
                    .body(jarBytes);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new byte[0]);
        }
    }

    /**
     * Localiza o jar do agente standalone a servir em {@code /nodes/jar}.
     * Procura, por ordem, o caminho configurado em
     * {@code cloud.system.node-agent-jar} e as localizações habituais de build
     * (incluindo o classifier {@code -node} gerado pelo maven-jar-plugin).
     */
    private Path resolveNodeAgentJar() {
        String configured = nodeAgentJarPath;
        List<String> candidates = new ArrayList<>();
        if (configured != null && !configured.isBlank()) {
            candidates.add(configured);
        }
        candidates.add("cloudsystem-node.jar");
        candidates.add("target/cloudsystem-node.jar");
        candidates.add("target/cloudsystem-0.0.1-SNAPSHOT-node.jar");
        for (String candidate : candidates) {
            Path path = Paths.get(candidate);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return path;
            }
        }
        return null;
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
    public ResponseEntity<List<StorageNodeResponse>> listOffline() {
        return ResponseEntity.ok(heartbeatMonitorService.getOfflineNodes().stream()
                .map(StorageNodeResponse::from)
                .toList());
    }

    @GetMapping("/degraded")
    public ResponseEntity<List<StorageNodeResponse>> listDegraded() {
        return ResponseEntity.ok(heartbeatMonitorService.getDegradedNodes().stream()
                .map(StorageNodeResponse::from)
                .toList());
    }

    private boolean isOwner(StorageNode node, User user) {
        return node.getOwner() != null && user != null
                && node.getOwner().getUserID().equals(user.getUserID());
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
