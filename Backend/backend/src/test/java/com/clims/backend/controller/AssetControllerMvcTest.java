package com.clims.backend.controller;

import com.clims.backend.dto.AssignRequest;
import com.clims.backend.model.Asset;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.model.User;
import com.clims.backend.service.AssetService;
import com.clims.backend.service.UserService;
import com.clims.backend.service.LocationService;
import com.clims.backend.service.VendorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AssetControllerMvcTest {

    @Mock AssetService assetService;
    @Mock UserService userService;
    @Mock LocationService locationService;
    @Mock VendorService vendorService;

    private MockMvc mvc;
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @WithMockUser(roles = "ADMIN")
    void assignEndpoint() throws Exception {
        AssetController controller = new AssetController(assetService, userService, locationService, vendorService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();

        Asset a = new Asset(); a.setId(1L); a.setStatus(AssetStatus.AVAILABLE);
        User u = new User(); u.setId(2L);
        when(assetService.getByIdOrThrow(1L)).thenReturn(a);
        when(userService.findById(2L)).thenReturn(java.util.Optional.of(u));
        when(assetService.assignToUser(a,u)).thenReturn(a);

        AssignRequest req = new AssignRequest(); req.setUserId(2L);
        mvc.perform(post("/api/assets/1/assign").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unassignEndpoint() throws Exception {
        AssetController controller = new AssetController(assetService, userService, locationService, vendorService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();

        Asset a = new Asset(); a.setId(1L); a.setStatus(AssetStatus.ASSIGNED);
        when(assetService.getByIdOrThrow(1L)).thenReturn(a);
        when(assetService.unassignFromUser(a)).thenReturn(a);

        mvc.perform(post("/api/assets/1/unassign")).andExpect(status().isOk());
    }
}
