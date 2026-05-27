package com.nutriscan.app.data.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor OkHttp que implementa rate limiting por sliding window
 * para proteger contra ban de IP na API Open Food Facts.
 *
 * Limites aplicados:
 * - Produto (GET /api/v2/product/...): 15 requisições por minuto
 * - Busca (GET /cgi/search.pl): 10 requisições por minuto
 *
 * Quando o limite é atingido, lança [RateLimitException] com mensagem
 * amigável ao usuário, em vez de bloquear a thread com sleep.
 */
class RateLimitInterceptor : Interceptor {

    companion object {
        private const val PRODUCT_LIMIT = 15
        private const val SEARCH_LIMIT = 10
        private const val WINDOW_MILLIS = 60_000L // 1 minuto

        private const val PRODUCT_PATH_MARKER = "/product/"
        private const val SEARCH_PATH_MARKER = "search.pl"
    }

    private val productTimestamps = mutableListOf<Long>()
    private val searchTimestamps = mutableListOf<Long>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        val endpointType = when {
            url.contains(PRODUCT_PATH_MARKER) -> EndpointType.PRODUCT
            url.contains(SEARCH_PATH_MARKER) -> EndpointType.SEARCH
            else -> null // Endpoint não rastreado — passa direto
        }

        if (endpointType != null) {
            checkRateLimit(endpointType)
        }

        return chain.proceed(request)
    }

    /**
     * Verifica se a requisição atual está dentro do limite permitido.
     * Se exceder, lança [RateLimitException].
     * Caso contrário, registra o timestamp da requisição.
     */
    private fun checkRateLimit(type: EndpointType) {
        synchronized(this) {
            val now = System.currentTimeMillis()
            val cutoff = now - WINDOW_MILLIS
            val timestamps = when (type) {
                EndpointType.PRODUCT -> productTimestamps
                EndpointType.SEARCH -> searchTimestamps
            }

            // Remove timestamps fora da janela de 60 segundos
            timestamps.removeAll { it < cutoff }

            val limit = when (type) {
                EndpointType.PRODUCT -> PRODUCT_LIMIT
                EndpointType.SEARCH -> SEARCH_LIMIT
            }

            if (timestamps.size >= limit) {
                throw RateLimitException(RateLimitException.RATE_LIMIT_MESSAGE)
            }

            timestamps.add(now)
        }
    }

    private enum class EndpointType {
        PRODUCT,
        SEARCH
    }
}
