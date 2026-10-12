package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import com.assemblyproject.cloudsystem.entity.NodeType;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.SystemNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/storage")
public class StorageController {

    private static final Set<String> VALID_POLICIES = Set.of("SYSTEM_FIRST", "DEVICE_FIRST", "MANUAL");
    private static final Map<String, String> POLICY_LABELS = Map.of(
            "SYSTEM_FIRST", "Nuvem do sistema primeiro",
            "DEVICE_FIRST", "Dispositivo pessoal primeiro",
            "MANUAL", "Escolher manualmente no upload");

    private static final Set<String> VALID_OFFLINE_BEHAVIORS = Set.of("DENY", "CACHE");
    private static final Map<String, String> OFFLINE_LABELS = Map.of(
            "DENY", "Negar acesso ao ficheiro",
            "CACHE", "Servir cópia em cache quando exista");

    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final StorageNodeRepository storageNodeRepository;
    private final SystemNodeService systemNodeService;

    @GetMapping("/summary")
    public ResponseEntity<?> summary(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();

        long usedBytes = fileMetadataRepository.sumOriginalSizeBytesByOwner(user);
        long fileCount = fileMetadataRepository.countByOwnerAndDeletedAtIsNull(user);
        Long quotaBytes = normalizeQuota(user.getQuotaBytes());

        StorageNode systemNode = systemNodeService.getOrCreateSystemNode();
        List<Map<String, Object>> devices = storageNodeRepository
                .findAllByOwnerAndTypeOrderByCreatedAtDesc(user, NodeType.DEVICE)
                .stream()
                .map(this::toNodeMap)
                .collect(Collectors.toList());

        long deviceTotal = devices.stream().mapToLong(d -> ((Number) d.get("totalBytes")).longValue()).sum();
        long deviceUsed = devices.stream().mapToLong(d -> ((Number) d.get("usedBytes")).longValue()).sum();

        Map<String, Object> systemMap = Map.of(
                "nodeID", systemNode.getNodeID(),
                "name", systemNode.getName(),
                "online", true,
                "totalBytes", systemNode.getTotalSpaceBytes() == null ? 0L : systemNode.getTotalSpaceBytes(),
                "usedBytes", systemNode.getUsedSpaceBytes() == null ? 0L : systemNode.getUsedSpaceBytes(),
                "unlimited", systemNode.getTotalSpaceBytes() == null || systemNode.getTotalSpaceBytes() <= 0);

        return ResponseEntity.ok(Map.of(
                "cloud", Map.of(
                        "usedBytes", usedBytes,
                        "quotaBytes", quotaBytes,
                        "hasQuota", user.getQuotaBytes() != null,
                        "fileCount", fileCount),
                "system", systemMap,
                "nodes", devices,
                "devices",
                Map.of("totalBytes", deviceTotal, "usedBytes", deviceUsed,
                        "count", devices.size(),
                        "online", devices.stream().filter(d -> Boolean.TRUE.equals(d.get("online"))).count()),
                "policy", normalizePolicy(user.getStoragePolicy()),
                "policyLabel", POLICY_LABELS.get(normalizePolicy(user.getStoragePolicy())),
                "offlineBehavior", normalizeOffline(user.getOfflineBehavior()),
                "offlineBehaviorLabel", OFFLINE_LABELS.get(normalizeOffline(user.getOfflineBehavior()))));
    }

    @GetMapping("/policy")
    public ResponseEntity<?> getPolicy(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        String policy = normalizePolicy(user.getStoragePolicy());
        return ResponseEntity.ok(Map.of("policy", policy, "label", POLICY_LABELS.get(policy)));
    }

    @PutMapping("/policy")
    public ResponseEntity<?> setPolicy(@AuthenticationPrincipal JWTUserData principal,
                                       @RequestBody Map<String, String> body) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        String policy = body.get("policy");
        if (policy == null || !VALID_POLICIES.contains(policy)) {
            return ResponseEntity.status(400).body(Map.of("message", "Política inválida. Use SYSTEM_FIRST, DEVICE_FIRST ou MANUAL."));
        }
        user.setStoragePolicy(policy);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("policy", policy, "label", POLICY_LABELS.get(policy)));
    }

    @PutMapping("/offline-behavior")
    public ResponseEntity<?> setOfflineBehavior(@AuthenticationPrincipal JWTUserData principal,
                                                @RequestBody Map<String, String> body) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        String behavior = body.get("offlineBehavior");
        if (behavior == null) behavior = body.get("behavior");
        if (behavior == null) {
            return ResponseEntity.status(400).body(Map.of("message", "Comportamento inválido. Use DENY ou CACHE."));
        }
        String normalized = behavior.toUpperCase();
        if (!VALID_OFFLINE_BEHAVIORS.contains(normalized)) {
            return ResponseEntity.status(400).body(Map.of("message", "Comportamento inválido. Use DENY ou CACHE."));
        }
        user.setOfflineBehavior(normalized);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("offlineBehavior", normalized, "label", OFFLINE_LABELS.get(normalized)));
    }

    private Map<String, Object> toNodeMap(StorageNode node) {
        long total = node.getTotalSpaceBytes() == null ? 0L : node.getTotalSpaceBytes();
        long used = node.getUsedSpaceBytes() == null ? 0L : node.getUsedSpaceBytes();
        long free = Math.max(total - used, 0L);
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("nodeID", node.getNodeID());
        map.put("name", node.getName());
        map.put("type", node.getType() != null ? node.getType().name() : "DEVICE");
        map.put("status", node.getStatus() != null ? node.getStatus().name() : "OFFLINE");
        map.put("online", node.getStatus() != null && node.getStatus().name().equals("ONLINE"));
        map.put("enabled", node.isEnabled());
        map.put("totalBytes", total);
        map.put("usedBytes", used);
        map.put("freeBytes", free);
        map.put("usagePercent", total > 0 ? (int) ((used * 100) / total) : 0);
        map.put("lastHeartbeat", node.getLastHeartbeat() != null ? node.getLastHeartbeat().toString() : null);
        map.put("localPath", node.getLocalPath());
        map.put("operatingSystem", node.getOperatingSystem());
        map.put("visibility", node.getVisibility() != null ? node.getVisibility().name() : "PERSONAL");
        return map;
    }

    private Long normalizeQuota(Long quotaBytes) {
        return quotaBytes != null && quotaBytes > 0 ? quotaBytes : null;
    }

    private String normalizePolicy(String policy) {
        return policy != null && VALID_POLICIES.contains(policy) ? policy : "SYSTEM_FIRST";
    }

    private String normalizeOffline(String behavior) {
        return behavior != null && VALID_OFFLINE_BEHAVIORS.contains(behavior.toUpperCase())
                ? behavior.toUpperCase() : "DENY";
    }

    private User resolveUser(JWTUserData principal) {
        if (principal == null || principal.userID() == null) return null;
        return userRepository.findById(principal.userID()).orElse(null);
    }
}
