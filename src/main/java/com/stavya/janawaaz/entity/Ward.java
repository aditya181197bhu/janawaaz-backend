package com.stavya.janawaaz.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ward {

    /** The ward number is the identifier, so "Ward 3" is always id 3. */
    @Id
    private Integer id;

    @Column(nullable = false)
    private String localityEn;

    @Column(nullable = false)
    private String localityHi;

    /** Ward centre and radius stand in for real ward boundaries in the demo. */
    @Column(nullable = false)
    private Double centerLat;

    @Column(nullable = false)
    private Double centerLng;

    @Column(nullable = false)
    private Integer radiusMeters;

    @Column(nullable = false)
    private Integer targetHouseholds;
}
