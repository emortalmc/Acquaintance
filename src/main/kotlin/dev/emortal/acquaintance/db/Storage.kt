package dev.emortal.acquaintance.db

import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.util.*

abstract class Storage {

    abstract fun blockPlayer(player: UUID)
    abstract fun unblockPlayer(player: UUID)

    abstract fun addFriend(player: UUID, friend: UUID)
    abstract fun removeFriend(player: UUID, friend: UUID)

    abstract suspend fun getFriendsAsync(player: UUID): MutableList<UUID>

    val hikari = createHikari()
    abstract fun createHikari(): HikariDataSource

    fun getConnection(): Connection = hikari.connection

}