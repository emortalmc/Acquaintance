package dev.emortal.acquaintance.db

import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.util.*

abstract class Storage {

    //abstract suspend fun getFriendsAsync(player: UUID): MutableList<UUID>

    abstract fun setCachedUsername(player: UUID, username: String)
    abstract suspend fun getCachedUsernameAsync(player: UUID): String?

    val hikari = createHikari()
    abstract fun createHikari(): HikariDataSource

    fun getConnection(): Connection = hikari.connection

}