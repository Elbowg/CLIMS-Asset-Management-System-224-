package com.clims.backend.models.entities;

import com.clims.backend.models.base.Auditable;
import com.clims.backend.models.enums.AssetStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
public class Asset extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @NotBlank
    private String assetTag; // generated id or QR code text

    @NotBlank
    @Column(nullable = false)
    private String serialNumber;

    @NotBlank
    @Column(nullable = false)
    private String make;

    @NotBlank
    @Column(nullable = false)
    private String model;

    @NotNull
    @Column(nullable = false)
    private LocalDate purchaseDate;

    private LocalDate warrantyExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status = AssetStatus.AVAILABLE;

    @ManyToOne
    @JoinColumn(name = "assigned_user_id")
    private AppUser assignedUser;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;
}
