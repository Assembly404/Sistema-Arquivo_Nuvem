package com.assemblyproject.cloudsystem.dto.response;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.FileReplica;
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
    private Long storedSizeBytes;
    private String checksumSha256;
    /** Algoritmo de compressão usado no container .dbs (NONE/DEFLATE/GZIP). */
    private String compressionAlgorithm;
    /** Percentagem de espaço poupada pela compressão (0 se não aplicável). */
    private Integer compressionRatio;
    private Integer containerVersion;
    private Long folderID;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private List<ReplicaInfo> replicas;
    /** Espaço onde o ficheiro está guardado: SYSTEM ou DEVICE (null se sem réplica). */
    private String space;

    public FileMetadataResponse() {
    }

    public static FileMetadataResponse fromEntity(FileMetadata metadata) {
        FileMetadataResponse dto = new FileMetadataResponse();
        dto.fileID = metadata.getFileID();
        dto.originalName = metadata.getOriginalName();
        dto.mimeType = metadata.getMimeType();
        dto.originalSizeBytes = metadata.getOriginalSizeBytes();
        dto.storedSizeBytes = metadata.getStoredSizeBytes();
        dto.checksumSha256 = metadata.getChecksumSha256();
        dto.containerVersion = metadata.getContainerVersion();
        dto.folderID = metadata.getFolder() != null ? metadata.getFolder().getFolderID() : null;
        dto.uploadedAt = metadata.getUploadedAt();
        dto.updatedAt = metadata.getUpdatedAt();
        dto.deletedAt = metadata.getDeletedAt();
        List<FileReplica> replicas = metadata.getReplicas();
        dto.replicas = replicas == null ? List.of() : replicas.stream()
                .map(r -> new ReplicaInfo(
                        r.getNode() != null ? r.getNode().getNodeID() : null,
                        r.getNode() != null ? r.getNode().getName() : "Unknown",
                        r.getNode() != null && r.getNode().getType() != null ? r.getNode().getType().name() : null,
                        r.getNode() != null && r.getNode().getStatus() == NodeStatus.ONLINE,
                        r.getStatus()))
                .collect(Collectors.toList());
        dto.space = replicas == null ? null : replicas.stream()
                .map(FileReplica::getNode)
                .filter(n -> n != null && n.getType() != null)
                .findFirst()
                .map(n -> n.getType().name())
                .orElse(null);
        return dto;
    }
}