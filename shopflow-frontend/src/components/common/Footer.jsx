import React from 'react';
import { Link } from 'react-router-dom';
import { Facebook, Twitter, Instagram, Youtube, Phone, Mail, MapPin } from 'lucide-react';

const Footer = () => {
  return (
    <footer className="bg-white border-t border-border mt-auto">
      <div className="container mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand */}
          <div className="space-y-4">
            <Link to="/" className="text-2xl font-bold text-primary flex items-center gap-2">
              <span className="bg-primary text-white p-1 rounded-md">SF</span>
              <span>ShopFlow</span>
            </Link>
            <p className="text-muted-foreground text-sm">
              Your one-stop destination for premium products at unbeatable prices. Experience the future of shopping.
            </p>
            <div className="flex gap-4">
              <a href="#" className="p-2 hover:bg-primary/10 hover:text-primary rounded-full transition-colors"><Facebook size={20} /></a>
              <a href="#" className="p-2 hover:bg-primary/10 hover:text-primary rounded-full transition-colors"><Twitter size={20} /></a>
              <a href="#" className="p-2 hover:bg-primary/10 hover:text-primary rounded-full transition-colors"><Instagram size={20} /></a>
              <a href="#" className="p-2 hover:bg-primary/10 hover:text-primary rounded-full transition-colors"><Youtube size={20} /></a>
            </div>
          </div>

          {/* Quick Links */}
          <div>
            <h4 className="font-bold mb-4">Shop</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><Link to="/shop" className="hover:text-primary">All Products</Link></li>
              <li><Link to="/categories" className="hover:text-primary">Categories</Link></li>
              <li><Link to="/orders" className="hover:text-primary">Track Order</Link></li>
              <li><Link to="/seller/register" className="hover:text-primary">Become a Seller</Link></li>
            </ul>
          </div>

          {/* Support */}
          <div>
            <h4 className="font-bold mb-4">Support</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><Link to="/faq" className="hover:text-primary">FAQ</Link></li>
              <li><Link to="/shipping" className="hover:text-primary">Shipping Policy</Link></li>
              <li><Link to="/privacy" className="hover:text-primary">Privacy Policy</Link></li>
              <li><Link to="/terms" className="hover:text-primary">Terms & Conditions</Link></li>
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h4 className="font-bold mb-4">Contact Us</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li className="flex items-center gap-2"><Phone size={16} /> +1 (555) 123-4567</li>
              <li className="flex items-center gap-2"><Mail size={16} /> support@shopflow.com</li>
              <li className="flex items-center gap-2"><MapPin size={16} /> 123 Commerce St, Tech City</li>
            </ul>
          </div>
        </div>
        
        <div className="border-t border-border mt-12 pt-8 text-center text-sm text-muted-foreground">
          <p>© {new Date().getFullYear()} ShopFlow. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
