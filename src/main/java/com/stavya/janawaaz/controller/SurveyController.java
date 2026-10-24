package com.stavya.janawaaz.controller;

import com.stavya.janawaaz.dto.SurveyDtos.SubmitSurveyRequest;
import com.stavya.janawaaz.dto.SurveyDtos.SurveyReceipt;
import com.stavya.janawaaz.dto.SurveyDtos.SurveySummary;
import com.stavya.janawaaz.service.SurveyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @PostMapping
    public SurveyReceipt submit(@Valid @RequestBody SubmitSurveyRequest request) {
        return surveyService.submit(request);
    }

    @GetMapping
    public List<SurveySummary> mine(@RequestParam Long volunteerId,
                                    @RequestParam(defaultValue = "20") int limit) {
        return surveyService.recentForVolunteer(volunteerId, Math.min(limit, 100));
    }
}
