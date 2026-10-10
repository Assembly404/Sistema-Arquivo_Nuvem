package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.NodeVisibility;
import com.assemblyproject.cloudsystem.entity.ReplicaStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.FileReplicaRepository;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReplicationService {

    private static final Logger log = LoggerFactory.getLogger(ReplicationService.class);
    private static final int REPLICATION_FACTOR = 3;

    private final FileMetadataRepository fileMetadataRepository;
    private final FileReplicaRepository fileReplicaRepository;
    private final StorageNodeRepository nodeRepository;
    private final ContainerClient containerClient;
    private final StorageNodeService nodeStorageService;

    @Async
    @Transactional
    public void replicateFile(Long fileID, User owner, byte[] containerData) {
        FileMetadata metadata = fileMetadataRepository.findById(fileID)
                .orElseThrow(() -> new IllegalArgumentException("FileMetadata not found: " + fileID));

        List<StorageNode> candidates = nodeRepository.findAllByStatus(NodeStatus.ONLINE);
        List<StorageNode> accessible = candidates.stream()
                .filter(n -> n.getVisibility() == NodeVisibility.PUBLIC
                        || (n.getOwner() != null && n.getOwner().getUserID().equals(owner.getUserID())))
                .collect(Collectors.toList());
        List<StorageNode> selected = selectNodesWithSpace(accessible, REPLICATION_FACTOR, containerData.length);

        if (selected.isEmpty()) {
            log.warn("Nenhum nó acessível com espaço para replicar o ficheiro {}", fileID);
            return;
        }

        for (StorageNode node : selected) {
            FileReplica replica = FileReplica.builder()
                    .file(metadata)
                    .node(node)
                    .status(ReplicaStatus.PENDING)
                    .updatedAt(LocalDateTime.now())
                    .build();
            fileReplicaRepository.save(replica);

            try {
                containerClient.uploadReplica(node, metadata.getFileID(), containerData);
                replica.setStatus(ReplicaStatus.SYNCED);
                fileReplicaRepository.save(replica);
                log.info("Réplica enviada para o nó {}", node.getNodeID());
            } catch (IOException e) {
                replica.setStatus(ReplicaStatus.FAILED);
                fileReplicaRepository.save(replica);
                log.warn("Falha ao replicar para o nó {}: {}", node.getNodeID(), e.getMessage());
            }
        }
    }

    @Transactional
    public void deleteReplicas(Long fileID) {
        FileMetadata metadata = fileMetadataRepository.findById(fileID).orElse(null);
        if (metadata == null) {
            return;
        }
        List<FileReplica> replicas = fileReplicaRepository.findAllByFile(metadata);
        for (FileReplica replica : replicas) {
            StorageNode node = replica.getNode();
            try {
                if (node != null) {
                    nodeStorageService.deleteReplica(node, fileID);
                }
            } catch (IOException e) {
                log.warn("Falha ao apagar réplica do ficheiro {} no nó {}", fileID,
                        node != null ? node.getNodeID() : null);
            } finally {
                fileReplicaRepository.delete(replica);
            }
        }
    }

    private List<StorageNode> selectNodesWithSpace(List<StorageNode> candidates, int factor, int requiredBytes) {
        return candidates.stream()
                .filter(n -> n.getTotalSpaceBytes() == null
                        || n.getFreeSpaceBytes() >= requiredBytes)
                .sorted((a, b) -> Long.compare(b.getFreeSpaceBytes(), a.getFreeSpaceBytes()))
                .limit(factor)
                .collect(Collectors.toList());
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
