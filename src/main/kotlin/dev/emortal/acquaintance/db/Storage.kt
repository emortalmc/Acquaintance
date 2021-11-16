package dev.emortal.acquaintance.db

import java.sql.Connection
import java.util.*

abstract class Storage {

    abstract fun addFriend(player: UUID, friend: UUID)
    abstract fun removeFriend(player: UUID, friend: UUID)

    abstract fun getFriends(player: UUID): List<UUID>

    val connection = createConnection()

    abstract fun createConnection(): Connection

}