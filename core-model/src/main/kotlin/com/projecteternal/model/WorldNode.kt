package com.projecteternal.model

import kotlinx.serialization.Serializable

/** A location/activity node in the world (mine, forest, monster territory...). */
@Serializable
data class WorldNode(
    val id: NodeId,
    val regionId: RegionId,
    val type: NodeType,
    val tier: Int = 1,
    val unlocked: Boolean = false,
    val assignedRetainerIds: List<RetainerId> = emptyList(),
    val craftRequirement: String = "", // unlock token required before this node is usable
) {
    fun withRetainers(ids: List<RetainerId>): WorldNode = copy(assignedRetainerIds = ids)
    fun isRetainerAssigned(id: RetainerId): Boolean = assignedRetainerIds.contains(id)
}

/** Retainer (worker) unit. Levels up from cumulative production. */
@Serializable
data class Retainer(
    val id: RetainerId,
    val name: String,
    val specialization: RetainerSpecialization,
    val level: Int = 1,
    val xp: Long = 0,
    val gatheringSpeed: Double = 1.0,
    val productionSpeed: Double = 1.0,
    val stamina: Int = 100,
    val maxStamina: Int = 100,
    val luck: Double = 1.0,
    val traitIds: List<String> = emptyList(),
    val assignedNodeId: NodeId? = null,
) {
    fun withAssignment(node: NodeId?): Retainer = copy(assignedNodeId = node)
    val staminaFraction: Double get() = if (maxStamina <= 0) 1.0 else stamina.toDouble() / maxStamina
}
