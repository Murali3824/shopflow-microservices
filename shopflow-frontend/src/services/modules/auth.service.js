import api from '../api/axiosInstance';

export const authService = {
  login: async (credentials) => {
    const response = await api.post('/auth/login', credentials);
    if (response.data.access_token) {
      localStorage.setItem('token', response.data.access_token);
      localStorage.setItem('refreshToken', response.data.refresh_token);
    }
    return response.data;
  },
  
  register: async (userData) => {
    return await api.post('/auth/register', userData);
  },
  
  verifyEmail: async (data) => {
    return await api.post('/auth/verify-email', data);
  },
  
  resendOtp: async (email) => {
    return await api.post('/auth/resend-otp', { email });
  },
  
  forgotPassword: async (email) => {
    return await api.post('/auth/forgot-password', { email });
  },
  
  resetPassword: async (data) => {
    return await api.post('/auth/reset-password', data);
  },
  
  logout: async () => {
    try {
      await api.post('/auth/logout');
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
    }
  }
};
