package com.smartshop.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportDashboardDTO {
    private long openTickets;
    private long inProgressTickets;
    private long waitingForCustomer;
    private long resolvedToday;
    private long closedTickets;
    private long totalTickets;
    private long totalConversations;
    private long activeConversations;
    private long waitingForAgent;
}
