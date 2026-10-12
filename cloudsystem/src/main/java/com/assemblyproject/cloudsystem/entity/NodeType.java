package com.assemblyproject.cloudsystem.entity;

/**
 * Tipo de nó de armazenamento (RF17).
 * <ul>
 *   <li>{@link #SYSTEM} — nuvem oficial do sistema, gerida pelo servidor. Está
 *   sempre disponível e é onde vive o armazenamento padrão.</li>
 *   <li>{@link #DEVICE} — computador pessoal do utilizador ligado à rede como
 *   espaço adicional (depende da disponibilidade do dispositivo).</li>
 * </ul>
 */
public enum NodeType {
    SYSTEM,
    DEVICE
}
