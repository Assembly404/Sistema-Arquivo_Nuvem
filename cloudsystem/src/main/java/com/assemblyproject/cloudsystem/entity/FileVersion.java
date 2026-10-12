package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Snapshot de conteudo anterior de um ficheiro. O container cifrado da versao
 * fica guardado localmente no servidor (storage-dir/versions/{fileID}/), de forma
 * a permitir restaurar versoes antigas independentemente do estado dos nos.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long versionID;

    @ManyToOne
    @JoinColumn(name = "fileID", nullable = false)
    private FileMetadata file;

    @Column(nullable = false)
    private Integer versionNumber;

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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedDataKey;

    @Column(nullable = false, length = 64)
    private String keyEncryptionIv;

    @Column(length = 500)
    private String localPath;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
