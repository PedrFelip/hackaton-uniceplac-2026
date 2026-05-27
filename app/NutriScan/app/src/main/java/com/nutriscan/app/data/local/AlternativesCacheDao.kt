package com.nutriscan.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO para operações de cache de alternativas mais saudáveis.
 */
@Dao
interface AlternativesCacheDao {

    /** Insere ou atualiza o cache de alternativas para um produto. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: AlternativesCache)

    /** Retorna o cache de alternativas para um produto específico, se existir. */
    @Query("SELECT * FROM alternatives_cache WHERE parentBarcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): AlternativesCache?

    /** Remove entradas de cache expiradas (mais antigas que [expireTime]). */
    @Query("DELETE FROM alternatives_cache WHERE cachedAt < :expireTime")
    suspend fun cleanExpired(expireTime: Long)

    /** Remove todo o cache de alternativas. */
    @Query("DELETE FROM alternatives_cache")
    suspend fun clearAll()
}
