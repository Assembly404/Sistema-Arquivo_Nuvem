package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Link público (RF15) que dá acesso de leitura a um ficheiro ou pasta sem
 * necessidade de conta. Pode ter expiração e/ou senha opcionais e ser revogado
 * a qualquer momento pelo dono.
 */
@Entity(name = "PublicLinks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long linkID;

    /**
     * Token opaco presente no URL (`/s/{token}`). Único e não sequencial.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne
    @JoinColumn(name = "ownerID", nullable = false)
    private User owner;

    @ManyToOne
    @JoinColumn(name = "fileID")
    private FileMetadata file;

    @ManyToOne
    @JoinColumn(name = "folderID")
    private Folder folder;

    /**
     * Hash BCrypt da senha (opcional). {@code null} = sem senha.
     */
    @Column(length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Data de expiração. {@code null} = sem expiração.
     */
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(nullable = false)
    @Builder.Default
    private Long accessCount = 0L;

    private LocalDateTime lastAccessAt;

    public boolean isPasswordProtected() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }
}
