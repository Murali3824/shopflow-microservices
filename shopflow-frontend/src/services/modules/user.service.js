import api from '../api/axiosInstance';

export const userService = {
  getProfile: async () => {
    const response = await api.get('/users/profile');
    return response.data;
  },
  
  updateProfile: async (profileData) => {
    return await api.put('/users/profile', profileData);
  },
  
  uploadAvatar: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return await api.post('/users/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
  
  getAddresses: async () => {
    const response = await api.get('/users/addresses');
    return response.data;
  },
  
  addAddress: async (addressData) => {
    return await api.post('/users/addresses', addressData);
  },
  
  updateAddress: async (id, addressData) => {
    return await api.put(`/users/addresses/${id}`, addressData);
  },
  
  deleteAddress: async (id) => {
    return await api.delete(`/users/addresses/${id}`);
  },
  
  setDefaultAddress: async (id) => {
    return await api.put(`/users/addresses/${id}/default`);
  }
};
