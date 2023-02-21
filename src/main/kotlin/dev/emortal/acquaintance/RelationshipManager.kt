package dev.emortal.acquaintance

import dev.emortal.acquaintance.RelationshipManager.getCachedUsername
import dev.emortal.acquaintance.util.JedisStorage.jedis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object RelationshipManager {

    suspend fun String.getCachedUsername(): String? = withContext(Dispatchers.IO) {
        val redisUsername = jedis.get("${this@getCachedUsername}username")
        if (redisUsername != null) {
            return@withContext redisUsername
        }

        AcquaintanceExtension.storage?.getCachedUsername(UUID.fromString(this@getCachedUsername))
    }
    fun String.setCachedUsername(newUsername: String) {
        jedis.set("${this@setCachedUsername}username", newUsername)
    }

//    suspend fun Player.getFriendsAsync(): List<UUID> = withContext(Dispatchers.IO) {
//        return@withContext redisson.getList<UUID>("${uuid}friends").readAllAsync().get()
//    }

}