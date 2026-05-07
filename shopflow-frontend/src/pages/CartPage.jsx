import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Trash2, Plus, Minus, ArrowRight, ShoppingBag, Loader2, Tag } from 'lucide-react';
import { useCart } from '../context/CartContext';
import { toast } from 'react-toastify';

const CartPage = () => {
  const { cart, loading, updateItem, removeItem, applyCoupon } = useCart();
  const [coupon, setCoupon] = useState('');
  const [isApplyingCoupon, setIsApplyingCoupon] = useState(false);
  const navigate = useNavigate();

  const handleUpdateQuantity = async (skuId, currentQty, delta) => {
    const newQty = currentQty + delta;
    if (newQty < 1) return;
    try {
      await updateItem(skuId, newQty);
    } catch (err) {
      toast.error("Failed to update quantity");
    }
  };

  const handleRemove = async (skuId) => {
    try {
      await removeItem(skuId);
      toast.info("Item removed from cart");
    } catch (err) {
      toast.error("Failed to remove item");
    }
  };

  const handleApplyCoupon = async (e) => {
    e.preventDefault();
    if (!coupon.trim()) return;
    setIsApplyingCoupon(true);
    try {
      await applyCoupon(coupon);
      toast.success("Coupon applied successfully!");
      setCoupon('');
    } catch (err) {
      toast.error("Invalid or expired coupon");
    } finally {
      setIsApplyingCoupon(false);
    }
  };

  if (!cart || cart.items?.length === 0) {
    return (
      <div className="text-center py-20 bg-muted/30 rounded-3xl border border-dashed border-border flex flex-col items-center">
        <div className="w-24 h-24 bg-white rounded-full flex items-center justify-center text-muted-foreground shadow-sm mb-6">
          <ShoppingBag size={48} />
        </div>
        <h2 className="text-3xl font-black mb-2">Your Cart is Empty</h2>
        <p className="text-muted-foreground mb-8 max-w-sm">
          Looks like you haven't added anything to your cart yet. Explore our premium collection and find something you love.
        </p>
        <Link to="/shop" className="btn-primary px-8 py-4 rounded-full flex items-center gap-2">
           Browse Products <ArrowRight size={20} />
        </Link>
      </div>
    );
  }

  return (
    <div className="py-8">
      <h1 className="text-4xl font-black mb-8 flex items-center gap-4">
         Your Shopping Cart
         <span className="text-sm font-bold bg-primary text-white px-3 py-1 rounded-full">
            {cart.items.length} {cart.items.length === 1 ? 'Item' : 'Items'}
         </span>
      </h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
        {/* Cart Items List */}
        <div className="lg:col-span-2 space-y-6">
          {cart.items.map((item) => (
            <div key={item.skuId} className="flex gap-6 p-6 bg-white border border-border rounded-2xl group transition-all hover:border-primary/50 hover:shadow-sm">
              <Link to={`/product/${item.productId}`} className="w-24 h-24 sm:w-32 sm:h-32 rounded-xl overflow-hidden bg-muted flex-shrink-0">
                <img 
                  src={item.imageUrl || 'https://via.placeholder.com/200x200?text=Product'} 
                  alt={item.productName} 
                  className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" 
                />
              </Link>

              <div className="flex flex-col justify-between flex-grow">
                <div className="flex justify-between items-start">
                  <div>
                    <Link to={`/product/${item.productId}`} className="font-bold text-xl hover:text-primary transition-colors block">
                      {item.productName}
                    </Link>
                    <p className="text-sm text-muted-foreground mt-1">
                      Option: <span className="font-semibold text-foreground">{item.skuCode}</span>
                    </p>
                  </div>
                  <button 
                    onClick={() => handleRemove(item.skuId)}
                    className="p-2 text-muted-foreground hover:text-red-500 hover:bg-red-50 rounded-full transition-all"
                  >
                    <Trash2 size={20} />
                  </button>
                </div>

                <div className="flex items-center justify-between mt-4">
                  <div className="flex items-center border border-border rounded-lg bg-white">
                    <button 
                      onClick={() => handleUpdateQuantity(item.skuId, item.quantity, -1)}
                      className="p-2 hover:bg-muted rounded-l-lg"
                    >
                      <Minus size={16} />
                    </button>
                    <span className="w-10 text-center font-bold">{item.quantity}</span>
                    <button 
                      onClick={() => handleUpdateQuantity(item.skuId, item.quantity, 1)}
                      className="p-2 hover:bg-muted rounded-r-lg"
                    >
                      <Plus size={16} />
                    </button>
                  </div>
                  <span className="text-xl font-black">${(item.price * item.quantity).toFixed(2)}</span>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Order Summary Sidebar */}
        <aside className="space-y-6">
          <div className="p-8 bg-muted/50 border border-border rounded-2xl sticky top-24">
            <h3 className="text-xl font-bold mb-6 border-b border-border pb-4">Order Summary</h3>
            
            <div className="space-y-4 mb-8">
              <div className="flex justify-between text-muted-foreground">
                <span>Subtotal</span>
                <span className="font-bold text-foreground">${cart.subtotal?.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>Shipping</span>
                <span className="text-primary font-bold">FREE</span>
              </div>
              {cart.discount > 0 && (
                <div className="flex justify-between text-green-600 font-bold bg-green-50 px-2 py-1 rounded">
                  <span>Discount</span>
                  <span>-${cart.discount?.toFixed(2)}</span>
                </div>
              )}
              <div className="flex justify-between text-muted-foreground">
                <span>Estimated Tax</span>
                <span className="font-bold text-foreground">${(cart.total * 0.05).toFixed(2)}</span>
              </div>
              <div className="pt-4 border-t border-border flex justify-between items-center">
                <span className="text-lg font-bold">Total</span>
                <span className="text-3xl font-black text-primary">${cart.total?.toFixed(2)}</span>
              </div>
            </div>

            {/* Coupon Code */}
            <form onSubmit={handleApplyCoupon} className="mb-8">
               <label className="text-xs font-bold uppercase tracking-wider text-muted-foreground mb-2 block">Promo Code</label>
               <div className="flex gap-2">
                  <div className="relative flex-grow">
                    <input 
                      type="text" 
                      placeholder="Enter code" 
                      className="w-full pl-8 pr-4 py-2 rounded-lg border border-border focus:ring-2 focus:ring-primary/20 outline-none"
                      value={coupon}
                      onChange={(e) => setCoupon(e.target.value.toUpperCase())}
                    />
                    <Tag className="absolute left-2 top-2.5 text-muted-foreground" size={16} />
                  </div>
                  <button 
                    disabled={isApplyingCoupon || !coupon}
                    className="bg-foreground text-white px-4 py-2 rounded-lg font-bold hover:bg-black transition-all disabled:opacity-50"
                  >
                    {isApplyingCoupon ? <Loader2 className="animate-spin" size={16} /> : 'Apply'}
                  </button>
               </div>
            </form>

            <button 
              onClick={() => navigate('/checkout')}
              className="w-full btn-primary py-4 rounded-xl flex items-center justify-center gap-3 font-black text-lg shadow-lg hover:shadow-primary/30"
            >
              PROCEED TO CHECKOUT <ArrowRight size={20} />
            </button>
            <div className="mt-4 flex items-center justify-center gap-4 text-xs text-muted-foreground">
               <span className="flex items-center gap-1"><ShieldCheck size={14} /> Secure</span>
               <span className="flex items-center gap-1">Fast Shipping</span>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
};

export default CartPage;
