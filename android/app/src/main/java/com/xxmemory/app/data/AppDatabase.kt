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
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Cards v2 added type, media URLs, tags and favorite support.
                db.execSQL("ALTER TABLE cards ADD COLUMN card_type TEXT NOT NULL DEFAULT 'qa'")
                db.execSQL("ALTER TABLE cards ADD COLUMN audio_url TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN image_url TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
                // ReviewLog entity defines both card_id and review_date indices.
                db.execSQL("CREATE INDEX IF NOT EXISTS index_card_id ON review_logs(card_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_review_date ON review_logs(review_date)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Cards v3 added word-learning extras and mastered flag.
                db.execSQL("ALTER TABLE cards ADD COLUMN phonetic TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN example TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN collocations TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN etymology TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN hint TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN mastered INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Cards v4 added rhyme hints and derivative words.
                db.execSQL("ALTER TABLE cards ADD COLUMN rhyme TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cards ADD COLUMN derivatives TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xx_memory_database"
                )
                    // 明确使用增量迁移，禁止静默破坏性降级，避免用户数据丢失。
                    // 若未来 schema 再次变更，请先新增 Migration 并升级 version。
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
