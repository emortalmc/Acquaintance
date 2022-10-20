package dev.emortal.acquaintance

import dev.emortal.acquaintance.util.JedisStorage.jedis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RelationshipManager {

    suspend fun String.getCachedUsername(): String? = withContext(Dispatchers.IO) {
        val bucket = jedis.get("${this@getCachedUsername}username")
        if (bucket != null) {
            return@withContext bucket
        }

        AcquaintanceExtension.storage?.getCachedUsernameAsync(this@getCachedUsername)
    }

//    suspend fun Player.getFriendsAsync(): List<UUID> = withContext(Dispatchers.IO) {
//        return@withContext redisson.getList<UUID>("${uuid}friends").readAllAsync().get()
//    }

}