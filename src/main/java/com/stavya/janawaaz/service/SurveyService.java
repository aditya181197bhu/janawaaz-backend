package com.stavya.janawaaz.service;

import com.stavya.janawaaz.dto.SurveyDtos.AnswerInput;
import com.stavya.janawaaz.dto.SurveyDtos.SubmitSurveyRequest;
import com.stavya.janawaaz.dto.SurveyDtos.SurveyReceipt;
import com.stavya.janawaaz.dto.SurveyDtos.SurveySummary;
import com.stavya.janawaaz.entity.*;
import com.stavya.janawaaz.exception.ApiException;
import com.stavya.janawaaz.repository.QuestionRepository;
import com.stavya.janawaaz.repository.SurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final MetaService metaService;
    private final LocationService locationService;

    @Transactional
    public SurveyReceipt submit(SubmitSurveyRequest req) {
        Ward ward = metaService.ward(req.wardId());
        Volunteer volunteer = metaService.volunteerEntity(req.volunteerId());

        Set<Integer> answeredIds = new HashSet<>();
        for (AnswerInput a : req.answers()) {
            if (!answeredIds.add(a.questionId())) {
                throw new ApiException("Question " + a.questionId() + " answered twice", HttpStatus.BAD_REQUEST);
            }
        }
        Map<Integer, Question> questions = questionRepository.findAllById(answeredIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        if (questions.size() != answeredIds.size()) {
            throw new ApiException("Unknown question in answers", HttpStatus.BAD_REQUEST);
        }

        List<Integer> priorities = req.priorityQuestionIds() == null ? List.of() : req.priorityQuestionIds();
        if (new HashSet<>(priorities).size() != priorities.size() || !answeredIds.containsAll(priorities)) {
            throw new ApiException("Priorities must be distinct answered questions", HttpStatus.BAD_REQUEST);
        }

        LocationService.Check location = locationService.check(ward, req.latitude(), req.longitude());
        Instant now = Instant.now();
        Instant startedAt = req.startedAt().isAfter(now) ? now : req.startedAt();

        Survey survey = Survey.builder()
                .volunteer(volunteer)
                .ward(ward)
                .respondentName(trimToNull(req.respondentName()))
                .respondentAge(req.respondentAge())
                .respondentGender(req.respondentGender())
                .occupation(trimToNull(req.occupation()))
                .mobile(trimToNull(req.mobile()))
                .consentGiven(true)
                .whatsappOptIn(req.whatsappOptIn() && trimToNull(req.mobile()) != null)
                .latitude(req.latitude())
                .longitude(req.longitude())
                .gpsAccuracyMeters(req.gpsAccuracyMeters())
                .distanceFromWardMeters(location.distanceMeters())
                .locationStatus(location.status())
                .startedAt(startedAt)
                .submittedAt(now)
                .durationSeconds((int) Duration.between(startedAt, now).getSeconds())
                .priorityQuestionIds(new ArrayList<>(priorities))
                .build();

        for (AnswerInput a : req.answers()) {
            survey.getAnswers().add(SurveyAnswer.builder()
                    .survey(survey)
                    .question(questions.get(a.questionId()))
                    .rating(a.rating())
                    .photoUrl(a.photoUrl())
                    .voiceUrl(a.voiceUrl())
                    .build());
        }

        surveyRepository.save(survey);
        survey.setReceiptNo(receiptNo(ward.getId(), survey.getId()));

        int[] counts = countRatings(survey.getAnswers());
        return new SurveyReceipt(survey.getReceiptNo(), ward.getId(), ward.getLocalityEn(), ward.getLocalityHi(),
                counts[0], counts[1], counts[2], counts[3], priorities,
                location.status(), location.distanceMeters(), now);
    }

    @Transactional(readOnly = true)
    public List<SurveySummary> recentForVolunteer(Long volunteerId, int limit) {
        return surveyRepository.findByVolunteerIdOrderBySubmittedAtDesc(volunteerId, PageRequest.of(0, limit))
                .stream()
                .map(s -> {
                    int[] c = countRatings(s.getAnswers());
                    return new SurveySummary(s.getReceiptNo(), s.getWard().getId(), s.getWard().getLocalityEn(),
                            s.getWard().getLocalityHi(), s.getRespondentName(), c[0], c[1], c[2],
                            s.getLocationStatus(), s.getSubmittedAt());
                })
                .toList();
    }

    public static String receiptNo(int wardId, long surveyId) {
        return String.format("JA-W%02d-S%05d", wardId, surveyId);
    }

    /** Returns [green, yellow, red, notApplicable]. */
    private static int[] countRatings(List<SurveyAnswer> answers) {
        int[] c = new int[4];
        for (SurveyAnswer a : answers) {
            c[a.getRating().ordinal()]++;
        }
        return c;
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
