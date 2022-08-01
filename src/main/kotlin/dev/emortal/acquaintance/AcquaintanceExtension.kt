package dev.emortal.acquaintance

import dev.emortal.acquaintance.config.DatabaseConfig
import dev.emortal.acquaintance.db.MySQLStorage
import dev.emortal.acquaintance.db.Storage
import dev.emortal.immortal.config.ConfigHelper
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerCommandEvent
import net.minestom.server.extensions.Extension
import org.slf4j.LoggerFactory
import org.tinylog.kotlin.Logger
import world.cepi.kstom.adventure.plainText
import world.cepi.kstom.event.listenOnly
import java.nio.file.Path

class AcquaintanceExtension : Extension() {

    companion object {
        var databaseConfig = DatabaseConfig()
        val databaseConfigPath = Path.of("./acquaintance.json")

        var storage: Storage? = null

        fun init(eventNode: EventNode<Event>) {
            databaseConfig = ConfigHelper.initConfigFile(databaseConfigPath, databaseConfig)

            if (databaseConfig.enabled) {
                storage = MySQLStorage()
            }

            val chatLogger = LoggerFactory.getLogger("Chat")
            val commandLogger = LoggerFactory.getLogger("Command")

            eventNode.listenOnly<PlayerCommandEvent> {
                commandLogger.info("${player.displayName?.plainText() ?: player.username} ran command: $command")
            }

            eventNode.listenOnly<PlayerChatEvent> {
                if (databaseConfig.logChat) chatLogger.info("${player.displayName?.plainText() ?: player.username}: $message")
            }

            Logger.info("[Acquaintance] Initialized!")
        }
    }

    override fun initialize() {
        init(eventNode)
    }

    override fun terminate() {
        logger.info("[Acquaintance] Terminated!")
    }

}