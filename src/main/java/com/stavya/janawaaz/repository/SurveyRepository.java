package com.stavya.janawaaz.repository;

import com.stavya.janawaaz.entity.LocationStatus;
import com.stavya.janawaaz.entity.Survey;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface SurveyRepository extends JpaRepository<Survey, Long> {

    String FLAGGED = "(s.locationStatus = com.stavya.janawaaz.entity.LocationStatus.OUTSIDE_WARD "
            + "or s.durationSeconds < :minSeconds)";

    long countByWardId(Integer wardId);

    long countBySubmittedAtAfter(Instant after);

    long countByVolunteerIdAndSubmittedAtAfter(Long volunteerId, Instant after);

    long countByLocationStatus(LocationStatus status);

    long countByDurationSecondsLessThan(int seconds);

    @Query("select count(s) from Survey s where " + FLAGGED)
    long countFlagged(int minSeconds);

    /** Rows of [wardId, count]. */
    @Query("select s.ward.id, count(s) from Survey s group by s.ward.id")
    List<Object[]> countPerWard();

    /** Rows of [wardId, count]. */
    @Query("select s.ward.id, count(s) from Survey s where " + FLAGGED + " group by s.ward.id")
    List<Object[]> countFlaggedPerWard(int minSeconds);

    @EntityGraph(attributePaths = {"ward", "answers"})
    List<Survey> findByVolunteerIdOrderBySubmittedAtDesc(Long volunteerId, Pageable pageable);

    @EntityGraph(attributePaths = {"ward", "volunteer"})
    @Query("select s from Survey s where " + FLAGGED + " order by s.submittedAt desc")
    List<Survey> findFlagged(int minSeconds, Pageable pageable);

    /** Rows of [lat, lng, wardId, locationStatus]. */
    @Query("select s.latitude, s.longitude, s.ward.id, s.locationStatus from Survey s where s.latitude is not null")
    List<Object[]> allPoints();

    /** Rows of [volunteerId, count, lastSubmittedAt] for one ward. */
    @Query("select s.volunteer.id, count(s), max(s.submittedAt) from Survey s where s.ward.id = :wardId "
            + "group by s.volunteer.id")
    List<Object[]> activityByVolunteerForWard(Integer wardId);

    /** Rows of [volunteerId, count] of flagged surveys in one ward. */
    @Query("select s.volunteer.id, count(s) from Survey s where s.ward.id = :wardId and " + FLAGGED
            + " group by s.volunteer.id")
    List<Object[]> flaggedByVolunteerForWard(Integer wardId, int minSeconds);
}
