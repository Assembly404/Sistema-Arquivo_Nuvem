package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "Shares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Share {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shareID;

    @ManyToOne
    @JoinColumn(name = "ownerID", nullable = false)
    private User owner;

    @ManyToOne
    @JoinColumn(name = "recipientID", nullable = false)
    private User recipient;

    @ManyToOne
    @JoinColumn(name = "fileID")
    private FileMetadata file;

    @ManyToOne
    @JoinColumn(name = "folderID")
    private Folder folder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SharePermission permission;

    @Column(nullable = false)
    private LocalDateTime sharedAt;

    private LocalDateTime updatedAt;

    private LocalDateTime revokedAt;
}