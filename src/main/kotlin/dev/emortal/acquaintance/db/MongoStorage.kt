package dev.emortal.acquaintance.db

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.UpdateOptions
import dev.emortal.acquaintance.AcquaintanceExtension
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.UuidRepresentation
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.*
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("EventListener")
class MongoStorage : Storage() {

    var client: CoroutineClient? = null
    var database: CoroutineDatabase? = null

    var usernames: CoroutineCollection<CachedUser>? = null

    var blocks: CoroutineCollection<BlockList>? = null
    var friends: CoroutineCollection<FriendList>? = null

    override fun init() {
        logger.info("MONGO!!!")

        client = KMongo.createClient(
            MongoClientSettings
                .builder()
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyConnectionString(ConnectionString(AcquaintanceExtension.databaseConfig.mongoAddress))
                .build()
        ).coroutine
        database = client!!.getDatabase("Mandem")

        usernames = database!!.getCollection("usernames")

        blocks = database!!.getCollection("blocks")
        friends = database!!.getCollection("friends")
    }

    // Blocks
    override suspend fun getBlockedPlayers(player: UUID): Set<UUID> =
        blocks?.findOne(BlockList::uuid eq player)?.blockedUUIDs ?: emptySet()

    override fun blockPlayers(player: UUID, playersToBlock: Set<UUID>): Unit = runBlocking {
        launch {
            val lastBlockList = blocks?.findOne(BlockList::uuid eq player) ?: BlockList(player, emptySet())

            blocks?.replaceOne(BlockList::uuid eq player, lastBlockList.copy(blockedUUIDs = lastBlockList.blockedUUIDs + playersToBlock), ReplaceOptions().upsert(true))
        }
    }

    override fun unblockPlayers(player: UUID, playersToUnblock: Set<UUID>): Unit = runBlocking {
        launch {
            val lastBlockList = blocks?.findOne(BlockList::uuid eq player) ?: BlockList(player, emptySet())

            blocks?.replaceOne(BlockList::uuid eq player, lastBlockList.copy(blockedUUIDs = lastBlockList.blockedUUIDs - playersToUnblock), ReplaceOptions().upsert(true))
        }
    }

    override suspend fun isBlocked(player: UUID, other: UUID): Boolean {
        val a = blocks?.aggregate<BlockList>(
            match(BlockList::uuid eq player),
            match(BlockList::blockedUUIDs contains other)
        )?.toList()

        return !a.isNullOrEmpty()
    }


    // Friends
    override suspend fun getFriends(player: UUID): Set<UUID> {
        return friends?.findOne(FriendList::uuid eq player)?.friendUUIDs ?: emptySet()
    }

    override fun addFriends(player: UUID, friendsToAdd: Set<UUID>): Unit = runBlocking {
        launch {
            friends?.updateOne(FriendList::uuid eq player, pushEach(FriendList::friendUUIDs, friendsToAdd.toList()), UpdateOptions().upsert(true))
        }
    }

    override fun removeFriends(player: UUID, friendsToRemove: Set<UUID>): Unit = runBlocking {
        launch {
            friends?.updateOne(FriendList::uuid eq player, pullAll(FriendList::friendUUIDs, friendsToRemove.toList()), UpdateOptions().upsert(true))
        }
    }

    override suspend fun isFriends(player: UUID, other: UUID): Boolean {
        val a = friends?.aggregate<FriendList>(
            match(FriendList::uuid eq player),
            match(FriendList::friendUUIDs contains other)
        )?.first()

        return a != null
    }


    // Username cache
    override fun setCachedUsername(player: UUID, username: String): Unit = runBlocking {
        launch {
            usernames?.replaceOne(CachedUser::uuid eq player, CachedUser(player, username), ReplaceOptions().upsert(true))
        }
    }

    override suspend fun getCachedUsername(player: UUID): String? =
        usernames?.findOne(CachedUser::uuid eq player)?.username

    override suspend fun getCachedUUID(username: String): UUID? =
        usernames?.findOne(CachedUser::username eq username)?.uuid
}