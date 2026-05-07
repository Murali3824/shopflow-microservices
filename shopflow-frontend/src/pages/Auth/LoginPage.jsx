import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Mail, Lock, Eye, EyeOff, Loader2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

const LoginPage = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    try {
      await login(data);
      toast.success('Login successful!');
      const from = location.state?.from?.pathname || '/';
      navigate(from, { replace: true });
    } catch (error) {
      toast.error(error.response?.data?.message || 'Login failed. Please check your credentials.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto my-12 p-8 bg-white border border-border rounded-2xl shadow-sm">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-foreground">Welcome Back</h1>
        <p className="text-muted-foreground mt-2">Log in to your ShopFlow account</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div>
          <label className="block text-sm font-medium mb-1 flex items-center gap-2">
            <Mail size={16} /> Email Address
          </label>
          <input
            {...register('email')}
            type="email"
            className={`w-full p-3 bg-muted border ${errors.email ? 'border-red-500' : 'border-border'} rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all`}
            placeholder="name@example.com"
          />
          {errors.email && <p className="text-red-500 text-xs mt-1">{errors.email.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1 flex items-center gap-2">
            <Lock size={16} /> Password
          </label>
          <div className="relative">
            <input
              {...register('password')}
              type={showPassword ? 'text' : 'password'}
              className={`w-full p-3 bg-muted border ${errors.password ? 'border-red-500' : 'border-border'} rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all`}
              placeholder="••••••••"
            />
            <button
              type="button"
              className="absolute right-3 top-3 text-muted-foreground hover:text-foreground"
              onClick={() => setShowPassword(!showPassword)}
            >
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
          {errors.password && <p className="text-red-500 text-xs mt-1">{errors.password.message}</p>}
          <div className="text-right mt-1">
            <Link to="/forgot-password" size="sm" className="text-sm text-primary hover:underline">Forgot password?</Link>
          </div>
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full btn-primary py-3 flex items-center justify-center gap-2"
        >
          {isSubmitting ? <Loader2 className="animate-spin" /> : 'Log In'}
        </button>
      </form>

      <div className="mt-8 pt-6 border-t border-border text-center">
        <p className="text-muted-foreground">
          Don't have an account?{' '}
          <Link to="/register" className="text-primary font-bold hover:underline">Create Account</Link>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;
