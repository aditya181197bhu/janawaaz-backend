package com.stavya.janawaaz.repository;

import com.stavya.janawaaz.entity.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Each query returns rows of [questionId, rating, count] (plus wardId where named). */
public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {

    @Query("select a.question.id, a.rating, count(a) from SurveyAnswer a group by a.question.id, a.rating")
    List<Object[]> countByQuestionAndRating();

    @Query("select a.question.id, a.rating, count(a) from SurveyAnswer a where a.survey.ward.id = :wardId "
            + "group by a.question.id, a.rating")
    List<Object[]> countByQuestionAndRatingForWard(Integer wardId);

    @Query("select a.survey.ward.id, a.question.id, a.rating, count(a) from SurveyAnswer a "
            + "group by a.survey.ward.id, a.question.id, a.rating")
    List<Object[]> countByWardQuestionAndRating();
}
