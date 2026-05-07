import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import MainLayout from './components/common/MainLayout';
import ErrorBoundary from './components/common/ErrorBoundary';

import LoginPage from './pages/Auth/LoginPage';
import RegisterPage from './pages/Auth/RegisterPage';
import VerifyOtpPage from './pages/Auth/VerifyOtpPage';
import SellerOnboarding from './pages/Auth/SellerOnboarding';

import HomePage from './pages/HomePage';
import ShopPage from './pages/ShopPage';
import ProductDetailPage from './pages/ProductDetailPage';
import CartPage from './pages/CartPage';
import CheckoutPage from './pages/CheckoutPage';
import OrderHistoryPage from './pages/OrderHistoryPage';
import OrderDetailPage from './pages/OrderDetailPage';
import ProfilePage from './pages/ProfilePage';
import SellerDashboard from './pages/SellerDashboard';
import AdminDashboard from './pages/AdminDashboard';

// Error Components
const NotFound = () => {
  return (
    <div className="text-center py-40 animate-in fade-in zoom-in duration-500">
      <h1 className="text-9xl font-black text-primary/20">404</h1>
      <h2 className="text-3xl font-bold mt-4">Lost in Space?</h2>
      <p className="text-muted-foreground mt-2 mb-8">The page you're looking for doesn't exist or has been moved.</p>
      <Link to="/" className="btn-primary px-8 py-3 rounded-full inline-block">Return Home</Link>
    </div>
  );
};

// Protected Route Component
const ProtectedRoute = ({ children, roles }) => {
  const { user, loading, isAuthenticated } = useAuth();

  if (loading) return <div className="flex justify-center py-20">Loading...</div>;
  if (!isAuthenticated) return <Navigate to="/login" />;
  
  if (roles && !roles.some(role => user.roles?.includes(role))) {
    return <Navigate to="/" />;
  }

  return children;
};

const App = () => {
  return (
    <Router>
      <AuthProvider>
        <CartProvider>
          <ErrorBoundary>
            <MainLayout>
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<HomePage />} />
              <Route path="/shop" element={<ShopPage />} />
              <Route path="/product/:id" element={<ProductDetailPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/verify-otp" element={<VerifyOtpPage />} />
              
              {/* Authenticated Routes */}
              <Route path="/cart" element={
                <ProtectedRoute>
                  <CartPage />
                </ProtectedRoute>
              } />
              <Route path="/checkout" element={
                <ProtectedRoute>
                  <CheckoutPage />
                </ProtectedRoute>
              } />
              <Route path="/profile" element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              } />
              <Route path="/orders" element={
                <ProtectedRoute>
                  <OrderHistoryPage />
                </ProtectedRoute>
              } />
              <Route path="/orders/:id" element={
                <ProtectedRoute>
                  <OrderDetailPage />
                </ProtectedRoute>
              } />
              
              {/* Admin Routes */}
              <Route path="/admin/*" element={
                <ProtectedRoute roles={['ADMIN']}>
                   <AdminDashboard />
                </ProtectedRoute>
              } />

              {/* Seller Routes */}
              <Route path="/seller/onboarding" element={
                <ProtectedRoute roles={['SELLER']}>
                   <SellerOnboarding />
                </ProtectedRoute>
              } />
              <Route path="/seller/*" element={
                <ProtectedRoute roles={['SELLER']}>
                   <SellerDashboard />
                </ProtectedRoute>
              } />

              <Route path="*" element={<NotFound />} />
            </Routes>
          </MainLayout>
          </ErrorBoundary>
        </CartProvider>
      </AuthProvider>
    </Router>
  );
};

export default App;
