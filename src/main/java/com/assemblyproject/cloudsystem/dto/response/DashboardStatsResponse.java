package com.assemblyproject.cloudsystem.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class DashboardStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long totalFiles;
    private long totalStorageBytes;
    private long totalStorageNodes;
    private long onlineNodes;
    private Map<String, Object> usageOverTime;
    private LocalDateTime generatedAt;

    public DashboardStatsResponse(long totalUsers, long activeUsers, long totalFiles, long totalStorageBytes, long totalStorageNodes, long onlineNodes, Map<String, Object> usageOverTime) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.totalFiles = totalFiles;
        this.totalStorageBytes = totalStorageBytes;
        this.totalStorageNodes = totalStorageNodes;
        this.onlineNodes = onlineNodes;
        this.usageOverTime = usageOverTime;
        this.generatedAt = LocalDateTime.now();
    }
}
