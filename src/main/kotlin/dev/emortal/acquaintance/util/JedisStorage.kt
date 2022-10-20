package dev.emortal.acquaintance.util

import dev.emortal.acquaintance.AcquaintanceExtension
import redis.clients.jedis.JedisPooled

object JedisStorage {

    val jedis = JedisPooled(AcquaintanceExtension.databaseConfig.redisAddress)

}