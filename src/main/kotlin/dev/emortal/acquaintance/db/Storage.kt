package dev.emortal.acquaintance.db

abstract class Storage {

    //abstract suspend fun getFriendsAsync(player: UUID): MutableList<UUID>

    abstract suspend fun setCachedUsername(uuid: String, username: String)
    abstract suspend fun getCachedUsernameAsync(uuid: String): String?

    abstract fun init()

}