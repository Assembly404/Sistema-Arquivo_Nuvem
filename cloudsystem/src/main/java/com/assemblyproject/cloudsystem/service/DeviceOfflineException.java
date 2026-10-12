package com.assemblyproject.cloudsystem.service;

/**
 * Lançada quando o destino escolhido é um dispositivo pessoal que está offline
 * (ou desativado) e a política do utilizador é negar o acesso (RF24/RF23).
 */
public class DeviceOfflineException extends RuntimeException {
    private final Long nodeID;

    public DeviceOfflineException(String message, Long nodeID) {
        super(message);
        this.nodeID = nodeID;
    }

    public Long getNodeID() {
        return nodeID;
    }
}
