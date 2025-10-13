package com.clims.backend.models.entities;

import com.clims.backend.models.base.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityName;
    private Long entityId;
    private String action; // CREATE, UPDATE, DELETE, ASSIGN, DISPOSE
    private String details;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;
}
