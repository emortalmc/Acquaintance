package dev.emortal.acquaintance.util

import dev.emortal.acquaintance.AcquaintanceExtension
import org.redisson.Redisson
import org.redisson.config.Config

object RedisStorage {

    val redisson = Redisson.create(Config().also { it.useSingleServer().setAddress(AcquaintanceExtension.databaseConfig.redisAddress).setClientName("Acquaintance") })

}