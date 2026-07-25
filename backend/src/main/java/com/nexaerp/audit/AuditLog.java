package com.nexaerp.audit;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who did it
    private Long userId;
    private String userName;

    // What entity was affected
    @Column(nullable = false)
    private String entityName;

    private Long entityId;

    // What action was performed
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    // Before and after values (JSON string)
    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    // Where from
    private String ipAddress;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
