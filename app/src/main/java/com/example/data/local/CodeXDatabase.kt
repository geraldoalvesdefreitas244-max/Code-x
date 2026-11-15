package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ProjectEntity::class,
        AiProviderEntity::class,
        FileHistoryEntity::class,
        BuildRecordEntity::class,
        AiChatMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CodeXDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun projectDao(): ProjectDao
    abstract fun aiProviderDao(): AiProviderDao
    abstract fun fileHistoryDao(): FileHistoryDao
    abstract fun buildRecordDao(): BuildRecordDao
    abstract fun aiChatDao(): AiChatDao

    companion object {
        @Volatile
        private var INSTANCE: CodeXDatabase? = null

        fun getInstance(context: Context): CodeXDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CodeXDatabase::class.java,
                    "codex_ide_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
