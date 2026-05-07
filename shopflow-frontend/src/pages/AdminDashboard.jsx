import React, { useState, useEffect } from 'react';
import { adminService } from '../services/modules/admin.service';
import { Users, ShoppingBag, Package, BarChart3, Search, Ban, CheckCircle, ShieldAlert, Loader2, ArrowUpRight, ArrowDownRight } from 'lucide-react';
import { toast } from 'react-toastify';

const AdminDashboard = () => {
  const [activeTab, setActiveTab] = useState('overview');
  const [users, setUsers] = useState([]);
  const [sellers, setSellers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalProducts: 0,
    totalOrders: 0,
    revenue: '$45,200',
    revenueGrowth: '+12.5%'
  });

  useEffect(() => {
    fetchData();
  }, [activeTab]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'users') {
        const data = await adminService.getAllUsers();
        setUsers(data.content || []);
      } else if (activeTab === 'sellers') {
        const data = await adminService.getPendingSellers();
        setSellers(data || []);
      }
      // Mock stats fetch for overview
      setStats({
        totalUsers: 1250,
        totalProducts: 450,
        totalOrders: 820,
        revenue: '$45,200',
        revenueGrowth: '+12.5%'
      });
    } catch (err) {
      toast.error("Failed to fetch admin data");
    } finally {
      setLoading(false);
    }
  };

  const handleBanToggle = async (userId, currentlyActive) => {
    try {
      if (currentlyActive) {
        await adminService.banUser(userId);
        toast.warning("User has been banned");
      } else {
        await adminService.unbanUser(userId);
        toast.success("User has been unbanned");
      }
      setUsers(users.map(u => u.id === userId ? { ...u, active: !currentlyActive } : u));
    } catch (err) {
      toast.error("Failed to update user status");
    }
  };

  const handleSellerAction = async (sellerId, action) => {
    try {
      if (action === 'approve') {
        await adminService.approveSeller(sellerId);
        toast.success("Seller approved!");
      } else {
        await adminService.rejectSeller(sellerId);
        toast.warning("Seller application rejected.");
      }
      setSellers(sellers.filter(s => s.userId !== sellerId));
    } catch (err) {
      toast.error("Action failed.");
    }
  };

  const StatCard = ({ icon: Icon, label, value, trend, positive }) => (
    <div className="bg-white border border-border p-8 rounded-3xl shadow-sm space-y-4">
       <div className="flex justify-between items-start">
          <div className="p-3 bg-primary/5 text-primary rounded-xl">
            <Icon size={24} />
          </div>
          <div className={`flex items-center text-xs font-bold ${positive ? 'text-green-600' : 'text-red-500'}`}>
             {trend}
             {positive ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
          </div>
       </div>
       <div>
          <p className="text-sm font-bold text-muted-foreground uppercase tracking-widest leading-none mb-2">{label}</p>
          <p className="text-3xl font-black">{value}</p>
       </div>
    </div>
  );

  return (
    <div className="py-8 space-y-12">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
           <h1 className="text-4xl font-black">Admin Control Center</h1>
           <p className="text-muted-foreground mt-2">Platform-wide oversight and management</p>
        </div>
        <div className="flex bg-muted p-1 rounded-2xl border border-border">
           <button 
             onClick={() => setActiveTab('overview')}
             className={`px-6 py-2 rounded-xl font-bold text-sm transition-all ${activeTab === 'overview' ? 'bg-white text-primary shadow-sm' : 'text-muted-foreground hover:text-foreground'}`}
           >
              Overview
           </button>
           <button 
             onClick={() => setActiveTab('users')}
             className={`px-6 py-2 rounded-xl font-bold text-sm transition-all ${activeTab === 'users' ? 'bg-white text-primary shadow-sm' : 'text-muted-foreground hover:text-foreground'}`}
           >
              Users
           </button>
           <button 
             onClick={() => setActiveTab('sellers')}
             className={`px-6 py-2 rounded-xl font-bold text-sm transition-all ${activeTab === 'sellers' ? 'bg-white text-primary shadow-sm' : 'text-muted-foreground hover:text-foreground'}`}
           >
              Sellers
           </button>
           <button 
             onClick={() => setActiveTab('orders')}
             className={`px-6 py-2 rounded-xl font-bold text-sm transition-all ${activeTab === 'orders' ? 'bg-white text-primary shadow-sm' : 'text-muted-foreground hover:text-foreground'}`}
           >
              Orders
           </button>
        </div>
      </div>

      {activeTab === 'overview' && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
            <StatCard icon={Users} label="Total Members" value={stats.totalUsers} trend="+3.2%" positive={true} />
            <StatCard icon={Package} label="Total Inventory" value={stats.totalProducts} trend="+1.5%" positive={true} />
            <StatCard icon={ShoppingBag} label="Monthly Orders" value={stats.totalOrders} trend="+5.4%" positive={true} />
            <StatCard icon={BarChart3} label="Gross Revenue" value={stats.revenue} trend={stats.revenueGrowth} positive={true} />
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
             <div className="bg-white border border-border rounded-3xl p-8 shadow-sm">
                <h3 className="text-xl font-bold mb-6">Recent Activity</h3>
                <div className="space-y-6">
                   {[
                     { user: "Sarah J.", action: "registered as Seller", time: "2 mins ago" },
                     { user: "John D.", action: "placed an order for $1,200", time: "15 mins ago" },
                     { user: "Admin", action: "banned user mark_99", time: "1 hour ago" },
                     { user: "Store A", action: "uploaded 5 new products", time: "3 hours ago" },
                   ].map((act, i) => (
                     <div key={i} className="flex gap-4 items-start pb-6 border-b border-border last:border-0 last:pb-0">
                        <div className="w-10 h-10 rounded-full bg-muted flex-shrink-0"></div>
                        <div>
                          <p className="text-sm font-bold text-foreground">
                             {act.user} <span className="font-normal text-muted-foreground">{act.action}</span>
                          </p>
                          <p className="text-xs text-muted-foreground mt-1">{act.time}</p>
                        </div>
                     </div>
                   ))}
                </div>
             </div>
             
             <div className="bg-primary text-white rounded-3xl p-10 shadow-xl shadow-primary/20 flex flex-col justify-between">
                <div className="space-y-4">
                   <h3 className="text-2xl font-black">Platform Health</h3>
                   <p className="opacity-80">All systems are operational. Configuration server is synced with latest microservices definitions.</p>
                </div>
                <div className="flex gap-8 mt-12 bg-white/10 p-6 rounded-2xl backdrop-blur-md">
                   <div>
                      <p className="text-xs font-bold opacity-60 uppercase tracking-widest">Gateway Latency</p>
                      <p className="text-2xl font-black">42ms</p>
                   </div>
                   <div className="border-l border-white/20 pl-8">
                      <p className="text-xs font-bold opacity-60 uppercase tracking-widest">Active Sessions</p>
                      <p className="text-2xl font-black">214</p>
                   </div>
                </div>
             </div>
          </div>
        </>
      )}

      {activeTab === 'sellers' && (
        <section className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
           <div className="flex justify-between items-center">
              <h2 className="text-2xl font-bold">Seller Applications</h2>
              <div className="bg-amber-100 text-amber-800 px-4 py-1 rounded-full text-xs font-bold">
                 {sellers.length} Pending
              </div>
           </div>

           {loading ? (
             <div className="flex justify-center py-20"><Loader2 className="animate-spin text-primary" size={48} /></div>
           ) : (
             <div className="bg-white border border-border rounded-3xl overflow-hidden shadow-sm">
                <table className="w-full text-left">
                   <thead className="bg-muted/50 border-b border-border text-xs font-bold text-muted-foreground uppercase tracking-widest">
                      <tr>
                         <th className="px-8 py-4">Store Details</th>
                         <th className="px-8 py-4">Business Email</th>
                         <th className="px-8 py-4">Commission</th>
                         <th className="px-8 py-4 text-right">Actions</th>
                      </tr>
                   </thead>
                   <tbody className="divide-y divide-border text-sm">
                      {sellers.map(s => (
                         <tr key={s.userId} className="hover:bg-muted/10 transition-colors">
                            <td className="px-8 py-6">
                               <div className="space-y-1">
                                  <p className="font-bold">{s.storeName}</p>
                                  <p className="text-xs text-muted-foreground line-clamp-1">{s.storeDescription}</p>
                               </div>
                            </td>
                            <td className="px-8 py-6 text-muted-foreground font-medium">{s.storeEmail}</td>
                            <td className="px-8 py-6">
                               <span className="font-black text-primary">10%</span>
                            </td>
                            <td className="px-8 py-6 text-right">
                               <div className="flex justify-end gap-2">
                                  <button 
                                    onClick={() => handleSellerAction(s.userId, 'approve')}
                                    className="bg-green-600 text-white px-4 py-2 rounded-xl font-bold hover:bg-green-700 transition-all flex items-center gap-2"
                                  >
                                     <CheckCircle size={16} /> Approve
                                  </button>
                                  <button 
                                    onClick={() => handleSellerAction(s.userId, 'reject')}
                                    className="bg-red-50 text-red-600 px-4 py-2 rounded-xl font-bold hover:bg-red-100 transition-all flex items-center gap-2"
                                  >
                                     <Ban size={16} /> Reject
                                  </button>
                               </div>
                            </td>
                         </tr>
                      ))}
                   </tbody>
                </table>
                {sellers.length === 0 && (
                   <div className="p-20 text-center flex flex-col items-center gap-4">
                      <ShieldAlert size={48} className="text-muted-foreground opacity-20" />
                      <p className="text-muted-foreground font-medium">No pending seller applications at the moment.</p>
                   </div>
                )}
             </div>
           )}
        </section>
      )}

      {activeTab === 'users' && (
        <section className="space-y-6">
          <div className="flex justify-between items-center">
             <h2 className="text-2xl font-bold">Member Directory</h2>
             <div className="relative w-80">
                <input 
                  type="text" 
                  placeholder="Search users by name or info..." 
                  className="w-full pl-10 pr-4 py-3 bg-white border border-border rounded-2xl shadow-sm focus:ring-2 focus:ring-primary/20 outline-none"
                />
                <Search className="absolute left-3 top-3.5 text-muted-foreground" size={18} />
             </div>
          </div>

          {loading ? (
             <div className="flex justify-center py-20"><Loader2 className="animate-spin text-primary" size={48} /></div>
          ) : (
            <div className="bg-white border border-border rounded-3xl overflow-hidden shadow-sm">
               <table className="w-full text-left">
                  <thead className="bg-muted/50 border-b border-border text-xs font-bold text-muted-foreground uppercase tracking-widest">
                     <tr>
                        <th className="px-8 py-4">User Details</th>
                        <th className="px-8 py-4">Roles</th>
                        <th className="px-8 py-4">Joined Date</th>
                        <th className="px-8 py-4">Status</th>
                        <th className="px-8 py-4 text-right">Protection</th>
                     </tr>
                  </thead>
                  <tbody className="divide-y divide-border text-sm">
                     {users.map(u => (
                        <tr key={u.id} className="hover:bg-muted/10 transition-colors group">
                           <td className="px-8 py-6">
                              <div>
                                 <p className="font-bold text-foreground">{u.name}</p>
                                 <p className="text-xs text-muted-foreground">{u.email}</p>
                              </div>
                           </td>
                           <td className="px-8 py-6">
                              <div className="flex gap-1 flex-wrap">
                                 {u.roles?.map(role => (
                                    <span key={role} className="text-[10px] font-black bg-primary/5 text-primary px-2 py-0.5 rounded border border-primary/10 tracking-widest">
                                       {role === 'ROLE_USER' ? 'CUSTOMER' : role.replace('ROLE_', '')}
                                    </span>
                                 ))}
                              </div>
                           </td>
                           <td className="px-8 py-6 text-muted-foreground">Oct 12, 2025</td>
                           <td className="px-8 py-6">
                              <div className={`flex items-center gap-2 font-bold ${u.active ? 'text-green-600' : 'text-red-500'}`}>
                                 {u.active ? <CheckCircle size={14} /> : <Ban size={14} />}
                                 {u.active ? 'Active' : 'Banned'}
                              </div>
                           </td>
                           <td className="px-8 py-6 text-right">
                              <button 
                                onClick={() => handleBanToggle(u.id, u.active)}
                                className={`px-4 py-2 rounded-xl font-bold transition-all flex items-center gap-2 ml-auto ${u.active ? 'bg-red-50 text-red-600 hover:bg-red-600 hover:text-white' : 'bg-green-50 text-green-600 hover:bg-green-600 hover:text-white'}`}
                              >
                                 {u.active ? <ShieldAlert size={16} /> : <CheckCircle size={16} />}
                                 {u.active ? 'Ban User' : 'Grant Access'}
                              </button>
                           </td>
                        </tr>
                     ))}
                  </tbody>
               </table>
            </div>
          )}
        </section>
      )}
    </div>
  );
};

export default AdminDashboard;
