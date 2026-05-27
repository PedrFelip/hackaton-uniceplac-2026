package com.nutriscan.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nutriscan.app.data.api.RateLimitException
import com.nutriscan.app.data.api.RetrofitClient
import com.nutriscan.app.data.local.AlternativesCache
import com.nutriscan.app.data.local.AlternativesCacheDao
import com.nutriscan.app.data.model.Product
import com.nutriscan.app.data.model.ProductResponse

/**
 * Repositório central de acesso aos dados de produtos.
 * Encapsula as chamadas à API e transforma as respostas em Result<T>.
 *
 * @param alternativesCacheDao DAO opcional para cache de alternativas.
 *   Quando fornecido, buscas de alternativas mais saudáveis são cacheadas
 *   por 7 dias, evitando requisições repetidas à API.
 */
class ProductRepository(
    private val alternativesCacheDao: AlternativesCacheDao? = null
) {
    private val api = RetrofitClient.api
    private val gson = Gson()

    companion object {
        private const val CACHE_TTL_DAYS = 7L
        private const val CACHE_TTL_MILLIS = CACHE_TTL_DAYS * 24 * 60 * 60 * 1000
    }

    /**
     * Busca produtos por texto livre.
     * Usado na tela de pesquisa.
     */
    suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val response = api.searchProducts(query)
            Result.success(response.products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca produto pelo código de barras.
     * Retorna dados completos (nutricionais, ingredientes, etc).
     * status = 1 na resposta significa produto encontrado.
     */
    suspend fun getProductByBarcode(barcode: String): Result<Product> {
        return try {
            val response: ProductResponse = api.getProductByBarcode(barcode)
            if (response.status == 1 && response.product != null) {
                val product = response.product
                // Garante que o code nunca seja nulo — usa o barcode da requisição como fallback
                if (product.code.isNullOrBlank()) {
                    val fixedProduct = product.copy(code = barcode)
                    Result.success(fixedProduct)
                } else {
                    Result.success(product)
                }
            } else {
                Result.failure(Exception("Produto não encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Busca alternativas mais saudáveis para o produto dado.
     *
     * Estratégia:
     * 1. Verifica cache Room (TTL de 7 dias)
     * 2. Se cache hit válido, retorna os dados cacheados
     * 3. Se cache miss/expirado, busca na API
     * 4. Salva resultado no cache para futuras consultas
     * 5. Se rate limit for atingido, retorna lista vazia silenciosamente
     *    (alternativas são nice-to-have, não devem bloquear a UX)
     *
     * @param product Produto atual para comparar
     * @return Lista de até 5 produtos com Nutri-Score melhor
     */
    suspend fun getHealthierAlternatives(product: Product): Result<List<Product>> {
        val barcode = product.code
        if (barcode.isNullOrBlank()) return Result.success(emptyList())

        // Limpa entradas de cache expiradas antes de consultar
        cleanExpiredAlternatives()

        // 1. Verifica cache
        alternativesCacheDao?.let { dao ->
            try {
                val cached = dao.getByBarcode(barcode)
                if (cached != null) {
                    val age = System.currentTimeMillis() - cached.cachedAt
                    if (age < CACHE_TTL_MILLIS) {
                        val type = object : TypeToken<List<Product>>() {}.type
                        val products: List<Product> = gson.fromJson(cached.alternativesJson, type)
                        return Result.success(products)
                    }
                }
            } catch (_: Exception) {
                // Ignora erro de cache e prossegue para API
            }
        }

        val currentGrade = product.nutriscoreGrade

        // Define o termo de busca: prioridade para categoria, senão usa palavras-chave do nome
        val query = if (!product.categories.isNullOrBlank()) {
            val category = product.categories.split(",").firstOrNull()?.trim() ?: ""
            if (category.isNotBlank()) category else extractKeywords(product.productName)
        } else {
            extractKeywords(product.productName)
        }

        if (query.isBlank()) return Result.success(emptyList())

        return try {
            val response = api.searchProducts(query = query, pageSize = 20)
            val alternatives = response.products
                .filter { it.code != product.code && it.code != null }
                .filter { isHealthier(it.nutriscoreGrade, currentGrade) }
                .sortedBy { nutriscoreRank(it.nutriscoreGrade) }
                .take(5)

            // Salva no cache
            saveAlternativesCache(barcode, alternatives)

            Result.success(alternatives)
        } catch (e: RateLimitException) {
            // Alternativas são nice-to-have: rate limit não deve quebrar a tela
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Serializa e salva a lista de alternativas no cache Room.
     */
    private suspend fun saveAlternativesCache(barcode: String, alternatives: List<Product>) {
        alternativesCacheDao?.let { dao ->
            try {
                val json = gson.toJson(alternatives)
                dao.upsert(
                    AlternativesCache(
                        parentBarcode = barcode,
                        alternativesJson = json,
                        cachedAt = System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {
                // Falha silenciosa no cache não deve quebrar o fluxo
            }
        }
    }

    /**
     * Remove entradas de cache de alternativas com mais de 7 dias.
     * Chamado automaticamente antes de consultar o cache.
     */
    private suspend fun cleanExpiredAlternatives() {
        alternativesCacheDao?.let { dao ->
            try {
                val expireTime = System.currentTimeMillis() - CACHE_TTL_MILLIS
                dao.cleanExpired(expireTime)
            } catch (_: Exception) {
                // Falha silenciosa no cleanup não deve quebrar o fluxo
            }
        }
    }

    /**
     * Extrai as 3 palavras mais relevantes do nome do produto.
     * Remove stop words (preposições, artigos) e palavras muito curtas.
     */
    private fun extractKeywords(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val stopWords = setOf("em","de","do","da","dos","das","no","na","com","para","por","sem","e","ou","en","la","el","le")
        return name.lowercase()
            .replace(",", " ")
            .split(Regex("\\s+"))
            .filter { it.length > 2 && it !in stopWords }
            .take(3)
            .joinToString(" ")
    }

    /** Converte a letra do Nutri-Score em número para comparação. A=0 (melhor), E=4 (pior). */
    private fun nutriscoreRank(grade: String?): Int = when (grade?.lowercase()) {
        "a" -> 0; "b" -> 1; "c" -> 2; "d" -> 3; "e" -> 4; else -> 5
    }

    /** Verifica se o candidato tem Nutri-Score melhor que o atual. */
    private fun isHealthier(candidate: String?, current: String?): Boolean {
        if (candidate == null) return false
        if (current == null) return true
        return nutriscoreRank(candidate) < nutriscoreRank(current)
    }
}
