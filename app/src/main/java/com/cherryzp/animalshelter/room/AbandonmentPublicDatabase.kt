package com.cherryzp.animalshelter.room

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cherryzp.animalshelter.room.dao.AbandonmentPublicDao
import com.cherryzp.animalshelter.room.entity.AbandonmentPublicEntity

@Database(entities = [AbandonmentPublicEntity::class], version = 1)
abstract class AbandonmentPublicDatabase : RoomDatabase(){

    abstract fun abandonmentPublicDao(): AbandonmentPublicDao

    companion object {
        private const val database_name = "abandonment_public"

        @Volatile
        private var INSTANCE: AbandonmentPublicDatabase? = null

        fun getInstance(context: Context): AbandonmentPublicDatabase? {
            if (INSTANCE == null) {
                synchronized(AbandonmentPublicDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        AbandonmentPublicDatabase::class.java, database_name)
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE
        }
    }
}