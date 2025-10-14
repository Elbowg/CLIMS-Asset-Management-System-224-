package com.clims.backend.controllers;

import com.clims.backend.dto.ReportDtos;
import com.clims.backend.services.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;
    private static final int DEFAULT_UNFILTERED_LIMIT = 5000;

    public ReportController(ReportService reportService) { this.reportService = reportService; }

    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR','FINANCE','MANAGER','IT_STAFF')")
    @PostMapping(value = "/inventory/csv", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> inventoryCsv(@RequestBody ReportDtos.InventoryFilter filter) {
        boolean unfiltered = isInventoryUnfiltered(filter);
        Integer limit = unfiltered ? DEFAULT_UNFILTERED_LIMIT : null;

        long count = reportService.countInventoryRecords(filter);
        boolean truncated = limit != null && count > limit;

        String date = LocalDate.now().toString();
        String filename = "inventory_" + date + ".csv";

        MediaType csvType = new MediaType("text", "csv", StandardCharsets.UTF_8);
        StreamingResponseBody body = outputStream -> {
            reportService.writeInventoryCsv(outputStream, filter, limit);
        };

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(csvType);
        if (truncated) {
            builder.header("X-Report-Limited", "true");
            builder.header("X-Report-Limit", String.valueOf(limit));
        }
        return builder.body(body);
    }

    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR','MANAGER','IT_STAFF')")
    @PostMapping(value = "/maintenance/csv", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StreamingResponseBody> maintenanceCsv(@RequestBody ReportDtos.MaintenanceFilter filter) {
        boolean unfiltered = isMaintenanceUnfiltered(filter);
        Integer limit = unfiltered ? DEFAULT_UNFILTERED_LIMIT : null;

        long count = reportService.countMaintenanceRecords(filter);
        boolean truncated = limit != null && count > limit;

        String date = LocalDate.now().toString();
        String filename = "maintenance_" + date + ".csv";

        MediaType csvType = new MediaType("text", "csv", StandardCharsets.UTF_8);
        StreamingResponseBody body = outputStream -> {
            reportService.writeMaintenanceCsv(outputStream, filter, limit);
        };

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(csvType);
        if (truncated) {
            builder.header("X-Report-Limited", "true");
            builder.header("X-Report-Limit", String.valueOf(limit));
        }
        return builder.body(body);
    }

    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR','FINANCE','MANAGER','IT_STAFF')")
    @PostMapping(value = "/inventory/pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> inventoryPdf(@RequestBody ReportDtos.InventoryFilter filter) {
        boolean unfiltered = isInventoryUnfiltered(filter);
        Integer limit = unfiltered ? DEFAULT_UNFILTERED_LIMIT : null;

        ReportService.ReportBytes result = reportService.inventoryPdfLimited(filter, limit);

        String date = LocalDate.now().toString();
        String filename = "inventory_" + date + ".pdf";

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF);
        if (result.truncated()) {
            builder.header("X-Report-Limited", "true");
            builder.header("X-Report-Limit", String.valueOf(limit));
        }
        return builder.body(result.bytes());
    }

    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR','MANAGER','IT_STAFF')")
    @PostMapping(value = "/maintenance/pdf", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> maintenancePdf(@RequestBody ReportDtos.MaintenanceFilter filter) {
        boolean unfiltered = isMaintenanceUnfiltered(filter);
        Integer limit = unfiltered ? DEFAULT_UNFILTERED_LIMIT : null;

        ReportService.ReportBytes result = reportService.maintenancePdfLimited(filter, limit);

        String date = LocalDate.now().toString();
        String filename = "maintenance_" + date + ".pdf";

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF);
        if (result.truncated()) {
            builder.header("X-Report-Limited", "true");
            builder.header("X-Report-Limit", String.valueOf(limit));
        }
        return builder.body(result.bytes());
    }

    private static boolean isInventoryUnfiltered(ReportDtos.InventoryFilter f) {
        if (f == null) return true;
        return f.status() == null && f.vendorId() == null && f.departmentId() == null && f.purchasedFrom() == null && f.purchasedTo() == null;
    }

    private static boolean isMaintenanceUnfiltered(ReportDtos.MaintenanceFilter f) {
        if (f == null) return true;
        return f.assetId() == null && f.status() == null && f.from() == null && f.to() == null;
    }
}
