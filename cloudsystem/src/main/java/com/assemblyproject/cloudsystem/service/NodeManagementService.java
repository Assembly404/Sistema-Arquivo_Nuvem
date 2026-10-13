package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.dto.request.RegisterNodeRequest;
import com.assemblyproject.cloudsystem.entity.FileReplica;
import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.NodeType;
import com.assemblyproject.cloudsystem.entity.NodeVisibility;
import com.assemblyproject.cloudsystem.entity.NotificationType;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.UploadDestination;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileReplicaRepository;
import com.assemblyproject.cloudsystem.repository.NodeAccessRepository;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestão do ciclo de vida dos dispositivos pessoais como nós de armazenamento
 * (RF17): registo com vinculação segura, validação de requisitos mínimos,
 * ativação/desativação, remoção com migração e cálculo de espaços.
 */
@Service
@RequiredArgsConstructor
public class NodeManagementService {

    private static final Logger log = LoggerFactory.getLogger(NodeManagementService.class);

    private final StorageNodeRepository nodeRepository;
    private final NodeAccessRepository nodeAccessRepository;
    private final FileReplicaRepository fileReplicaRepository;
    private final StorageNodeService nodeStorageService;
    private final NodeSecretService nodeSecretService;
    private final ContainerClient containerClient;
    private final ReplicationService replicationService;
    private final NotificationService notificationService;

    public record RegisteredNode(StorageNode node, String deviceToken) {
    }

    @Transactional
    public RegisteredNode registerDevice(User owner, RegisterNodeRequest request) {
        if (owner == null) {
            throw new IllegalArgumentException("É necessário estar autenticado para registar um dispositivo.");
        }
        if (request.localPath() == null || request.localPath().isBlank()) {
            throw new IllegalArgumentException("Indica a pasta do dispositivo a usar como armazenamento (RF17.1).");
        }

        long totalBytes = resolveTotalBytes(request);
        if (totalBytes <= 0) {
            throw new IllegalArgumentException("Define o espaço em disco a disponibilizar (RF17.2).");
        }
        validateRequirements(request, totalBytes);

        String token = nodeSecretService.generateToken();
        StorageNode node = StorageNode.builder()
                .owner(owner)
                .name(request.name().trim())
                .type(NodeType.DEVICE)
                .status(NodeStatus.ONLINE)
                .totalSpaceBytes(totalBytes)
                .usedSpaceBytes(0L)
                .visibility(parseVisibility(request.visibility()))
                .sessionId(normalizeAddress(request.address()))
                .localPath(request.localPath().trim())
                .operatingSystem(trimToNull(request.operatingSystem()))
                .agentVersion(trimToNull(request.agentVersion()))
                .reportedFreeBytes(request.reportedFreeBytes())
                .deviceTokenEncrypted(nodeSecretService.encrypt(token))
                .enabled(true)
                .lastHeartbeat(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        StorageNode saved = nodeRepository.save(node);

        // Verifica se o servidor consegue realmente alcançar o agente. Sem isto
        // o dispositivo aparecia como ONLINE mesmo estando atrás de NAT, e as
        // operações seguintes (replicação/leitura) falhavam com "não foi
        // possível aceder ao armazenamento".
        if (!safeIsReachable(saved)) {
            saved.setStatus(NodeStatus.DEGRADED);
            saved = nodeRepository.save(saved);
            log.warn("[node] Dispositivo {} registado, mas ainda não acessível em '{}'. "
                            + "Se o servidor está na nuvem e o PC está atrás de NAT, configura "
                            + "cloud.node.public-url (túnel cloudflared/ngrok) no agente.",
                    saved.getNodeID(), saved.getSessionId());
        }

        log.info("Dispositivo '{}' registado para o utilizador {} (nó {}).",
                saved.getName(), owner.getUserID(), saved.getNodeID());
        return new RegisteredNode(saved, token);
    }

    /**
     * Testa, de forma segura, se o servidor consegue chegar ao agente do
     * dispositivo. Devolve {@code false} se não houver endereço ou se a ligação
     * falhar (nunca propaga exceções para não impedir o registo).
     */
    private boolean safeIsReachable(StorageNode node) {
        if (node.getSessionId() == null || node.getSessionId().isBlank()) {
            return false;
        }
        try {
            return containerClient.isNodeOnline(node);
        } catch (Exception e) {
            return false;
        }
    }

    private long resolveTotalBytes(RegisterNodeRequest request) {
        if (request.totalSpaceBytes() != null && request.totalSpaceBytes() > 0) {
            return request.totalSpaceBytes();
        }
        if (request.reportedFreeGb() != null && request.reportedFreeGb() > 0) {
            return Math.round(request.reportedFreeGb() * 1024 * 1024 * 1024);
        }
        return 0L;
    }

    /**
     * Valida requisitos mínimos do dispositivo (RF17.8): sistema operativo
     * suportado e espaço livre suficiente para a quota pedida.
     */
    private void validateRequirements(RegisterNodeRequest request, long totalBytes) {
        String os = request.operatingSystem();
        if (os != null && !os.isBlank()) {
            String lower = os.toLowerCase();
            boolean supported = lower.contains("windows") || lower.contains("linux")
                    || lower.contains("mac") || lower.contains("os x");
            if (!supported) {
                throw new IllegalArgumentException(
                        "Sistema operativo não suportado: " + os + ". Suportados: Windows, macOS e Linux.");
            }
        }
        if (request.reportedFreeBytes() != null && request.reportedFreeBytes() > 0
                && totalBytes > request.reportedFreeBytes()) {
            throw new IllegalArgumentException(
                    "Espaço livre insuficiente no dispositivo: pediste " + totalBytes
                            + " bytes mas só há " + request.reportedFreeBytes() + " livres.");
        }
    }

    @Transactional
    public StorageNode setEnabled(User user, Long nodeId, boolean enabled) {
        StorageNode node = requireOwnedDevice(user, nodeId);
        node.setEnabled(enabled);
        if (!enabled) {
            node.setStatus(NodeStatus.OFFLINE);
        }
        StorageNode saved = nodeRepository.save(node);
        notificationService.notify(user, NotificationType.SYSTEM,
                enabled ? "Dispositivo ativado" : "Dispositivo desativado",
                "O dispositivo \"" + node.getName() + "\" foi "
                        + (enabled ? "ativado" : "desativado") + " como espaço de armazenamento.",
                "/dashboard/armazenamento");
        return saved;
    }

    @Transactional
    public void removeDevice(User user, Long nodeId, boolean migrate) {
        StorageNode node = requireOwnedDevice(user, nodeId);
        if (migrate) {
            migrateReplicas(node);
        }
        for (FileReplica replica : fileReplicaRepository.findAllByNode(node)) {
            try {
                containerClient.deleteReplica(node, replica.getFile().getFileID());
            } catch (Exception ignored) {
                // ficheiro já ausente
            }
            fileReplicaRepository.delete(replica);
        }
        nodeAccessRepository.findAllByNode(node).forEach(nodeAccessRepository::delete);
        nodeStorageService.deleteNodeStorage(node);
        nodeRepository.delete(node);
        notificationService.notify(user, NotificationType.SYSTEM,
                "Dispositivo removido",
                "O dispositivo \"" + node.getName() + "\" deixou de ser usado como armazenamento."
                        + (migrate ? " Os ficheiros foram migrados para a nuvem do sistema." : ""),
                "/dashboard/armazenamento");
        log.info("Dispositivo {} removido (migração={}).", nodeId, migrate);
    }

    /**
     * Migra as réplicas guardadas num dispositivo para a nuvem do sistema antes
     * da remoção (RF17.7). Requer que o dispositivo esteja acessível para ler as
     * réplicas.
     */
    @Transactional
    public int migrateReplicas(StorageNode node) {
        List<FileReplica> replicas = fileReplicaRepository.findAllByNode(node);
        int migrated = 0;
        for (FileReplica replica : replicas) {
            if (replica.getFile() == null) {
                continue;
            }
            Long fileID = replica.getFile().getFileID();
            byte[] container;
            try {
                container = containerClient.downloadReplica(node, fileID);
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Não foi possível migrar \"" + replica.getFile().getOriginalName()
                                + "\": o dispositivo \"" + node.getName()
                                + "\" tem de estar online. Liga-o e tenta novamente.");
            }
            replicationService.replicateFile(fileID, node.getOwner(), container,
                    UploadDestination.SYSTEM, null);
            try {
                containerClient.deleteReplica(node, fileID);
            } catch (Exception ignored) {
                // ficheiro já ausente
            }
            // A réplica deixa de existir no dispositivo: a cópia autoritativa
            // está agora na nuvem do sistema.
            fileReplicaRepository.delete(replica);
            migrated++;
        }
        return migrated;
    }

