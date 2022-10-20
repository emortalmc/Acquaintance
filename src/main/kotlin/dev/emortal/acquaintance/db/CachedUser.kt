package dev.emortal.acquaintance.db

import kotlinx.serialization.Serializable

@Serializable
data class CachedUser(val uuid: String, val username: String)