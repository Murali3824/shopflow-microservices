import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShoppingCart, User, Search, Menu, X, LogOut, Package, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';

const Navbar = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const { itemCount } = useCart();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const navigate = useNavigate();

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/shop?q=${encodeURIComponent(searchQuery)}`);
    }
  };

  return (
    <nav className="bg-white border-b border-border sticky top-0 z-50">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="text-2xl font-bold text-primary flex items-center gap-2">
            <span className="bg-primary text-white p-1 rounded-md">SF</span>
            <span className="hidden sm:inline">ShopFlow</span>
          </Link>

          {/* Desktop Search */}
          <div className="hidden md:flex flex-1 max-w-md mx-8">
            <form onSubmit={handleSearch} className="relative w-full">
              <input
                type="text"
                placeholder="Search products..."
                className="w-full bg-muted border border-border rounded-full py-2 px-4 pl-10 focus:outline-none focus:ring-2 focus:ring-primary/20"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <Search className="absolute left-3 top-2.5 text-muted-foreground w-5 h-5" />
            </form>
          </div>

          {/* Actions */}
          <div className="flex items-center gap-4">
            <Link to="/cart" className="relative p-2 text-foreground hover:bg-muted rounded-full transition-colors">
              <ShoppingCart className="w-6 h-6" />
              {itemCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-accent text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
                  {itemCount}
                </span>
              )}
            </Link>

            {isAuthenticated ? (
              <div className="relative group">
                <button className="flex items-center gap-2 p-1 hover:bg-muted rounded-full transition-colors">
                  <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">
                    {user?.name?.charAt(0) || 'U'}
                  </div>
                </button>
                <div className="absolute right-0 mt-2 w-48 bg-white border border-border rounded-lg shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 py-2">
                  <div className="px-4 py-2 border-b border-border mb-2">
                    <p className="font-semibold truncate">{user?.name}</p>
                    <p className="text-xs text-muted-foreground truncate">{user?.email}</p>
                  </div>
                  <Link to="/profile" className="flex items-center gap-3 px-4 py-2 hover:bg-muted text-sm capitalize">
                    <User className="w-4 h-4" /> Profile
                  </Link>
                  <Link to="/orders" className="flex items-center gap-3 px-4 py-2 hover:bg-muted text-sm capitalize">
                    <Package className="w-4 h-4" /> My Orders
                  </Link>
                  {user?.roles?.includes('ADMIN') && (
                    <Link to="/admin" className="flex items-center gap-3 px-4 py-2 hover:bg-accent/10 text-accent text-sm font-semibold capitalize">
                      <ShieldCheck className="w-4 h-4" /> Admin Panel
                    </Link>
                  )}
                   {user?.roles?.includes('SELLER') && (
                    <Link to="/seller" className="flex items-center gap-3 px-4 py-2 hover:bg-primary/10 text-primary text-sm font-semibold capitalize">
                      <ShieldCheck className="w-4 h-4" /> Seller Hub
                    </Link>
                  )}
                  <button onClick={logout} className="w-full flex items-center gap-3 px-4 py-2 hover:bg-red-50 text-red-600 text-sm capitalize">
                    <LogOut className="w-4 h-4" /> Logout
                  </button>
                </div>
              </div>
            ) : (
              <Link to="/login" className="btn-primary flex items-center gap-2 text-sm">
                <User className="w-4 h-4" /> Login
              </Link>
            )}

            {/* Mobile menu button */}
            <button
              className="md:hidden p-2 text-foreground hover:bg-muted rounded-md"
              onClick={() => setIsMenuOpen(!isMenuOpen)}
            >
              {isMenuOpen ? <X /> : <Menu />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Search & Menu */}
      {isMenuOpen && (
        <div className="md:hidden border-t border-border p-4 bg-white animate-in slide-in-from-top duration-200">
          <form onSubmit={handleSearch} className="mb-4">
            <input
              type="text"
              placeholder="Search products..."
              className="w-full bg-muted border border-border rounded-md py-2 px-4 focus:outline-none"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </form>
          <div className="flex flex-col gap-2">
            <Link to="/categories" className="py-2 hover:text-primary transition-colors">Categories</Link>
            <Link to="/shop" className="py-2 hover:text-primary transition-colors">Latest Deals</Link>
          </div>
        </div>
      )}
    </nav>
  );
};

export default Navbar;
