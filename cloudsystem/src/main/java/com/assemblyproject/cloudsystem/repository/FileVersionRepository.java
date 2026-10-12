package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    List<FileVersion> findAllByFileOrderByVersionNumberDesc(FileMetadata file);

    long countByFile(FileMetadata file);

    Optional<FileVersion> findByFileAndVersionNumber(FileMetadata file, Integer versionNumber);
}
