import { apiClient } from './client';

export const auditLogService = {
  /**
   * Get complete audit history for a specific transaction
   */
  getTransactionAuditLog: async (transactionId) => {
    return apiClient.get(`/admin/audit/transactions/${transactionId}`);
  },

  /**
   * Get complete audit history for a specific user
   */
  getUserAuditLog: async (userId) => {
    return apiClient.get(`/admin/audit/users/${userId}`);
  },

  /**
   * Get paginated audit history by entity
   */
  getAuditHistory: async (entityId, entityType, page = 0, size = 10) => {
    return apiClient.get('/admin/audit/history', {
      params: { entityId, entityType, page, size },
    });
  },

  /**
   * Get recent changes within last N hours
   */
  getRecentChanges: async (hours = 24) => {
    return apiClient.get('/admin/audit/recent', {
      params: { hours },
    });
  },

  /**
   * Get all changes made by a specific user
   */
  getChangesByUser: async (email, page = 0, size = 10) => {
    return apiClient.get('/admin/audit/user', {
      params: { email, page, size },
    });
  },

  /**
   * Get all soft-deleted records
   */
  getDeletedRecords: async (page = 0, size = 10) => {
    return apiClient.get('/admin/audit/deleted', {
      params: { page, size },
    });
  },

  /**
   * Get count of changes to a specific entity
   */
  getChangeCount: async (entityId, entityType) => {
    return apiClient.get('/admin/audit/count', {
      params: { entityId, entityType },
    });
  },
};
