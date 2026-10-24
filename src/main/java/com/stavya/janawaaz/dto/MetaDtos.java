package com.stavya.janawaaz.dto;

import com.stavya.janawaaz.entity.Question;
import com.stavya.janawaaz.entity.SurveyPart;
import com.stavya.janawaaz.entity.Ward;

public final class MetaDtos {

    private MetaDtos() {
    }

    public record WardResponse(Integer id, String localityEn, String localityHi,
                               Double centerLat, Double centerLng, Integer radiusMeters,
                               Integer targetHouseholds) {
        public static WardResponse from(Ward w) {
            return new WardResponse(w.getId(), w.getLocalityEn(), w.getLocalityHi(),
                    w.getCenterLat(), w.getCenterLng(), w.getRadiusMeters(), w.getTargetHouseholds());
        }
    }

    public record QuestionResponse(Integer id, SurveyPart part, String titleEn, String titleHi,
                                   String shortEn, String shortHi,
                                   String greenEn, String greenHi, String yellowEn, String yellowHi,
                                   String redEn, String redHi) {
        public static QuestionResponse from(Question q) {
            return new QuestionResponse(q.getId(), q.getPart(), q.getTitleEn(), q.getTitleHi(),
                    q.getShortEn(), q.getShortHi(), q.getGreenEn(), q.getGreenHi(),
                    q.getYellowEn(), q.getYellowHi(), q.getRedEn(), q.getRedHi());
        }
    }

    public record VolunteerResponse(Long id, String code, String name, Integer wardId,
                                    String localityEn, String localityHi, long surveysToday) {
    }

    public record FileUploadResponse(String url) {
    }
}
