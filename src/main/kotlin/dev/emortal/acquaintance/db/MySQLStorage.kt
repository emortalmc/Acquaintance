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
    }

    override fun setCachedUsername(player: UUID, username: String): Unit = runBlocking {
        launch {
            val conn = getConnection()
            conn.autoCommit = false

            val statement = conn.prepareStatement("DELETE FROM namecache WHERE player=?")
            statement.setBinaryStream(1, player.toInputStream())

            statement.executeUpdate()
            statement.close()

            val statement2 = conn.prepareStatement("INSERT INTO namecache VALUES(?, ?)")
            statement2.setBinaryStream(1, player.toInputStream())
            statement2.setString(2, username)

            statement2.executeBatch()
            statement2.close()

            conn.autoCommit = true

            statement.close()
            conn.close()
        }
    }

    override suspend fun getCachedUsernameAsync(player: UUID): String? = coroutineScope {
        return@coroutineScope async {
            val conn = getConnection()
            val statement = conn.prepareStatement("SELECT username FROM namecache WHERE player=?")

            statement.setBinaryStream(1, player.toInputStream())

            val results = statement.executeQuery()

            var name: String? = null
            if (results.first()) name = results.getString(1)

            statement.close()
            conn.close()

            return@async name
        }.await()
    }

    override fun createHikari(): HikariDataSource {
        val dbConfig = AcquaintanceExtension.databaseConfig

        val dbName = URLEncoder.encode(dbConfig.tableName, StandardCharsets.UTF_8.toString())
        val dbUsername = URLEncoder.encode(dbConfig.username, StandardCharsets.UTF_8.toString())
        val dbPassword = URLEncoder.encode(dbConfig.password, StandardCharsets.UTF_8.toString())

        // docker 172.17.0.1

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