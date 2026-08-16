package com.projecteternal.persist.db

import com.projecteternal.persist.SaveStore
import com.projecteternal.persist.StoredSave

/** Room-backed [SaveStore]. SQLite transactions make each slot write atomic. */
class RoomSaveStore(private val dao: SaveSlotDao) : SaveStore {

    private fun toEntity(save: StoredSave) = SaveSlotEntity(
        slotId = save.slotId,
        schemaVersion = save.schemaVersion,
        checksum = save.checksum,
        updatedAtEpochMs = save.updatedAtEpochMs,
        payload = save.payload,
    )

    private fun toStored(entity: SaveSlotEntity) = StoredSave(
        slotId = entity.slotId,
        schemaVersion = entity.schemaVersion,
        checksum = entity.checksum,
        updatedAtEpochMs = entity.updatedAtEpochMs,
        payload = entity.payload,
    )

    override suspend fun readLive(): StoredSave? = dao.bySlot(StoredSave.SLOT_LIVE)?.let(::toStored)

    override suspend fun readBackup(): StoredSave? = dao.bySlot(StoredSave.SLOT_BACKUP)?.let(::toStored)

    override suspend fun writeLive(save: StoredSave) = dao.upsert(toEntity(save))

    override suspend fun writeBackup(save: StoredSave) = dao.upsert(toEntity(save))

    override suspend fun clearAll() {
        dao.delete(StoredSave.SLOT_LIVE)
        dao.delete(StoredSave.SLOT_BACKUP)
    }
}
