package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE ownerUserId = :ownerId ORDER BY updatedAt DESC")
    fun observeByOwner(ownerId: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM projects WHERE ownerUserId = :ownerId")
    fun observeCount(ownerId: String): Flow<Int>
}

@Dao
interface AiProviderDao {
    @Query("SELECT * FROM ai_providers ORDER BY isDefault DESC, name ASC")
    fun observeAll(): Flow<List<AiProviderEntity>>

    @Query("SELECT * FROM ai_providers ORDER BY isDefault DESC, name ASC")
    suspend fun getAll(): List<AiProviderEntity>

    @Query("SELECT * FROM ai_providers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AiProviderEntity?

    @Query("SELECT * FROM ai_providers WHERE isDefault = 1 AND isEnabled = 1 LIMIT 1")
    suspend fun getDefaultActive(): AiProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(provider: AiProviderEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(providers: List<AiProviderEntity>)

    @Query("UPDATE ai_providers SET isDefault = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setDefault(id: String)

    @Query("DELETE FROM ai_providers WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface FileHistoryDao {
    @Query("SELECT * FROM file_history WHERE projectId = :projectId AND relativePath = :path ORDER BY createdAt DESC LIMIT 25")
    fun observeForFile(projectId: String, path: String): Flow<List<FileHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FileHistoryEntity)

    @Query("DELETE FROM file_history WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: String)
}

@Dao
interface BuildRecordDao {
    @Query("SELECT * FROM build_records ORDER BY createdAt DESC LIMIT 50")
    fun observeAll(): Flow<List<BuildRecordEntity>>

    @Query("SELECT * FROM build_records WHERE projectId = :projectId ORDER BY createdAt DESC LIMIT 20")
    fun observeForProject(projectId: String): Flow<List<BuildRecordEntity>>

    @Query("SELECT * FROM build_records ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<BuildRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: BuildRecordEntity)

    @Query("DELETE FROM build_records WHERE projectId = :projectId")
    suspend fun deleteForProject(projectId: String)
}

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_messages WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun observeForProject(projectId: String): Flow<List<AiChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: AiChatMessageEntity)

    @Query("DELETE FROM ai_messages WHERE projectId = :projectId")
    suspend fun clearForProject(projectId: String)
}
