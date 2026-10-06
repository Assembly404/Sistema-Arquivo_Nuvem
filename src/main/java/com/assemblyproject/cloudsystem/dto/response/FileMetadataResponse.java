package com.assemblyproject.cloudsystem.dto.response;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.ReplicaStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class FileMetadataResponse {
    private Long fileID;
    private String originalName;
    private String mimeType;
    private Long originalSizeBytes;
    private LocalDateTime uploadedAt;
    private List<ReplicaInfo> replicas;

    public FileMetadataResponse() {
    }

    public static FileMetadataResponse fromEntity(FileMetadata metadata) {
        FileMetadataResponse dto = new FileMetadataResponse();
        dto.fileID = metadata.getFileID();
        dto.originalName = metadata.getOriginalName();
        dto.mimeType = metadata.getMimeType();
        dto.originalSizeBytes = metadata.getOriginalSizeBytes();
        dto.uploadedAt = metadata.getUploadedAt();
        dto.replicas = metadata.getReplicas().stream()
                .map(r -> new ReplicaInfo(
                        r.getNode() != null ? r.getNode().getNodeID() : null,
                        r.getNode() != null ? r.getNode().getName() : "Unknown",
                        r.getStatus()
                ))
                .collect(Collectors.toList());
        return dto;
    }
}