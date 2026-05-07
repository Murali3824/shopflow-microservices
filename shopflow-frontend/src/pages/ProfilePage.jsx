import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { userService } from '../services/modules/user.service';
import { User, Mail, MapPin, Camera, Save, Plus, Trash2, Shield, Loader2, CheckCircle2 } from 'lucide-react';
import { toast } from 'react-toastify';

const ProfilePage = () => {
  const { user, login } = useAuth();
  const [activeTab, setActiveTab] = useState('profile');
  const [addresses, setAddresses] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  
  const [profileForm, setProfileForm] = useState({
    name: user?.name || '',
    email: user?.email || '',
    phoneNumber: user?.phoneNumber || ''
  });

  useEffect(() => {
    if (activeTab === 'addresses') {
      fetchAddresses();
    }
  }, [activeTab]);

  const fetchAddresses = async () => {
    setIsLoading(true);
    try {
      const data = await userService.getAddresses();
      setAddresses(data || []);
    } catch (err) {
      toast.error("Failed to load addresses");
    } finally {
      setIsLoading(false);
    }
  };

  const handleProfileUpdate = async (e) => {
    e.preventDefault();
    setIsSaving(true);
    try {
      await userService.updateProfile(profileForm);
      toast.success("Profile updated successfully!");
    } catch (err) {
      toast.error("Update failed.");
    } finally {
      setIsSaving(false);
    }
  };

  const handleAvatarUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    const formData = new FormData();
    formData.append('file', file);
    
    setIsLoading(true);
    try {
      await userService.uploadAvatar(file);
      toast.success("Avatar updated!");
      // Optionally refresh profile here
    } catch (err) {
      toast.error("Upload failed.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteAddress = async (id) => {
    if (!window.confirm("Delete this address?")) return;
    try {
      await userService.deleteAddress(id);
      setAddresses(addresses.filter(a => a.id !== id));
      toast.info("Address deleted");
    } catch (err) {
      toast.error("Failed to delete address");
    }
  };

  const handleSetDefault = async (id) => {
    try {
      await userService.setDefaultAddress(id);
      fetchAddresses();
      toast.success("Default address updated");
    } catch (err) {
      toast.error("Failed to update default");
    }
  };

  const SidebarItem = ({ id, icon: Icon, label }) => (
    <button
      onClick={() => setActiveTab(id)}
      className={`w-full flex items-center gap-4 px-6 py-4 rounded-2xl font-bold transition-all ${activeTab === id ? 'bg-primary text-white shadow-lg shadow-primary/20' : 'text-muted-foreground hover:bg-muted'}`}
    >
      <Icon size={20} />
      {label}
    </button>
  );

  return (
    <div className="py-8 space-y-8">
      <h1 className="text-4xl font-black">Account Settings</h1>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-12">
        {/* Sidebar */}
        <aside className="space-y-2">
          <SidebarItem id="profile" icon={User} label="Profile Info" />
          <SidebarItem id="addresses" icon={MapPin} label="Manage Addresses" />
          <SidebarItem id="security" icon={Shield} label="Security" />
        </aside>

        {/* Main Content */}
        <div className="lg:col-span-3">
          {activeTab === 'profile' && (
            <div className="bg-white border border-border rounded-3xl p-8 shadow-sm space-y-12">
              {/* Avatar Section */}
              <div className="flex flex-col md:flex-row items-center gap-8">
                <div className="relative group">
                  <div className="w-32 h-32 rounded-full overflow-hidden border-4 border-muted bg-muted flex items-center justify-center text-muted-foreground">
                    {user?.avatarUrl ? (
                      <img src={user.avatarUrl} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <User size={64} />
                    )}
                  </div>
                  <label className="absolute bottom-0 right-0 p-2 bg-primary text-white rounded-full cursor-pointer shadow-lg hover:bg-primary-dark transition-all">
                    <Camera size={20} />
                    <input type="file" className="hidden" onChange={handleAvatarUpload} accept="image/*" />
                  </label>
                </div>
                <div>
                   <h2 className="text-2xl font-black mb-1">{user?.name}</h2>
                   <p className="text-muted-foreground mb-4">{user?.email}</p>
                   <div className="flex gap-2">
                      {user?.roles?.map(role => (
                        <span key={role} className="text-[10px] font-black tracking-widest bg-primary/10 text-primary px-2 py-0.5 rounded border border-primary/20">
                          {role}
                        </span>
                      ))}
                   </div>
                </div>
              </div>

              {/* Form */}
              <form onSubmit={handleProfileUpdate} className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-8 border-t border-border">
                <div className="space-y-2">
                  <label className="text-sm font-bold flex items-center gap-2">Name</label>
                  <input 
                    className="w-full p-3 bg-muted border border-border rounded-xl focus:ring-2 focus:ring-primary/20 outline-none"
                    value={profileForm.name}
                    onChange={e => setProfileForm({...profileForm, name: e.target.value})}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-bold flex items-center gap-2 text-muted-foreground">Email (Managed by Auth)</label>
                  <input 
                    className="w-full p-3 bg-muted border border-border rounded-xl opacity-50 cursor-not-allowed outline-none"
                    value={profileForm.email}
                    disabled
                  />
                </div>
                <div className="space-y-2">
                   <label className="text-sm font-bold flex items-center gap-2">Phone Number</label>
                   <input 
                    className="w-full p-3 bg-muted border border-border rounded-xl focus:ring-2 focus:ring-primary/20 outline-none"
                    placeholder="+1 (555) 000-0000"
                    value={profileForm.phoneNumber}
                    onChange={e => setProfileForm({...profileForm, phoneNumber: e.target.value})}
                  />
                </div>
                <div className="md:col-span-2 flex justify-end">
                   <button 
                    disabled={isSaving}
                    className="btn-primary px-8 py-3 rounded-xl flex items-center gap-2 font-bold shadow-lg shadow-primary/20"
                   >
                     {isSaving ? <Loader2 className="animate-spin" /> : <Save size={20} />}
                     Save Changes
                   </button>
                </div>
              </form>
            </div>
          )}

          {activeTab === 'addresses' && (
            <div className="space-y-6">
               <div className="flex justify-between items-center bg-white border border-border rounded-3xl p-6 sm:px-8 shadow-sm">
                  <div>
                    <h2 className="text-xl font-bold">Saved Addresses</h2>
                    <p className="text-sm text-muted-foreground">Manage your delivery locations</p>
                  </div>
                  <button className="bg-primary text-white p-3 rounded-2xl hover:bg-primary-dark transition-all shadow-lg shadow-primary/20 flex items-center gap-2 font-bold px-6">
                    <Plus size={20} /> Add New
                  </button>
               </div>

               {isLoading ? (
                 <div className="flex justify-center py-20"><Loader2 className="animate-spin text-primary" size={48} /></div>
               ) : (
                 <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {addresses.map(addr => (
                      <div key={addr.id} className={`bg-white border-2 rounded-3xl p-6 shadow-sm flex flex-col justify-between transition-all ${addr.default ? 'border-primary' : 'border-border'}`}>
                         <div className="space-y-4">
                            <div className="flex justify-between items-start">
                               <div className="flex items-center gap-2">
                                  <h4 className="font-bold text-lg">{addr.name}</h4>
                                  {addr.default && <CheckCircle2 size={16} className="text-green-500" />}
                               </div>
                               <span className="text-[10px] font-black tracking-widest bg-muted px-2 py-0.5 rounded border border-border uppercase">
                                  {addr.type || 'HOME'}
                               </span>
                            </div>
                            <div className="text-sm text-muted-foreground space-y-1">
                               <p>{addr.streetAddress}</p>
                               <p>{addr.city}, {addr.state} {addr.zipCode}</p>
                               <p className="pt-2 text-foreground font-semibold">{addr.phoneNumber}</p>
                            </div>
                         </div>

                         <div className="flex items-center gap-4 mt-8 pt-6 border-t border-border">
                            {!addr.default && (
                              <button 
                                onClick={() => handleSetDefault(addr.id)}
                                className="text-xs font-black text-primary hover:underline uppercase tracking-wider"
                              >
                                Set As Default
                              </button>
                            )}
                            <button className="text-xs font-black text-muted-foreground hover:text-foreground uppercase tracking-wider ml-auto">
                               Edit
                            </button>
                            <button 
                              onClick={() => handleDeleteAddress(addr.id)}
                              className="text-xs font-black text-red-500 hover:bg-red-50 p-2 rounded-lg transition-colors"
                            >
                               <Trash2 size={16} />
                            </button>
                         </div>
                      </div>
                    ))}
                    {addresses.length === 0 && (
                      <div className="md:col-span-2 text-center py-20 bg-muted/30 rounded-3xl border-2 border-dashed border-border">
                        <MapPin size={48} className="mx-auto text-muted-foreground opacity-20 mb-4" />
                        <p className="text-muted-foreground">You haven't added any addresses yet.</p>
                      </div>
                    )}
                 </div>
               )}
            </div>
          )}

          {activeTab === 'security' && (
            <div className="bg-white border border-border rounded-3xl p-8 shadow-sm">
               <h2 className="text-xl font-bold mb-8">Security Settings</h2>
               <div className="space-y-8">
                  <div className="flex items-center justify-between p-6 bg-muted/50 rounded-2xl order border-border group hover:border-primary/50 transition-all">
                     <div className="space-y-1">
                        <h4 className="font-bold">Password</h4>
                        <p className="text-sm text-muted-foreground">Security level: Strong</p>
                     </div>
                     <button className="px-6 py-2 border-2 border-primary text-primary rounded-xl font-bold hover:bg-primary hover:text-white transition-all">
                        Change Password
                     </button>
                  </div>
                  <div className="flex items-center justify-between p-6 bg-muted/50 rounded-2xl order border-border opacity-50 select-none">
                     <div className="space-y-1">
                        <h4 className="font-bold flex items-center gap-2">Two-Factor Authentication <span className="text-[8px] bg-primary text-white px-1 rounded">PRO</span></h4>
                        <p className="text-sm text-muted-foreground">Add an extra layer of security</p>
                     </div>
                     <button className="px-6 py-2 border-2 border-border text-muted-foreground rounded-xl font-bold cursor-not-allowed" disabled>
                        Enable 2FA
                     </button>
                  </div>
               </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
