package com.clims.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        // Redirect to Swagger UI if available; otherwise, just 200 OK
        return ResponseEntity.status(302)
                .header("Location", "/swagger-ui/index.html")
                .build();
    }
}
