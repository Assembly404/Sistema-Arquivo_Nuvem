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

    private final FileMetadataRepository fileMetadataRepository;
    private final FileReplicaRepository fileReplicaRepository;
    private final StorageNodeRepository nodeRepository;
    private final ContainerClient containerClient;
    private final ContainerService containerService;
    private final StorageNodeService nodeStorageService;
    private final int REPLICATION_FACTOR = 3;

    @Async
    @Transactional
    public void replicateFile(Long fileID, User owner, byte[] containerData) {
        FileMetadata metadata = fileMetadataRepository.findById(fileID)
                .orElseThrow(() -> new IllegalArgumentException("FileMetadata not found: " + fileID));

        List<StorageNode> candidates = nodeRepository.findAllByStatus(NodeStatus.ONLINE);
        List<StorageNode> accessible = candidates.stream()
                .filter(n -> n.getVisibility() == NodeVisibility.PUBLIC
                        || n.getOwner().getUserID().equals(owner.getUserID()))
                .collect(Collectors.toList());
        List<StorageNode> selected = selectNodesWithSpace(accessible, REPLICATION_FACTOR);

        if (selected.isEmpty()) {
            System.out.println("⚠️ No accessible nodes for replication");
            return;
        }

        for (StorageNode node : selected) {
            com.assemblyproject.cloudsystem.entity.FileReplica replica = com.assemblyproject.cloudsystem.entity.FileReplica.builder()
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
                System.out.println("✅ Replica uploaded to node " + node.getNodeID());
            } catch (IOException e) {
                replica.setStatus(ReplicaStatus.FAILED);
                fileReplicaRepository.save(replica);
                System.out.println("⚠️ Replica failed for node " + node.getNodeID() + ": " + e.getMessage());
            }
        }
    }

    @Async
    @Transactional
    public void deleteReplicas(Long fileID) {
        List<com.assemblyproject.cloudsystem.entity.FileReplica> replicas = fileReplicaRepository.findAllByFile(
                fileMetadataRepository.findById(fileID)
                        .orElseThrow(() -> new IllegalArgumentException("File not found")));
        for (com.assemblyproject.cloudsystem.entity.FileReplica replica : replicas) {
            try {
                StorageNode node = replica.getNode();
                nodeStorageService.deleteReplica(node, fileID);
                fileReplicaRepository.delete(replica);
                System.out.println("✅ Replica deleted on node " + node.getNodeID());
            } catch (IOException e) {
                System.out.println("⚠️ Failed to delete replica on node " + replica.getNode().getNodeID());
            }
        }
    }

    private List<StorageNode> selectNodesWithSpace(List<StorageNode> candidates, int factor) {
        return candidates.stream()
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
                    System.out.println("✅ Replica " + status + " confirmed for file " + fileID + " on node " + nodeID);
                }, () -> {
                    System.out.println("⚠️ Replica not found for file " + fileID + " and node " + nodeID);
                });
    }
}
