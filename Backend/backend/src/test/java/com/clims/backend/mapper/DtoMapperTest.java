package com.clims.backend.mapper;

import com.clims.backend.dto.*;
import com.clims.backend.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DtoMapperTest {

    @Test
    void assetToDto() {
        Asset a = new Asset();
        a.setId(1L);
        a.setName("Laptop A");
    a.setType(com.clims.backend.model.AssetType.LAPTOP);
        a.setSerialNumber("SN123");
    a.setStatus(com.clims.backend.model.AssetStatus.AVAILABLE);
        a.setPurchaseDate(LocalDate.of(2023,1,1));
        User u = new User(); u.setId(2L);
        a.setAssignedUser(u);
        assertNotNull(com.clims.backend.mapper.DtoMapper.toDto(a));
        AssetDTO d = com.clims.backend.mapper.DtoMapper.toDto(a);
        assertEquals(1L, d.getId());
        assertEquals("Laptop A", d.getName());
        assertEquals(2L, d.getAssignedUserId());
        assertEquals(LocalDate.of(2023,1,1), d.getPurchaseDate());
    }

    @Test
    void maintenanceToDto() {
        Maintenance m = new Maintenance();
        m.setId(10L);
        Asset a = new Asset(); a.setId(5L);
        User u = new User(); u.setId(6L);
        m.setAsset(a);
        m.setReportedBy(u);
    m.setDescription("Broken screen");
    m.setStatus(com.clims.backend.model.MaintenanceStatus.REPORTED);
        m.setReportedAt(LocalDateTime.now());
        MaintenanceDTO dto = com.clims.backend.mapper.DtoMapper.toDto(m);
        assertEquals(10L, dto.getId());
        assertEquals(5L, dto.getAssetId());
        assertEquals(6L, dto.getReportedById());
        assertEquals("Broken screen", dto.getDescription());
    }

    @Test
    void userVendorLocationReportDto() {
        User u = new User(); u.setId(7L); u.setUsername("bob"); u.setEmail("b@x"); u.setFullName("Bob");
        assertEquals("bob", com.clims.backend.mapper.DtoMapper.toDto(u).getUsername());

        Vendor v = new Vendor(); v.setId(8L); v.setVendorName("V1"); v.setContactInfo("c");
        assertEquals("V1", com.clims.backend.mapper.DtoMapper.toDto(v).getVendorName());

        Location l = new Location(); l.setId(9L); l.setRoomNumber("101"); l.setBuilding("Main");
        assertEquals("101", com.clims.backend.mapper.DtoMapper.toDto(l).getRoomNumber());

        Report r = new Report(); r.setId(11L); r.setContent("rpt"); r.setCreatedAt(LocalDateTime.now());
        assertEquals("rpt", com.clims.backend.mapper.DtoMapper.toDto(r).getContent());
    }
}