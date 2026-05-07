import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Package, ChevronRight, Clock, CheckCircle2, Truck, XCircle, RotateCcw, Loader2, ShoppingBag } from 'lucide-react';
import { orderService } from '../services/modules/order.service';
import { toast } from 'react-toastify';

const OrderHistoryPage = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        const data = await orderService.getOrders();
        setOrders(data || []);
      } catch (err) {
        toast.error("Failed to load your orders");
      } finally {
        setLoading(false);
      }
    };
    fetchOrders();
  }, []);

  const getStatusInfo = (status) => {
    switch (status) {
      case 'PENDING': return { icon: Clock, color: 'text-amber-500', bg: 'bg-amber-50' };
      case 'CONFIRMED': return { icon: CheckCircle2, color: 'text-blue-500', bg: 'bg-blue-50' };
      case 'SHIPPED': return { icon: Truck, color: 'text-indigo-500', bg: 'bg-indigo-50' };
      case 'DELIVERED': return { icon: CheckCircle2, color: 'text-green-500', bg: 'bg-green-50' };
      case 'CANCELLED': return { icon: XCircle, color: 'text-red-500', bg: 'bg-red-50' };
      case 'RETURN_REQUESTED': return { icon: RotateCcw, color: 'text-purple-500', bg: 'bg-purple-50' };
      default: return { icon: Package, color: 'text-muted-foreground', bg: 'bg-muted' };
    }
  };

  if (loading) return (
    <div className="flex flex-col items-center justify-center py-40 gap-4">
      <Loader2 className="animate-spin text-primary" size={48} />
      <p className="text-muted-foreground animate-pulse">Fetching your order history...</p>
    </div>
  );

  return (
    <div className="py-8 space-y-8">
      <h1 className="text-4xl font-black">My Orders</h1>

      {orders.length > 0 ? (
        <div className="space-y-6">
          {orders.map((order) => {
            const status = getStatusInfo(order.status);
            return (
              <div key={order.id} className="bg-white border border-border rounded-3xl overflow-hidden shadow-sm hover:shadow-md transition-all group">
                <div className="p-6 md:p-8 flex flex-col md:flex-row md:items-center justify-between gap-6">
                  {/* Order Info */}
                  <div className="space-y-1">
                    <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground">Order ID</p>
                    <p className="font-mono text-sm font-black truncate max-w-[200px]">{order.id}</p>
                    <p className="text-sm text-muted-foreground">Placed on {new Date(order.createdAt).toLocaleDateString()}</p>
                  </div>

                  {/* Order Preview (first 3 items) */}
                  <div className="flex -space-x-3 overflow-hidden py-1">
                    {order.items?.slice(0, 3).map((item, idx) => (
                      <div key={idx} className="w-12 h-12 rounded-full border-2 border-white bg-muted overflow-hidden">
                        <img src={item.imageUrl} alt="" className="w-full h-full object-cover" />
                      </div>
                    ))}
                    {order.items?.length > 3 && (
                      <div className="w-12 h-12 rounded-full border-2 border-white bg-primary/10 text-primary flex items-center justify-center text-xs font-bold">
                        +{order.items.length - 3}
                      </div>
                    )}
                  </div>

                  {/* Status & Total */}
                  <div className="flex items-center gap-8">
                    <div className="text-right">
                       <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground mb-1">Total</p>
                       <p className="text-2xl font-black text-primary">${order.totalAmount?.toFixed(2)}</p>
                    </div>
                    
                    <div className={`flex items-center gap-2 px-4 py-2 rounded-full font-bold text-sm ${status.bg} ${status.color}`}>
                       <status.icon size={18} />
                       {order.status}
                    </div>

                    <Link 
                      to={`/orders/${order.id}`}
                      className="p-3 bg-muted rounded-full hover:bg-primary hover:text-white transition-all group-hover:translate-x-1"
                    >
                      <ChevronRight size={24} />
                    </Link>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="text-center py-24 bg-muted/30 rounded-3xl border border-dashed border-border">
          <Package size={64} className="mx-auto text-muted-foreground opacity-20 mb-6" />
          <h2 className="text-2xl font-bold mb-2">No orders yet</h2>
          <p className="text-muted-foreground mb-8">Ready to start your first shopping spree?</p>
          <Link to="/shop" className="btn-primary px-8 py-4 rounded-full flex items-center gap-2 mx-auto w-fit">
             Start Shopping <ShoppingBag size={20} />
          </Link>
        </div>
      )}
    </div>
  );
};

export default OrderHistoryPage;
