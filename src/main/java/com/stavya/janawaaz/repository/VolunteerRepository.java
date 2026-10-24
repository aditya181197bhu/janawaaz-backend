package com.stavya.janawaaz.repository;

import com.stavya.janawaaz.entity.Volunteer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {

    @EntityGraph(attributePaths = "ward")
    Optional<Volunteer> findWithWardById(Long id);

    List<Volunteer> findByWardId(Integer wardId);

    /** Rows of [wardId, count]. */
    @Query("select v.ward.id, count(v) from Volunteer v group by v.ward.id")
    List<Object[]> countPerWard();
}
