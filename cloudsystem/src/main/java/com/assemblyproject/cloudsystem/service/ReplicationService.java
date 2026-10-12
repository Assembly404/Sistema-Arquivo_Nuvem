package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.FileReplica;
import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.NotificationType;
import com.assemblyproject.cloudsystem.entity.ReplicaStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.UploadDestination;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.FileReplicaRepository;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReplicationService {

    private static final Logger log = LoggerFactory.getLogger(ReplicationService.class);

    private final FileMetadataRepository fileMetadataRepository;
    private final FileReplicaRepository fileReplicaRepository;
    private final StorageNodeRepository nodeRepository;
    private final ContainerClient containerClient;
    private final SystemNodeService systemNodeService;
    private final NotificationService notificationService;

    /**
     * Coloca uma réplica do ficheiro segundo a política/destino do utilizador
     * (RF20/RF22). A operação é síncrona: se nenhum espaço aceitar o ficheiro,
     * lança exceção em vez de deixar um upload sem qualquer cópia persistida.
     */
    @Transactional
    public List<StorageNode> replicateFile(Long fileID, User owner, byte[] containerData) {
        return replicateFile(fileID, owner, containerData, UploadDestination.AUTO, null);
    }

    @Transactional
    public List<StorageNode> replicateFile(Long fileID, User owner, byte[] containerData,
                                           UploadDestination destination, Long targetNodeId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileID)
                .orElseThrow(() -> new IllegalArgumentException("FileMetadata not found: " + fileID));

        UploadDestination dest = destination != null ? destination : UploadDestination.AUTO;
        List<StorageNode> targets = resolveTargets(owner, dest, targetNodeId, containerData.length);

        List<StorageNode> stored = new ArrayList<>();
        for (StorageNode node : targets) {
            FileReplica existing = fileReplicaRepository
                    .findByFile_FileIDAndNode_NodeID(metadata.getFileID(), node.getNodeID())
                    .orElse(null);
            boolean isNewReplica = existing == null;
            FileReplica replica = existing != null ? existing : FileReplica.builder()
                    .file(metadata)
                    .node(node)
                    .build();
            replica.setStatus(ReplicaStatus.PENDING);
            replica.setUpdatedAt(LocalDateTime.now());
            replica = fileReplicaRepository.save(replica);
            try {
                containerClient.uploadReplica(node, metadata.getFileID(), containerData);
                replica.setStatus(ReplicaStatus.SYNCED);
                fileReplicaRepository.save(replica);
                if (isNewReplica) {
                    refreshUsedSpace(node, containerData.length);
                }
                stored.add(node);
                log.info("Réplica do ficheiro {} guardada no nó {} ({})", fileID, node.getNodeID(), node.getType());
            } catch (IOException e) {
                replica.setStatus(ReplicaStatus.FAILED);
                fileReplicaRepository.save(replica);
                log.warn("Falha ao replicar para o nó {}: {}", node.getNodeID(), e.getMessage());
                notificationService.notify(owner, NotificationType.REPLICA_FAILED,
                        "Falha de sincronização",
                        "Falha ao sincronizar \"" + metadata.getOriginalName()
                                + "\" no dispositivo \"" + node.getName() + "\".",
                        "/dashboard/armazenamento");
            }
        }

        if (stored.isEmpty()) {
            notificationService.notify(owner, NotificationType.REPLICA_FAILED,
                    "Erro de sincronização",
                    "Não foi possível guardar \"" + metadata.getOriginalName()
                            + "\": nenhum espaço de armazenamento disponível.",
                    "/dashboard/armazenamento");
            throw new NoStorageAvailableException(
                    "Não foi possível guardar o ficheiro: nenhum espaço de armazenamento disponível.");
        }
        return stored;
    }

    /**
     * Decide em que nó(s) guardar, segundo o destino explícito ou a política
     * padrão do utilizador (RF20/RF22/RF24).
     */
    private List<StorageNode> resolveTargets(User owner, UploadDestination destination,
                                             Long targetNodeId, int requiredBytes) {
        return switch (destination) {
            case SYSTEM -> List.of(requireSystemSpace(requiredBytes));
            case DEVICE -> List.of(requireDevice(owner, targetNodeId, requiredBytes, true));
            case AUTO -> resolveByPolicy(owner, targetNodeId, requiredBytes);
        };
    }

    private List<StorageNode> resolveByPolicy(User owner, Long targetNodeId, int requiredBytes) {
        String policy = owner.getStoragePolicy();
        if (policy == null || policy.isBlank()) {
            policy = "SYSTEM_FIRST";
        }
        return switch (policy.toUpperCase()) {
            case "DEVICE_FIRST" -> {
                StorageNode device = findBestDevice(owner, requiredBytes);
                yield device != null ? List.of(device) : List.of(requireSystemSpace(requiredBytes));
            }
            case "MANUAL" -> {
                if (targetNodeId != null) {
                    yield List.of(requireDevice(owner, targetNodeId, requiredBytes, true));
                }
                yield List.of(requireSystemSpace(requiredBytes));
            }
            default -> List.of(requireSystemSpace(requiredBytes));
        };
    }

    private StorageNode requireSystemSpace(int requiredBytes) {
        StorageNode system = systemNodeService.getOrCreateSystemNode();
        if (system.getTotalSpaceBytes() != null && system.getTotalSpaceBytes() > 0
                && system.getFreeSpaceBytes() < requiredBytes) {
            throw new NoStorageAvailableException("A nuvem do sistema não tem espaço disponível.");
        }
        return system;
    }

    /**
     * Devolve o dispositivo pessoal pedido (ou o melhor disponível). Quando
     * {@code explicit} é verdadeiro e o dispositivo está offline/desativado,
     * lança {@link DeviceOfflineException} (RF24). Caso contrário devolve
     * {@code null} para permitir o fallback para a nuvem do sistema.
     */
    private StorageNode requireDevice(User owner, Long targetNodeId, int requiredBytes, boolean explicit) {
        StorageNode device;
        if (targetNodeId != null) {
            device = nodeRepository.findById(targetNodeId).orElse(null);
            if (device == null || device.getOwner() == null
                    || !device.getOwner().getUserID().equals(owner.getUserID())) {
                throw new IllegalArgumentException("Dispositivo inexistente ou não te pertence.");
            }
        } else {
            device = findBestDevice(owner, requiredBytes);
            if (device == null) {
                if (explicit) {
                    throw new NoStorageAvailableException(
                            "Não tens nenhum dispositivo configurado com espaço disponível.");
                }
                return null;
            }
        }

        if (!device.isEnabled()) {
            throw new DeviceOfflineException(
                    "O dispositivo \"" + device.getName() + "\" está desativado.", device.getNodeID());
        }
        if (device.getStatus() != NodeStatus.ONLINE) {
            throw new DeviceOfflineException(
                    "O dispositivo \"" + device.getName() + "\" está offline. "
                            + "Os ficheiros guardados nele ficam indisponíveis até voltar a ligar-se.",
                    device.getNodeID());
        }
        if (device.getTotalSpaceBytes() != null && device.getTotalSpaceBytes() > 0
                && device.getFreeSpaceBytes() < requiredBytes) {
            throw new NoStorageAvailableException(
                    "O dispositivo \"" + device.getName() + "\" não tem espaço disponível.");
        }
        return device;
    }

    private StorageNode findBestDevice(User owner, int requiredBytes) {
        return nodeRepository.findAllByOwnerAndTypeAndEnabledTrue(owner, com.assemblyproject.cloudsystem.entity.NodeType.DEVICE)
                .stream()
                .filter(n -> n.getStatus() == NodeStatus.ONLINE)
                .filter(n -> n.getTotalSpaceBytes() == null || n.getTotalSpaceBytes() <= 0
                        || n.getFreeSpaceBytes() >= requiredBytes)
                .max((a, b) -> Long.compare(a.getFreeSpaceBytes(), b.getFreeSpaceBytes()))
                .orElse(null);
    }

    private void refreshUsedSpace(StorageNode node, int addedBytes) {
        if (node.isSystem()) {
            systemNodeService.refreshSystemUsage();
        } else {
            node.setUsedSpaceBytes((node.getUsedSpaceBytes() == null ? 0L : node.getUsedSpaceBytes()) + addedBytes);
            nodeRepository.save(node);
        }
    }

    @Transactional
    public void deleteReplicas(Long fileID) {
        deleteReplicasExcept(fileID, Set.of());
    }

    /**
     * Apaga as réplicas do ficheiro que NÃO estejam nos nós a preservar
     * ({@code keepNodeIds}). É usado depois de replicar para o novo destino,
     * garantindo que nunca se fica sem qualquer cópia persistida: as réplicas
     * novas só são removidas quando as antigas já foram gravadas com sucesso
     * (RFC: replicate-first, delete-later).
     */
    @Transactional
    public void deleteReplicasExcept(Long fileID, Set<Long> keepNodeIds) {
        FileMetadata metadata = fileMetadataRepository.findById(fileID).orElse(null);
        if (metadata == null) {
            return;
        }
        Set<Long> keep = keepNodeIds == null ? Set.of() : keepNodeIds;
        List<FileReplica> replicas = fileReplicaRepository.findAllByFile(metadata);
        Set<StorageNode> touchedNodes = new HashSet<>();
        for (FileReplica replica : replicas) {
            StorageNode node = replica.getNode();
            if (node != null && keep.contains(node.getNodeID())) {
                continue;
            }
            try {
                if (node != null) {
                    containerClient.deleteReplica(node, fileID);
                    touchedNodes.add(node);
                }
            } catch (IOException e) {
                log.warn("Falha ao apagar réplica do ficheiro {} no nó {}", fileID,
                        node != null ? node.getNodeID() : null);
            } finally {
                fileReplicaRepository.delete(replica);
            }
        }
        for (StorageNode node : touchedNodes) {
            // A nuvem do sistema recalcula a partir do disco local; os
            // dispositivos já atualizam o seu próprio espaço no agente.
            if (node.isSystem()) {
                systemNodeService.refreshSystemUsage();
            }
        }
    }

    public Set<Long> nodeIds(List<StorageNode> nodes) {
        Set<Long> ids = new HashSet<>();
        for (StorageNode node : nodes) {
            if (node != null && node.getNodeID() != null) {
                ids.add(node.getNodeID());
            }
        }
        return ids;
    }

    public void handleNodeAck(Long fileID, Long nodeID, ReplicaStatus status) {
        fileReplicaRepository.findByFile_FileIDAndNode_NodeID(fileID, nodeID)
                .ifPresentOrElse(replica -> {
                    replica.setStatus(status);
                    replica.setUpdatedAt(LocalDateTime.now());
                    fileReplicaRepository.save(replica);
                    log.info("Réplica {} confirmada para o ficheiro {} no nó {}", status, fileID, nodeID);
                }, () -> log.warn("Réplica não encontrada para o ficheiro {} e nó {}", fileID, nodeID));
    }
}
