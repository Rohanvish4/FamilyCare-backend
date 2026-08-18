package com.familycareai.identity.controller;

import com.familycareai.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health Check", description = "System operational status monitoring")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Check health status of FamilyCare AI backend")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        Map<String, String> statusMap = Map.of(
                "status", "UP",
                "service", "familycare-ai-backend"
        );
        return ResponseEntity.ok(ApiResponse.success(statusMap, "Service is healthy"));
    }
}
