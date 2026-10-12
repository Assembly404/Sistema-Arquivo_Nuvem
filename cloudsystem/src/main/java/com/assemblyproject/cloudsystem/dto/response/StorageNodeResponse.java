package com.assemblyproject.cloudsystem.dto.response;

import com.assemblyproject.cloudsystem.entity.StorageNode;

import java.time.LocalDateTime;

public record StorageNodeResponse(
        Long nodeID,
        String name,
        String type,
        Boolean system,
        String status,
        String visibility,
        Boolean online,
        Boolean enabled,
        Long totalSpaceBytes,
        Long usedSpaceBytes,
        Long freeBytes,
        Integer usagePercent,
        String sessionId,
        String localPath,
        String operatingSystem,
        String agentVersion,
        Long reportedFreeBytes,
        LocalDateTime lastHeartbeat,
        LocalDateTime createdAt,
        Long ownerId,
        String ownerName,
        String ownerEmail) {

    public static StorageNodeResponse from(StorageNode node) {
        long total = node.getTotalSpaceBytes() == null ? 0L : node.getTotalSpaceBytes();
        long used = node.getUsedSpaceBytes() == null ? 0L : node.getUsedSpaceBytes();
        long free = Math.max(total - used, 0L);
        return new StorageNodeResponse(
                node.getNodeID(),
                node.getName(),
                node.getType() != null ? node.getType().name() : "DEVICE",
                node.isSystem(),
                node.getStatus() != null ? node.getStatus().name() : "OFFLINE",
                node.getVisibility() != null ? node.getVisibility().name() : "PERSONAL",
                node.getStatus() != null && node.getStatus().name().equals("ONLINE"),
                node.isEnabled(),
                total,
                used,
                free,
                total > 0 ? (int) ((used * 100) / total) : 0,
                node.getSessionId(),
                node.getLocalPath(),
                node.getOperatingSystem(),
                node.getAgentVersion(),
                node.getReportedFreeBytes(),
                node.getLastHeartbeat(),
                node.getCreatedAt(),
                node.getOwner() != null ? node.getOwner().getUserID() : null,
                node.getOwner() != null ? node.getOwner().getName() : null,
                node.getOwner() != null ? node.getOwner().getEmail() : null);
    }
}
