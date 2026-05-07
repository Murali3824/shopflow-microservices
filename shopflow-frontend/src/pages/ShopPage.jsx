import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, SlidersHorizontal, ChevronDown, LayoutGrid, List as ListIcon, Loader2 } from 'lucide-react';
import ProductCard from '../components/product/ProductCard';
import { productService } from '../services/modules/product.service';

const ShopPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get('q') || '';
  const categoryId = searchParams.get('category') || '';
  
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [viewMode, setViewMode] = useState('grid');
  const [sortBy, setSortBy] = useState('newest');

  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const data = await productService.getCategories();
        setCategories(data || []);
      } catch (err) {
        console.error("Failed to fetch categories", err);
      }
    };
    fetchCategories();
  }, []);

  useEffect(() => {
    const fetchProducts = async () => {
      setLoading(true);
      try {
        let response;
        if (query) {
          response = await productService.searchProducts(query, page, 12);
        } else {
          response = await productService.getProducts(page, 12);
        }
        setProducts(response.content || []);
        setTotalElements(response.totalElements || 0);
      } catch (err) {
        console.error("Failed to fetch products", err);
      } finally {
        setLoading(false);
      }
    };
    fetchProducts();
  }, [query, categoryId, page, sortBy]);

  const handleCategoryChange = (id) => {
    if (categoryId === id) {
      searchParams.delete('category');
    } else {
      searchParams.set('category', id);
    }
    setSearchParams(searchParams);
    setPage(0);
  };

  return (
    <div className="flex flex-col md:flex-row gap-8 py-8">
      {/* Sidebar Filters */}
      <aside className="w-full md:w-64 space-y-8 flex-shrink-0">
        <div>
          <h3 className="text-sm font-bold uppercase tracking-wider mb-4 border-b pb-2 flex items-center gap-2">
             <SlidersHorizontal size={16} /> Filters
          </h3>
          
          <div className="space-y-6">
            {/* Categories */}
            <div>
              <h4 className="font-bold mb-3">Category</h4>
              <div className="space-y-2">
                {categories.map((cat) => (
                  <label key={cat.id} className="flex items-center gap-2 cursor-pointer group">
                    <input 
                      type="checkbox" 
                      className="w-4 h-4 rounded border-border text-primary focus:ring-primary/20"
                      checked={categoryId === cat.id}
                      onChange={() => handleCategoryChange(cat.id)}
                    />
                    <span className={`text-sm group-hover:text-primary transition-colors ${categoryId === cat.id ? 'text-primary font-bold' : 'text-muted-foreground'}`}>
                      {cat.name}
                    </span>
                  </label>
                ))}
              </div>
            </div>

            {/* Price range mockup */}
            <div>
              <h4 className="font-bold mb-3">Price Range</h4>
              <div className="space-y-4">
                <input type="range" min="0" max="1000" className="w-full accent-primary" />
                <div className="flex items-center justify-between text-xs font-mono text-muted-foreground">
                  <span>$0</span>
                  <span>$1000+</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-grow">
        {/* Toolbar */}
        <div className="flex flex-wrap items-center justify-between gap-4 mb-8 bg-muted/30 p-4 rounded-xl border border-border">
          <div className="text-sm">
            Showing <span className="font-bold">{products.length}</span> of <span className="font-bold">{totalElements}</span> results
            {query && <span> for "<span className="italic">{query}</span>"</span>}
          </div>

          <div className="flex items-center gap-4">
            <div className="flex border border-border rounded-lg bg-white overflow-hidden">
               <button 
                  onClick={() => setViewMode('grid')}
                  className={`p-2 transition-colors ${viewMode === 'grid' ? 'bg-primary text-white' : 'hover:bg-muted'}`}
               >
                 <LayoutGrid size={18} />
               </button>
               <button 
                  onClick={() => setViewMode('list')}
                  className={`p-2 transition-colors ${viewMode === 'list' ? 'bg-primary text-white' : 'hover:bg-muted'}`}
               >
                 <ListIcon size={18} />
               </button>
            </div>

            <div className="relative">
              <select 
                className="appearance-none bg-white border border-border rounded-lg py-2 pl-4 pr-10 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
              >
                <option value="newest">Newest First</option>
                <option value="price-low">Price: Low to High</option>
                <option value="price-high">Price: High to Low</option>
                <option value="rating">Top Rated</option>
              </select>
              <ChevronDown className="absolute right-3 top-2.5 text-muted-foreground pointer-events-none" size={16} />
            </div>
          </div>
        </div>

        {/* Product Grid */}
        {loading ? (
          <div className="flex flex-col items-center justify-center py-40 gap-4">
             <Loader2 className="animate-spin text-primary" size={48} />
             <p className="text-muted-foreground animate-pulse font-medium">Fetching the best deals...</p>
          </div>
        ) : (
          <>
            {products.length > 0 ? (
              <div className={`grid gap-8 ${viewMode === 'grid' ? 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-3' : 'grid-cols-1'}`}>
                {products.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            ) : (
              <div className="text-center py-40 space-y-4">
                <Search size={64} className="mx-auto text-muted-foreground opacity-20" />
                <h3 className="text-2xl font-bold">No products found</h3>
                <p className="text-muted-foreground">Try adjusting your filters or search terms.</p>
                <button 
                  onClick={() => {
                    setSearchParams({});
                    setPage(0);
                  }}
                  className="btn-primary mt-4"
                >
                  Clear All Filters
                </button>
              </div>
            )}

            {/* Pagination */}
            {totalElements > 0 && (
              <div className="flex justify-center mt-12 gap-2">
                <button 
                  disabled={page === 0}
                  onClick={() => setPage(page - 1)}
                  className="px-4 py-2 border border-border rounded-lg hover:bg-muted disabled:opacity-50"
                >
                   Previous
                </button>
                <span className="px-4 py-2 bg-primary text-white rounded-lg font-bold">{page + 1}</span>
                <button 
                  disabled={(page + 1) * 12 >= totalElements}
                  onClick={() => setPage(page + 1)}
                  className="px-4 py-2 border border-border rounded-lg hover:bg-muted disabled:opacity-50"
                >
                   Next
                </button>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
};

export default ShopPage;
