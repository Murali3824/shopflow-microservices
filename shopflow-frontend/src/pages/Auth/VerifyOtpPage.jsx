import React, { useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { Loader2, KeyRound, ArrowLeft } from 'lucide-react';
import { authService } from '../../services/modules/auth.service';
import { toast } from 'react-toastify';

const VerifyOtpPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const email = location.state?.email;
  
  const [otp, setOtp] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResending, setIsResending] = useState(false);

  if (!email) {
    return (
      <div className="text-center py-20">
        <p className="text-red-500 mb-4">No email provided for verification.</p>
        <Link to="/register" className="text-primary flex items-center justify-center gap-2 hover:underline">
          <ArrowLeft size={16} /> Back to Register
        </Link>
      </div>
    );
  }

  const handleVerify = async (e) => {
    e.preventDefault();
    if (otp.length !== 6) {
      return toast.warn('Please enter a 6-digit OTP');
    }

    setIsSubmitting(true);
    try {
      await authService.verifyEmail({ email, otp });
      toast.success('Email verified! You can now log in.');
      navigate('/login');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Verification failed.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResend = async () => {
    setIsResending(true);
    try {
      await authService.resendOtp(email);
      toast.success('New OTP sent to your email.');
    } catch (error) {
      toast.error('Failed to resend OTP.');
    } finally {
      setIsResending(false);
    }
  };

  return (
    <div className="max-w-md mx-auto my-12 p-8 bg-white border border-border rounded-2xl shadow-sm">
      <div className="text-center mb-8">
        <h1 className="text-3xl font-bold text-foreground">Verify Email</h1>
        <p className="text-muted-foreground mt-2">
          We've sent a 6-digit code to <span className="text-foreground font-semibold">{email}</span>
        </p>
      </div>

      <form onSubmit={handleVerify} className="space-y-6">
        <div>
          <label className="block text-sm font-medium mb-1 flex items-center gap-2">
            <KeyRound size={16} /> Enter Verification Code
          </label>
          <input
            type="text"
            maxLength={6}
            value={otp}
            onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
            className="w-full p-4 bg-muted border border-border rounded-lg text-center text-3xl tracking-widest focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all font-mono"
            placeholder="000000"
          />
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full btn-primary py-3 flex items-center justify-center gap-2"
        >
          {isSubmitting ? <Loader2 className="animate-spin" /> : 'Verify Account'}
        </button>
      </form>

      <div className="mt-8 text-center space-y-4">
        <p className="text-sm text-muted-foreground">
          Didn't receive the code?{' '}
          <button 
            onClick={handleResend}
            disabled={isResending}
            className="text-primary font-bold hover:underline disabled:opacity-50"
          >
            {isResending ? 'Resending...' : 'Resend OTP'}
          </button>
        </p>
        
        <Link to="/register" className="block text-sm text-muted-foreground hover:text-foreground">
          Entered wrong email? Change it here.
        </Link>
      </div>
    </div>
  );
};

export default VerifyOtpPage;
