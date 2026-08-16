package com.projecteternal.persist

import com.projecteternal.model.GameState
import com.projecteternal.model.SaveSchema
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads and saves the game atomically against a [SaveStore].
 *
 * Load order: live slot -> (if corrupt) backup slot -> fail. Every successful
 * save first snapshots the current live save into the backup slot, so a torn
 * write can never lose both copies. Schema upgrades run through the supplied
 * [SaveMigration] registry in order.
 */
class SaveManager(
    private val store: SaveStore,
    private val codec: SaveCodec = SaveCodec,
    private val currentSchema: Int = SaveSchema.CURRENT,
    private val migrations: List<SaveMigration> = emptyList(),
    private val clock: () -> Long = System::currentTimeMillis,
) {

    enum class Source { LIVE, BACKUP }

    sealed class LoadResult {
        data class Success(
            val state: GameState,
            val source: Source,
            val restoredFromBackup: Boolean,
            val warnings: List<String>,
        ) : LoadResult()

        data class Failure(val reason: String) : LoadResult()

        object NoSave : LoadResult()
    }

    suspend fun load(): LoadResult = withContext(Dispatchers.IO) {
        val live = store.readLive()
        if (live != null) {
            when (val r = validateAndMigrate(live)) {
                is Validation.Valid -> LoadResult.Success(r.state, Source.LIVE, restoredFromBackup = false, warnings = r.warnings)
                is Validation.Invalid -> {
                    val backup = store.readBackup()
                    if (backup != null) {
                        when (val b = validateAndMigrate(backup)) {
                            is Validation.Valid -> LoadResult.Success(
                                b.state,
                                Source.BACKUP,
                                restoredFromBackup = true,
                                warnings = b.warnings + "Live save was corrupt (${r.reason}); restored from backup.",
                            )
                            is Validation.Invalid -> LoadResult.Failure("Both save slots are unreadable (${r.reason}; ${b.reason}).")
                        }
                    } else {
                        LoadResult.Failure("Live save corrupt (${r.reason}) and no backup exists.")
                    }
                }
            }
        } else {
            val backup = store.readBackup()
            if (backup != null) {
                when (val b = validateAndMigrate(backup)) {
                    is Validation.Valid -> LoadResult.Success(
                        b.state, Source.BACKUP, restoredFromBackup = true,
                        warnings = b.warnings + "Primary save missing; restored from backup.",
                    )
                    is Validation.Invalid -> LoadResult.Failure("Backup save unreadable (${b.reason}).")
                }
            } else {
                LoadResult.NoSave
            }
        }
    }

    /** Persist [state]: snapshot the current live save to backup, then write the new live save. */
    suspend fun save(state: GameState): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = codec.encode(state)
            val envelope = StoredSave(
                slotId = StoredSave.SLOT_LIVE,
                schemaVersion = state.schemaVersion,
                checksum = codec.sha256(payload),
                updatedAtEpochMs = clock(),
                payload = payload,
            )
            val currentLive = store.readLive()
            if (currentLive != null) {
                store.writeBackup(currentLive)
            }
            store.writeLive(envelope)
        }
    }

    private sealed class Validation {
        data class Valid(val state: GameState, val warnings: List<String>) : Validation()
        data class Invalid(val reason: String) : Validation()
    }

    private fun validateAndMigrate(save: StoredSave): Validation {
        if (!codec.verify(save.payload, save.checksum)) {
            return Validation.Invalid("checksum mismatch")
        }
        val decoded = try {
            codec.decode(save.payload)
        } catch (e: Exception) {
            return Validation.Invalid("decode failed: ${e.message}")
        }

        val warnings = mutableListOf<String>()
        val migrated = try {
            migrate(decoded, save.schemaVersion, warnings)
        } catch (e: Exception) {
            return Validation.Invalid("migration failed: ${e.message}")
        }
        return Validation.Valid(migrated, warnings)
    }

    private fun migrate(state: GameState, fromSchema: Int, warnings: MutableList<String>): GameState {
        if (fromSchema > currentSchema) {
            throw IOException("Save is from a newer game version ($fromSchema > $currentSchema).")
        }
        var schema = fromSchema
        var s = state
        while (schema < currentSchema) {
            val step = migrations.firstOrNull { it.fromSchema == schema }
                ?: throw IOException("No migration path from schema $schema.")
            s = step.migrate(s)
            schema = step.toSchema
            warnings.add("Save migrated from schema ${step.fromSchema} to ${step.toSchema}.")
        }
        return if (schema == currentSchema) s.copy(schemaVersion = currentSchema) else s
    }
}
