package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    List<FileMetadata> findAllByOwner(User owner);

    Optional<FileMetadata> findByFileIDAndOwner(Long fileID, User owner);

    List<FileMetadata> findAllByOwnerAndDeletedAtIsNullOrderByUploadedAtDesc(User owner);

    List<FileMetadata> findAllByOwnerAndFolderAndDeletedAtIsNullOrderByUploadedAtDesc(User owner, Folder folder);

    List<FileMetadata> findAllByOwnerAndFolderIsNullAndDeletedAtIsNullOrderByUploadedAtDesc(User owner);

    List<FileMetadata> findAllByOwnerAndDeletedAtIsNotNullOrderByDeletedAtDesc(User owner);

    List<FileMetadata> findAllByFolder(Folder folder);

    @Query("""
            select f from FileMetadata f
            where f.owner = :owner
              and f.deletedAt is null
              and (:name is null or lower(f.originalName) like lower(concat('%', :name, '%')))
              and (:mime is null or lower(f.mimeType) like lower(concat('%', :mime, '%')))
              and (:from is null or f.uploadedAt >= :from)
              and (:to is null or f.uploadedAt <= :to)
            order by f.uploadedAt desc
            """)
    List<FileMetadata> search(@Param("owner") User owner,
                              @Param("name") String name,
                              @Param("mime") String mime,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(f.storedSizeBytes), 0) from FileMetadata f where f.owner = :owner and f.deletedAt is null")
    long sumStoredSizeBytesByOwner(@Param("owner") User owner);

    @Query("select coalesce(sum(f.originalSizeBytes), 0) from FileMetadata f where f.owner = :owner and f.deletedAt is null")
    long sumOriginalSizeBytesByOwner(@Param("owner") User owner);

    long countByOwnerAndDeletedAtIsNull(User owner);

    List<FileMetadata> findAllByDeletedAtBefore(LocalDateTime cutoff);

    @Query("select coalesce(sum(f.storedSizeBytes), 0) from FileMetadata f")
    long sumStoredSizeBytes();
}
