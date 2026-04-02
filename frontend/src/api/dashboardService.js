import { apiClient } from './client';

export const dashboardService = {
  /**
   * Hits GET /api/dashboard/summary
   * Requires ANALYST or ADMIN role.
   */
  getSummary: async () => {
    return apiClient.get('/dashboard/summary');
  },

  /**
   * Hits GET /api/dashboard/monthly-trends
   */
  getMonthlyTrends: async (months = 6) => {
    return apiClient.get('/dashboard/monthly-trends', { params: { months } });
  },

  /**
   * Hits GET /api/dashboard/recent
   * Requires VIEWER, ANALYST, or ADMIN role.
   */
  getRecentActivity: async (limit = 10) => {
    return apiClient.get('/dashboard/recent', { params: { limit } });
  }
};
