package com.stavya.janawaaz.service;

import com.stavya.janawaaz.dto.MetaDtos.QuestionResponse;
import com.stavya.janawaaz.dto.MetaDtos.VolunteerResponse;
import com.stavya.janawaaz.dto.MetaDtos.WardResponse;
import com.stavya.janawaaz.entity.Volunteer;
import com.stavya.janawaaz.entity.Ward;
import com.stavya.janawaaz.exception.ApiException;
import com.stavya.janawaaz.repository.QuestionRepository;
import com.stavya.janawaaz.repository.SurveyRepository;
import com.stavya.janawaaz.repository.VolunteerRepository;
import com.stavya.janawaaz.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetaService {

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final WardRepository wardRepository;
    private final QuestionRepository questionRepository;
    private final VolunteerRepository volunteerRepository;
    private final SurveyRepository surveyRepository;

    public List<WardResponse> wards() {
        return wardRepository.findAll(Sort.by("id")).stream().map(WardResponse::from).toList();
    }

    public List<QuestionResponse> questions() {
        return questionRepository.findAll(Sort.by("id")).stream().map(QuestionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VolunteerResponse volunteer(Long id) {
        Volunteer v = volunteerRepository.findWithWardById(id)
                .orElseThrow(() -> new ApiException("Volunteer not found", HttpStatus.NOT_FOUND));
        Ward w = v.getWard();
        long today = surveyRepository.countByVolunteerIdAndSubmittedAtAfter(id, startOfTodayIst());
        return new VolunteerResponse(v.getId(), v.getCode(), v.getName(), w.getId(),
                w.getLocalityEn(), w.getLocalityHi(), today);
    }

    public Ward ward(Integer id) {
        return wardRepository.findById(id)
                .orElseThrow(() -> new ApiException("Ward " + id + " not found", HttpStatus.NOT_FOUND));
    }

    public Volunteer volunteerEntity(Long id) {
        return volunteerRepository.findById(id)
                .orElseThrow(() -> new ApiException("Volunteer not found", HttpStatus.NOT_FOUND));
    }

    public static Instant startOfTodayIst() {
        return LocalDate.now(IST).atStartOfDay(IST).toInstant();
    }
}
