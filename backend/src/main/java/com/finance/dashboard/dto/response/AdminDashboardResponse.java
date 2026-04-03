package com.finance.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminDashboardResponse {
    private AnalystDashboardResponse analyticsData;
    private long totalUsers;
    private long activeUsers;
    private long totalTransactions;
}
