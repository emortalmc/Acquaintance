package dev.emortal.acquaintance.db

import java.util.*

abstract class Storage {

    abstract suspend fun getBlockedPlayers(player: UUID): Set<UUID>
    abstract fun blockPlayers(player: UUID, playersToBlock: Set<UUID>)
    abstract fun unblockPlayers(player: UUID, playersToUnblock: Set<UUID>)
    abstract suspend fun isBlocked(player: UUID, other: UUID): Boolean

    abstract suspend fun getFriends(player: UUID): Set<UUID>
    abstract fun addFriends(player: UUID, friendsToAdd: Set<UUID>)
    abstract fun removeFriends(player: UUID, friendsToRemove: Set<UUID>)
    abstract suspend fun isFriends(player: UUID, other: UUID): Boolean

    abstract fun setCachedUsername(player: UUID, username: String)
    abstract suspend fun getCachedUsername(player: UUID): String?
    abstract suspend fun getCachedUUID(username: String): UUID?

    abstract fun init()

}