package dev.emortal.acquaintance.db

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class CachedUser(@SerialName("_id")
                      @Contextual val uuid: UUID, val username: String)