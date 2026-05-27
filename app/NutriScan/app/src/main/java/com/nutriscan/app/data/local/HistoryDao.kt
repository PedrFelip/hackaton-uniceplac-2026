package com.nutriscan.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO para operações de leitura e escrita no histórico de produtos.
 */
@Dao
interface HistoryDao {

    /** Insere ou atualiza um item. Se o barcode já existe, atualiza os dados e o timestamp. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: HistoryItem)

    /** Retorna todos os itens ordenados pela data de visualização (mais recentes primeiro). */
    @Query("SELECT * FROM history ORDER BY viewedAt DESC")
    fun getAllFlow(): kotlinx.coroutines.flow.Flow<List<HistoryItem>>

    @Query("SELECT * FROM history ORDER BY viewedAt DESC")
    suspend fun getAll(): List<HistoryItem>

    @Query("SELECT * FROM history WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): HistoryItem?

    /** Remove todos os itens do histórico. */
    @Query("DELETE FROM history")
    suspend fun clearAll()

    /** Remove um item específico pelo barcode. */
    @Query("DELETE FROM history WHERE barcode = :barcode")
    suspend fun delete(barcode: String)
}
