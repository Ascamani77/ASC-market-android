package com.asc.markets.data.trade

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TradeEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradeDao(): TradeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trade_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
