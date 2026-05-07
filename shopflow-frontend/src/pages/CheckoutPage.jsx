import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapPin, CreditCard, ChevronRight, Loader2, CheckCircle2, ShieldCheck, Home } from 'lucide-react';
import { userService } from '../services/modules/user.service';
import { orderService } from '../services/modules/order.service';
import { useCart } from '../context/CartContext';
import { toast } from 'react-toastify';

const CheckoutPage = () => {
  const { cart, clearCart } = useCart();
  const navigate = useNavigate();
  
  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('COD');
  const [loading, setLoading] = useState(true);
  const [isPlacingOrder, setIsPlacingOrder] = useState(false);

  useEffect(() => {
    if (!cart || cart.items?.length === 0) {
      navigate('/cart');
      return;
    }

    const fetchAddresses = async () => {
      try {
        const data = await userService.getAddresses();
        setAddresses(data || []);
        const defaultAddr = data.find(a => a.default) || data[0];
        if (defaultAddr) setSelectedAddressId(defaultAddr.id);
      } catch (err) {
        toast.error("Failed to load addresses");
      } finally {
        setLoading(false);
      }
    };
    fetchAddresses();
  }, [cart, navigate]);

  const handlePlaceOrder = async () => {
    if (!selectedAddressId) return toast.warn("Please select a delivery address");

    setIsPlacingOrder(true);
    try {
      const orderData = {
        addressId: selectedAddressId,
        paymentMethod: paymentMethod
      };
      const response = await orderService.placeOrder(orderData);
      toast.success("Order placed successfully!");
      clearCart();
      navigate(`/orders/${response.data.id}`, { state: { justPlaced: true } });
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to place order");
    } finally {
      setIsPlacingOrder(false);
    }
  };

  if (loading) return (
    <div className="flex flex-col items-center justify-center py-40 gap-4">
      <Loader2 className="animate-spin text-primary" size={48} />
      <p className="text-muted-foreground animate-pulse font-medium">Preparing your checkout...</p>
    </div>
  );

  return (
    <div className="py-8">
      <h1 className="text-4xl font-black mb-8">Secure Checkout</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
        {/* Main Steps */}
        <div className="lg:col-span-2 space-y-8">
          {/* Address Section */}
          <section className="bg-white border border-border rounded-3xl p-8 shadow-sm">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold flex items-center gap-2">
                <MapPin className="text-primary" /> 1. Delivery Address
              </h2>
              <button className="text-primary font-bold text-sm hover:underline">+ Add New</button>
            </div>

            <div className="space-y-4">
              {addresses.map((addr) => (
                <label 
                  key={addr.id} 
                  className={`block border-2 p-6 rounded-2xl cursor-pointer transition-all ${selectedAddressId === addr.id ? 'border-primary bg-primary/5 ring-4 ring-primary/5' : 'border-border hover:border-primary/50'}`}
                >
                  <div className="flex items-start gap-4">
                    <input 
                      type="radio" 
                      name="address" 
                      className="mt-1 w-5 h-5 text-primary"
                      checked={selectedAddressId === addr.id}
                      onChange={() => setSelectedAddressId(addr.id)}
                    />
                    <div className="flex-grow">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="font-bold text-lg">{addr.name}</span>
                        {addr.type && (
                          <span className="text-[10px] font-black uppercase tracking-widest bg-muted px-2 py-0.5 rounded border border-border">
                            {addr.type}
                          </span>
                        )}
                        {addr.default && <CheckCircle2 size={16} className="text-green-500" />}
                      </div>
                      <p className="text-muted-foreground">{addr.streetAddress}, {addr.city}</p>
                      <p className="text-muted-foreground">{addr.state}, {addr.zipCode}</p>
                      <p className="text-sm font-bold mt-2 text-foreground flex items-center gap-2">
                         <span className="text-muted-foreground font-normal">Phone:</span> {addr.phoneNumber}
                      </p>
                    </div>
                  </div>
                </label>
              ))}

              {addresses.length === 0 && (
                 <div className="text-center py-10 bg-muted/30 rounded-2xl border-2 border-dashed border-border">
                    <MapPin size={40} className="mx-auto text-muted-foreground mb-4 opacity-20" />
                    <p className="text-muted-foreground mb-4">You haven't saved any addresses yet.</p>
                    <button className="btn-primary">Add Your First Address</button>
                 </div>
              )}
            </div>
          </section>

          {/* Payment Section */}
          <section className="bg-white border border-border rounded-3xl p-8 shadow-sm">
            <h2 className="text-xl font-bold flex items-center gap-2 mb-6">
              <CreditCard className="text-primary" /> 2. Payment Method
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {[
                { id: 'COD', label: 'Cash on Delivery', desc: 'Pay when you receive' },
                { id: 'CARD', label: 'Credit / Debit Card', desc: 'Secure payment via Stripe' },
                { id: 'UPI', label: 'UPI / NetBanking', desc: 'Instant verification' },
              ].map((method) => (
                <label 
                  key={method.id} 
                  className={`block border-2 p-6 rounded-2xl cursor-pointer transition-all ${paymentMethod === method.id ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/50'}`}
                >
                  <div className="flex items-start gap-4">
                    <input 
                      type="radio" 
                      name="payment" 
                      className="mt-1 w-5 h-5 text-primary"
                      checked={paymentMethod === method.id}
                      onChange={() => setPaymentMethod(method.id)}
                    />
                    <div>
                      <span className="font-bold block">{method.label}</span>
                      <span className="text-xs text-muted-foreground">{method.desc}</span>
                    </div>
                  </div>
                </label>
              ))}
            </div>
          </section>
        </div>

        {/* Sidebar Summary */}
        <aside className="space-y-6">
          <div className="p-8 bg-muted/50 border border-border rounded-3xl sticky top-24">
            <h3 className="text-xl font-bold mb-6">Review Items</h3>
            <div className="space-y-4 mb-8 max-h-60 overflow-y-auto pr-2 custom-scrollbar">
              {cart.items.map((item) => (
                <div key={item.skuId} className="flex gap-4">
                   <div className="w-16 h-16 rounded-xl bg-white overflow-hidden flex-shrink-0 border border-border">
                      <img src={item.imageUrl} alt="" className="w-full h-full object-cover" />
                   </div>
                   <div className="flex-grow min-w-0">
                      <p className="font-bold text-sm truncate">{item.productName}</p>
                      <p className="text-xs text-muted-foreground">Qty: {item.quantity} × ${item.price}</p>
                   </div>
                </div>
              ))}
            </div>

            <div className="space-y-4 pt-6 border-t border-border">
              <div className="flex justify-between text-muted-foreground">
                <span>Subtotal</span>
                <span className="font-bold text-foreground">${cart.subtotal?.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>Shipping</span>
                <span className="text-primary font-bold">FREE</span>
              </div>
              <div className="flex justify-between text-xl font-black pt-4 border-t border-border">
                <span>Total</span>
                <span>${cart.total?.toFixed(2)}</span>
              </div>
            </div>

            <button 
              onClick={handlePlaceOrder}
              disabled={isPlacingOrder || !selectedAddressId}
              className="w-full btn-primary py-4 rounded-xl mt-8 flex items-center justify-center gap-3 font-black text-lg disabled:opacity-50"
            >
              {isPlacingOrder ? <Loader2 className="animate-spin" /> : <>CONFIRM ORDER <ChevronRight /></>}
            </button>
            <p className="text-[10px] text-center text-muted-foreground mt-4 flex items-center justify-center gap-1">
               <ShieldCheck size={12} /> By placing this order, you agree to our Terms of Service.
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
};

export default CheckoutPage;
