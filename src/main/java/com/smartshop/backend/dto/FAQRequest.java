package com.smartshop.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FAQRequest {
    @NotBlank(message = "Question is required")
    @Size(max = 500, message = "Question must be under 500 characters")
    private String question;

    @NotBlank(message = "Answer is required")
    private String answer;

    private String category;
    private String status;
    private Integer sortOrder;
}
