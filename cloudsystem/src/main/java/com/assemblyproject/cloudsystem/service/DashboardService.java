package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.FileReplicaRepository;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final StorageNodeRepository nodeRepository;
    private final FileReplicaRepository fileReplicaRepository;

    public Map<String, Object> getStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countBySuspendedFalse();
        long totalFiles = fileMetadataRepository.count();
        long totalStorageBytes = fileMetadataRepository.sumStoredSizeBytes();
        long totalNodes = nodeRepository.count();
        long onlineNodes = nodeRepository.countByStatus(NodeStatus.ONLINE);
        long offlineNodes = nodeRepository.countByStatus(NodeStatus.OFFLINE);
        long degradedNodes = nodeRepository.countByStatus(NodeStatus.DEGRADED);
        long totalReplicas = fileReplicaRepository.count();

        Map<String, Object> usageOverTime = buildUsageOverTime(totalFiles, totalStorageBytes);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalFiles", totalFiles);
        stats.put("totalStorageBytes", totalStorageBytes);
        stats.put("totalNodes", totalNodes);
        stats.put("onlineNodes", onlineNodes);
        stats.put("offlineNodes", offlineNodes);
        stats.put("degradedNodes", degradedNodes);
        stats.put("totalReplicas", totalReplicas);
        stats.put("usageOverTime", usageOverTime);
        stats.put("generatedAt", LocalDateTime.now());
        return stats;
    }

    public List<Map<String, Object>> getStorageByNode() {
        return nodeRepository.findAll().stream().map(node -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nodeID", node.getNodeID());
            m.put("name", node.getName());
            m.put("status", node.getStatus().name());
            m.put("visibility", node.getVisibility().name());
            m.put("totalBytes", node.getTotalSpaceBytes());
            m.put("usedBytes", node.getUsedSpaceBytes());
            m.put("freeBytes", node.getFreeSpaceBytes());
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getReplicasByNode() {
        return nodeRepository.findAll().stream().map(node -> {
            List<com.assemblyproject.cloudsystem.entity.FileReplica> replicas = fileReplicaRepository.findAllByNode(node);
            long synced = replicas.stream().filter(r -> r.getStatus() == com.assemblyproject.cloudsystem.entity.ReplicaStatus.SYNCED).count();
            long pending = replicas.stream().filter(r -> r.getStatus() == com.assemblyproject.cloudsystem.entity.ReplicaStatus.PENDING).count();
            long failed = replicas.stream().filter(r -> r.getStatus() == com.assemblyproject.cloudsystem.entity.ReplicaStatus.FAILED).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nodeID", node.getNodeID());
            m.put("name", node.getName());
            m.put("synced", synced);
            m.put("pending", pending);
            m.put("failed", failed);
            return m;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> buildUsageOverTime(long totalFiles, long totalStorageBytes) {
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("labels", List.of("hoje", "esta semana", "este mês"));
        usage.put("filesUploaded", List.of(totalFiles, totalFiles, totalFiles));
        usage.put("storageBytes", List.of(totalStorageBytes, totalStorageBytes, totalStorageBytes));
        return usage;
    }
}
