package com.projecteternal.persist

import com.projecteternal.model.GameState

/** A single schema migration step. Content evolves as data, schema rarely. */
interface SaveMigration {
    val fromSchema: Int
    val toSchema: Int

    fun migrate(state: GameState): GameState
}
