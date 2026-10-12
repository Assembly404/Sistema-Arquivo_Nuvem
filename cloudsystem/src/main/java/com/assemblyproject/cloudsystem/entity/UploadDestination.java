package com.assemblyproject.cloudsystem.entity;

/**
 * Espaço de destino escolhido pelo utilizador no momento do upload (RF20).
 * <ul>
 *   <li>{@link #AUTO} — respeita a política padrão definida pelo utilizador (RF22).</li>
 *   <li>{@link #SYSTEM} — força o armazenamento na nuvem oficial do sistema.</li>
 *   <li>{@link #DEVICE} — força o armazenamento num dispositivo pessoal concreto.</li>
 * </ul>
 */
public enum UploadDestination {
    AUTO,
    SYSTEM,
    DEVICE
}
