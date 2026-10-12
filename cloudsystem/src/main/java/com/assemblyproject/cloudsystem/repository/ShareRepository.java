package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.Share;
import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {

    Optional<Share> findByShareIDAndOwner(Long shareID, User owner);

    List<Share> findAllByRecipientAndRevokedAtIsNullOrderBySharedAtDesc(User recipient);

    List<Share> findAllByOwnerOrderBySharedAtDesc(User owner);

    List<Share> findAllByOwnerAndRevokedAtIsNullOrderBySharedAtDesc(User owner);

    List<Share> findAllByRecipientAndFileAndRevokedAtIsNull(User recipient, FileMetadata file);

    Optional<Share> findByOwnerAndRecipientAndFileAndRevokedAtIsNull(User owner, User recipient, FileMetadata file);

    Optional<Share> findByOwnerAndRecipientAndFolderAndRevokedAtIsNull(User owner, User recipient, Folder folder);

    List<Share> findAllByRecipientAndFolderAndRevokedAtIsNull(User recipient, Folder folder);

    List<Share> findAllByFileAndRevokedAtIsNull(FileMetadata file);

    List<Share> findAllByFolderAndRevokedAtIsNull(Folder folder);
}