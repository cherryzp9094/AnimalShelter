package com.cherryzp.animalshelter.room.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cherryzp.animalshelter.room.entity.AbandonmentPublicEntity

@Dao
interface AbandonmentPublicDao {

    @Query("SELECT * FROM abandonmentPublic ORDER BY id ASC")
    fun getAll(): LiveData<List<AbandonmentPublicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(abandonmentPublicEntity: AbandonmentPublicEntity)

    @Delete
    fun delete(abandonmentPublicEntity: AbandonmentPublicEntity)
}