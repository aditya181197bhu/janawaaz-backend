package com.stavya.janawaaz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** One row per status change, so every ticket has a visible history. */
@Entity
@Table(name = "complaint_updates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private Instant createdAt;
}
