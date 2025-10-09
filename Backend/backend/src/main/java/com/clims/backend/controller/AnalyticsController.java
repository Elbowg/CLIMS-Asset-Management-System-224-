package com.clims.backend.controller;

import com.clims.backend.repository.MaintenanceRepository;
import com.clims.backend.repository.summary.AssetStatusDailyRepository;
import com.clims.backend.repository.summary.AuditActionDailyRepository;
import com.clims.backend.model.summary.AssetStatusDaily;
import com.clims.backend.model.summary.AuditActionDaily;
import com.clims.backend.repository.projection.MaintenanceDailyStatusCount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class AnalyticsController {

    private final MaintenanceRepository maintenanceRepository;
    private final AssetStatusDailyRepository assetStatusDailyRepository;
    private final AuditActionDailyRepository auditActionDailyRepository;
    private static final int MAX_WINDOW_DAYS = 366; // ~1 year

    public AnalyticsController(MaintenanceRepository maintenanceRepository,
                               AssetStatusDailyRepository assetStatusDailyRepository,
                               AuditActionDailyRepository auditActionDailyRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.assetStatusDailyRepository = assetStatusDailyRepository;
        this.auditActionDailyRepository = auditActionDailyRepository;
    }

    @GetMapping("/maintenance/workload")
    @PreAuthorize("hasAnyRole('ADMIN','REPORT_VIEWER')")
    @Operation(
        summary = "Daily maintenance workload by status",
        description = "Returns per-day counts of maintenance items by status in the given date range.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "from", in = ParameterIn.QUERY, required = true, description = "Start date (inclusive)", example = "2025-10-01"),
            @Parameter(name = "to", in = ParameterIn.QUERY, required = true, description = "End date (inclusive)", example = "2025-10-09")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MaintenanceDailyStatusCount.class))),
            @ApiResponse(responseCode = "400", description = "Invalid dates or window too large"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    public ResponseEntity<List<MaintenanceDailyStatusCount>> maintenanceWorkload(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null || to == null) {
            return ResponseEntity.badRequest().build();
        }
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().build();
        }
        long days = Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() + 1;
        if (days > MAX_WINDOW_DAYS) {
            return ResponseEntity.badRequest().build();
        }

        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.atTime(LocalTime.MAX);
        List<MaintenanceDailyStatusCount> result = maintenanceRepository.findDailyStatusCounts(fromTs, toTs);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/assets/daily-status")
    @PreAuthorize("hasAnyRole('ADMIN','REPORT_VIEWER')")
    @Operation(
        summary = "Daily asset counts by status (from summary)",
        description = "Returns per-day counts of assets by status from the asset_status_daily summary table.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "from", in = ParameterIn.QUERY, required = true, description = "Start date (inclusive)", example = "2025-10-01"),
            @Parameter(name = "to", in = ParameterIn.QUERY, required = true, description = "End date (inclusive)", example = "2025-10-09")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Invalid dates or window too large"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    public ResponseEntity<List<AssetStatusDaily>> assetDailyStatus(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null || to == null) {
            return ResponseEntity.badRequest().build();
        }
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().build();
        }
        long days = Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() + 1;
        if (days > MAX_WINDOW_DAYS) {
            return ResponseEntity.badRequest().build();
        }

        var result = assetStatusDailyRepository.findAllById_BucketDateBetweenOrderById_BucketDateAsc(from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/audit/daily-actions")
    @PreAuthorize("hasAnyRole('ADMIN','REPORT_VIEWER')")
    @Operation(
        summary = "Daily audit action counts (from summary)",
        description = "Returns per-day counts of audit actions from the audit_action_daily summary table.",
        security = { @SecurityRequirement(name = "bearerAuth") },
        parameters = {
            @Parameter(name = "from", in = ParameterIn.QUERY, required = true, description = "Start date (inclusive)", example = "2025-10-01"),
            @Parameter(name = "to", in = ParameterIn.QUERY, required = true, description = "End date (inclusive)", example = "2025-10-09")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Invalid dates or window too large"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
        }
    )
    public ResponseEntity<List<AuditActionDaily>> auditDailyActions(
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null || to == null) {
            return ResponseEntity.badRequest().build();
        }
        if (to.isBefore(from)) {
            return ResponseEntity.badRequest().build();
        }
        long days = Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() + 1;
        if (days > MAX_WINDOW_DAYS) {
            return ResponseEntity.badRequest().build();
        }
        var result = auditActionDailyRepository.findAllById_BucketDateBetweenOrderById_BucketDateAsc(from, to);
        return ResponseEntity.ok(result);
    }
}
