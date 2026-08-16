package com.projecteternal.persist

/**
 * Thin storage abstraction behind [SaveManager]. The Room implementation lives
 * in the db package; tests use an in-memory fake. Keeps all checksum, fallback
 * and migration logic pure-JVM.
 */
interface SaveStore {
    suspend fun readLive(): StoredSave?
    suspend fun readBackup(): StoredSave?
    suspend fun writeLive(save: StoredSave)
    suspend fun writeBackup(save: StoredSave)
    suspend fun clearAll()
}
