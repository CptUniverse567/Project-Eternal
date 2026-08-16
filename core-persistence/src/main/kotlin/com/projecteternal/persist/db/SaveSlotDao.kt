package com.projecteternal.persist.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SaveSlotDao {

    @Query("SELECT * FROM save_slot WHERE slotId = :slotId")
    suspend fun bySlot(slotId: String): SaveSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SaveSlotEntity)

    @Query("DELETE FROM save_slot WHERE slotId = :slotId")
    suspend fun delete(slotId: String)
}