    /**
     * Atualiza o endereço público de um dispositivo já registado. Autentica o
     * pedido pelo token do dispositivo (não pela sessão do utilizador), porque
     * quem chama é o próprio agente local ao arrancar. Isto permite que o
     * servidor central alcance nós que estejam atrás de NAT/túnel e mantém o
     * endereço fresco quando o URL do túnel muda.
     */
    @Transactional
    public StorageNode updateAddress(Long nodeId, String deviceToken, String address) {
        StorageNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null || node.isSystem()) {
            throw new IllegalArgumentException("Nó não encontrado.");
        }
        if (!nodeSecretService.matches(node.getDeviceTokenEncrypted(), deviceToken)) {
            throw new SecurityException("Token do dispositivo inválido.");
        }
        node.setSessionId(normalizeAddress(address));
        StorageNode saved = nodeRepository.save(node);
        log.info("[node] Endereço do dispositivo {} atualizado para {}.", nodeId, saved.getSessionId());
        return saved;
    }

    public StorageNode requireOwnedDevice(User user, Long nodeId) {
        StorageNode node = nodeRepository.findById(nodeId).orElse(null);
        if (node == null) {
            throw new IllegalArgumentException("Nó não encontrado.");
        }
        if (node.isSystem()) {
            throw new IllegalArgumentException("O nó de sistema não pode ser gerido desta forma.");
        }
        if (node.getOwner() == null || user == null
                || !node.getOwner().getUserID().equals(user.getUserID())) {
            throw new IllegalArgumentException("O dispositivo não te pertence.");
        }
        return node;
    }

    private NodeVisibility parseVisibility(String value) {
        if (value == null || value.isBlank()) {
            return NodeVisibility.PERSONAL;
        }
        try {
            return NodeVisibility.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NodeVisibility.PERSONAL;
        }
    }

    private String normalizeAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String value = address.trim();
        // Endereço completo (com esquema) — usado por túneis públicos
        // (ex.: https://meu-pc.trycloudflare.com). Preserva o esquema.
        if (value.startsWith("http://") || value.startsWith("https://")) {
            while (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            return value;
        }
        // Endereço simples: host[:porta]. Assume http e a porta padrão do nó.
        if (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        if (!value.contains(":")) value = value + ":8081";
        return value;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
