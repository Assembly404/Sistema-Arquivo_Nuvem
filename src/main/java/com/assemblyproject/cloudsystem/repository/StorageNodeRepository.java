package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StorageNodeRepository extends JpaRepository<StorageNode, Long> {

    List<StorageNode> findAllByOwner(User owner);

    Optional<StorageNode> findBySessionId(String sessionId);

    List<StorageNode> findAllByStatusAndLastHeartbeatBefore(NodeStatus status, LocalDateTime threshold);

    List<StorageNode> findAllByStatus(NodeStatus status);

    List<StorageNode> findAllByStatusOrderByUsedSpaceBytesAsc(NodeStatus status);
}