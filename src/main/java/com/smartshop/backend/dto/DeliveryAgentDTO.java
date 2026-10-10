package com.smartshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAgentDTO {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String email;
}
