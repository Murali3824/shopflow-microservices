import axiosInstance from '../api/axiosInstance';

export const sellerService = {
  registerSeller: async (storeData) => {
    return await axiosInstance.post('/sellers/register', storeData);
  },

  getSellerProfile: async () => {
    const response = await axiosInstance.get('/sellers/me');
    return response.data;
  },

  updateStore: async (storeData) => {
    return await axiosInstance.put('/sellers/me/store', storeData);
  },

  getEarnings: async () => {
    const response = await axiosInstance.get('/sellers/me/earnings');
    return response.data;
  },

  createCoupon: async (couponData) => {
    return await axiosInstance.post('/sellers/me/coupons', couponData);
  },

  getCoupons: async () => {
    const response = await axiosInstance.get('/sellers/me/coupons');
    return response.data;
  },

  deleteCoupon: async (id) => {
    return await axiosInstance.delete(`/sellers/me/coupons/${id}`);
  }
};
