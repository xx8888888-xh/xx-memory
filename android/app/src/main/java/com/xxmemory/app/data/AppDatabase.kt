package com.xxmemory.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xxmemory.app.data.dao.CardDao
import com.xxmemory.app.data.dao.ReviewLogDao
import com.xxmemory.app.data.entity.Card
import com.xxmemory.app.data.entity.ReviewLog

@Database(
    entities = [Card::class, ReviewLog::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS index_review_date ON review_logs(review_date)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xx_memory_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}