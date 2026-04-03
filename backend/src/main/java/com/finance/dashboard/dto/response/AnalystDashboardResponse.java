package com.finance.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AnalystDashboardResponse {
    private UserResponse profile;
    private DashboardSummaryResponse summary;
    private List<MonthlyTrendEntry> monthlyTrends;
}
