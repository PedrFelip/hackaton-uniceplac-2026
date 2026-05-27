package com.nutriscan.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room que armazena em cache a lista de alternativas mais saudáveis
 * para um produto específico.
 *
 * Os dados são serializados em JSON para simplificar leitura e escrita.
 * O campo [cachedAt] armazena o timestamp Unix em milissegundos para
 * controle de expiração do cache (TTL de 7 dias).
 */
@Entity(tableName = "alternatives_cache")
data class AlternativesCache(
    @PrimaryKey val parentBarcode: String,
    val alternativesJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)
