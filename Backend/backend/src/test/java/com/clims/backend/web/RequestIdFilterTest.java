package com.clims.backend.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import com.clims.backend.testutil.TestAssertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestIdFilterTest {

    @RestController
    static class DummyController {
        @GetMapping("/ping")
        public String ping() { return "pong"; }
    }

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @Test
    void generatesRequestIdWhenMissing() throws Exception {
        var result = mvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andReturn();
        String header = result.getResponse().getHeader(RequestIdFilter.HEADER);
    TestAssertions.assertUuid(header);
    }

    @Test
    void preservesExistingRequestId() throws Exception {
        String supplied = UUID.randomUUID().toString();
        var result = mvc.perform(get("/ping").header(RequestIdFilter.HEADER, supplied))
                .andExpect(status().isOk())
                .andReturn();
        String header = result.getResponse().getHeader(RequestIdFilter.HEADER);
        assertThat(header).isEqualTo(supplied);
    }

    // helper removed in favor of centralized TestAssertions
}
