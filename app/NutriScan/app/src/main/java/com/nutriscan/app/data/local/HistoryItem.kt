package com.nutriscan.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nutriscan.app.data.model.Nutriments
import com.nutriscan.app.data.model.Product

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey val barcode: String,
    val productName: String? = null,
    val brands: String? = null,
    val imageUrl: String? = null,
    val nutriscoreGrade: String? = null,
    val novaGroup: Int? = null,
    val ingredientsText: String? = null,
    val categories: String? = null,
    val nutritionDataPer: String? = null,
    // nutriments como JSON string
    val nutrimentsJson: String? = null,
    val viewedAt: Long = System.currentTimeMillis()
) {
    companion object {
        private val gson = com.google.gson.Gson()

        fun fromProduct(product: Product): HistoryItem? {
            val barcode = product.code
            if (barcode.isNullOrBlank()) return null

            val nutrimentsJson = product.nutriments?.let {
                try { gson.toJson(it) } catch (_: Exception) { null }
            }

            return HistoryItem(
                barcode = barcode,
                productName = product.productName,
                brands = product.brands,
                imageUrl = product.imageUrl,
                nutriscoreGrade = product.nutriscoreGrade,
                novaGroup = product.novaGroup,
                ingredientsText = product.ingredientsText,
                categories = product.categories,
                nutritionDataPer = product.nutritionDataPer,
                nutrimentsJson = nutrimentsJson
            )
        }
    }

    fun toProduct(): Product {
        val nutriments = nutrimentsJson?.let {
            try { gson.fromJson(it, Nutriments::class.java) } catch (_: Exception) { null }
        }
        return Product(
            code = barcode,
            productName = productName,
            brands = brands,
            imageUrl = imageUrl,
            ingredientsText = ingredientsText,
            _nutriscoreGrade = nutriscoreGrade,
            _novaGroup = novaGroup,
            categories = categories,
            nutritionDataPer = nutritionDataPer,
            nutriments = nutriments
        )
    }
}
