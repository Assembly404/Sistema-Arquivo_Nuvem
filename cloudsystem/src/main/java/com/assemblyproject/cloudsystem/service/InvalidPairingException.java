package com.assemblyproject.cloudsystem.service;

/**
 * Lançada quando um código de pareamento (RF17.4) é inválido, expirado ou não
 * corresponde a nenhum utilizador. Impede o registo anónimo de dispositivos.
 */
public class InvalidPairingException extends RuntimeException {
    public InvalidPairingException(String message) {
        super(message);
    }
}
