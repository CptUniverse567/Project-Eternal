package com.projecteternal.persist

import com.projecteternal.model.GameState
import java.security.MessageDigest
import kotlinx.serialization.json.Json

/**
 * Serializes [GameState] to/from a compact JSON byte blob and fingerprints it
 * with SHA-256. Pure JVM — no Android dependencies, fully unit-testable.
 */
object SaveCodec {

    private val json = Json {
        ignoreUnknownKeys = true // tolerate fields added by newer clients
        encodeDefaults = true
        isLenient = false
    }

    fun encode(state: GameState): ByteArray =
        json.encodeToString(GameState.serializer(), state).encodeToByteArray()

    fun decode(bytes: ByteArray): GameState =
        json.decodeFromString(GameState.serializer(), bytes.decodeToString())

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verify(bytes: ByteArray, checksum: String): Boolean =
        sha256(bytes).equals(checksum, ignoreCase = true)
}
