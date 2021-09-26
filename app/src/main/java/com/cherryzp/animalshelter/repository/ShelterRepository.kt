package com.cherryzp.animalshelter.repository

import android.util.Log
import com.cherryzp.animalshelter.AppApplication
import com.cherryzp.animalshelter.room.AbandonmentPublicDatabase
import com.cherryzp.animalshelter.room.dao.AbandonmentPublicDao
import com.cherryzp.animalshelter.room.entity.AbandonmentPublicEntity

class ShelterRepository(appApplication: AppApplication) {

    private val abandonmentPublicDatabase = AbandonmentPublicDatabase.getInstance(appApplication)!!
    private val abandonmentPublicDao: AbandonmentPublicDao = abandonmentPublicDatabase.abandonmentPublicDao()
    private lateinit var animalList: List<AbandonmentPublicEntity>

    fun getAll(): List<AbandonmentPublicEntity> {
        animalList = abandonmentPublicDao.getAll()
        Log.e("TAG", animalList.toString())

        return animalList
    }

    fun getByDesertionNo(desertionNo: String): AbandonmentPublicEntity {
        return abandonmentPublicDao.getByDesertionNo(desertionNo)
    }

    fun insert(abandonmentPublicEntity: AbandonmentPublicEntity) {
        Log.e("TAG", abandonmentPublicEntity.toString())
        abandonmentPublicDao.insert(abandonmentPublicEntity)
        Log.e("TAG", "success")
    }

    fun deleteByDesertionNo(desertionNo: String) {
        Log.e("TAG", desertionNo)
        abandonmentPublicDao.deleteByDesertionNo(desertionNo)
        Log.e("TAG", "success")
    }

}