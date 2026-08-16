package com.projecteternal.persist

import com.projecteternal.model.GameState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory fake backing a [SaveStore]; lets tests tamper slots directly. */
class FakeStore : SaveStore {
    var live: StoredSave? = null
    var backup: StoredSave? = null

    override suspend fun readLive(): StoredSave? = live
    override suspend fun readBackup(): StoredSave? = backup
    override suspend fun writeLive(save: StoredSave) {
        live = save
    }

    override suspend fun writeBackup(save: StoredSave) {
        backup = save
    }

    override suspend fun clearAll() {
        live = null
        backup = null
    }
}

class SaveManagerTest {

    private fun state(name: String, schema: Int = 1) =
        GameState(saveId = "sv_$name", schemaVersion = schema).addItem("ore_iron", name.length.toLong())

    private fun manager(
        store: FakeStore,
        schema: Int = 1,
        migrations: List<SaveMigration> = emptyList(),
    ) = SaveManager(store, currentSchema = schema, migrations = migrations, clock = { 12345L })

    @Test
    fun `no save yields NoSave`() = runBlocking {
        assertEquals(SaveManager.LoadResult.NoSave, manager(FakeStore()).load())
    }

    @Test
    fun `save then load round-trips the state from live`() = runBlocking {
        val store = FakeStore()
        val m = manager(store)
        val s = state("tester")
        m.save(s)

        val result = m.load()
        assertTrue(result is SaveManager.LoadResult.Success)
        result as SaveManager.LoadResult.Success
        assertEquals(s, result.state)
        assertEquals(SaveManager.Source.LIVE, result.source)
        assertFalse(result.restoredFromBackup)
        assertTrue(result.warnings.isEmpty())
        assertNotNull(store.live)
        assertNull(store.backup) // first save has no previous live to snapshot
    }

    @Test
    fun `backup holds the previous live save`() = runBlocking {
        val store = FakeStore()
        val m = manager(store)
        m.save(state("first"))
        m.save(state("second"))

        assertEquals("sv_first", SaveCodec.decode(store.backup!!.payload).saveId)
        assertEquals("sv_second", SaveCodec.decode(store.live!!.payload).saveId)
    }

    @Test
    fun `corrupt live falls back to a clean backup`() = runBlocking {
        val store = FakeStore()
        val m = manager(store)
        m.save(state("first"))
        m.save(state("good")) // second save snapshots the first into backup

        // Tamper the live slot in place (flip a payload byte, keep the stale checksum).
        val live = store.live!!
        val tampered = live.payload.copyOf().also { it[4] = (it[4].toInt() xor 0xFF).toByte() }
        store.live = live.copy(payload = tampered)

        val result = m.load()
        assertTrue(result is SaveManager.LoadResult.Success)
        result as SaveManager.LoadResult.Success
        assertEquals("sv_first", result.state.saveId) // backup holds the previous live save
        assertEquals(SaveManager.Source.BACKUP, result.source)
        assertTrue(result.restoredFromBackup)
        assertTrue(result.warnings.any { it.contains("corrupt") })
    }

    @Test
    fun `both slots corrupt yields Failure`() = runBlocking {
        val store = FakeStore()
        val m = manager(store)
        m.save(state("first"))
        m.save(state("doomed")) // second save creates the backup slot

        fun corrupt(s: StoredSave) = s.copy(payload = s.payload.copyOf().also { it[3] = (it[3].toInt() xor 0xFF).toByte() })
        store.live = corrupt(store.live!!)
        store.backup = corrupt(store.backup!!)

        val result = m.load()
        assertTrue(result is SaveManager.LoadResult.Failure)
    }

    @Test
    fun `live missing but backup present restores from backup`() = runBlocking {
        val store = FakeStore()
        val m = manager(store)
        m.save(state("first"))
        m.save(state("recover")) // second save creates the backup slot
        store.live = null

        val result = m.load()
        assertTrue(result is SaveManager.LoadResult.Success)
        result as SaveManager.LoadResult.Success
        assertEquals(SaveManager.Source.BACKUP, result.source)
        assertTrue(result.restoredFromBackup)
    }

    @Test
    fun `migrations run in order and stamp the schema`() = runBlocking {
        val store = FakeStore()
        val m = SaveManager(
            store,
            currentSchema = 2,
            migrations = listOf(object : SaveMigration {
                override val fromSchema: Int = 1
                override val toSchema: Int = 2
                override fun migrate(state: GameState): GameState =
                    state.copy(character = state.character.copy(name = state.character.name.uppercase()))
            }),
        )

        val old = state("migrate", schema = 1)
        store.writeLive(StoredSave("live", 1, SaveCodec.sha256(SaveCodec.encode(old)), 1, SaveCodec.encode(old)))

        val result = m.load()
        assertTrue(result is SaveManager.LoadResult.Success)
        result as SaveManager.LoadResult.Success
        assertEquals(2, result.state.schemaVersion)
        assertEquals(old.character.name.uppercase(), result.state.character.name)
        assertTrue(result.warnings.any { it.contains("migrated") })
    }

    @Test
    fun `save from a newer version is rejected`() = runBlocking {
        val store = FakeStore()
        val s = state("future", schema = 99)
        store.writeLive(StoredSave("live", 99, SaveCodec.sha256(SaveCodec.encode(s)), 1, SaveCodec.encode(s)))

        val result = manager(store).load()
        assertTrue(result is SaveManager.LoadResult.Failure)
        assertTrue((result as SaveManager.LoadResult.Failure).reason.contains("newer"))
    }

    @Test
    fun `missing migration step fails loudly`() = runBlocking {
        val store = FakeStore()
        val s = state("gap", schema = 1)
        store.writeLive(StoredSave("live", 1, SaveCodec.sha256(SaveCodec.encode(s)), 1, SaveCodec.encode(s)))

        val result = SaveManager(store, currentSchema = 3, migrations = emptyList()).load()
        assertTrue(result is SaveManager.LoadResult.Failure)
        assertTrue((result as SaveManager.LoadResult.Failure).reason.contains("migration path"))
    }

    @Test
    fun `clearAll empties both slots`() = runBlocking {
        val store = FakeStore()
        val m = manager(store)
        m.save(state("temp"))
        m.save(state("temp2"))
        store.clearAll()
        assertNull(store.live)
        assertNull(store.backup)
    }
}
