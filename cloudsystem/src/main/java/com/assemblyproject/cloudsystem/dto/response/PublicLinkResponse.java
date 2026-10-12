package com.assemblyproject.cloudsystem.dto.response;

import com.assemblyproject.cloudsystem.entity.PublicLink;

import java.time.LocalDateTime;

/**
 * Link público tal como apresentado ao dono (inclui o token para construir o URL).
 */
public record PublicLinkResponse(
        Long linkID,
        String token,
        String type,
        Long itemID,
        String name,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean revoked,
        boolean expired,
        boolean passwordProtected,
        Long accessCount,
        LocalDateTime lastAccessAt
) {

    public static PublicLinkResponse from(PublicLink link) {
        boolean isFile = link.getFile() != null;
        return new PublicLinkResponse(
                link.getLinkID(),
                link.getToken(),
                isFile ? "FILE" : "FOLDER",
                isFile ? link.getFile().getFileID() : link.getFolder().getFolderID(),
                isFile ? link.getFile().getOriginalName() : link.getFolder().getName(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.isRevoked(),
                link.isExpired(),
                link.isPasswordProtected(),
                link.getAccessCount(),
                link.getLastAccessAt());
    }
}
