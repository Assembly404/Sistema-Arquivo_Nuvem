package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final PurgeService purgeService;

    @Transactional
    public Folder create(User owner, Long parentId, String name) {
        String cleanName = normalizeName(name);
        Folder parent = resolveParent(owner, parentId);
        if (nameExists(owner, parent, cleanName)) {
            throw new IllegalStateException("Ja existe uma pasta com esse nome nesta localizacao.");
        }
        Folder folder = Folder.builder()
                .owner(owner)
                .parent(parent)
                .name(cleanName)
                .createdAt(LocalDateTime.now())
                .build();
        return folderRepository.save(folder);
    }

    @Transactional
    public Folder update(User owner, Long folderId, String name, Long parentId) {
        Folder folder = requireOwned(owner, folderId);
        if (name != null && !name.isBlank()) {
            String cleanName = normalizeName(name);
            Folder parent = folder.getParent();
            Folder candidateParent = parentId != null ? resolveParent(owner, parentId) : parent;
            if (nameExistsExcluding(owner, candidateParent, cleanName, folder)) {
                throw new IllegalStateException("Ja existe uma pasta com esse nome nesta localizacao.");
            }
            folder.setName(cleanName);
        }
        if (parentId != null) {
            Folder newParent = resolveParent(owner, parentId);
            if (isDescendant(folder, newParent)) {
                throw new IllegalArgumentException("Nao e possivel mover uma pasta para dentro de si mesma.");
            }
            folder.setParent(newParent);
        }
        folder.setUpdatedAt(LocalDateTime.now());
        return folderRepository.save(folder);
    }

    public List<Folder> listChildren(User owner, Long parentId) {
        if (parentId == null) {
            return folderRepository.findAllByOwnerAndParentIsNullAndDeletedAtIsNullOrderByNameAsc(owner);
        }
        Folder parent = resolveParent(owner, parentId);
        return folderRepository.findAllByOwnerAndParentAndDeletedAtIsNullOrderByNameAsc(owner, parent);
    }

    public List<Folder> listTrash(User owner) {
        return folderRepository.findAllByOwnerAndDeletedAtIsNotNullOrderByDeletedAtDesc(owner);
    }

    @Transactional
    public Folder softDelete(User owner, Long folderId) {
        Folder folder = requireOwned(owner, folderId);
        LocalDateTime now = LocalDateTime.now();
        markDeletedRecursively(folder, now);
        return folder;
    }

    @Transactional
    public Folder restore(User owner, Long folderId) {
        Folder folder = requireOwned(owner, folderId);
        LocalDateTime deletedAt = folder.getDeletedAt();
        if (deletedAt == null) {
            throw new IllegalStateException("A pasta nao esta na lixeira.");
        }
        restoreRecursively(folder, deletedAt);
        return folder;
    }

    @Transactional
    public void purge(User owner, Long folderId) {
        Folder folder = requireOwned(owner, folderId);
        purgeService.purgeFolder(folder);
    }

    public Folder requireOwned(User owner, Long folderId) {
        return folderRepository.findByFolderIDAndOwner(folderId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Pasta inexistente."));
    }

    private void markDeletedRecursively(Folder folder, LocalDateTime now) {
        folder.setDeletedAt(now);
        folderRepository.save(folder);
        for (FileMetadata file : fileMetadataRepository.findAllByFolder(folder)) {
            if (file.getDeletedAt() == null) {
                file.setDeletedAt(now);
                fileMetadataRepository.save(file);
            }
        }
        for (Folder child : folderRepository.findAllByOwnerAndParent(folder.getOwner(), folder)) {
            if (child.getDeletedAt() == null) {
                markDeletedRecursively(child, now);
            }
        }
    }

    private void restoreRecursively(Folder folder, LocalDateTime deletedAt) {
        folder.setDeletedAt(null);
        folderRepository.save(folder);
        for (FileMetadata file : fileMetadataRepository.findAllByFolder(folder)) {
            if (deletedAt.equals(file.getDeletedAt())) {
                file.setDeletedAt(null);
                fileMetadataRepository.save(file);
            }
        }
        for (Folder child : folderRepository.findAllByOwnerAndParent(folder.getOwner(), folder)) {
            if (deletedAt.equals(child.getDeletedAt())) {
                restoreRecursively(child, deletedAt);
            }
        }
    }

    private boolean isDescendant(Folder folder, Folder candidate) {
        Folder current = candidate;
        while (current != null) {
            if (current.getFolderID().equals(folder.getFolderID())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private Folder resolveParent(User owner, Long parentId) {
        if (parentId == null) {
            return null;
        }
        Folder parent = requireOwned(owner, parentId);
        if (parent.getDeletedAt() != null) {
            throw new IllegalArgumentException("A pasta de destino esta na lixeira.");
        }
        return parent;
    }

    private boolean nameExists(User owner, Folder parent, String name) {
        return parent == null
                ? folderRepository.existsByOwnerAndParentIsNullAndNameAndDeletedAtIsNull(owner, name)
                : folderRepository.existsByOwnerAndParentAndNameAndDeletedAtIsNull(owner, parent, name);
    }

    private boolean nameExistsExcluding(User owner, Folder parent, String name, Folder exclude) {
        List<Folder> siblings = parent == null
                ? folderRepository.findAllByOwnerAndParentIsNullAndDeletedAtIsNullOrderByNameAsc(owner)
                : folderRepository.findAllByOwnerAndParentAndDeletedAtIsNullOrderByNameAsc(owner, parent);
        return siblings.stream()
                .anyMatch(f -> !f.getFolderID().equals(exclude.getFolderID()) && f.getName().equalsIgnoreCase(name));
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da pasta e obrigatorio.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("O nome da pasta e demasiado longo.");
        }
        return trimmed;
    }
}
