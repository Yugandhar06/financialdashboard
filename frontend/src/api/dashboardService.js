import { apiClient } from './client';

export const dashboardService = {
  getViewerDashboard: async () => {
    return apiClient.get('/viewer/dashboard');
  },

  getAnalystDashboard: async () => {
    return apiClient.get('/analyst/dashboard');
  },

  getAdminDashboard: async () => {
    return apiClient.get('/admin/dashboard');
  }
};
