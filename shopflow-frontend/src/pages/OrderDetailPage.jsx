import React, { useState, useEffect } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import { Package, MapPin, CreditCard, ChevronLeft, Truck, CheckCircle2, RotateCcw, XCircle, AlertCircle, Loader2 } from 'lucide-react';
import { orderService } from '../services/modules/order.service';
import { toast } from 'react-toastify';

const OrderDetailPage = () => {
  const { id } = useParams();
  const location = useLocation();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isCancelling, setIsCancelling] = useState(false);

  useEffect(() => {
    const fetchOrder = async () => {
      try {
        const data = await orderService.getOrderById(id);
        setOrder(data);
        if (location.state?.justPlaced) {
           toast.success("Welcome aboard! Your order is being processed.");
        }
      } catch (err) {
        toast.error("Failed to load order details");
      } finally {
        setLoading(false);
      }
    };
    fetchOrder();
  }, [id, location.state]);

  const handleCancel = async () => {
    if (!window.confirm("Are you sure you want to cancel this order?")) return;
    setIsCancelling(true);
    try {
      const updated = await orderService.cancelOrder(id);
      setOrder(updated.data);
      toast.success("Order cancelled successfully");
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to cancel order");
    } finally {
      setIsCancelling(false);
    }
  };

  if (loading) return (
    <div className="flex flex-col items-center justify-center py-40 gap-4">
      <Loader2 className="animate-spin text-primary" size={48} />
      <p className="text-muted-foreground animate-pulse font-medium">Loading tracking data...</p>
    </div>
  );

  if (!order) return <div className="text-center py-20">Order not found.</div>;

  const steps = [
    { label: 'Pending', icon: Package, status: 'PENDING' },
    { label: 'Confirmed', icon: CheckCircle2, status: 'CONFIRMED' },
    { label: 'Shipped', icon: Truck, status: 'SHIPPED' },
    { label: 'Delivered', icon: CheckCircle2, status: 'DELIVERED' },
  ];

  const currentStep = steps.findIndex(s => s.status === order.status);
  const isCancelled = order.status === 'CANCELLED';

  return (
    <div className="py-8 space-y-12">
      <div className="flex items-center justify-between">
        <Link to="/orders" className="flex items-center gap-2 text-muted-foreground hover:text-primary transition-colors font-bold">
           <ChevronLeft size={20} /> Back to My Orders
        </Link>
        <div className="flex gap-4">
           {['PENDING', 'CONFIRMED'].includes(order.status) && (
             <button 
                onClick={handleCancel}
                disabled={isCancelling}
                className="px-6 py-2 border-2 border-red-500 text-red-500 rounded-full font-bold hover:bg-red-50 transition-all flex items-center gap-2"
             >
                {isCancelling ? <Loader2 size={18} className="animate-spin" /> : <XCircle size={18} />} 
                Cancel Order
             </button>
           )}
           {order.status === 'DELIVERED' && (
             <button className="px-6 py-2 bg-primary text-white rounded-full font-bold hover:bg-primary-dark transition-all flex items-center gap-2">
                <RotateCcw size={18} /> Request Return
             </button>
           )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
        <div className="lg:col-span-2 space-y-8">
          {/* Tracking */}
          <section className="bg-white border border-border rounded-3xl p-8 shadow-sm">
            <h2 className="text-xl font-bold mb-12">Order Tracking</h2>
            
            {isCancelled ? (
               <div className="flex items-center justify-center gap-4 p-8 bg-red-50 border border-red-100 rounded-2xl text-red-600">
                  <XCircle size={32} />
                  <div>
                    <h4 className="font-bold text-lg">Order Cancelled</h4>
                    <p className="text-sm">This order was cancelled on {new Date(order.updatedAt || order.createdAt).toLocaleDateString()}.</p>
                  </div>
               </div>
            ) : (
              <div className="relative flex justify-between">
                <div className="absolute top-1/2 left-0 w-full h-1 bg-muted -translate-y-1/2 -z-10"></div>
                <div 
                  className="absolute top-1/2 left-0 h-1 bg-primary -translate-y-1/2 -z-10 transition-all duration-1000"
                  style={{ width: `${(currentStep / (steps.length - 1)) * 100}%` }}
                ></div>

                {steps.map((step, i) => {
                  const Icon = step.icon;
                  const completed = i <= currentStep;
                  return (
                    <div key={i} className="flex flex-col items-center gap-3">
                       <div className={`w-12 h-12 rounded-full flex items-center justify-center transition-all duration-500 border-4 ${completed ? 'bg-primary text-white border-primary/20' : 'bg-white text-muted-foreground border-muted'}`}>
                          <Icon size={20} />
                       </div>
                       <span className={`text-xs font-bold uppercase tracking-wider ${completed ? 'text-primary' : 'text-muted-foreground'}`}>
                          {step.label}
                       </span>
                    </div>
                  );
                })}
              </div>
            )}
          </section>

          {/* Delivery Details */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
             <section className="bg-white border border-border rounded-3xl p-8 shadow-sm h-full">
                <h3 className="font-bold flex items-center gap-3 mb-4 text-muted-foreground uppercase text-xs tracking-widest">
                   <MapPin size={16} /> Delivery Address
                </h3>
                <div className="space-y-1">
                   <p className="font-black text-lg">{order.shippingAddress?.name}</p>
                   <p className="text-muted-foreground">{order.shippingAddress?.streetAddress}</p>
                   <p className="text-muted-foreground">{order.shippingAddress?.city}, {order.shippingAddress?.state} {order.shippingAddress?.zipCode}</p>
                   <p className="text-sm font-bold mt-4 pt-4 border-t border-border flex items-center gap-2">
                       <span className="text-muted-foreground font-normal">Phone:</span> {order.shippingAddress?.phoneNumber}
                   </p>
                </div>
             </section>

             <section className="bg-white border border-border rounded-3xl p-8 shadow-sm h-full">
                <h3 className="font-bold flex items-center gap-3 mb-4 text-muted-foreground uppercase text-xs tracking-widest">
                   <CreditCard size={16} /> Payment Info
                </h3>
                <div className="space-y-4">
                   <div className="flex items-center gap-4 bg-muted/50 p-4 rounded-2xl border border-border">
                      <div className="w-10 h-10 bg-white rounded-lg flex items-center justify-center text-primary border border-border shadow-xs">
                         <CreditCard size={20} />
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground uppercase font-bold tracking-tight">Method</p>
                        <p className="font-bold">{order.paymentMethod}</p>
                      </div>
                   </div>
                   <div className="flex items-center gap-2 text-green-600 bg-green-50 px-3 py-1 rounded-full w-fit text-xs font-bold ring-1 ring-green-100">
                      <CheckCircle2 size={14} /> Payment Captured
                   </div>
                </div>
             </section>
          </div>

          {/* Items */}
          <section className="bg-white border border-border rounded-3xl overflow-hidden shadow-sm">
             <div className="bg-muted/30 px-8 py-4 border-b border-border flex justify-between items-center text-xs font-bold uppercase tracking-widest text-muted-foreground">
                <span>Products</span>
                <span>Subtotal</span>
             </div>
             <div className="divide-y divide-border">
                {order.items?.map((item, idx) => (
                   <div key={idx} className="p-8 flex items-center justify-between gap-6 group hover:bg-muted/10 transition-colors">
                      <div className="flex items-center gap-6">
                         <div className="w-16 h-16 rounded-xl overflow-hidden border border-border bg-white flex-shrink-0 group-hover:shadow-md transition-all">
                            <img src={item.imageUrl} alt="" className="w-full h-full object-cover" />
                         </div>
                         <div>
                            <h4 className="font-bold hover:text-primary transition-colors">{item.productName}</h4>
                            <p className="text-xs text-muted-foreground mt-1">Qty: {item.quantity} × ${item.price.toFixed(2)}</p>
                            {item.skuCode && <span className="text-[10px] font-bold bg-muted px-2 py-0.5 rounded mt-2 inline-block uppercase">{item.skuCode}</span>}
                         </div>
                      </div>
                      <span className="font-black text-lg">${(item.price * item.quantity).toFixed(2)}</span>
                   </div>
                ))}
             </div>
          </section>
        </div>

        {/* Sidebar Summary */}
        <aside className="space-y-6">
           <div className="bg-primary text-white rounded-3xl p-8 shadow-xl shadow-primary/20 space-y-6">
              <h3 className="text-xl font-bold flex items-center gap-2 border-b border-white/20 pb-4">
                 <Package /> Order Summary
              </h3>
              <div className="space-y-4">
                 <div className="flex justify-between text-primary-foreground/70">
                    <span>Subtotal</span>
                    <span className="font-bold text-white">${(order.totalAmount - (order.discount || 0)).toFixed(2)}</span>
                 </div>
                 <div className="flex justify-between text-primary-foreground/70">
                    <span>Shipping</span>
                    <span className="font-bold text-white">FREE</span>
                 </div>
                 {order.discount > 0 && (
                   <div className="flex justify-between text-green-300 font-bold">
                      <span>Discount Applied</span>
                      <span>-${order.discount.toFixed(2)}</span>
                   </div>
                 )}
                 <div className="flex justify-between text-primary-foreground/70">
                    <span>Tax</span>
                    <span className="font-bold text-white">${(order.totalAmount * 0.05).toFixed(2)}</span>
                 </div>
                 <div className="pt-6 border-t border-white/20 flex justify-between items-center">
                    <span className="text-lg font-bold">Grand Total</span>
                    <span className="text-4xl font-black">${order.totalAmount?.toFixed(2)}</span>
                 </div>
              </div>
           </div>

           <div className="bg-white border border-border rounded-3xl p-8 shadow-sm">
               <h4 className="font-bold mb-4 flex items-center gap-2">
                  <AlertCircle size={18} className="text-primary" /> Need Help?
               </h4>
               <p className="text-sm text-muted-foreground leading-relaxed mb-6">
                  If you have any issues with this order, please contact our 24/7 customer support.
               </p>
               <button className="w-full btn-primary py-3 rounded-xl border border-primary hover:bg-white hover:text-primary transition-all">
                  Chat With Support
               </button>
           </div>
        </aside>
      </div>
    </div>
  );
};

export default OrderDetailPage;
