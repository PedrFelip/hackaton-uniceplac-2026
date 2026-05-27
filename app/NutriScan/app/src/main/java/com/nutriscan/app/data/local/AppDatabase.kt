package com.nutriscan.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HistoryItem::class, CartItem::class, AlternativesCache::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun cartDao(): CartDao
    abstract fun alternativesCacheDao(): AlternativesCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migração de v2 para v3: cria a tabela alternatives_cache
         * sem destruir dados existentes (histórico e carrinho).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS alternatives_cache (
                        parentBarcode TEXT PRIMARY KEY NOT NULL,
                        alternativesJson TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutriscan.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
