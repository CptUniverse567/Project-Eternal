package com.projecteternal.persist.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row of the atomic single-slot save table: LIVE and BACKUP slots. */
@Entity(tableName = "save_slot")
data class SaveSlotEntity(
    @PrimaryKey val slotId: String,
    val schemaVersion: Int,
    val checksum: String,
    @ColumnInfo(name = "updated_at_epoch_ms") val updatedAtEpochMs: Long,
    @ColumnInfo(name = "payload", typeAffinity = ColumnInfo.BLOB) val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is SaveSlotEntity &&
            other.slotId == slotId &&
            other.schemaVersion == schemaVersion &&
            other.checksum == checksum &&
            other.updatedAtEpochMs == updatedAtEpochMs &&
            other.payload.contentEquals(payload)

    override fun hashCode(): Int {
        var h = slotId.hashCode()
        h = 31 * h + schemaVersion
        h = 31 * h + checksum.hashCode()
        h = 31 * h + updatedAtEpochMs.hashCode()
        h = 31 * h + payload.contentHashCode()
        return h
    }
}
