package com.clims.backend.models.entities;

import com.clims.backend.models.base.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
public class Report extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String name;
    @Column(name = "report_type")
    private String type; // e.g., INVENTORY, MAINTENANCE, DEPRECIATION

    @ManyToOne
    @JoinColumn(name = "generated_by")
    private AppUser generatedBy;

    private String filterJson;
    @Column(length = 50)
    private String exportFormat; // CSV/PDF
}
