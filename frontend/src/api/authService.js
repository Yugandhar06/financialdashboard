import { apiClient } from './client';

export const authService = {
  /**
   * Hits POST /api/auth/login
   * @param {Object} credentials - { email, password }
   */
  login: async (credentials) => {
    return apiClient.post('/auth/login', credentials);
  },

  /**
   * Hits POST /api/auth/register
   * @param {Object} userData - { name, email, password }
   */
  register: async (userData) => {
    return apiClient.post('/auth/register', userData);
  }
};
