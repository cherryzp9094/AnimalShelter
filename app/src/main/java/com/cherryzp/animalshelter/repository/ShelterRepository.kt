package com.cherryzp.animalshelter.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.cherryzp.animalshelter.AppApplication
import com.cherryzp.animalshelter.room.AbandonmentPublicDatabase
import com.cherryzp.animalshelter.room.dao.AbandonmentPublicDao
import com.cherryzp.animalshelter.room.entity.AbandonmentPublicEntity
import java.lang.Exception

class ShelterRepository(appApplication: AppApplication) {

    private val abandonmentPublicDatabase = AbandonmentPublicDatabase.getInstance(appApplication)!!
    private val abandonmentPublicDao: AbandonmentPublicDao = abandonmentPublicDatabase.abandonmentPublicDao()
    private val contacts: LiveData<List<AbandonmentPublicEntity>> = abandonmentPublicDao.getAll()

    fun getAll(): LiveData<List<AbandonmentPublicEntity>> {
        return contacts
    }

    fun insert(abandonmentPublicEntity: AbandonmentPublicEntity) {
        try {
            val thread = Thread {
                abandonmentPublicDao.insert(abandonmentPublicEntity)
                Log.e("데이터", "데이터 들어감")
            }
            thread.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun delete(abandonmentPublicEntity: AbandonmentPublicEntity) {
        try {
            val thread = Thread {
                abandonmentPublicDao.delete(abandonmentPublicEntity)
            }
            thread.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}