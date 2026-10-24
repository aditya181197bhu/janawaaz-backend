package com.stavya.janawaaz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "surveys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String receiptNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private Volunteer volunteer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;

    private String respondentName;
    private Integer respondentAge;

    @Enumerated(EnumType.STRING)
    private Gender respondentGender;

    private String occupation;
    private String mobile;

    @Column(nullable = false)
    private boolean consentGiven;

    @Column(nullable = false)
    private boolean whatsappOptIn;

    private Double latitude;
    private Double longitude;
    private Double gpsAccuracyMeters;
    private Integer distanceFromWardMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationStatus locationStatus;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false)
    private Integer durationSeconds;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SurveyAnswer> answers = new ArrayList<>();

    /** Question ids of the household's top 3 priorities, in rank order. */
    @ElementCollection
    @CollectionTable(name = "survey_priorities", joinColumns = @JoinColumn(name = "survey_id"))
    @OrderColumn(name = "rank_index")
    @Column(name = "question_id")
    @Builder.Default
    private List<Integer> priorityQuestionIds = new ArrayList<>();
}
