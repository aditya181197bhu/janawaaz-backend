package com.stavya.janawaaz.controller;

import com.stavya.janawaaz.dto.MetaDtos.WardResponse;
import com.stavya.janawaaz.dto.StatsDtos.WardReport;
import com.stavya.janawaaz.service.InsightsService;
import com.stavya.janawaaz.service.MetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only ward data for Janta (and the volunteer's ward view). Area totals only, no personal data. */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final MetaService metaService;
    private final InsightsService insightsService;

    @GetMapping("/wards")
    public List<WardResponse> wards() {
        return metaService.wards();
    }

    @GetMapping("/wards/{wardId}")
    public WardReport ward(@PathVariable Integer wardId) {
        return insightsService.wardReport(wardId);
    }
}
