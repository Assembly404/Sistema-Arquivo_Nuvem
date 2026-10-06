package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    List<FileMetadata> findAllByOwner(User owner);

    Optional<FileMetadata> findByFileIDAndOwner(Long fileID, User owner);
}
