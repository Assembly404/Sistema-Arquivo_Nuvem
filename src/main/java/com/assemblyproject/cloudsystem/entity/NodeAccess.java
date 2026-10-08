package com.assemblyproject.cloudsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "NodeAccesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accessID;

    @ManyToOne
    @JoinColumn(name = "nodeID", nullable = false)
    private StorageNode node;

    @ManyToOne
    @JoinColumn(name = "userID", nullable = false)
    private User grantedUser;

    private LocalDateTime expiresAt;

    private LocalDateTime grantedAt;
}
