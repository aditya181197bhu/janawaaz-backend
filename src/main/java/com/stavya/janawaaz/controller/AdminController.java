package com.stavya.janawaaz.controller;

import com.stavya.janawaaz.dto.StatsDtos.*;
import com.stavya.janawaaz.service.InsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Insights for the leader. Will sit behind admin login once auth is added. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final InsightsService insightsService;

    @GetMapping("/overview")
    public AdminOverview overview() {
        return insightsService.adminOverview();
    }

    @GetMapping("/wards/{wardId}")
    public AdminWardDetail ward(@PathVariable Integer wardId) {
        return insightsService.adminWardDetail(wardId);
    }

    @GetMapping("/heatmap")
    public Heatmap heatmap() {
        return insightsService.heatmap();
    }

    @GetMapping("/quality")
    public Quality quality() {
        return insightsService.quality();
    }

    @GetMapping("/map")
    public AdminMap map() {
        return insightsService.adminMap();
    }
}
