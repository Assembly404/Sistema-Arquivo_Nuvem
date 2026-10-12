package com.assemblyproject.cloudsystem.dto.request;

/**
 * Mover para uma pasta. {@code folderId} nulo significa mover para a raiz.
 */
public record MoveRequest(Long folderId) {
}
