package com.cherryzp.animalshelter.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.cherryzp.animalshelter.AppApplication
import com.cherryzp.animalshelter.room.AbandonmentPublicDatabase
import com.cherryzp.animalshelter.room.dao.AbandonmentPublicDao
import com.cherryzp.animalshelter.room.entity.AbandonmentPublicEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception

class ShelterRepository(appApplication: AppApplication) {

    private val abandonmentPublicDatabase = AbandonmentPublicDatabase.getInstance(appApplication)!!
    private val abandonmentPublicDao: AbandonmentPublicDao = abandonmentPublicDatabase.abandonmentPublicDao()
    private lateinit var contacts: List<AbandonmentPublicEntity>

    fun getAll(): List<AbandonmentPublicEntity> {
        contacts = abandonmentPublicDao.getAll()
        Log.e("TAG", contacts.toString())

        return contacts
    }

    fun insert(abandonmentPublicEntity: AbandonmentPublicEntity) {
        Log.e("TAG", abandonmentPublicEntity.toString())
        abandonmentPublicDao.insert(abandonmentPublicEntity)
        Log.e("TAG", "success")
    }

    fun delete(abandonmentPublicEntity: AbandonmentPublicEntity) {
        Log.e("TAG", abandonmentPublicEntity.toString())
        abandonmentPublicDao.delete(abandonmentPublicEntity)
        Log.e("TAG", "success")
    }

}