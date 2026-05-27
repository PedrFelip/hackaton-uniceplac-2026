package com.nutriscan.app.data.repository

import com.nutriscan.app.data.local.CartDao
import com.nutriscan.app.data.local.CartItem
import com.nutriscan.app.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CartRepository(
    private val cartDao: CartDao,
    private val productRepository: ProductRepository
) {
    fun getCartItems(): Flow<List<CartItem>> {
        return cartDao.getAllCartItems()
    }
    
    suspend fun getProductDetails(barcode: String): Result<Product> {
        return productRepository.getProductByBarcode(barcode)
    }

    suspend fun addToCart(barcode: String) {
        cartDao.insert(CartItem(barcode = barcode))
    }

    suspend fun removeFromCart(id: Long) {
        cartDao.deleteItem(id)
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }
}
