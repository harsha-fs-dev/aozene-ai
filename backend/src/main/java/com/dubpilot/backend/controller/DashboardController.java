package com.dubpilot.backend.controller;

import com.dubpilot.backend.dto.DashboardSummaryResponse;
import com.dubpilot.backend.service.ProjectService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:5173")
public class DashboardController {

    private final ProjectService projectService;

    public DashboardController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/production-summary")
    public DashboardSummaryResponse getProductionSummary() {
        return projectService.computeDashboardSummary();
    }
}