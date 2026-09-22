package com.dubpilot.backend.controller;

import com.dubpilot.backend.dto.AttentionItem;
import com.dubpilot.backend.dto.DashboardSummaryResponse;
import com.dubpilot.backend.dto.ProductionBriefingResponse;
import com.dubpilot.backend.service.ProductionAttentionService;
import com.dubpilot.backend.service.ProjectService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/production")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductionController {

    private final ProductionAttentionService attentionService;
    private final ProjectService projectService;

    public ProductionController(ProductionAttentionService attentionService, ProjectService projectService) {
        this.attentionService = attentionService;
        this.projectService = projectService;
    }

    @GetMapping("/attention")
    public List<AttentionItem> getAttentionItems() {
        return attentionService.getAttentionItems();
    }

    @GetMapping("/briefing")
    public ProductionBriefingResponse getBriefing() {
        DashboardSummaryResponse summary = projectService.computeDashboardSummary();
        List<AttentionItem> attentionItems = attentionService.getAttentionItems();

        return new ProductionBriefingResponse(
                summary.getTotalProjects(),
                summary.getActiveProjects(),
                summary.getCompletedProjects(),
                summary.getDelayedProjects(),
                summary.getPendingConfirmations(),
                summary.getUpcomingSessions(),
                attentionItems);
    }
}