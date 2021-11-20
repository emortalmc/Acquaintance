package dev.emortal.acquaintance.db

import com.google.common.io.ByteStreams
import com.mysql.cj.jdbc.Driver
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.DriverManager
import java.util.*


class MariaStorage : Storage() {

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

        val dbName = "s7_friends"
        val dbUsername = "u7_eeFphQHpOy"
        val dbPassword = "x6zZQR1hra8Qm^q1wK!AT3hJ"

        return DriverManager.getConnection("jdbc:mysql://172.17.0.1:3306/${dbName}?user=${dbUsername}&password=${dbPassword}&useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true")
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