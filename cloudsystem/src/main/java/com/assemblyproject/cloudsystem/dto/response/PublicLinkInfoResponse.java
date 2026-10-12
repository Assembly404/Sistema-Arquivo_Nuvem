package com.assemblyproject.cloudsystem.dto.response;

import java.time.LocalDateTime;

/**
 * Metadados de um link público acessível sem autenticação. Não revela a chave
 * de dados nem o conteúdo do ficheiro.
 */
public record PublicLinkInfoResponse(
        String type,
        String name,
        String mimeType,
        Long sizeBytes,
        String ownerName,
        boolean passwordProtected,
        LocalDateTime expiresAt
) {
}
