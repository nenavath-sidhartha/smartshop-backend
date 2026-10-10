package com.smartshop.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_locations", indexes = {
        @Index(name = "idx_deliv_loc_assignment", columnList = "delivery_assignment_id"),
        @Index(name = "idx_deliv_loc_recorded", columnList = "recordedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_assignment_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private DeliveryAssignment deliveryAssignment;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double accuracy; // meters

    @Column(nullable = false)
    private LocalDateTime recordedAt;
}
