package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.FileReplica;
import com.assemblyproject.cloudsystem.entity.ReplicaStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileReplicaRepository extends JpaRepository<FileReplica, Long> {

    List<FileReplica> findAllByFile(FileMetadata file);

    List<FileReplica> findAllByFileAndStatus(FileMetadata file, ReplicaStatus status);

    List<FileReplica> findAllByNode(StorageNode node);

    Optional<FileReplica> findByFile_FileIDAndNode_NodeID(Long fileID, Long nodeID);
}