package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "StorageNodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long nodeID;

    @ManyToOne
    @JoinColumn(name = "ownerID", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NodeStatus status = NodeStatus.OFFLINE;

    @Column(nullable = false)
    private Long totalSpaceBytes;

    @Column(nullable = false)
    @Builder.Default
    private Long usedSpaceBytes = 0L;

    private LocalDateTime lastHeartbeat;

    @Column(length = 100)
    private String sessionId;

    @Column(nullable = false)
    @Builder.Default
    private NodeVisibility visibility = NodeVisibility.PERSONAL;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public long getFreeSpaceBytes() {
        return totalSpaceBytes - usedSpaceBytes;
    }
}
