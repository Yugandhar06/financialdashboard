package com.finance.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ViewerDashboardResponse {
    private UserResponse profile;
    private BigDecimal currentBalance;
    private List<TransactionResponse> recentTransactions;
}
