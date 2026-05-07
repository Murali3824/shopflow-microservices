import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { productService } from '../services/modules/product.service';
import { sellerService } from '../services/modules/seller.service';
import { ShoppingBag, Plus, Search, Edit3, Trash2, Power, PowerOff, Loader2, DollarSign, Package, Star, Tag, Ticket, AlertCircle } from 'lucide-react';
import { toast } from 'react-toastify';
import { Link, useNavigate } from 'react-router-dom';

const SellerDashboard = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState('inventory');
  
  // Seller Profile & Stats
  const [sellerProfile, setSellerProfile] = useState(null);
  const [earnings, setEarnings] = useState(0);
  
  // Coupons
  const [coupons, setCoupons] = useState([]);
  const [showCouponModal, setShowCouponModal] = useState(false);
  const [newCoupon, setNewCoupon] = useState({ code: '', discountPercent: 10, minOrderAmount: 50, expiryDate: '' });

  useEffect(() => {
    const initDashboard = async () => {
      setLoading(true);
      try {
        const profile = await sellerService.getSellerProfile();
        setSellerProfile(profile);
        
        if (profile.status === 'APPROVED') {
          await Promise.all([
            fetchProducts(),
            fetchEarnings(),
            fetchCoupons()
          ]);
        }
      } catch (err) {
        if (err.response?.status === 404) {
          // Seller not registered in seller-service yet
          toast.info("Please complete your store setup");
          navigate('/seller/onboarding');
        } else {
          toast.error("Failed to load dashboard data");
        }
      } finally {
        setLoading(false);
      }
    };
    initDashboard();
  }, []);

  const fetchEarnings = async () => {
    try {
      const data = await sellerService.getEarnings();
      setEarnings(data || 0);
    } catch (err) {}
  };

  const fetchCoupons = async () => {
    try {
      const data = await sellerService.getCoupons();
      setCoupons(data || []);
    } catch (err) {}
  };

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const data = await productService.getProductsBySeller(user.id, 0, 50);
      setProducts(data.content || []);
    } catch (err) {
      toast.error("Failed to load your products");
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async (id, currentStatus) => {
    try {
      await productService.toggleStatus(id, !currentStatus);
      setProducts(products.map(p => p.id === id ? { ...p, active: !currentStatus } : p));
      toast.success(`Product ${!currentStatus ? 'activated' : 'deactivated'}`);
    } catch (err) {
      toast.error("Failed to update status");
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this product?")) return;
    try {
      await productService.deleteProduct(id);
      setProducts(products.filter(p => p.id !== id));
      toast.info("Product deleted");
    } catch (err) {
      toast.error("Failed to delete product");
    }
  };

  const filteredProducts = products.filter(p => 
    p.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="py-8 space-y-12">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
           <div className="flex items-center gap-3">
              <h1 className="text-4xl font-black">Seller Dashboard</h1>
              {sellerProfile && (
                <span className={`text-[10px] font-black tracking-widest px-2 py-0.5 rounded border uppercase ${sellerProfile.status === 'APPROVED' ? 'bg-green-50 text-green-600 border-green-200' : 'bg-amber-50 text-amber-600 border-amber-200'}`}>
                  {sellerProfile.status}
                </span>
              )}
           </div>
           <p className="text-muted-foreground mt-2">{sellerProfile?.storeName || 'Manage your inventory and track performance'}</p>
        </div>
        <div className="flex gap-4">
           <button 
             onClick={() => setActiveTab(activeTab === 'inventory' ? 'coupons' : 'inventory')}
             className="bg-white border-2 border-border text-foreground px-6 py-4 rounded-2xl font-bold flex items-center gap-2 hover:bg-muted transition-all"
           >
              {activeTab === 'inventory' ? <><Ticket size={20} /> Manage Coupons</> : <><Package size={20} /> View Inventory</>}
           </button>
           <button className="bg-primary text-white px-8 py-4 rounded-2xl font-bold flex items-center gap-2 shadow-lg shadow-primary/20 hover:bg-primary-dark transition-all">
              <Plus size={20} /> Add New Product
           </button>
        </div>
      </div>

      {sellerProfile?.status === 'PENDING' && (
        <div className="bg-amber-50 border border-amber-200 rounded-3xl p-8 flex items-start gap-6 text-amber-800">
           <AlertCircle size={32} className="shrink-0" />
           <div>
              <h4 className="font-bold text-lg">Application Under Review</h4>
              <p className="opacity-80 leading-relaxed">Your store application is currently being reviewed by our administration. You will be able to list products and create coupons once your account is approved. Usually, this takes 24-48 hours.</p>
           </div>
        </div>
      )}

      {/* Stats Section */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {[
          { icon: DollarSign, label: "Total Revenue", value: `$${earnings.toFixed(2)}`, color: "text-green-600", bg: "bg-green-50" },
          { icon: Package, label: "Active Items", value: products.filter(p => p.active).length, color: "text-blue-600", bg: "bg-blue-50" },
          { icon: ShoppingBag, label: "Orders Monthly", value: "0", color: "text-purple-600", bg: "bg-purple-50" },
          { icon: Star, label: "Seller Rating", value: "4.8", color: "text-amber-600", bg: "bg-amber-50" },
        ].map((stat, i) => (
          <div key={i} className="bg-white border border-border p-6 rounded-3xl shadow-sm flex items-center gap-4">
             <div className={`p-4 rounded-2xl ${stat.bg} ${stat.color}`}>
                <stat.icon size={24} />
             </div>
             <div>
                <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest leading-none mb-1">{stat.label}</p>
                <p className="text-2xl font-black">{stat.value}</p>
             </div>
          </div>
        ))}
      </div>

      {/* Inventory Section */}
      {activeTab === 'inventory' && (
        <section className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
          <div className="flex justify-between items-center">
             <h2 className="text-2xl font-bold">Your Inventory</h2>
             <div className="relative w-64">
                <input 
                  type="text" 
                  placeholder="Search inventory..." 
                  className="w-full pl-10 pr-4 py-2 bg-muted border border-border rounded-xl focus:ring-2 focus:ring-primary/20 outline-none text-sm"
                  value={searchTerm}
                  onChange={e => setSearchTerm(e.target.value)}
                />
                <Search className="absolute left-3 top-2.5 text-muted-foreground" size={16} />
             </div>
          </div>

          {loading ? (
             <div className="flex justify-center py-20"><Loader2 className="animate-spin text-primary" size={48} /></div>
          ) : (
            <div className="bg-white border border-border rounded-3xl overflow-hidden shadow-sm overflow-x-auto">
               <table className="w-full text-left">
                  <thead className="bg-muted/50 border-b border-border text-xs font-bold text-muted-foreground uppercase tracking-widest">
                     <tr>
                        <th className="px-8 py-4">Product</th>
                        <th className="px-8 py-4">Category</th>
                        <th className="px-8 py-4">Price</th>
                        <th className="px-8 py-4">Status</th>
                        <th className="px-8 py-4 text-right">Actions</th>
                     </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                     {filteredProducts.map(product => (
                        <tr key={product.id} className="hover:bg-muted/20 transition-colors group">
                           <td className="px-8 py-6">
                              <div className="flex items-center gap-4">
                                 <div className="w-12 h-12 rounded-lg overflow-hidden border border-border flex-shrink-0">
                                    <img src={product.primaryImage?.url || 'https://via.placeholder.com/100'} alt="" className="w-full h-full object-cover" />
                                 </div>
                                 <div>
                                    <p className="font-bold group-hover:text-primary transition-colors">{product.name}</p>
                                    <p className="text-xs text-muted-foreground font-mono">{product.id.substring(0, 8)}...</p>
                                 </div>
                              </div>
                           </td>
                           <td className="px-8 py-6">
                              <span className="text-xs font-bold bg-muted px-2 py-1 rounded border border-border">
                                 {product.category?.name || 'General'}
                              </span>
                           </td>
                           <td className="px-8 py-6 font-black">${product.price.toFixed(2)}</td>
                           <td className="px-8 py-6">
                              <div className={`flex items-center gap-2 font-bold text-xs ${product.active ? 'text-green-600' : 'text-red-500'}`}>
                                 <div className={`w-2 h-2 rounded-full ${product.active ? 'bg-green-600' : 'bg-red-500'}`}></div>
                                 {product.active ? 'ACTIVE' : 'DEACTIVATED'}
                              </div>
                           </td>
                           <td className="px-8 py-6 text-right">
                              <div className="flex items-center justify-end gap-2">
                                 <button 
                                   onClick={() => handleToggleStatus(product.id, product.active)}
                                   className={`p-2 rounded-lg border border-border transition-all ${product.active ? 'hover:bg-red-50 hover:text-red-500' : 'hover:bg-green-50 hover:text-green-600'}`}
                                   title={product.active ? "Deactivate" : "Activate"}
                                 >
                                    {product.active ? <PowerOff size={16} /> : <Power size={16} />}
                                 </button>
                                 <button className="p-2 rounded-lg border border-border hover:bg-muted transition-all" title="Edit">
                                    <Edit3 size={16} />
                                 </button>
                                 <button 
                                   onClick={() => handleDelete(product.id)}
                                   className="p-2 rounded-lg border border-border hover:bg-red-50 hover:text-red-500 transition-all text-muted-foreground" title="Delete"
                                 >
                                    <Trash2 size={16} />
                                 </button>
                              </div>
                           </td>
                        </tr>
                     ))}
                  </tbody>
               </table>
               {filteredProducts.length === 0 && (
                  <div className="p-20 text-center flex flex-col items-center gap-4">
                     <ShoppingBag size={48} className="text-muted-foreground opacity-20" />
                     <p className="text-muted-foreground">No matching products found in your inventory.</p>
                  </div>
               )}
            </div>
          )}
        </section>
      )}

      {/* Coupons Section */}
      {activeTab === 'coupons' && (
        <section className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
           <div className="flex justify-between items-center">
              <h2 className="text-2xl font-bold">Promo Coupons</h2>
              <button 
                onClick={() => setShowCouponModal(true)}
                className="bg-primary text-white px-6 py-2 rounded-xl font-bold flex items-center gap-2"
              >
                 <Plus size={18} /> Create Coupon
              </button>
           </div>

           <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {coupons.map(coupon => (
                 <div key={coupon.id} className="bg-white border-2 border-dashed border-border p-6 rounded-3xl relative overflow-hidden group">
                    <div className="absolute top-0 right-0 w-16 h-16 bg-primary/5 rounded-bl-3xl -z-0"></div>
                    <div className="flex justify-between items-start mb-4 relative z-10">
                       <div>
                          <p className="text-xs font-black text-primary uppercase tracking-widest">{coupon.discountPercent}% OFF</p>
                          <h4 className="text-2xl font-black font-mono tracking-tighter">{coupon.code}</h4>
                       </div>
                       <button 
                        onClick={() => sellerService.deleteCoupon(coupon.id).then(fetchCoupons)}
                        className="text-muted-foreground hover:text-red-500 transition-colors"
                       >
                          <Trash2 size={18} />
                       </button>
                    </div>
                    <div className="space-y-1 text-sm text-muted-foreground">
                       <p>Min Order: <span className="font-bold text-foreground">${coupon.minOrderAmount}</span></p>
                       <p>Expires: <span className="font-bold text-foreground">{new Date(coupon.expiryDate).toLocaleDateString()}</span></p>
                    </div>
                 </div>
              ))}
              {coupons.length === 0 && (
                <div className="md:col-span-2 lg:col-span-3 py-20 text-center bg-muted/30 rounded-3xl border-2 border-dashed border-border">
                   <Ticket size={48} className="mx-auto text-muted-foreground opacity-20 mb-4" />
                   <p className="text-muted-foreground">No active coupons. Create one to boost your sales!</p>
                </div>
              )}
           </div>
        </section>
      )}
    </div>
  );
};

export default SellerDashboard;
