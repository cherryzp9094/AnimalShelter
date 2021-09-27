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
        return animalList
    }

    fun getByDesertionNo(desertionNo: String): AbandonmentPublicEntity {
        return abandonmentPublicDao.getByDesertionNo(desertionNo)
    }

    fun insert(abandonmentPublicEntity: AbandonmentPublicEntity) {
        abandonmentPublicDao.insert(abandonmentPublicEntity)
    }

    fun deleteByDesertionNo(desertionNo: String) {
        abandonmentPublicDao.deleteByDesertionNo(desertionNo)
    }

}