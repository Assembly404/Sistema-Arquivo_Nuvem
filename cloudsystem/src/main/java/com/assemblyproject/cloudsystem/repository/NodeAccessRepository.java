package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.NodeAccess;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeAccessRepository extends JpaRepository<NodeAccess, Long> {
    Optional<NodeAccess> findByNodeAndGrantedUser(StorageNode node, User grantedUser);
    List<NodeAccess> findAllByNode(StorageNode node);
    List<NodeAccess> findAllByGrantedUser(User grantedUser);
}
