package com.stavya.janawaaz.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    /** Doubles as the display order (1-25). */
    @Id
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SurveyPart part;

    @Column(nullable = false)
    private String titleEn;

    @Column(nullable = false)
    private String titleHi;

    /** Short label for charts and lists, e.g. "Drinking water". */
    @Column(nullable = false)
    private String shortEn;

    @Column(nullable = false)
    private String shortHi;

    @Column(nullable = false, length = 500)
    private String greenEn;

    @Column(nullable = false, length = 500)
    private String greenHi;

    @Column(nullable = false, length = 500)
    private String yellowEn;

    @Column(nullable = false, length = 500)
    private String yellowHi;

    @Column(nullable = false, length = 500)
    private String redEn;

    @Column(nullable = false, length = 500)
    private String redHi;

}
