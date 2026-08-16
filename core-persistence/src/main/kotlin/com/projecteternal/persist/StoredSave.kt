package com.projecteternal.persist

/** A raw saved snapshot as persisted in a storage slot. */
data class StoredSave(
    val slotId: String,
    val schemaVersion: Int,
    val checksum: String,
    val updatedAtEpochMs: Long,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is StoredSave &&
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

    companion object {
        const val SLOT_LIVE = "live"
        const val SLOT_BACKUP = "backup"
    }
}
