package com.cherryzp.animalshelter.room.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cherryzp.animalshelter.room.entity.AbandonmentPublicEntity

@Dao
interface AbandonmentPublicDao {

    @Query("SELECT * FROM abandonment_public ORDER BY id DESC")
    fun getAll(): List<AbandonmentPublicEntity>

    @Query("SELECT * FROM abandonment_public WHERE desertionNo= :desertionNo")
    fun getByDesertionNo(desertionNo: String): AbandonmentPublicEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(abandonmentPublicEntity: AbandonmentPublicEntity)

    @Query("DELETE FROM abandonment_public WHERE desertionNo = :desertionNo")
    fun deleteByDesertionNo(desertionNo: String)
}