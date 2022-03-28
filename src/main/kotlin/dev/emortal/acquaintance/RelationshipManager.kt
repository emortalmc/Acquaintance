package dev.emortal.acquaintance

import dev.emortal.acquaintance.util.RedisStorage
import dev.emortal.acquaintance.util.RedisStorage.redisson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.minestom.server.entity.Player
import java.util.*

object RelationshipManager {

    suspend fun UUID.getCachedUsername(): String? = withContext(Dispatchers.IO) {
        val bucket = redisson.getBucket<String>("${this}username")
        if (bucket.isExists) {
            return@withContext bucket.get()
        }

        AcquaintanceExtension.storage?.getCachedUsernameAsync(this@getCachedUsername)
    }

    suspend fun Player.getFriendsAsync(): List<UUID> = withContext(Dispatchers.IO) {
        return@withContext redisson.getList<UUID>("${uuid}friends").readAllAsync().get()
    }

}