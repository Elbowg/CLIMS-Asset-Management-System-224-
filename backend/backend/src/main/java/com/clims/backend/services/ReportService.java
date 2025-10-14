package com.clims.backend.services;

import com.clims.backend.dto.ReportDtos;
import com.clims.backend.models.entities.Asset;
import com.clims.backend.models.entities.Maintenance;
import com.clims.backend.repositories.AssetRepository;
import com.clims.backend.repositories.MaintenanceRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final AssetRepository assetRepository;
    private final MaintenanceRepository maintenanceRepository;

    public ReportService(AssetRepository assetRepository, MaintenanceRepository maintenanceRepository) {
        this.assetRepository = assetRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    public String inventoryCsv(ReportDtos.InventoryFilter f) {
        List<Asset> assets = assetRepository.findAll().stream()
                .filter(a -> f.status() == null || a.getStatus() == f.status())
                .filter(a -> f.vendorId() == null || (a.getVendor() != null && f.vendorId().equals(a.getVendor().getId())))
                .filter(a -> f.departmentId() == null || (a.getAssignedUser() != null && a.getAssignedUser().getDepartment() != null && f.departmentId().equals(a.getAssignedUser().getDepartment().getId())))
                .filter(a -> {
                    LocalDate pd = a.getPurchaseDate();
                    return (f.purchasedFrom() == null || (pd != null && !pd.isBefore(f.purchasedFrom()))) &&
                           (f.purchasedTo() == null || (pd != null && !pd.isAfter(f.purchasedTo())));
                })
                .collect(Collectors.toList());
        try (StringWriter out = new StringWriter(); CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("AssetTag","SerialNumber","Make","Model","Status","AssignedTo","Department","Location","Vendor"))) {
            for (Asset a : assets) {
                printer.printRecord(
                        a.getAssetTag(), a.getSerialNumber(), a.getMake(), a.getModel(), a.getStatus(),
                        a.getAssignedUser() != null ? a.getAssignedUser().getUsername() : "",
                        a.getAssignedUser() != null && a.getAssignedUser().getDepartment() != null ? a.getAssignedUser().getDepartment().getName() : "",
                        a.getLocation() != null ? a.getLocation().getName() : "",
                        a.getVendor() != null ? a.getVendor().getName() : ""
                );
            }
            printer.flush();
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed generating CSV", e);
        }
    }

    public String maintenanceCsv(ReportDtos.MaintenanceFilter f) {
        List<Maintenance> list = maintenanceRepository.findAll().stream()
                .filter(m -> f.assetId() == null || (m.getAsset() != null && f.assetId().equals(m.getAsset().getId())))
                .filter(m -> f.status() == null || m.getStatus() == f.status())
                .filter(m -> {
                    LocalDate d = m.getScheduledDate();
                    return (f.from() == null || (d != null && !d.isBefore(f.from()))) &&
                           (f.to() == null || (d != null && !d.isAfter(f.to())));
                })
                .collect(Collectors.toList());
        try (StringWriter out = new StringWriter(); CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("AssetTag","Description","Status","Scheduled","Completed"))) {
            for (Maintenance m : list) {
                printer.printRecord(
                        m.getAsset() != null ? m.getAsset().getAssetTag() : "",
                        m.getDescription(),
                        m.getStatus(),
                        m.getScheduledDate(),
                        m.getCompletedDate()
                );
            }
            printer.flush();
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed generating CSV", e);
        }
    }

    public byte[] inventoryPdf(ReportDtos.InventoryFilter f) {
        List<Asset> assets = assetRepository.findAll().stream()
                .filter(a -> f.status() == null || a.getStatus() == f.status())
                .filter(a -> f.vendorId() == null || (a.getVendor() != null && f.vendorId().equals(a.getVendor().getId())))
                .filter(a -> f.departmentId() == null || (a.getAssignedUser() != null && a.getAssignedUser().getDepartment() != null && f.departmentId().equals(a.getAssignedUser().getDepartment().getId())))
                .filter(a -> {
                    LocalDate pd = a.getPurchaseDate();
                    return (f.purchasedFrom() == null || (pd != null && !pd.isBefore(f.purchasedFrom()))) &&
                           (f.purchasedTo() == null || (pd != null && !pd.isAfter(f.purchasedTo())));
                })
                .collect(Collectors.toList());

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontReg = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float margin = 40;
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            float y = page.getMediaBox().getHeight() - margin;
            float x = margin;
            String[] headers = {"AssetTag","Serial","Make","Model","Status","AssignedTo","Dept","Location","Vendor"};

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            try {
                // Title
                cs.setFont(fontBold, 14);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Inventory Report");
                cs.endText();
                y -= 24;

                // Header
                cs.setFont(fontBold, 10);
                writeRow(cs, x, y, headers);
                y -= 16;

                cs.setFont(fontReg, 10);
                for (Asset a : assets) {
                    if (y < margin + 40) {
                        cs.close();
                        page = new PDPage(PDRectangle.LETTER);
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - margin;
                        cs = new PDPageContentStream(doc, page);
                        cs.setFont(fontBold, 10);
                        writeRow(cs, x, y, headers);
                        y -= 16;
                        cs.setFont(fontReg, 10);
                    }

                    String[] row = new String[]{
                            safe(a.getAssetTag()), safe(a.getSerialNumber()), safe(a.getMake()), safe(a.getModel()),
                            String.valueOf(a.getStatus()),
                            a.getAssignedUser() != null ? safe(a.getAssignedUser().getUsername()) : "",
                            (a.getAssignedUser() != null && a.getAssignedUser().getDepartment() != null) ? safe(a.getAssignedUser().getDepartment().getName()) : "",
                            a.getLocation() != null ? safe(a.getLocation().getName()) : "",
                            a.getVendor() != null ? safe(a.getVendor().getName()) : ""
                    };
                    writeRow(cs, x, y, row);
                    y -= 14;
                }
            } finally {
                cs.close();
            }
            doc.save(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed generating PDF", e);
        }
    }

    public byte[] maintenancePdf(ReportDtos.MaintenanceFilter f) {
        List<Maintenance> list = maintenanceRepository.findAll().stream()
                .filter(m -> f.assetId() == null || (m.getAsset() != null && f.assetId().equals(m.getAsset().getId())))
                .filter(m -> f.status() == null || m.getStatus() == f.status())
                .filter(m -> {
                    LocalDate d = m.getScheduledDate();
                    return (f.from() == null || (d != null && !d.isBefore(f.from()))) &&
                           (f.to() == null || (d != null && !d.isAfter(f.to())));
                })
                .collect(Collectors.toList());

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontReg = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float margin = 40;
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            float y = page.getMediaBox().getHeight() - margin;
            float x = margin;
            String[] headers = {"AssetTag","Description","Status","Scheduled","Completed"};

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            try {
                // Title
                cs.setFont(fontBold, 14);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Maintenance Report");
                cs.endText();
                y -= 24;

                // Header
                cs.setFont(fontBold, 10);
                writeRow(cs, x, y, headers);
                y -= 16;

                cs.setFont(fontReg, 10);
                for (Maintenance m : list) {
                    if (y < margin + 40) {
                        cs.close();
                        page = new PDPage(PDRectangle.LETTER);
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - margin;
                        cs = new PDPageContentStream(doc, page);
                        cs.setFont(fontBold, 10);
                        writeRow(cs, x, y, headers);
                        y -= 16;
                        cs.setFont(fontReg, 10);
                    }

                    String[] row = new String[]{
                            m.getAsset() != null ? safe(m.getAsset().getAssetTag()) : "",
                            safe(m.getDescription()),
                            String.valueOf(m.getStatus()),
                            m.getScheduledDate() != null ? m.getScheduledDate().toString() : "",
                            m.getCompletedDate() != null ? m.getCompletedDate().toString() : ""
                    };
                    writeRow(cs, x, y, row);
                    y -= 14;
                }
            } finally {
                cs.close();
            }
            doc.save(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed generating PDF", e);
        }
    }

    private static void writeRow(PDPageContentStream cs, float x, float y, String[] cols) throws IOException {
        float colWidth = 65; // simple fixed-width columns
        float currentX = x;
        cs.beginText();
        cs.newLineAtOffset(currentX, y);
        String sep = "";
        for (String col : cols) {
            String text = truncate(col, 18);
            cs.showText(sep + text);
            sep = "  ";
        }
        cs.endText();
    }

    private static String truncate(String s, int max) {
        String v = safe(s);
        return v.length() > max ? v.substring(0, max - 1) + "…" : v;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // New helpers for streaming and limiting
    public long countInventoryRecords(ReportDtos.InventoryFilter f) {
        return assetRepository.findAll().stream()
                .filter(a -> f == null || f.status() == null || a.getStatus() == f.status())
                .filter(a -> f == null || f.vendorId() == null || (a.getVendor() != null && f.vendorId().equals(a.getVendor().getId())))
                .filter(a -> f == null || f.departmentId() == null || (a.getAssignedUser() != null && a.getAssignedUser().getDepartment() != null && f.departmentId().equals(a.getAssignedUser().getDepartment().getId())))
                .filter(a -> {
                    if (f == null) return true;
                    LocalDate pd = a.getPurchaseDate();
                    return (f.purchasedFrom() == null || (pd != null && !pd.isBefore(f.purchasedFrom()))) &&
                           (f.purchasedTo() == null || (pd != null && !pd.isAfter(f.purchasedTo())));
                })
                .count();
    }

    public long countMaintenanceRecords(ReportDtos.MaintenanceFilter f) {
        return maintenanceRepository.findAll().stream()
                .filter(m -> f == null || f.assetId() == null || (m.getAsset() != null && f.assetId().equals(m.getAsset().getId())))
                .filter(m -> f == null || f.status() == null || m.getStatus() == f.status())
                .filter(m -> {
                    if (f == null) return true;
                    LocalDate d = m.getScheduledDate();
                    return (f.from() == null || (d != null && !d.isBefore(f.from()))) &&
                           (f.to() == null || (d != null && !d.isAfter(f.to())));
                })
                .count();
    }

    public void writeInventoryCsv(OutputStream os, ReportDtos.InventoryFilter f, Integer limit) {
        try (CSVPrinter printer = new CSVPrinter(new java.io.OutputStreamWriter(os), CSVFormat.DEFAULT.withHeader("AssetTag","SerialNumber","Make","Model","Status","AssignedTo","Department","Location","Vendor"))) {
            final int[] written = {0};
            assetRepository.findAll().stream()
                    .filter(a -> f == null || f.status() == null || a.getStatus() == f.status())
                    .filter(a -> f == null || f.vendorId() == null || (a.getVendor() != null && f.vendorId().equals(a.getVendor().getId())))
                    .filter(a -> f == null || f.departmentId() == null || (a.getAssignedUser() != null && a.getAssignedUser().getDepartment() != null && f.departmentId().equals(a.getAssignedUser().getDepartment().getId())))
                    .filter(a -> {
                        if (f == null) return true;
                        LocalDate pd = a.getPurchaseDate();
                        return (f.purchasedFrom() == null || (pd != null && !pd.isBefore(f.purchasedFrom()))) &&
                               (f.purchasedTo() == null || (pd != null && !pd.isAfter(f.purchasedTo())));
                    })
                    .limit(limit != null ? limit : Long.MAX_VALUE)
                    .forEach(a -> {
                        try {
                            printer.printRecord(
                                    a.getAssetTag(), a.getSerialNumber(), a.getMake(), a.getModel(), a.getStatus(),
                                    a.getAssignedUser() != null ? a.getAssignedUser().getUsername() : "",
                                    a.getAssignedUser() != null && a.getAssignedUser().getDepartment() != null ? a.getAssignedUser().getDepartment().getName() : "",
                                    a.getLocation() != null ? a.getLocation().getName() : "",
                                    a.getVendor() != null ? a.getVendor().getName() : ""
                            );
                            written[0]++;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            printer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed writing CSV", e);
        }
    }

    public void writeMaintenanceCsv(OutputStream os, ReportDtos.MaintenanceFilter f, Integer limit) {
        try (CSVPrinter printer = new CSVPrinter(new java.io.OutputStreamWriter(os), CSVFormat.DEFAULT.withHeader("AssetTag","Description","Status","Scheduled","Completed"))) {
            maintenanceRepository.findAll().stream()
                    .filter(m -> f == null || f.assetId() == null || (m.getAsset() != null && f.assetId().equals(m.getAsset().getId())))
                    .filter(m -> f == null || f.status() == null || m.getStatus() == f.status())
                    .filter(m -> {
                        if (f == null) return true;
                        LocalDate d = m.getScheduledDate();
                        return (f.from() == null || (d != null && !d.isBefore(f.from()))) &&
                               (f.to() == null || (d != null && !d.isAfter(f.to())));
                    })
                    .limit(limit != null ? limit : Long.MAX_VALUE)
                    .forEach(m -> {
                        try {
                            printer.printRecord(
                                    m.getAsset() != null ? m.getAsset().getAssetTag() : "",
                                    m.getDescription(),
                                    m.getStatus(),
                                    m.getScheduledDate(),
                                    m.getCompletedDate()
                            );
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            printer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed writing CSV", e);
        }
    }

    public record ReportBytes(byte[] bytes, boolean truncated) {}

    public ReportBytes inventoryPdfLimited(ReportDtos.InventoryFilter f, Integer limit) {
        List<Asset> assets = assetRepository.findAll().stream()
                .filter(a -> f == null || f.status() == null || a.getStatus() == f.status())
                .filter(a -> f == null || f.vendorId() == null || (a.getVendor() != null && f.vendorId().equals(a.getVendor().getId())))
                .filter(a -> f == null || f.departmentId() == null || (a.getAssignedUser() != null && a.getAssignedUser().getDepartment() != null && f.departmentId().equals(a.getAssignedUser().getDepartment().getId())))
                .filter(a -> {
                    if (f == null) return true;
                    LocalDate pd = a.getPurchaseDate();
                    return (f.purchasedFrom() == null || (pd != null && !pd.isBefore(f.purchasedFrom()))) &&
                           (f.purchasedTo() == null || (pd != null && !pd.isAfter(f.purchasedTo())));
                })
                .collect(Collectors.toList());
        boolean truncated = false;
        if (limit != null && assets.size() > limit) {
            assets = assets.subList(0, limit);
            truncated = true;
        }
        return new ReportBytes(inventoryPdfInternal(assets), truncated);
    }

    public ReportBytes maintenancePdfLimited(ReportDtos.MaintenanceFilter f, Integer limit) {
        List<Maintenance> list = maintenanceRepository.findAll().stream()
                .filter(m -> f == null || f.assetId() == null || (m.getAsset() != null && f.assetId().equals(m.getAsset().getId())))
                .filter(m -> f == null || f.status() == null || m.getStatus() == f.status())
                .filter(m -> {
                    if (f == null) return true;
                    LocalDate d = m.getScheduledDate();
                    return (f.from() == null || (d != null && !d.isBefore(f.from()))) &&
                           (f.to() == null || (d != null && !d.isAfter(f.to())));
                })
                .collect(Collectors.toList());
        boolean truncated = false;
        if (limit != null && list.size() > limit) {
            list = list.subList(0, limit);
            truncated = true;
        }
        return new ReportBytes(maintenancePdfInternal(list), truncated);
    }

    // Extracted internals to reuse for limited PDFs
    private byte[] inventoryPdfInternal(List<Asset> assets) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontReg = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float margin = 40;
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            float y = page.getMediaBox().getHeight() - margin;
            float x = margin;
            String[] headers = {"AssetTag","Serial","Make","Model","Status","AssignedTo","Dept","Location","Vendor"};

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            try {
                cs.setFont(fontBold, 14);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Inventory Report");
                cs.endText();
                y -= 24;

                cs.setFont(fontBold, 10);
                writeRow(cs, x, y, headers);
                y -= 16;

                cs.setFont(fontReg, 10);
                for (Asset a : assets) {
                    if (y < margin + 40) {
                        cs.close();
                        page = new PDPage(PDRectangle.LETTER);
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - margin;
                        cs = new PDPageContentStream(doc, page);
                        cs.setFont(fontBold, 10);
                        writeRow(cs, x, y, headers);
                        y -= 16;
                        cs.setFont(fontReg, 10);
                    }

                    String[] row = new String[]{
                            safe(a.getAssetTag()), safe(a.getSerialNumber()), safe(a.getMake()), safe(a.getModel()),
                            String.valueOf(a.getStatus()),
                            a.getAssignedUser() != null ? safe(a.getAssignedUser().getUsername()) : "",
                            (a.getAssignedUser() != null && a.getAssignedUser().getDepartment() != null) ? safe(a.getAssignedUser().getDepartment().getName()) : "",
                            a.getLocation() != null ? safe(a.getLocation().getName()) : "",
                            a.getVendor() != null ? safe(a.getVendor().getName()) : ""
                    };
                    writeRow(cs, x, y, row);
                    y -= 14;
                }
            } finally {
                cs.close();
            }
            doc.save(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed generating PDF", e);
        }
    }

    private byte[] maintenancePdfInternal(List<Maintenance> list) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontReg = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float margin = 40;
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            float y = page.getMediaBox().getHeight() - margin;
            float x = margin;
            String[] headers = {"AssetTag","Description","Status","Scheduled","Completed"};

            PDPageContentStream cs = new PDPageContentStream(doc, page);
            try {
                cs.setFont(fontBold, 14);
                cs.beginText();
                cs.newLineAtOffset(margin, y);
                cs.showText("Maintenance Report");
                cs.endText();
                y -= 24;

                cs.setFont(fontBold, 10);
                writeRow(cs, x, y, headers);
                y -= 16;

                cs.setFont(fontReg, 10);
                for (Maintenance m : list) {
                    if (y < margin + 40) {
                        cs.close();
                        page = new PDPage(PDRectangle.LETTER);
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - margin;
                        cs = new PDPageContentStream(doc, page);
                        cs.setFont(fontBold, 10);
                        writeRow(cs, x, y, headers);
                        y -= 16;
                        cs.setFont(fontReg, 10);
                    }

                    String[] row = new String[]{
                            m.getAsset() != null ? safe(m.getAsset().getAssetTag()) : "",
                            safe(m.getDescription()),
                            String.valueOf(m.getStatus()),
                            m.getScheduledDate() != null ? m.getScheduledDate().toString() : "",
                            m.getCompletedDate() != null ? m.getCompletedDate().toString() : ""
                    };
                    writeRow(cs, x, y, row);
                    y -= 14;
                }
            } finally {
                cs.close();
            }
            doc.save(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed generating PDF", e);
        }
    }
}
