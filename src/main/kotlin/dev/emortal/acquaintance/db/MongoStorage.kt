package dev.emortal.acquaintance.db

import com.mongodb.client.model.ReplaceOptions
import dev.emortal.acquaintance.AcquaintanceExtension
import dev.emortal.acquaintance.AcquaintanceExtension.Companion.databaseConfig
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo

class MongoStorage : Storage() {
    override suspend fun setCachedUsername(uuid: String, username: String) {
        cachedUsernames?.replaceOne(CachedUser::uuid eq uuid, CachedUser(uuid, username), ReplaceOptions().upsert(true))
    }

    override suspend fun getCachedUsernameAsync(uuid: String): String? =
        cachedUsernames?.findOne(CachedUser::uuid eq uuid)?.username

    var client: CoroutineClient? = null
    var database: CoroutineDatabase? = null

    var cachedUsernames: CoroutineCollection<CachedUser>? = null

    override fun init() {
        client = KMongo.createClient(databaseConfig.mongoAddress).coroutine
        database = client!!.getDatabase("Acquaintance")
        cachedUsernames = database!!.getCollection("cachedUsernames")
    }
}