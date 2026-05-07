import React, { createContext, useState, useEffect, useContext } from 'react';
import { orderService } from '../services/modules/order.service';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

export const CartProvider = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(false);

  const fetchCart = async () => {
    if (!isAuthenticated) return;
    setLoading(true);
    try {
      const data = await orderService.getCart();
      setCart(data);
    } catch (error) {
      console.error("Failed to fetch cart", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isAuthenticated) {
      fetchCart();
    } else {
      setCart(null);
    }
  }, [isAuthenticated]);

  const addItem = async (skuId, quantity) => {
    setLoading(true);
    try {
      const updatedCart = await orderService.addItemToCart(skuId, quantity);
      setCart(updatedCart.data);
    } finally {
      setLoading(false);
    }
  };

  const updateItem = async (skuId, quantity) => {
    setLoading(true);
    try {
      const updatedCart = await orderService.updateCartItem(skuId, quantity);
      setCart(updatedCart.data);
    } finally {
      setLoading(false);
    }
  };

  const removeItem = async (skuId) => {
    setLoading(true);
    try {
      const updatedCart = await orderService.removeCartItem(skuId);
      setCart(updatedCart.data);
    } finally {
      setLoading(false);
    }
  };

  const applyCoupon = async (code) => {
    setLoading(true);
    try {
      const updatedCart = await orderService.applyCoupon(code);
      setCart(updatedCart.data);
    } finally {
      setLoading(false);
    }
  };

  const clearCart = () => {
    setCart(null);
  };

  const value = {
    cart,
    loading,
    addItem,
    updateItem,
    removeItem,
    applyCoupon,
    clearCart,
    refreshCart: fetchCart,
    itemCount: cart?.items?.reduce((sum, item) => sum + item.quantity, 0) || 0
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};
