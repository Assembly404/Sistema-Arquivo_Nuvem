package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.ReplicaStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.FileReplicaRepository;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReplicationService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileReplicaRepository fileReplicaRepository;
    private final StorageNodeRepository nodeRepository;
    private final int REPLICATION_FACTOR = 3;

    @Async
    @Transactional
    public void replicateFile(Long fileID) {
        FileMetadata metadata = fileMetadataRepository.findById(fileID)
                .orElseThrow(() -> new IllegalArgumentException("FileMetadata not found: " + fileID));

        List<StorageNode> candidates = nodeRepository.findAllByStatus(NodeStatus.ONLINE);
        List<StorageNode> selected = selectNodesWithSpace(candidates, REPLICATION_FACTOR);

        if (selected.size() < REPLICATION_FACTOR) {
            System.out.println("⚠️ Not enough online nodes for replication. Available: " + candidates.size());
            return;
        }

        for (StorageNode node : selected) {
            com.assemblyproject.cloudsystem.entity.FileReplica replica =
                    com.assemblyproject.cloudsystem.entity.FileReplica.builder()
                            .file(metadata)
                    .node(node)
                    .status(ReplicaStatus.PENDING)
                    .updatedAt(LocalDateTime.now())
                    .build();
            fileReplicaRepository.save(replica);
        }

        System.out.println("✅ Replication initiated for file " + fileID + " across " + selected.size() + " nodes");
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