package dev.emortal.acquaintance.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.emortal.acquaintance.AcquaintanceExtension
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*


class MySQLStorage : Storage() {

    private val logger = LoggerFactory.getLogger("MySQLStorage")

    init {
        logger.info("Init MySQLStorage")

        val conn = getConnection()
        conn.autoCommit = false

        val statement =
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS friends (`playerA` BINARY(16), `playerB` BINARY(16))")
        statement.executeUpdate()
        statement.close()

        val statement2 =
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS blocks (`playerA` BINARY(16), `playerB` BINARY(16))")
        statement2.executeUpdate()
        statement2.close()

        conn.autoCommit = true
        conn.close()
    }

    override fun blockPlayer(player: UUID) {

    }

    override fun unblockPlayer(player: UUID) {

    }

    override fun addFriend(player: UUID, friend: UUID): Unit = runBlocking {
        launch {
            val conn = getConnection()
            conn.autoCommit = false

            val statement = conn.prepareStatement("INSERT INTO friends VALUES(?, ?)")
            statement.setBinaryStream(1, player.toInputStream())
            statement.setBinaryStream(2, friend.toInputStream())
            statement.addBatch()

            statement.setBinaryStream(1, friend.toInputStream())
            statement.setBinaryStream(2, player.toInputStream())
            statement.addBatch()

            statement.executeBatch()
            statement.close()

            conn.autoCommit = true

            statement.close()
            conn.close()
        }
    }

    override fun removeFriend(player: UUID, friend: UUID): Unit = runBlocking {
        launch {
            val conn = getConnection()

            conn.autoCommit = false

            val statement = conn.prepareStatement("DELETE FROM friends WHERE playerA=? AND playerB=?")
            statement.setBinaryStream(1, player.toInputStream())
            statement.setBinaryStream(2, friend.toInputStream())
            statement.addBatch()

            statement.setBinaryStream(1, friend.toInputStream())
            statement.setBinaryStream(2, player.toInputStream())
            statement.addBatch()

            statement.executeBatch()
            statement.close()

            conn.autoCommit = true

            statement.close()
            conn.close()
        }
    }

    override suspend fun getFriendsAsync(player: UUID): MutableList<UUID> = coroutineScope {
        return@coroutineScope async {
            val conn = getConnection()
            val statement = conn.prepareStatement("SELECT playerB FROM friends WHERE playerA=?")

            statement.setBinaryStream(1, player.toInputStream())

            val results = statement.executeQuery()

            val uuidList = mutableListOf<UUID>()
            while (results.next()) {
                results.getBinaryStream(1).toUUID()?.let { uuidList.add(it) }
            }
            statement.close()
            conn.close()

            return@async uuidList
        }.await()
    }

    override fun createHikari(): HikariDataSource {
        val dbConfig = AcquaintanceExtension.databaseConfig

        val dbName = URLEncoder.encode(dbConfig.tableName, StandardCharsets.UTF_8.toString())
        val dbUsername = URLEncoder.encode(dbConfig.username, StandardCharsets.UTF_8.toString())
        val dbPassword = URLEncoder.encode(dbConfig.password, StandardCharsets.UTF_8.toString())

        //172.17.0.1

        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = "jdbc:mysql://${dbConfig.address}:${dbConfig.port}/${dbName}?user=${dbUsername}&password=${dbPassword}"
        hikariConfig.driverClassName = "com.mysql.cj.jdbc.Driver"

        val hikariSource = HikariDataSource(hikariConfig)
        return hikariSource
    }

    fun UUID.toInputStream(): InputStream {
        val bytes = ByteArray(16)
        ByteBuffer.wrap(bytes)
            .putLong(mostSignificantBits)
            .putLong(leastSignificantBits)
        return ByteArrayInputStream(bytes)
    }

    fun InputStream.toUUID(): UUID? {
        val buffer = ByteBuffer.allocate(16)
        try {
            buffer.put(this.readAllBytes())
            buffer.flip()
            return UUID(buffer.long, buffer.long)
        } catch (e: IOException) {
        }
        return null
    }
}