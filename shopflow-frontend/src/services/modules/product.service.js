import api from '../api/axiosInstance';

export const productService = {
  getProducts: async (page = 0, size = 20) => {
    const response = await api.get(`/products?page=${page}&size=${size}`);
    return response.data;
  },
  
  getProductById: async (id) => {
    const response = await api.get(`/products/${id}`);
    return response.data;
  },
  
  searchProducts: async (query, page = 0, size = 20) => {
    const response = await api.get(`/products/search?q=${query}&page=${page}&size=${size}`);
    return response.data;
  },
  
  getCategories: async () => {
    const response = await api.get('/categories');
    return response.data;
  },
  
  getProductsBySeller: async (sellerId, page = 0, size = 20) => {
    const response = await api.get(`/products/seller/${sellerId}?page=${page}&size=${size}`);
    return response.data;
  },
  
  // Seller endpoints
  createProduct: async (productData) => {
    return await api.post('/products', productData);
  },
  
  updateProduct: async (id, productData) => {
    return await api.put(`/products/${id}`, productData);
  },
  
  deleteProduct: async (id) => {
    return await api.delete(`/products/${id}`);
  },
  
  uploadImage: async (productId, file, primary = false) => {
    const formData = new FormData();
    formData.append('file', file);
    return await api.post(`/products/${productId}/images?primary=${primary}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
  
  toggleStatus: async (id, active) => {
    return await api.patch(`/products/${id}/status`, { active });
  }
};
