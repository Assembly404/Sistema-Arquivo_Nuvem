package com.assemblyproject.cloudsystem.dto.request;

/**
 * Pedido de criação de um link público (RF15). Deve indicar exatamente um
 * ficheiro ou uma pasta. {@code expiresInHours} e {@code password} são opcionais.
 */
public record PublicLinkCreateRequest(
        Long fileId,
        Long folderId,
        Integer expiresInHours,
        String password
) {
}
