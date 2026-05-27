package com.nutriscan.app.data.repository

import android.content.Context
import android.util.Log
import com.nutriscan.app.data.local.AppDatabase
import com.nutriscan.app.data.local.HistoryItem
import com.nutriscan.app.data.model.Product
import kotlinx.coroutines.flow.Flow

class HistoryRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).historyDao()

    suspend fun save(product: Product) {
        val barcode = product.code
        if (barcode.isNullOrBlank()) {
            Log.w("HistoryRepo", "SKIP: barcode vazio - ${product.productName}")
            return
        }
        val item = HistoryItem.fromProduct(product)
        if (item == null) {
            Log.w("HistoryRepo", "SKIP: fromProduct retornou null - $barcode")
            return
        }
        try {
            dao.upsert(item)
            Log.d("HistoryRepo", "OK: $barcode - ${product.productName}")
        } catch (e: Exception) {
            Log.e("HistoryRepo", "ERRO upsert $barcode: ${e.message}", e)
        }
    }

    suspend fun getAll(): List<HistoryItem> {
        return try { dao.getAll() } catch (e: Exception) {
            Log.e("HistoryRepo", "ERRO getAll: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getProduct(barcode: String): Product? {
        return try {
            dao.getByBarcode(barcode)?.toProduct()
        } catch (e: Exception) {
            Log.e("HistoryRepo", "ERRO getProduct $barcode: ${e.message}", e)
            null
        }
    }

    fun getAllFlow(): Flow<List<HistoryItem>> = dao.getAllFlow()

    suspend fun clearAll() {
        try { dao.clearAll() } catch (e: Exception) {
            Log.e("HistoryRepo", "ERRO clearAll: ${e.message}", e)
        }
    }
}
