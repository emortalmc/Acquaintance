package dev.emortal.acquaintance.config

import kotlinx.serialization.Serializable

@Serializable
class PlayerCacheConfig(
    var cache: HashMap<String, String> = hashMapOf()
)