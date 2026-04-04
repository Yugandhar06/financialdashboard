import { apiClient } from './client';

export const transactionService = {
  /**
   * Hits GET /api/transactions
   * Accepts pagination and filters correctly.
   */
  getTransactions: async (params = {}) => {
    // Clean params
    const cleanParams = {};
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        cleanParams[key] = value;
      }
    });

    return apiClient.get('/admin/transactions', { params: cleanParams });
  },

  /**
   * Hits GET /api/transactions/{id}
   */
  getTransaction: async (id) => {
    return apiClient.get(`/admin/transactions/${id}`);
  },

  /**
   * Hits POST /api/transactions
   */
  createTransaction: async (data) => {
    return apiClient.post('/admin/transactions', data);
  },

  /**
   * Hits PUT /api/transactions/{id}
   */
  updateTransaction: async (id, data) => {
    return apiClient.put(`/admin/transactions/${id}`, data);
  },

  /**
   * Hits DELETE /api/transactions/{id}
   */
  deleteTransaction: async (id) => {
    return apiClient.delete(`/admin/transactions/${id}`);
  }
};
