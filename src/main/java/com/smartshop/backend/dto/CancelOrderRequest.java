package com.smartshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest {
    /** The ID of the customer requesting cancellation (used for ownership validation). */
    private Long userId;

    /** Short reason label chosen from a dropdown (e.g. "Ordered by mistake"). */
    private String cancellationReason;

    /** Optional free-text note provided by the customer. */
    private String cancellationNote;
}
