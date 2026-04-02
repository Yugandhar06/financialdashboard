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
    
    return apiClient.get('/transactions', { params: cleanParams });
  },

  /**
   * Hits GET /api/transactions/{id}
   */
  getTransaction: async (id) => {
    return apiClient.get(`/transactions/${id}`);
  },

  /**
   * Hits POST /api/transactions
   */
  createTransaction: async (data) => {
    return apiClient.post('/transactions', data);
  },

  /**
   * Hits PUT /api/transactions/{id}
   */
  updateTransaction: async (id, data) => {
    return apiClient.put(`/transactions/${id}`, data);
  },

  /**
   * Hits DELETE /api/transactions/{id}
   */
  deleteTransaction: async (id) => {
    return apiClient.delete(`/transactions/${id}`);
  }
};
