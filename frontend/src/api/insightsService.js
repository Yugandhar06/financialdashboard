import { apiClient } from './client';

export const insightsService = {
  /**
   * Get advanced analytics and insights for the current user
   * Includes: spending trends, category analysis, budget status, recommendations
   */
  getInsights: async () => {
    return apiClient.get('/analyst/insights');
  },
};
