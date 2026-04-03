import { apiClient } from './client';

export const userService = {
  /**
   * Hits GET /api/users
   */
  getUsers: async () => {
    return apiClient.get('/admin/users');
  },

  /**
   * Hits PATCH /api/users/{id}/role
   */
  updateRole: async (id, role) => {
    return apiClient.patch(`/admin/users/${id}/role`, { role });
  },

  /**
   * Hits PATCH /api/users/{id}/status
   */
  updateStatus: async (id, status) => {
    return apiClient.patch(`/admin/users/${id}/status`, { status });
  },

  /**
   * Hits DELETE /api/users/{id}
   */
  deleteUser: async (id) => {
    return apiClient.delete(`/admin/users/${id}`);
  }
};
