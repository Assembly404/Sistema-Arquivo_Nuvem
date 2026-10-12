package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "StorageNodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long nodeID;

    /**
     * Dono do dispositivo. Pode ser {@code null} para o nó de sistema, que é
     * partilhado por todos os utilizadores (nuvem oficial).
     */
    @ManyToOne
    @JoinColumn(name = "ownerID")
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Distingue a nuvem do sistema (SYSTEM) dos dispositivos pessoais (DEVICE).
     * Pode ser nulo em registos antigos; nesse caso assume-se DEVICE.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private NodeType type = NodeType.DEVICE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NodeStatus status = NodeStatus.OFFLINE;

    @Column(nullable = false)
    private Long totalSpaceBytes;

    @Column(nullable = false)
    @Builder.Default
    private Long usedSpaceBytes = 0L;

    private LocalDateTime lastHeartbeat;

    @Column(length = 100)
    private String sessionId;

    /**
     * Diretório local escolhido no dispositivo para o armazenamento (RF17.1).
     */
    @Column(length = 1024)
    private String localPath;

    /**
     * Sistema operativo reportado pelo agente local (RF17.8).
     */
    @Column(length = 60)
    private String operatingSystem;

    /**
     * Versão do agente/cliente local (RF17.3).
     */
    @Column(length = 40)
    private String agentVersion;

    /**
     * Espaço livre reportado pelo dispositivo no momento do registo (RF17.8).
     */
    private Long reportedFreeBytes;

    /**
     * Token de vinculação do dispositivo (RF17.4), cifrado de forma reversível
     * com a master key para que o servidor o possa enviar nas chamadas ao agente.
     */
    @Column(columnDefinition = "TEXT")
    private String deviceTokenEncrypted;

    /**
     * Indica se o dispositivo está ativo. Um dispositivo desativado deixa de
     * receber réplicas e de ser usado como destino (RF17.6). Nulo = ativo.
     */
    @Column
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private NodeVisibility visibility = NodeVisibility.PERSONAL;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public long getFreeSpaceBytes() {
        long total = totalSpaceBytes == null ? 0L : totalSpaceBytes;
        long used = usedSpaceBytes == null ? 0L : usedSpaceBytes;
        return total - used;
    }

    public boolean isSystem() {
        return type == NodeType.SYSTEM;
    }

    public boolean isDevice() {
        return type == NodeType.DEVICE;
    }

    public boolean isEnabled() {
        return enabled == null || enabled;
    }
}
