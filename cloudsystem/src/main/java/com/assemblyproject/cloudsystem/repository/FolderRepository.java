package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findAllByOwnerAndDeletedAtIsNullOrderByNameAsc(User owner);

    List<Folder> findAllByOwnerAndParentAndDeletedAtIsNullOrderByNameAsc(User owner, Folder parent);

    List<Folder> findAllByOwnerAndParentIsNullAndDeletedAtIsNullOrderByNameAsc(User owner);

    List<Folder> findAllByOwnerAndParent(User owner, Folder parent);

    List<Folder> findAllByOwnerAndDeletedAtIsNotNullOrderByDeletedAtDesc(User owner);

    Optional<Folder> findByFolderIDAndOwner(Long folderID, User owner);

    List<Folder> findAllByDeletedAtBefore(LocalDateTime cutoff);

    boolean existsByOwnerAndParentAndNameAndDeletedAtIsNull(User owner, Folder parent, String name);

    boolean existsByOwnerAndParentIsNullAndNameAndDeletedAtIsNull(User owner, String name);
}
