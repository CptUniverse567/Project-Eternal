package com.projecteternal.persist.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SaveSlotEntity::class], version = 1, exportSchema = false)
abstract class EternalDatabase : RoomDatabase() {
    abstract fun saveSlotDao(): SaveSlotDao
}
