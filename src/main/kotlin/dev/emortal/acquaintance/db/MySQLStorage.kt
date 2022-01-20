package dev.emortal.acquaintance.db

import com.google.common.io.ByteStreams
import com.mysql.cj.jdbc.Driver
import dev.emortal.acquaintance.AcquaintanceExtension
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.DriverManager
import java.util.*


class MySQLStorage : Storage() {

    init {
        val statement =
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS friends (`playerA` BINARY(16), `playerB` BINARY(16))")
        statement.executeUpdate()
        statement.close()
    }

    override fun addFriend(player: UUID, friend: UUID) {
        connection.autoCommit = false;

        val statement = connection.prepareStatement("INSERT INTO friends VALUES(?, ?)")
        statement.setBinaryStream(1, player.toInputStream())
        statement.setBinaryStream(2, friend.toInputStream())
        statement.addBatch()

        statement.setBinaryStream(1, friend.toInputStream())
        statement.setBinaryStream(2, player.toInputStream())
        statement.addBatch()

        statement.executeBatch()
        statement.close()

        connection.autoCommit = true
    }

    override fun removeFriend(player: UUID, friend: UUID) {
        connection.autoCommit = false;

        val statement = connection.prepareStatement("DELETE FROM friends WHERE playerA=? AND playerB=?")
        statement.setBinaryStream(1, player.toInputStream())
        statement.setBinaryStream(2, friend.toInputStream())
        statement.addBatch()

        statement.setBinaryStream(1, friend.toInputStream())
        statement.setBinaryStream(2, player.toInputStream())
        statement.addBatch()

        statement.executeBatch()
        statement.close()

        connection.autoCommit = true
    }

    override fun getFriends(player: UUID): List<UUID> {
        val statement = connection.prepareStatement("SELECT playerB FROM friends WHERE playerA=?")
        statement.setBinaryStream(1, player.toInputStream())

        val results = statement.executeQuery()

        val uuidList = mutableListOf<UUID>()
        while (results.next()) {
            results.getBinaryStream(1).toUUID()?.let { uuidList.add(it) }
        }
        statement.close()
        results.close()
        return uuidList
    }

    override fun createConnection(): Connection {
        Driver()

        val dbConfig = AcquaintanceExtension.databaseConfig

        val dbName = URLEncoder.encode(dbConfig.tableName, StandardCharsets.UTF_8.toString())
        val dbUsername = URLEncoder.encode(dbConfig.username, StandardCharsets.UTF_8.toString())
        val dbPassword = URLEncoder.encode(dbConfig.password, StandardCharsets.UTF_8.toString())

        //172.17.0.1
        return DriverManager.getConnection("jdbc:mysql://${dbConfig.address}:${dbConfig.port}/${dbName}?user=${dbUsername}&password=${dbPassword}&useUnicode=true&characterEncoding=UTF-8")
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
            buffer.put(ByteStreams.toByteArray(this))
            buffer.flip()
            return UUID(buffer.long, buffer.long)
        } catch (e: IOException) {
        }
        return null
    }
}