package dev.emortal.acquaintance

import dev.emortal.acquaintance.config.DatabaseConfig
import dev.emortal.acquaintance.db.MongoStorage
import dev.emortal.acquaintance.db.Storage
import dev.emortal.immortal.config.ConfigHelper
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import java.nio.file.Path

class AcquaintanceExtension {

    companion object {
        var databaseConfig = DatabaseConfig()
        val databaseConfigPath = Path.of("./acquaintance.json")

        var storage: Storage? = null

        fun init(eventNode: EventNode<Event>) {

            databaseConfig = ConfigHelper.initConfigFile(databaseConfigPath, databaseConfig)

            if (databaseConfig.enabled) {
                storage = MongoStorage()
                storage!!.init()
            }
        }
    }

}