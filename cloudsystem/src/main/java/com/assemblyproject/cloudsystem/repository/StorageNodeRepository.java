package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.NodeType;
import com.assemblyproject.cloudsystem.entity.NodeVisibility;
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

    List<StorageNode> findAllByOwnerOrderByCreatedAtDesc(User owner);

    List<StorageNode> findAllByVisibility(NodeVisibility visibility);

    Optional<StorageNode> findBySessionId(String sessionId);

    List<StorageNode> findAllByStatusAndLastHeartbeatBefore(NodeStatus status, LocalDateTime threshold);

    List<StorageNode> findAllByStatus(NodeStatus status);

    List<StorageNode> findAllByStatusOrderByUsedSpaceBytesAsc(NodeStatus status);

    long countByStatus(NodeStatus status);

    // --- Nós de sistema e dispositivos pessoais (RF17/RF18) ---

    Optional<StorageNode> findFirstByType(NodeType type);

    Optional<StorageNode> findFirstByTypeAndEnabledTrue(NodeType type);

    List<StorageNode> findAllByType(NodeType type);

    List<StorageNode> findAllByTypeAndEnabledTrue(NodeType type);

    List<StorageNode> findAllByOwnerAndType(User owner, NodeType type);

    List<StorageNode> findAllByOwnerAndTypeOrderByCreatedAtDesc(User owner, NodeType type);

    List<StorageNode> findAllByOwnerAndTypeAndEnabledTrue(User owner, NodeType type);

    long countByOwnerAndType(User owner, NodeType type);
}
