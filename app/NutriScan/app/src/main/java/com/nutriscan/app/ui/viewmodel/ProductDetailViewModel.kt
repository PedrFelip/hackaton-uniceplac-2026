package com.nutriscan.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscan.app.data.api.RateLimitException
import com.nutriscan.app.data.local.AppDatabase
import com.nutriscan.app.data.model.Product
import com.nutriscan.app.data.repository.HistoryRepository
import com.nutriscan.app.data.repository.ProductRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = false,
    val isFromCache: Boolean = false,
    val error: String? = null,
    val alternatives: List<Product> = emptyList(),
    val isLoadingAlternatives: Boolean = false
)

class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val alternativesCacheDao = AppDatabase.getInstance(application).alternativesCacheDao()
    private val repository = ProductRepository(alternativesCacheDao)
    private val historyRepository = HistoryRepository(application)

    private val _state = MutableStateFlow(ProductDetailUiState())
    val state: StateFlow<ProductDetailUiState> = _state.asStateFlow()

    fun loadProduct(barcode: String) {
        Log.d("ProductDetail", "loadProduct: $barcode")
        viewModelScope.launch {
            // 1. Tenta cache
            val cached = historyRepository.getProduct(barcode)
            if (cached != null) {
                Log.d("ProductDetail", "cache HIT: $barcode")
                _state.value = _state.value.copy(
                    product = cached, isLoading = false, isFromCache = true,
                    error = null, alternatives = emptyList()
                )
                refreshFromApi(barcode)
            } else {
                Log.d("ProductDetail", "cache MISS: $barcode")
                _state.value = _state.value.copy(isLoading = true, error = null, alternatives = emptyList())
                fetchFromApi(barcode)
            }
        }
    }

    private suspend fun fetchFromApi(barcode: String) {
        repository.getProductByBarcode(barcode)
            .onSuccess { product ->
                _state.value = _state.value.copy(product = product, isLoading = false, isFromCache = false)
                historyRepository.save(product)
                loadAlternatives(product)
            }
            .onFailure { e ->
                val errorMessage = when (e) {
                    is RateLimitException -> e.message
                    else -> e.message ?: "Erro ao carregar produto"
                }
                _state.value = _state.value.copy(
                    isLoading = false, error = errorMessage
                )
            }
    }

    private fun refreshFromApi(barcode: String) {
        viewModelScope.launch {
            repository.getProductByBarcode(barcode)
                .onSuccess { product ->
                    _state.value = _state.value.copy(product = product, isFromCache = false)
                    historyRepository.save(product)
                    loadAlternatives(product)
                }
                .onFailure { e ->
                    Log.w("ProductDetail", "API refresh failed, keeping cache", e)
                }
        }
    }

    private fun loadAlternatives(product: Product) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingAlternatives = true)
            delay(350)
            repository.getHealthierAlternatives(product)
                .onSuccess { alternatives ->
                    _state.value = _state.value.copy(alternatives = alternatives, isLoadingAlternatives = false)
                    alternatives.forEach { historyRepository.save(it) }
                }
                .onFailure {
                    _state.value = _state.value.copy(isLoadingAlternatives = false)
                }
        }
    }
}
