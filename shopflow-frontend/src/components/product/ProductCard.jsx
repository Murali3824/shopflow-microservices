import React from 'react';
import { Link } from 'react-router-dom';
import { ShoppingCart, Star } from 'lucide-react';
import { useCart } from '../../context/CartContext';
import { toast } from 'react-toastify';

const ProductCard = ({ product }) => {
  const { addItem } = useCart();
  
  // Format price - assuming backend gives it in cents or standard currency units
  const formattedPrice = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(product.price);

  const handleAddToCart = async (e) => {
    e.preventDefault(); // Prevent navigation to detail page
    try {
      // Find default SKU if multiple exist
      const skuId = product.skus?.[0]?.id || product.id; 
      await addItem(skuId, 1);
      toast.success(`${product.name} added to cart!`);
    } catch (error) {
      toast.error('Failed to add to cart. Please log in.');
    }
  };

  return (
    <div className="card-premium group overflow-hidden flex flex-col h-full">
      <Link to={`/product/${product.id}`} className="relative block h-64 overflow-hidden">
        <img
          src={product.primaryImage?.url || 'https://via.placeholder.com/400x400?text=No+Image'}
          alt={product.name}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
        />
        {product.discount > 0 && (
          <span className="absolute top-2 left-2 bg-red-500 text-white text-xs font-bold px-2 py-1 rounded">
            -{product.discount}%
          </span>
        )}
      </Link>

      <div className="p-4 flex flex-col flex-grow">
        <div className="flex justify-between items-start mb-2">
          <Link to={`/product/${product.id}`} className="hover:text-primary transition-colors">
            <h3 className="font-bold text-lg line-clamp-1">{product.name}</h3>
          </Link>
          <div className="flex items-center text-xs text-yellow-500 bg-yellow-50 px-1 rounded">
            <Star size={12} fill="currentColor" />
            <span className="ml-0.3 font-bold">{product.rating || '4.5'}</span>
          </div>
        </div>

        <p className="text-muted-foreground text-sm line-clamp-2 mb-4 flex-grow">
          {product.description}
        </p>

        <div className="flex items-center justify-between mt-auto">
          <div>
            <span className="block text-xl font-black text-foreground">{formattedPrice}</span>
            {product.oldPrice && (
              <span className="text-xs text-muted-foreground line-through">${product.oldPrice}</span>
            )}
          </div>
          <button
            onClick={handleAddToCart}
            className="p-2 bg-primary text-white rounded-full hover:bg-primary-dark transition-colors shadow-sm hover:shadow-md"
            title="Add to Cart"
          >
            <ShoppingCart size={20} />
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductCard;
