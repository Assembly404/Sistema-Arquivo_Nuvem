package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Elimina automaticamente itens que permanecem na lixeira para lá do prazo de
 * retenção configurado (RF11). Corre diariamente por omissão.
 */
@Service
@RequiredArgsConstructor
public class TrashCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TrashCleanupService.class);

    private final FileMetadataRepository fileMetadataRepository;
    private final FolderRepository folderRepository;
    private final PurgeService purgeService;

    @Value("${cloud.trash.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${cloud.trash.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpired() {
        if (retentionDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        // Pastas na lixeira: processar apenas as raízes (as subpastas são
        // removidas em cascata pela raiz), para não haver eliminações em duplicado.
        List<Folder> expiredFolders = folderRepository.findAllByDeletedAtBefore(cutoff);
        int folders = 0;
        for (Folder folder : expiredFolders) {
            Folder parent = folder.getParent();
            if (parent != null && parent.getDeletedAt() != null && !parent.getDeletedAt().isAfter(cutoff)) {
                continue;
            }
            purgeService.purgeFolder(folder);
            folders++;
        }

        // Ficheiros soltos na lixeira (os que estavam em pastas já foram removidos).
        int files = 0;
        for (FileMetadata file : fileMetadataRepository.findAllByDeletedAtBefore(cutoff)) {
            if (fileMetadataRepository.existsById(file.getFileID())) {
                purgeService.purgeFile(file);
                files++;
            }
        }

        if (folders > 0 || files > 0) {
            log.info("Limpeza da lixeira: {} pastas e {} ficheiros removidos definitivamente (retenção {} dias).",
                    folders, files, retentionDays);
        }
    }
}
