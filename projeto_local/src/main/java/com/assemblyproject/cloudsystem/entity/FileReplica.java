package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "FileReplicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileReplica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long replicaID;

    @ManyToOne
    @JoinColumn(name = "fileID", nullable = false)
    private FileMetadata file;

    @ManyToOne
    @JoinColumn(name = "nodeID", nullable = false)
    private StorageNode node;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReplicaStatus status = ReplicaStatus.PENDING;

    @Column(length = 255)
    private String remotePath;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
