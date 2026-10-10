package com.smartshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryLocationDTO {
    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private LocalDateTime recordedAt;
    private Boolean isStale;
    private Long secondsAgo;
}
