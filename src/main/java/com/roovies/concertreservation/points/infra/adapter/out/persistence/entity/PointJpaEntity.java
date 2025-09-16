package com.roovies.concertreservation.points.infra.adapter.out.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class PointJpaEntity {
    @Id
    private Long userId;
    private Integer points;
}
