package com.projecteternal.persist

import com.projecteternal.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveCodecTest {

    private fun state(): GameState = GameState(saveId = "sv_abc123").addItem("ore_iron", 7)

    @Test
    fun `round-trip preserves the whole state`() {
        val original = state()
        val bytes = SaveCodec.encode(original)
        val decoded = SaveCodec.decode(bytes)
        assertEquals(original, decoded)
    }

    @Test
    fun `sha256 is stable and content-sensitive`() {
        val a = SaveCodec.encode(state())
        val b = SaveCodec.encode(state())
        assertEquals(SaveCodec.sha256(a), SaveCodec.sha256(a))
        assertEquals(SaveCodec.sha256(a), SaveCodec.sha256(b)) // identical states, identical bytes
        assertTrue(SaveCodec.verify(a, SaveCodec.sha256(a)))

        val tampered = a.copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }
        assertFalse(SaveCodec.verify(tampered, SaveCodec.sha256(a)))
    }

    @Test
    fun `schema version round-trips with the state`() {
        val s = state()
        assertEquals(s.schemaVersion, SaveCodec.decode(SaveCodec.encode(s)).schemaVersion)
    }

    @Test
    fun `round-trip preserves the plus 31 enhancement and quest progression`() {
        val sword = com.projecteternal.model.ItemInstance(
            uid = "blade", defId = "sword_bronze", enhancementLevel = 31, durability = 45, maxDurability = 80,
        )
        val enhancedStats = com.projecteternal.model.GameState(saveId = "x").stats.recordEnhance(31)
        val s = GameState(saveId = "sv_31")
            .addEquipmentItem(sword)
            .copy(stats = enhancedStats)
            .unlock("region:dawnreach")
            .acceptQuests(listOf("q_light_beyond"), 1L)
            .completeQuest("q_light_beyond", 2L)
        val back = SaveCodec.decode(SaveCodec.encode(s))
        assertEquals(s, back)
        assertEquals(31, back.itemInstance("blade")!!.enhancementLevel)
        assertEquals(31, back.stats.maxEnhanceAchieved)
        assertTrue(back.hasUnlock("region:dawnreach"))
        assertEquals(
            com.projecteternal.model.QuestStatus.COMPLETED,
            back.quest("q_light_beyond")?.status
        )
    }
}
