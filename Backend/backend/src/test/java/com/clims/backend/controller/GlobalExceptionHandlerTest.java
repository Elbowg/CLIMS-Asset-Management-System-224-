package com.clims.backend.controller;

import com.clims.backend.exception.BusinessRuleException;
import com.clims.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @RestController
    @RequestMapping("/test")
    static class DummyController {
        @GetMapping("/nf")
        public String nf() { throw new ResourceNotFoundException("Asset", 99L); }

        @PostMapping("/val")
        public String validate(@Valid @RequestBody SampleRequest req) { return "ok"; }

        @GetMapping("/br")
        public String br() { throw new BusinessRuleException("Cannot transition state"); }
    }

    static class SampleRequest {
        @NotBlank
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notFoundMapped() throws Exception {
        mvc.perform(get("/test/nf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/test/nf"));
    }

    @Test
    void validationErrorMapped() throws Exception {
        mvc.perform(post("/test/val").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.fields.name").exists());
    }

    @Test
    void businessRuleMapped() throws Exception {
        mvc.perform(get("/test/br"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }
}
