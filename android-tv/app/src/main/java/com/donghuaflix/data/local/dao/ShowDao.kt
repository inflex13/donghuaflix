package com.donghuaflix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.donghuaflix.data.local.entity.ShowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShowDao {

    @Query("SELECT * FROM shows ORDER BY updatedAt DESC")
    fun getAllShows(): Flow<List<ShowEntity>>

    @Query("SELECT * FROM shows ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getShowsPaged(limit: Int, offset: Int): List<ShowEntity>

    @Query("SELECT * FROM shows WHERE id = :id")
    suspend fun getShowById(id: Int): ShowEntity?

    @Query("SELECT * FROM shows WHERE id = :id")
    fun observeShowById(id: Int): Flow<ShowEntity?>

    @Query("SELECT * FROM shows WHERE title LIKE '%' || :query || '%' ORDER BY title")
    suspend fun searchShows(query: String): List<ShowEntity>

    @Query("SELECT * FROM shows WHERE genres LIKE '%' || :genre || '%' ORDER BY rating DESC")
    suspend fun getShowsByGenre(genre: String): List<ShowEntity>

    @Query("SELECT * FROM shows WHERE status = :status ORDER BY updatedAt DESC")
    suspend fun getShowsByStatus(status: String): List<ShowEntity>

    @Query("SELECT * FROM shows WHERE category = :category ORDER BY updatedAt DESC")
    suspend fun getShowsByCategory(category: String): List<ShowEntity>

    @Query("SELECT * FROM shows ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentShows(limit: Int = 20): List<ShowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShows(shows: List<ShowEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShow(show: ShowEntity)

    @Query("DELETE FROM shows")
    suspend fun deleteAll()

    @Query("SELECT DISTINCT genres FROM shows WHERE genres IS NOT NULL")
    suspend fun getAllGenreStrings(): List<String>

    @Query("SELECT * FROM shows WHERE id IN (:ids)")
    suspend fun getShowsByIds(ids: List<Int>): List<ShowEntity>
}
