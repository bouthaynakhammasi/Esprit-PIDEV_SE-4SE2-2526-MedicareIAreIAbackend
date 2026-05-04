package com.aziz.demosec.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GlobalStatsDTO {
    private long totalUsers;
    private long patients;
    private long pharmacists;
    private long providers;
    private long doctors;
    private long admins;

    private long pendingOrders;
    private long deliveredOrders;
    private long rejectedOrders;
    private long cancelledOrders;
    private long validatedOrders;

    private long pendingRequests;
    private long completedRequests;
    private long inProgressRequests;
}
