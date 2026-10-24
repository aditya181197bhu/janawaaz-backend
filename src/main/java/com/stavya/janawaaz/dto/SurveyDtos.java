package com.stavya.janawaaz.dto;

import com.stavya.janawaaz.entity.Gender;
import com.stavya.janawaaz.entity.LocationStatus;
import com.stavya.janawaaz.entity.Rating;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class SurveyDtos {

    private SurveyDtos() {
    }

    public record SubmitSurveyRequest(
            @NotNull Long volunteerId,
            @NotNull Integer wardId,
            String respondentName,
            Integer respondentAge,
            Gender respondentGender,
            String occupation,
            String mobile,
            @AssertTrue(message = "the respondent must give consent") boolean consentGiven,
            boolean whatsappOptIn,
            Double latitude,
            Double longitude,
            Double gpsAccuracyMeters,
            @NotNull Instant startedAt,
            @NotEmpty @Valid List<AnswerInput> answers,
            @Size(max = 3) List<Integer> priorityQuestionIds) {
    }

    public record AnswerInput(@NotNull Integer questionId, @NotNull Rating rating,
                              String photoUrl, String voiceUrl) {
    }

    public record SurveyReceipt(String receiptNo, Integer wardId, String localityEn, String localityHi,
                                int green, int yellow, int red, int notApplicable,
                                List<Integer> priorityQuestionIds,
                                LocationStatus locationStatus, Integer distanceFromWardMeters,
                                Instant submittedAt) {
    }

    public record SurveySummary(String receiptNo, Integer wardId, String localityEn, String localityHi,
                                String respondentName, int green, int yellow, int red,
                                LocationStatus locationStatus, Instant submittedAt) {
    }
}
