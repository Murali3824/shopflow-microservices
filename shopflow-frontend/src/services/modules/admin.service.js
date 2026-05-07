import api from '../api/axiosInstance';

export const adminService = {
  // Users
  getAllUsers: async (page = 0, size = 20) => {
    const response = await api.get(`/admin/users?page=${page}&size=${size}`);
    return response.data;
  },
  
  banUser: async (userId) => {
    return await api.put(`/admin/users/${userId}/ban`);
  },
  
  unbanUser: async (userId) => {
    return await api.put(`/admin/users/${userId}/unban`);
  },
  
  // Products
  getAllProducts: async (page = 0, size = 20) => {
    const response = await api.get(`/admin/products?page=${page}&size=${size}`);
    return response.data;
  },
  
  deleteProduct: async (productId) => {
    return await api.delete(`/admin/products/${productId}`);
  },
  
  // Orders
  getAllOrders: async (page = 0, size = 20) => {
    const response = await api.get(`/admin/orders?page=${page}&size=${size}`);
    return response.data;
  },
  
  // Reports
  getSalesReport: async (startDate, endDate) => {
    const response = await api.get(`/admin/reports/sales?start=${startDate}&end=${endDate}`);
    return response.data;
  },

  // ─── Seller Management ──────────────────────────────────────────────
  getPendingSellers: async () => {
    const response = await api.get('/admin/sellers/pending');
    return response.data;
  },

  approveSeller: async (id) => {
    return await api.put(`/admin/sellers/${id}/approve`);
  },

  rejectSeller: async (id) => {
    return await api.put(`/admin/sellers/${id}/reject`);
  },

  updateCommission: async (id, rate) => {
    return await api.put(`/admin/sellers/${id}/commission`, { commissionRate: rate });
  }
};
