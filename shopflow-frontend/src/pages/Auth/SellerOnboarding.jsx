import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Store, ShoppingBag, ShieldCheck, ArrowRight, Loader2, Info } from 'lucide-react';
import { sellerService } from '../../services/modules/seller.service';
import { toast } from 'react-toastify';

const SellerOnboarding = () => {
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState({
    storeName: '',
    storeDescription: '',
    storeEmail: '',
    storePhone: '',
    storeAddress: ''
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.storeName || !formData.storeEmail) {
      return toast.warn("Store Name and Email are required");
    }

    setIsSubmitting(true);
    try {
      await sellerService.registerSeller(formData);
      toast.success("Store registered! Your application is now pending admin approval.");
      navigate('/seller/dashboard');
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to register store.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto py-12 px-6">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 items-center">
        {/* Info Section */}
        <div className="space-y-8">
          <div className="inline-flex items-center gap-2 bg-primary/10 text-primary px-4 py-2 rounded-full text-sm font-black tracking-widest uppercase">
            <Store size={18} /> Partner Program
          </div>
          <h1 className="text-5xl font-black leading-tight text-foreground">
            Launch Your Store on <span className="text-primary italic">ShopFlow</span>
          </h1>
          <p className="text-xl text-muted-foreground leading-relaxed">
            Join thousands of sellers and reach millions of customers globally with our premium marketplace platform.
          </p>

          <div className="space-y-6 pt-6">
            {[
              { icon: ShoppingBag, title: "Massive Audience", desc: "Instantly expose your products to active shoppers." },
              { icon: ShieldCheck, title: "Secure Payouts", desc: "Automatic weekly settlements directly to your bank." },
              { icon: Info, title: "Powerful Tools", desc: "Advanced analytics and inventory management." },
            ].map((item, i) => (
              <div key={i} className="flex gap-4">
                 <div className="bg-white p-3 rounded-2xl shadow-sm border border-border text-primary shrink-0">
                    <item.icon size={24} />
                 </div>
                 <div>
                    <h4 className="font-bold">{item.title}</h4>
                    <p className="text-sm text-muted-foreground">{item.desc}</p>
                 </div>
              </div>
            ))}
          </div>
        </div>

        {/* Form Section */}
        <div className="bg-white p-10 rounded-3xl border border-border shadow-2xl relative overflow-hidden">
           <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full -translate-y-1/2 translate-x-1/2"></div>
           
           <h3 className="text-2xl font-black mb-6">Store Setup</h3>
           <form onSubmit={handleSubmit} className="space-y-6">
              <div className="space-y-2">
                 <label className="text-sm font-bold">Store Name</label>
                 <input 
                   className="w-full p-4 bg-muted border border-border rounded-xl focus:ring-2 focus:ring-primary/20 outline-none font-medium"
                   placeholder="e.g. Minimalist Tech Gear"
                   value={formData.storeName}
                   onChange={e => setFormData({...formData, storeName: e.target.value})}
                 />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                   <label className="text-sm font-bold">Business Email</label>
                   <input 
                     type="email"
                     className="w-full p-4 bg-muted border border-border rounded-xl focus:ring-2 focus:ring-primary/20 outline-none font-medium"
                     placeholder="store@example.com"
                     value={formData.storeEmail}
                     onChange={e => setFormData({...formData, storeEmail: e.target.value})}
                   />
                </div>
                <div className="space-y-2">
                   <label className="text-sm font-bold">Business Phone</label>
                   <input 
                     className="w-full p-4 bg-muted border border-border rounded-xl focus:ring-2 focus:ring-primary/20 outline-none font-medium"
                     placeholder="+1 (555) 000-0000"
                     value={formData.storePhone}
                     onChange={e => setFormData({...formData, storePhone: e.target.value})}
                   />
                </div>
              </div>

              <div className="space-y-2">
                 <label className="text-sm font-bold">Store Description</label>
                 <textarea 
                   className="w-full p-4 bg-muted border border-border rounded-xl focus:ring-2 focus:ring-primary/20 outline-none font-medium h-32"
                   placeholder="Tell us about what you sell..."
                   value={formData.storeDescription}
                   onChange={e => setFormData({...formData, storeDescription: e.target.value})}
                 />
              </div>

              <div className="space-y-2">
                 <label className="text-sm font-bold">Store Address</label>
                 <input 
                   className="w-full p-4 bg-muted border border-border rounded-xl focus:ring-2 focus:ring-primary/20 outline-none font-medium"
                   placeholder="123 Retail Lane, Commerce City"
                   value={formData.storeAddress}
                   onChange={e => setFormData({...formData, storeAddress: e.target.value})}
                 />
              </div>

              <button 
                type="submit"
                disabled={isSubmitting}
                className="w-full btn-primary py-5 rounded-2xl flex items-center justify-center gap-3 font-black text-xl shadow-xl shadow-primary/30"
              >
                {isSubmitting ? <Loader2 className="animate-spin" /> : <>CREATE MY STORE <ArrowRight size={24} /></>}
              </button>
           </form>
        </div>
      </div>
    </div>
  );
};

export default SellerOnboarding;
