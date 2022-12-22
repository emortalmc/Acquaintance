package dev.emortal.acquaintance.config

import kotlinx.serialization.Serializable

@Serializable
class DatabaseConfig(
    val logChat: Boolean = true,
    val logCommands: Boolean = true,
    val redisAddress: String = "redis://172.17.0.1:6379",
    val enabled: Boolean = false,
    val mongoAddress: String = "mongodb://172.17.0.1:27017"
)