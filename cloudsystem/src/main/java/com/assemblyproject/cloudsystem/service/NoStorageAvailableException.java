package com.assemblyproject.cloudsystem.service;

/**
 * Lançada quando não existe nenhum espaço (nuvem do sistema ou dispositivo)
 * disponível para guardar uma réplica. Evita o upload "silencioso" sem réplica
 * (perda de dados).
 */
public class NoStorageAvailableException extends RuntimeException {
    public NoStorageAvailableException(String message) {
        super(message);
    }
}
