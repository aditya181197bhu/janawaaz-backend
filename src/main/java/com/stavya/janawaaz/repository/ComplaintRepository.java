package com.stavya.janawaaz.repository;

import com.stavya.janawaaz.entity.Complaint;
import com.stavya.janawaaz.entity.ComplaintStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    @EntityGraph(attributePaths = {"ward", "updates"})
    Optional<Complaint> findByTicketNoIgnoreCase(String ticketNo);

    @EntityGraph(attributePaths = "ward")
    @Query("select c from Complaint c where (:wardId is null or c.ward.id = :wardId) "
            + "and (:status is null or c.status = :status) order by c.createdAt desc")
    List<Complaint> search(Integer wardId, ComplaintStatus status);

    @EntityGraph(attributePaths = "ward")
    List<Complaint> findByWardIdOrderByCreatedAtDesc(Integer wardId, Pageable pageable);

    long countByCreatedAtAfter(Instant after);

    /** Rows of [wardId, status, count]. */
    @Query("select c.ward.id, c.status, count(c) from Complaint c group by c.ward.id, c.status")
    List<Object[]> countByWardAndStatus();

    /** Rows of [category, status, count]. */
    @Query("select c.category, c.status, count(c) from Complaint c where c.ward.id = :wardId "
            + "group by c.category, c.status")
    List<Object[]> countByCategoryAndStatusForWard(Integer wardId);

    /** Rows of [lat, lng, status, category]. */
    @Query("select c.latitude, c.longitude, c.status, c.category from Complaint c "
            + "where c.ward.id = :wardId and c.latitude is not null")
    List<Object[]> pointsForWard(Integer wardId);

    /** Rows of [lat, lng, wardId, ticketNo, status, category]. */
    @Query("select c.latitude, c.longitude, c.ward.id, c.ticketNo, c.status, c.category from Complaint c "
            + "where c.latitude is not null")
    List<Object[]> allPoints();

    /** Rows of [createdAt, resolvedAt]. */
    @Query("select c.createdAt, c.resolvedAt from Complaint c where c.resolvedAt is not null")
    List<Object[]> resolutionTimes();

    /** Rows of [volunteerId, count, lastCreatedAt] for one ward. */
    @Query("select c.volunteer.id, count(c), max(c.createdAt) from Complaint c where c.ward.id = :wardId "
            + "group by c.volunteer.id")
    List<Object[]> activityByVolunteerForWard(Integer wardId);
}
