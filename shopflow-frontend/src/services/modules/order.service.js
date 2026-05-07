import api from '../api/axiosInstance';

export const orderService = {
  // Cart
  getCart: async () => {
    const response = await api.get('/cart');
    return response.data;
  },
  
  addItemToCart: async (skuId, quantity) => {
    return await api.post('/cart/items', { skuId, quantity });
  },
  
  updateCartItem: async (skuId, quantity) => {
    return await api.put(`/cart/items/${skuId}?quantity=${quantity}`);
  },
  
  removeCartItem: async (skuId) => {
    return await api.delete(`/cart/items/${skuId}`);
  },
  
  applyCoupon: async (couponCode) => {
    return await api.post(`/cart/coupon?couponCode=${couponCode}`);
  },
  
  // Orders
  placeOrder: async (orderData) => {
    return await api.post('/orders', orderData);
  },
  
  getOrders: async (page = 0, size = 10) => {
    const response = await api.get(`/orders?page=${page}&size=${size}`);
    return response.data;
  },
  
  getOrderById: async (id) => {
    const response = await api.get(`/orders/${id}`);
    return response.data;
  },
  
  cancelOrder: async (id) => {
    return await api.post(`/orders/${id}/cancel`);
  },
  
  raiseReturn: async (id, returnData) => {
    return await api.post(`/orders/${id}/return`, returnData);
  },
  
  // Seller
  markOrderShipped: async (id, trackingNumber) => {
    return await api.post(`/orders/${id}/ship`, { trackingNumber });
  }
};
