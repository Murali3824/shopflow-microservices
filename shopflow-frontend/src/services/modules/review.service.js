import axiosInstance from '../api/axiosInstance';

export const reviewService = {
  submitReview: async (reviewData) => {
    return await axiosInstance.post('/reviews', reviewData);
  },

  getProductReviews: async (productId, page = 0, size = 10) => {
    const response = await axiosInstance.get(`/reviews/product/${productId}`, {
      params: { page, size }
    });
    return response.data;
  },

  updateReview: async (reviewId, reviewData) => {
    return await axiosInstance.put(`/reviews/${reviewId}`, reviewData);
  },

  deleteReview: async (reviewId) => {
    return await axiosInstance.delete(`/reviews/${reviewId}`);
  }
};
