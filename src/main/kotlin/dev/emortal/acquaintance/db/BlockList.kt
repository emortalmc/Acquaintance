package dev.emortal.acquaintance.db

import com.github.jershell.kbson.UUIDSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class BlockList(
    @SerialName("_id")
    @Contextual
    val uuid: UUID,

    @Contextual
    val blockedUUIDs: Set<@Serializable(with = UUIDSerializer::class) UUID>
)