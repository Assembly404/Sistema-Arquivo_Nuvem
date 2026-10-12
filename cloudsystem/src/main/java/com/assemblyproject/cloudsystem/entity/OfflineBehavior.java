package com.assemblyproject.cloudsystem.entity;

/**
 * Comportamento do sistema quando um dispositivo pessoal que guarda o ficheiro
 * está offline (RF24).
 * <ul>
 *   <li>{@link #DENY} — nega o acesso/upload para esse destino e informa que o
 *   dispositivo está offline.</li>
 *   <li>{@link #CACHE} — serve uma cópia em cache na nuvem do sistema, quando
 *   exista, em vez de falhar.</li>
 * </ul>
 */
public enum OfflineBehavior {
    DENY,
    CACHE
}
