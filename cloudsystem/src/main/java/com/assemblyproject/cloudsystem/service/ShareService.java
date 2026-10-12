package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.NotificationType;
import com.assemblyproject.cloudsystem.entity.Share;
import com.assemblyproject.cloudsystem.entity.SharePermission;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.FolderRepository;
import com.assemblyproject.cloudsystem.repository.ShareRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRepository shareRepository;
    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FolderRepository folderRepository;
    private final NotificationService notificationService;

    @Transactional
    public Share createOrUpdate(User from, String email, Long fileId, Long folderId, SharePermission permission) {
        SharePermission perm = permission != null ? permission : SharePermission.VIEW;
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O email é obrigatório.");
        }
        User recipient = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Não existe nenhuma conta com esse email."));
        if (recipient.getUserID().equals(from.getUserID())) {
            throw new IllegalArgumentException("Não podes partilhar itens contigo próprio.");
        }

        if (fileId != null && folderId != null) {
            throw new IllegalArgumentException("Indica apenas um ficheiro ou uma pasta.");
        }
        if (fileId != null) {
            FileMetadata file = fileMetadataRepository.findByFileIDAndOwner(fileId, from)
                    .orElseThrow(() -> new IllegalArgumentException("Ficheiro inexistente."));
            if (file.getDeletedAt() != null) {
                throw new IllegalStateException("O ficheiro está na lixeira.");
            }
            Share existing = shareRepository.findByOwnerAndRecipientAndFileAndRevokedAtIsNull(from, recipient, file)
                    .orElse(null);
            if (existing != null) {
                existing.setPermission(perm);
                existing.setUpdatedAt(LocalDateTime.now());
                return shareRepository.save(existing);
            }
            Share created = shareRepository.save(Share.builder()
                    .owner(from)
                    .recipient(recipient)
                    .file(file)
                    .permission(perm)
                    .sharedAt(LocalDateTime.now())
                    .build());
            notifyShareReceived(from, recipient, file.getOriginalName());
            return created;
        }

        if (folderId != null) {
            Folder folder = folderRepository.findByFolderIDAndOwner(folderId, from)
                    .orElseThrow(() -> new IllegalArgumentException("Pasta inexistente."));
            if (folder.getDeletedAt() != null) {
                throw new IllegalStateException("A pasta está na lixeira.");
            }
            Share existing = shareRepository.findByOwnerAndRecipientAndFolderAndRevokedAtIsNull(from, recipient, folder)
                    .orElse(null);
            if (existing != null) {
                existing.setPermission(perm);
                existing.setUpdatedAt(LocalDateTime.now());
                return shareRepository.save(existing);
            }
            Share created = shareRepository.save(Share.builder()
                    .owner(from)
                    .recipient(recipient)
                    .folder(folder)
                    .permission(perm)
                    .sharedAt(LocalDateTime.now())
                    .build());
            notifyShareReceived(from, recipient, folder.getName());
            return created;
        }

        throw new IllegalArgumentException("Indica um ficheiro ou uma pasta para partilhar.");
    }

    public List<Share> sharedRootItems(User user) {
        return shareRepository.findAllByRecipientAndRevokedAtIsNullOrderBySharedAtDesc(user).stream()
                .filter(share -> {
                    if (share.getFile() != null) {
                        FileMetadata file = share.getFile();
                        if (file.getDeletedAt() != null) return false;
                        return accessToFolder(user, file.getFolder()) == null;
                    }
                    Folder folder = share.getFolder();
                    if (folder == null || folder.getDeletedAt() != null) return false;
                    return !hasSharedAncestor(user, folder.getParent());
                })
                .collect(Collectors.toList());
    }

    public List<Share> listOwned(User owner) {
        return shareRepository.findAllByOwnerOrderBySharedAtDesc(owner);
    }

    public Folder requireReadableFolder(User user, Long folderId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Pasta inexistente."));
        if (accessToFolder(user, folder) == null || folder.getDeletedAt() != null) {
            throw new IllegalArgumentException("Pasta inexistente.");
        }
        return folder;
    }

    public List<Folder> childrenOfSharedFolder(User user, Folder folder) {
        return folderRepository.findAllByOwnerAndParentAndDeletedAtIsNullOrderByNameAsc(folder.getOwner(), folder);
    }

    public List<FileMetadata> filesOfSharedFolder(User user, Folder folder) {
        return fileMetadataRepository.findAllByOwnerAndFolderAndDeletedAtIsNullOrderByUploadedAtDesc(folder.getOwner(), folder);
    }

    @Transactional
    public Share updatePermission(User owner, Long shareId, SharePermission permission) {
        Share share = shareRepository.findByShareIDAndOwner(shareId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Partilha inexistente."));
        if (permission == null) {
            throw new IllegalArgumentException("A permissão é obrigatória.");
        }
        share.setPermission(permission);
        share.setUpdatedAt(LocalDateTime.now());
        return shareRepository.save(share);
    }

    @Transactional
    public Share revoke(User owner, Long shareId) {
        Share share = shareRepository.findByShareIDAndOwner(shareId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Partilha inexistente."));
        share.setRevokedAt(LocalDateTime.now());
        share.setUpdatedAt(LocalDateTime.now());
        Share saved = shareRepository.save(share);
        notificationService.notify(share.getRecipient(), NotificationType.SHARE_REVOKED,
                "Acesso removido",
                owner.getName() + " removeu o acesso a um item que partilhou contigo.",
                "/dashboard/arquivos/compartilhados");
        return saved;
    }

    private void notifyShareReceived(User from, User recipient, String itemName) {
        notificationService.notify(recipient, NotificationType.SHARE_RECEIVED,
                "Novo compartilhamento",
                from.getName() + " " + from.getSurname() + " partilhou \"" + itemName + "\" contigo.",
                "/dashboard/arquivos/compartilhados");
    }

    /**
     * Permissão efetiva de um utilizador sobre um ficheiro, ou {@code null} se
     * não tiver acesso (não é dono nem tem partilha de leitura).
     */
    public SharePermission accessToFile(User user, FileMetadata file) {
        if (file == null) return null;
        if (file.getOwner().getUserID().equals(user.getUserID())) return SharePermission.EDIT;
        if (file.getDeletedAt() != null) return null;
        for (Share share : shareRepository.findAllByRecipientAndFileAndRevokedAtIsNull(user, file)) {
            return share.getPermission();
        }
        return accessToFolder(user, file.getFolder());
    }

    /**
     * Permissão efetiva de um utilizador sobre uma pasta (ou ascendentes).
     */
    public SharePermission accessToFolder(User user, Folder folder) {
        Folder current = folder;
        while (current != null) {
            if (current.getOwner().getUserID().equals(user.getUserID())) return SharePermission.EDIT;
            for (Share share : shareRepository.findAllByRecipientAndFolderAndRevokedAtIsNull(user, current)) {
                return share.getPermission();
            }
            current = current.getParent();
        }
        return null;
    }

    private boolean hasSharedAncestor(User user, Folder folder) {
        return accessToFolder(user, folder) != null;
    }
}