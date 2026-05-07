import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, Loader2, Store } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';

const registerSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters'),
  email: z.string().email('Invalid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  role: z.enum(['CUSTOMER', 'SELLER']),
});

const RegisterPage = () => {
  const { register: registerAuth } = useAuth();
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(registerSchema),
    defaultValues: { role: 'CUSTOMER' }
  });

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    try {
      await registerAuth(data);
      toast.info('OTP sent to your email. Please verify.');
      navigate('/verify-otp', { state: { email: data.email } });
    } catch (error) {
      toast.error(error.response?.data?.message || 'Registration failed.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-md mx-auto my-12 p-8 bg-white border border-border rounded-2xl shadow-sm">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-foreground">Create Account</h1>
        <p className="text-muted-foreground mt-2">Join ShopFlow today</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div>
          <label className="block text-sm font-medium mb-1 flex items-center gap-2">
            <User size={16} /> Full Name
          </label>
          <input
            {...register('name')}
            className={`w-full p-3 bg-muted border ${errors.name ? 'border-red-500' : 'border-border'} rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20`}
            placeholder="John Doe"
          />
          {errors.name && <p className="text-red-500 text-xs mt-1">{errors.name.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1 flex items-center gap-2">
            <Mail size={16} /> Email Address
          </label>
          <input
            {...register('email')}
            type="email"
            className={`w-full p-3 bg-muted border ${errors.email ? 'border-red-500' : 'border-border'} rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20`}
            placeholder="name@example.com"
          />
          {errors.email && <p className="text-red-500 text-xs mt-1">{errors.email.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1 flex items-center gap-2">
            <Lock size={16} /> Password
          </label>
          <input
            {...register('password')}
            type="password"
            className={`w-full p-3 bg-muted border ${errors.password ? 'border-red-500' : 'border-border'} rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20`}
            placeholder="••••••••"
          />
          {errors.password && <p className="text-red-500 text-xs mt-1">{errors.password.message}</p>}
        </div>

        <div>
          <label className="block text-sm font-medium mb-3">I want to:</label>
          <div className="grid grid-cols-2 gap-4">
            <label className={`cursor-pointer border-2 p-3 rounded-xl flex flex-col items-center gap-2 transition-all ${register('role').value === 'CUSTOMER' ? 'border-primary bg-primary/5' : 'border-border'}`}>
              <input {...register('role')} type="radio" value="CUSTOMER" className="hidden" />
              <User size={24} className={register('role').value === 'CUSTOMER' ? 'text-primary' : 'text-muted-foreground'} />
              <span className="text-sm font-bold">Shop Only</span>
            </label>
            <label className={`cursor-pointer border-2 p-3 rounded-xl flex flex-col items-center gap-2 transition-all ${register('role').value === 'SELLER' ? 'border-primary bg-primary/5' : 'border-border'}`}>
              <input {...register('role')} type="radio" value="SELLER" className="hidden" />
              <Store size={24} className={register('role').value === 'SELLER' ? 'text-primary' : 'text-muted-foreground'} />
              <span className="text-sm font-bold">Sell Products</span>
            </label>
          </div>
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full btn-primary py-3 flex items-center justify-center gap-2"
        >
          {isSubmitting ? <Loader2 className="animate-spin" /> : 'Register'}
        </button>
      </form>

      <div className="mt-8 pt-6 border-t border-border text-center">
        <p className="text-muted-foreground">
          Already have an account?{' '}
          <Link to="/login" className="text-primary font-bold hover:underline">Log In</Link>
        </p>
      </div>
    </div>
  );
};

export default RegisterPage;
