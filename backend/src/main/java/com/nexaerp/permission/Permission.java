package com.nexaerp.permission;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique code used in @PreAuthorize ex- create invoice
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    // Module this permission example-- invoice, report
    @Column(nullable = false)
    private String module;
}
