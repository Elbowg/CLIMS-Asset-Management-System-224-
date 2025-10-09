package com.clims.backend.controller;

import com.clims.backend.service.reporting.ReportingEtlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/etl")
public class AdminEtlController {

    private final ReportingEtlService etlService;

    public AdminEtlController(ReportingEtlService etlService) {
        this.etlService = etlService;
    }

    @PostMapping("/maintenance-activity-daily")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Trigger ETL for maintenance_activity_daily",
        description = "Deletes and backfills summary rows for the given inclusive date window.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "from", in = ParameterIn.QUERY, required = true, description = "Start date (inclusive)"),
            @Parameter(name = "to", in = ParameterIn.QUERY, required = true, description = "End date (inclusive)")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Backfill completed"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    public ResponseEntity<ReportingEtlService.EtlRunResult> runMaintenanceActivityDaily(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            ReportingEtlService.EtlRunResult result = etlService.backfillMaintenanceActivityDaily(from, to);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/asset-status-daily")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Trigger ETL for asset_status_daily",
        description = "Deletes and backfills summary rows for the given inclusive date window.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "from", in = ParameterIn.QUERY, required = true, description = "Start date (inclusive)"),
            @Parameter(name = "to", in = ParameterIn.QUERY, required = true, description = "End date (inclusive)")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Backfill completed"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    public ResponseEntity<ReportingEtlService.EtlRunResult> runAssetStatusDaily(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            ReportingEtlService.EtlRunResult result = etlService.backfillAssetStatusDaily(from, to);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/audit-action-daily")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Trigger ETL for audit_action_daily",
        description = "Deletes and backfills summary rows for the given inclusive date window.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "from", in = ParameterIn.QUERY, required = true, description = "Start date (inclusive)"),
            @Parameter(name = "to", in = ParameterIn.QUERY, required = true, description = "End date (inclusive)")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Backfill completed"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    public ResponseEntity<ReportingEtlService.EtlRunResult> runAuditActionDaily(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            ReportingEtlService.EtlRunResult result = etlService.backfillAuditActionDaily(from, to);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
