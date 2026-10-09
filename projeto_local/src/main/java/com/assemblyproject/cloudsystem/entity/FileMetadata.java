package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "FileMetadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileID;

    @ManyToOne
    @JoinColumn(name = "ownerID", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(length = 150)
    private String mimeType;

    @Column(nullable = false)
    private Long originalSizeBytes;

    @Column(nullable = false)
    private Long storedSizeBytes;

    @Column(nullable = false, length = 64)
    private String checksumSha256;

    @Column(nullable = false)
    @Builder.Default
    private Integer containerVersion = 1;

    /**
     * Chave AES-256 gerada por arquivo, cifrada com a master key do servidor.
     * Guardada em Base64.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedDataKey;

    /**
     * IV/nonce usado para cifrar a encryptedDataKey acima (Base64).
     */
    @Column(nullable = false, length = 64)
    private String keyEncryptionIv;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FileReplica> replicas = new ArrayList<>();
}
