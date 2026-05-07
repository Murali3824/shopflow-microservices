import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ShoppingCart, Star, Heart, Share2, Truck, ShieldCheck, RotateCcw, Plus, Minus, Loader2 } from 'lucide-react';
import { productService } from '../services/modules/product.service';
import { reviewService } from '../services/modules/review.service';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { toast } from 'react-toastify';

const ProductDetailPage = () => {
  const { id } = useParams();
  const { addItem } = useCart();
  
  const { user } = useAuth();
  
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedSku, setSelectedSku] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [activeImage, setActiveImage] = useState(null);
  const [activeTab, setActiveTab] = useState('details');

  // Review State
  const [reviews, setReviews] = useState([]);
  const [reviewsLoading, setReviewsLoading] = useState(false);
  const [newReview, setNewReview] = useState({ rating: 5, comment: '' });
  const [isSubmittingReview, setIsSubmittingReview] = useState(false);

  useEffect(() => {
    const fetchProduct = async () => {
      try {
        const data = await productService.getProductById(id);
        setProduct(data);
        if (data.skus?.length > 0) {
          setSelectedSku(data.skus[0]);
        }
        setActiveImage(data.primaryImage?.url || 'https://via.placeholder.com/600x600?text=ShopFlow+Product');
        fetchReviews();
      } catch (err) {
        console.error("Failed to fetch product", err);
        toast.error("Failed to load product details.");
      } finally {
        setLoading(false);
      }
    };
    fetchProduct();
  }, [id]);

  const fetchReviews = async () => {
    setReviewsLoading(true);
    try {
      const data = await reviewService.getProductReviews(id);
      setReviews(data.content || []);
    } catch (err) {
      console.error("Failed to fetch reviews");
    } finally {
      setReviewsLoading(false);
    }
  };

  const handleReviewSubmit = async (e) => {
    e.preventDefault();
    if (!user) return toast.info("Please login to leave a review");
    if (!newReview.comment.trim()) return toast.warn("Please add a comment");

    setIsSubmittingReview(true);
    try {
      await reviewService.submitReview({
        productId: id,
        rating: newReview.rating,
        comment: newReview.comment
      });
      toast.success("Review submitted!");
      setNewReview({ rating: 5, comment: '' });
      fetchReviews();
    } catch (err) {
      toast.error("Failed to submit review");
    } finally {
      setIsSubmittingReview(false);
    }
  };

  const handleAddToCart = async () => {
    if (!selectedSku) return toast.warn("Please select an option");
    try {
      await addItem(selectedSku.id, quantity);
      toast.success(`${product.name} added to cart!`);
    } catch (err) {
      toast.error("Failed to add to cart. Try logging in.");
    }
  };

  if (loading) return (
    <div className="flex flex-col items-center justify-center py-40 gap-4">
      <Loader2 className="animate-spin text-primary" size={48} />
      <p className="text-muted-foreground animate-pulse font-medium">Loading product details...</p>
    </div>
  );

  if (!product) return <div className="text-center py-20">Product not found.</div>;

  const images = [
    { url: product.primaryImage?.url },
    ...(product.images || [])
  ].filter(img => img?.url);

  return (
    <div className="py-8 space-y-12">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
        {/* Left: Images */}
        <div className="space-y-4">
          <div className="aspect-square bg-muted rounded-3xl overflow-hidden border border-border">
            <img 
              src={activeImage} 
              alt={product.name} 
              className="w-full h-full object-cover transition-transform duration-700 hover:scale-110" 
            />
          </div>
          <div className="flex gap-4 overflow-x-auto pb-2 scrollbar-hide">
            {images.map((img, i) => (
              <button 
                key={i} 
                onClick={() => setActiveImage(img.url)}
                className={`flex-shrink-0 w-20 h-20 rounded-xl overflow-hidden border-2 transition-all ${activeImage === img.url ? 'border-primary' : 'border-border'}`}
              >
                <img src={img.url} alt="thumbnail" className="w-full h-full object-cover" />
              </button>
            ))}
          </div>
        </div>

        {/* Right: Info */}
        <div className="flex flex-col">
          <div className="mb-6">
            <Link to={`/shop?category=${product.category?.id}`} className="text-primary text-sm font-bold uppercase tracking-widest hover:underline mb-2 block">
              {product.category?.name || 'Category'}
            </Link>
            <h1 className="text-4xl font-black mb-4 leading-tight">{product.name}</h1>
            
            <div className="flex items-center gap-4 mb-6">
              <div className="flex items-center bg-yellow-50 text-yellow-600 px-3 py-1 rounded-full text-sm font-bold border border-yellow-200">
                <Star size={16} fill="currentColor" className="mr-1" />
                {product.rating || '4.5'}
              </div>
              <span className="text-muted-foreground text-sm border-l pl-4">{reviews.length} Reviews</span>
            </div>

            <div className="mb-8">
              <span className="text-4xl font-black text-primary">
                ${selectedSku?.price || product.price}
              </span>
              {product.oldPrice && (
                <span className="ml-4 text-xl text-muted-foreground line-through">${product.oldPrice}</span>
              )}
            </div>

            <p className="text-muted-foreground leading-relaxed mb-8">
              {product.description}
            </p>
          </div>

          <div className="space-y-8 mb-8 border-y border-border py-8">
            {/* SKU Selection mockup (e.g., Size) */}
            {product.skus?.length > 1 && (
              <div>
                <h4 className="font-bold mb-3 uppercase text-xs tracking-wider">Select Option</h4>
                <div className="flex flex-wrap gap-2">
                  {product.skus.map((sku) => (
                    <button
                      key={sku.id}
                      onClick={() => setSelectedSku(sku)}
                      className={`px-6 py-2 rounded-xl font-bold border-2 transition-all ${selectedSku?.id === sku.id ? 'border-primary bg-primary text-white' : 'border-border bg-white text-foreground hover:bg-muted'}`}
                    >
                      {sku.skuCode}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <div className="flex items-center gap-6">
              <div className="flex items-center border border-border rounded-xl bg-white">
                <button 
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  className="p-3 hover:bg-muted rounded-l-xl transition-colors"
                >
                  <Minus size={20} />
                </button>
                <span className="w-12 text-center font-bold text-lg">{quantity}</span>
                <button 
                  onClick={() => setQuantity(quantity + 1)}
                  className="p-3 hover:bg-muted rounded-r-xl transition-colors"
                >
                  <Plus size={20} />
                </button>
              </div>

              <button 
                onClick={handleAddToCart}
                className="flex-grow btn-primary py-4 flex items-center justify-center gap-3 font-black text-lg"
              >
                <ShoppingCart size={24} /> ADD TO CART
              </button>

              <button className="p-4 border border-border rounded-xl hover:bg-red-50 hover:text-red-500 transition-all">
                <Heart size={24} />
              </button>
            </div>
          </div>

          {/* Delivery & Returns Info */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-4">
             <div className="flex items-start gap-3">
                <Truck className="text-primary flex-shrink-0" size={24} />
                <div>
                  <h5 className="font-bold text-sm">Free Delivery</h5>
                  <p className="text-xs text-muted-foreground">Orders over $100</p>
                </div>
             </div>
             <div className="flex items-start gap-3">
                <ShieldCheck className="text-primary flex-shrink-0" size={24} />
                <div>
                  <h5 className="font-bold text-sm">2 Year Warranty</h5>
                  <p className="text-xs text-muted-foreground">Manufacturer coverage</p>
                </div>
             </div>
             <div className="flex items-start gap-3">
                <RotateCcw className="text-primary flex-shrink-0" size={24} />
                <div>
                  <h5 className="font-bold text-sm">30 Day Return</h5>
                  <p className="text-xs text-muted-foreground">Hassle-free process</p>
                </div>
             </div>
          </div>
        </div>
      </div>

      {/* Tabs / Bottom Section mockup */}
      <section className="bg-muted/30 rounded-3xl p-8 border border-border">
         <div className="flex gap-8 border-b border-border mb-8 overflow-x-auto pb-1">
            <button 
              onClick={() => setActiveTab('details')}
              className={`px-4 py-2 font-black transition-all ${activeTab === 'details' ? 'border-b-4 border-primary text-primary' : 'text-muted-foreground hover:text-primary'}`}
            >
              DETAILS
            </button>
            <button 
              onClick={() => setActiveTab('specs')}
              className={`px-4 py-2 font-black transition-all ${activeTab === 'specs' ? 'border-b-4 border-primary text-primary' : 'text-muted-foreground hover:text-primary'}`}
            >
              SPECIFICATIONS
            </button>
            <button 
              onClick={() => setActiveTab('reviews')}
              className={`px-4 py-2 font-black transition-all ${activeTab === 'reviews' ? 'border-b-4 border-primary text-primary' : 'text-muted-foreground hover:text-primary'}`}
            >
              REVIEWS ({reviews.length})
            </button>
         </div>

         {activeTab === 'details' && (
           <div className="prose max-w-none text-muted-foreground animate-in fade-in slide-in-from-bottom-2 duration-300">
              <p>
                Experience the next level of performance with the {product.name}. Designed for creators, thinkers, and professionals who demand nothing but the absolute best. Crafted from high-grade materials and featuring state-of-the-art technology, this product redefines what's possible in its category.
              </p>
              <ul className="list-disc ml-6 mt-4 space-y-2">
                <li>Premium grade construction for long-lasting durability</li>
                <li>Efficient performance optimized for power users</li>
                <li>Ergonomic design focused on user comfort and accessibility</li>
                <li>Comprehensive software/hardware ecosystem compatibility</li>
              </ul>
           </div>
         )}

         {activeTab === 'specs' && (
           <div className="grid grid-cols-1 md:grid-cols-2 gap-4 animate-in fade-in slide-in-from-bottom-2 duration-300">
              {[
                { label: 'Brand', value: 'ShopFlow Premium' },
                { label: 'Model', value: product.id?.substring(0,8).toUpperCase() },
                { label: 'Category', value: product.category?.name },
                { label: 'Weight', value: '1.2 kg' },
                { label: 'Dimensions', value: '30 x 20 x 5 cm' },
                { label: 'Warranty', value: '2 Years Manufacturer' },
              ].map((spec, i) => (
                <div key={i} className="flex justify-between p-4 bg-white rounded-xl border border-border">
                   <span className="text-muted-foreground font-bold">{spec.label}</span>
                   <span className="font-black text-foreground">{spec.value}</span>
                </div>
              ))}
           </div>
         )}

         {activeTab === 'reviews' && (
           <div className="space-y-12 animate-in fade-in slide-in-from-bottom-2 duration-300">
              {/* Submission Form */}
              <div className="bg-white p-8 rounded-2xl border border-border shadow-sm">
                 <h3 className="text-xl font-black mb-6">Write a Review</h3>
                 <form onSubmit={handleReviewSubmit} className="space-y-6">
                    <div>
                       <label className="block text-sm font-bold mb-2">Rating</label>
                       <div className="flex gap-2">
                          {[1, 2, 3, 4, 5].map((star) => (
                             <button
                                key={star}
                                type="button"
                                onClick={() => setNewReview({ ...newReview, rating: star })}
                                className={`p-1 transition-colors ${newReview.rating >= star ? 'text-yellow-500' : 'text-muted-foreground'}`}
                             >
                                <Star size={24} fill={newReview.rating >= star ? 'currentColor' : 'none'} />
                             </button>
                          ))}
                       </div>
                    </div>
                    <div>
                       <label className="block text-sm font-bold mb-2">Comment</label>
                       <textarea
                          className="w-full p-4 bg-muted border border-border rounded-xl focus:ring-2 focus:ring-primary/20 outline-none h-32"
                          placeholder="What did you think about this product?"
                          value={newReview.comment}
                          onChange={(e) => setNewReview({ ...newReview, comment: e.target.value })}
                       ></textarea>
                    </div>
                    <button 
                       disabled={isSubmittingReview}
                       className="btn-primary px-8 py-3 rounded-xl flex items-center gap-2 font-bold"
                    >
                       {isSubmittingReview ? <Loader2 className="animate-spin" /> : 'Submit Review'}
                    </button>
                 </form>
              </div>

              {/* Review List */}
              <div className="space-y-6">
                 {reviewsLoading ? (
                   <div className="flex justify-center py-10"><Loader2 className="animate-spin text-primary" size={32} /></div>
                 ) : reviews.length > 0 ? (
                    reviews.map((rev) => (
                      <div key={rev.id} className="bg-white p-8 rounded-2xl border border-border flex flex-col md:flex-row gap-8">
                         <div className="flex-shrink-0 text-center">
                            <div className="w-16 h-16 rounded-full bg-primary/10 text-primary flex items-center justify-center font-black text-xl mx-auto mb-2">
                               {rev.userName?.charAt(0) || 'U'}
                            </div>
                            <p className="font-bold text-sm">{rev.userName || 'Anonymous'}</p>
                            <p className="text-[10px] text-muted-foreground uppercase font-black">{new Date(rev.createdAt).toLocaleDateString()}</p>
                         </div>
                         <div className="flex-grow">
                            <div className="flex gap-1 mb-2">
                               {[1, 2, 3, 4, 5].map((s) => (
                                  <Star key={s} size={14} fill={rev.rating >= s ? '#eab308' : 'none'} className={rev.rating >= s ? 'text-yellow-500' : 'text-muted-foreground'} />
                               ))}
                            </div>
                            <p className="text-muted-foreground leading-relaxed">{rev.comment}</p>
                         </div>
                      </div>
                    ))
                 ) : (
                    <div className="text-center py-20 bg-white rounded-2xl border border-dashed border-border">
                       <Star size={48} className="mx-auto text-muted-foreground opacity-20 mb-4" />
                       <p className="text-muted-foreground">No reviews yet. Be the first to share your thoughts!</p>
                    </div>
                 )}
              </div>
           </div>
         )}
      </section>
    </div>
  );
};

export default ProductDetailPage;
