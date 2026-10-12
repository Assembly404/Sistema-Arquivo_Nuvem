package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.PublicLink;
import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicLinkRepository extends JpaRepository<PublicLink, Long> {

    Optional<PublicLink> findByToken(String token);

    Optional<PublicLink> findByLinkIDAndOwner(Long linkID, User owner);

    List<PublicLink> findAllByOwnerOrderByCreatedAtDesc(User owner);

    List<PublicLink> findAllByFile(FileMetadata file);

    List<PublicLink> findAllByFolder(Folder folder);
}
