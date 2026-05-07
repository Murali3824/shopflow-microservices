import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Star, ShoppingBag, ShieldCheck, Truck, RotateCcw } from 'lucide-react';
import ProductCard from '../components/product/ProductCard';
import { productService } from '../services/modules/product.service';

const HomePage = () => {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [prodData, catData] = await Promise.all([
          productService.getProducts(0, 8),
          productService.getCategories()
        ]);
        setProducts(prodData.content || []);
        setCategories(catData || []);
      } catch (error) {
        console.error("Error fetching homepage data", error);
        // Fallback or error state
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  return (
    <div className="space-y-16 pb-20">
      {/* Hero Section */}
      <section className="relative h-[500px] rounded-3xl overflow-hidden bg-gradient-to-r from-primary to-primary-dark text-white flex items-center">
        <div className="container mx-auto px-12 z-10">
          <div className="max-w-xl space-y-6">
            <span className="bg-white/20 px-3 py-1 rounded-full text-sm font-bold backdrop-blur-md">New Collection 2026</span>
            <h1 className="text-5xl md:text-7xl font-black leading-tight">Elevate Your Lifestyle</h1>
            <p className="text-lg text-primary-foreground/80">
              Discover the finest tech, fashion, and home essentials curated specifically for those who demand excellence.
            </p>
            <div className="flex gap-4">
              <Link to="/shop" className="bg-white text-primary px-8 py-4 rounded-full font-bold flex items-center gap-2 hover:bg-opacity-90 transition-all">
                Shop Now <ArrowRight size={20} />
              </Link>
              <Link to="/categories" className="bg-transparent border border-white/30 px-8 py-4 rounded-full font-bold hover:bg-white/10 transition-all">
                Browse Categories
              </Link>
            </div>
          </div>
        </div>
        <div className="absolute right-0 top-0 w-1/2 h-full hidden lg:block">
           <div className="w-full h-full bg-[url('https://images.unsplash.com/photo-1523275335684-37898b6baf30?ixlib=rb-1.2.1&auto=format&fit=crop&w=1000&q=80')] bg-cover bg-center opacity-40 mix-blend-overlay"></div>
        </div>
      </section>

      {/* Features Wrapper */}
      <section className="grid grid-cols-2 md:grid-cols-4 gap-8 py-8 border-y border-border">
        {[
          { icon: Truck, title: "Fast Delivery", desc: "Across the globe" },
          { icon: ShieldCheck, title: "Secure Payment", desc: "100% guarantee" },
          { icon: RotateCcw, title: "Easy Returns", desc: "30-day window" },
          { icon: Star, title: "Premium Quality", desc: "Certified products" },
        ].map((feat, i) => (
          <div key={i} className="flex flex-col items-center text-center space-y-2">
            <div className="p-3 bg-primary/5 text-primary rounded-xl mb-2">
              <feat.icon size={28} />
            </div>
            <h4 className="font-bold">{feat.title}</h4>
            <p className="text-xs text-muted-foreground">{feat.desc}</p>
          </div>
        ))}
      </section>

      {/* Categories */}
      <section>
        <div className="flex justify-between items-end mb-8">
          <div>
            <h2 className="text-3xl font-bold">Shop by Category</h2>
            <p className="text-muted-foreground">Find what you need in our curated sections</p>
          </div>
          <Link to="/categories" className="text-primary font-bold flex items-center gap-1 hover:underline">
            View All <ArrowRight size={16} />
          </Link>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
          {categories.map((cat) => (
            <Link 
              key={cat.id} 
              to={`/shop?category=${cat.id}`}
              className="group p-6 bg-muted rounded-2xl flex flex-col items-center text-center gap-4 hover:bg-primary hover:text-white transition-all duration-300"
            >
              <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center text-primary shadow-sm group-hover:bg-white/20 group-hover:text-white transition-all">
                 <ShoppingBag size={32} />
              </div>
              <span className="font-bold text-sm">{(cat.name || 'Category').toUpperCase()}</span>
            </Link>
          ))}
          {categories.length === 0 && Array(6).fill(0).map((_, i) => (
             <div key={i} className="h-40 bg-muted animate-pulse rounded-2xl"></div>
          ))}
        </div>
      </section>

      {/* Featured Products */}
      <section>
        <div className="flex justify-between items-end mb-8">
          <div>
            <h2 className="text-3xl font-bold">New Arrivals</h2>
            <p className="text-muted-foreground">The latest trends and tech just for you</p>
          </div>
          <Link to="/shop" className="text-primary font-bold flex items-center gap-1 hover:underline">
            View Shop <ArrowRight size={16} />
          </Link>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-8">
            {Array(8).fill(0).map((_, i) => (
              <div key={i} className="h-80 bg-muted animate-pulse rounded-2xl"></div>
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-8">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}

        {products.length === 0 && !loading && (
          <div className="text-center py-20 bg-muted rounded-3xl">
             <ShoppingBag size={48} className="mx-auto text-muted-foreground mb-4 opacity-20" />
             <p className="text-muted-foreground">No products found. Start by adding some to your inventory!</p>
          </div>
        )}
      </section>

      {/* Newsletter */}
      <section className="bg-secondary rounded-3xl p-12 flex flex-col md:flex-row items-center justify-between gap-8">
        <div className="max-w-md">
          <h2 className="text-3xl font-bold mb-2">Join the inner circle</h2>
          <p className="text-muted-foreground">Subscribe to receive updates, access to exclusive deals, and more.</p>
        </div>
        <form className="flex w-full md:w-auto gap-2">
          <input 
            type="email" 
            placeholder="Enter your email" 
            className="flex-grow md:w-80 px-4 py-3 rounded-full border border-border focus:outline-none focus:ring-2 focus:ring-primary/20"
          />
          <button type="submit" className="bg-primary text-white px-8 py-3 rounded-full font-bold hover:bg-primary-dark transition-all">
            Subscribe
          </button>
        </form>
      </section>
    </div>
  );
};

export default HomePage;
