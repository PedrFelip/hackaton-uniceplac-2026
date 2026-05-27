package com.nutriscan.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.local.AppDatabase
import com.nutriscan.app.data.model.Product
import com.nutriscan.app.data.repository.CartRepository
import com.nutriscan.app.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

data class CartItemUI(
    val id: Long,
    val barcode: String,
    val product: Product?
)

data class CartState(
    val items: List<CartItemUI> = emptyList(),
    val isLoading: Boolean = false,
    val averageNutriScore: String? = null,
    val totalItems: Int = 0,
    val totalSugar: Double = 0.0,
    val totalSodium: Double = 0.0,
    val totalSaturatedFat: Double = 0.0,
    val alertCount: Int = 0
)

class CartViewModel(application: Application) : AndroidViewModel(application) {
    private val cartDao = AppDatabase.getInstance(application).cartDao()
    private val productRepository = ProductRepository()
    private val repository = CartRepository(cartDao, productRepository)

    private val _uiState = MutableStateFlow(CartState(isLoading = true))
    val uiState: StateFlow<CartState> = _uiState.asStateFlow()

    init {
        loadCart()
    }

    private fun loadCart() {
        viewModelScope.launch {
            repository.getCartItems()
                .catch { 
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { cartItems ->
                    _uiState.value = _uiState.value.copy(isLoading = true)
                    
                    // Fetch products in parallel for better performance
                    val deferredItems = cartItems.map { item ->
                        async {
                            val productResult = repository.getProductDetails(item.barcode)
                            CartItemUI(item.id, item.barcode, productResult.getOrNull())
                        }
                    }
                    val itemsUI = deferredItems.awaitAll()
                    
                    calculateStats(itemsUI)
                }
        }
    }

    private fun calculateStats(itemsUI: List<CartItemUI>) {
        var totalNutriScoreValue = 0.0
        var scoredItemsCount = 0
        
        var totalSugar = 0.0
        var totalSodium = 0.0
        var totalSaturatedFat = 0.0
        var alertCount = 0

        for (item in itemsUI) {
            val product = item.product ?: continue
            val nutriments = product.nutriments ?: continue

            nutriments.sugars100g?.let { 
                totalSugar += it 
                // Define 15g per 100g as high sugar risk
                if (it > 15.0) {
                    alertCount++
                }
            }
            nutriments.sodium100g?.let { totalSodium += it }
            nutriments.saturatedFat100g?.let { totalSaturatedFat += it }

            product.nutriscoreGrade?.let { grade ->
                val value = when(grade.lowercase()) {
                    "a" -> 1
                    "b" -> 2
                    "c" -> 3
                    "d" -> 4
                    "e" -> 5
                    else -> 0
                }
                if (value > 0) {
                    totalNutriScoreValue += value
                    scoredItemsCount++
                }
            }
        }

        val averageNutriScore = if (scoredItemsCount > 0) {
            val avg = Math.round(totalNutriScoreValue / scoredItemsCount).toInt()
            when (avg) {
                1 -> "A"
                2 -> "B"
                3 -> "C"
                4 -> "D"
                5 -> "E"
                else -> null
            }
        } else null

        _uiState.value = CartState(
            items = itemsUI,
            isLoading = false,
            averageNutriScore = averageNutriScore,
            totalItems = itemsUI.size,
            totalSugar = totalSugar,
            totalSodium = totalSodium,
            totalSaturatedFat = totalSaturatedFat,
            alertCount = alertCount
        )
    }

    fun addToCart(barcode: String) {
        viewModelScope.launch {
            repository.addToCart(barcode)
        }
    }

    fun removeFromCart(id: Long) {
        viewModelScope.launch {
            repository.removeFromCart(id)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }
}
