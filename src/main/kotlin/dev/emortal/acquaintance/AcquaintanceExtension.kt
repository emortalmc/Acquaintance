package dev.emortal.acquaintance

import dev.emortal.acquaintance.RelationshipManager.channel
import dev.emortal.acquaintance.RelationshipManager.friendPrefix
import dev.emortal.acquaintance.RelationshipManager.party
import dev.emortal.acquaintance.RelationshipManager.partyPrefix
import dev.emortal.acquaintance.channel.ChatChannel
import dev.emortal.acquaintance.commands.*
import dev.emortal.acquaintance.config.DatabaseConfig
import dev.emortal.acquaintance.db.MySQLStorage
import dev.emortal.acquaintance.db.Storage
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.config.ConfigHelper.noPrettyPrintFormat
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import net.minestom.server.timer.Task
import org.slf4j.LoggerFactory
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.plainText
import world.cepi.kstom.event.listenOnly
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class AcquaintanceExtension : Extension() {

    companion object {
        val leavingTasks = ConcurrentHashMap<Player, Task>()
        var storage: Storage? = null

        var databaseConfig = DatabaseConfig()
        val databaseConfigPath = Path.of("./acquaintance.json")

        var playerCache = mutableMapOf<String, String>()
        val playerCacheConfigPath = Path.of("./playerCache.json")
    }

    override fun initialize() = runBlocking {
        playerCache = ConfigHelper.initConfigFile(playerCacheConfigPath, playerCache, noPrettyPrintFormat)
        databaseConfig = ConfigHelper.initConfigFile(databaseConfigPath, databaseConfig)

        if (databaseConfig.enabled) {
            storage = MySQLStorage()
        }

        eventNode.listenOnly<PlayerLoginEvent> {
            if (databaseConfig.enabled) {
                launch {
                    Audience.audience(storage!!.getFriendsAsync(player.uuid).mapNotNull { Manager.connection.getPlayer(it) })
                        .sendMessage(
                            Component.text()
                                .append(friendPrefix)
                                .append(Component.text(player.username, NamedTextColor.GREEN))
                                .append(Component.text(" joined the server", NamedTextColor.GRAY))
                        )
                }
            }

            leavingTasks[player]?.cancel()
            leavingTasks.remove(player)

            if (playerCache[player.uuid.toString()] != player.username) {
                playerCache[player.uuid.toString()] = player.username
                ConfigHelper.writeObjectToPath(playerCacheConfigPath, playerCache, noPrettyPrintFormat)
            }
        }

        eventNode.listenOnly<PlayerDisconnectEvent> {
            RelationshipManager.partyInviteMap.remove(player.uuid)
            RelationshipManager.friendRequestMap.remove(player.uuid)

            if (databaseConfig.enabled) {
                launch {
                    Audience.audience(storage!!.getFriendsAsync(player.uuid).mapNotNull { Manager.connection.getPlayer(it) })
                        .sendMessage(
                            Component.text()
                                .append(friendPrefix)
                                .append(Component.text(player.username, NamedTextColor.RED))
                                .append(Component.text(" left the server", NamedTextColor.GRAY))
                        )
                }
            }

            RelationshipManager.partyInviteMap.remove(player.uuid)

            if (player.party != null) leavingTasks[player] =
                Manager.scheduler.buildTask {
                    player.party?.sendMessage(
                        Component.text(
                            "${player.username} was kicked from the party because they were offline",
                            NamedTextColor.GOLD
                        )
                    )
                    player.party?.remove(player, false)
                }.delay(Duration.ofMinutes(5)).schedule()

        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            if (isFirstSpawn) {
                RelationshipManager.partyInviteMap[player.uuid] = mutableListOf()
                RelationshipManager.friendRequestMap[player.uuid] = mutableListOf()
            }
        }

        val chatLogger = LoggerFactory.getLogger("Chat")
        val commandLogger = LoggerFactory.getLogger("Command")

        eventNode.listenOnly<PlayerCommandEvent> {
            commandLogger.info("${player.displayName!!.plainText()} ran command: $command")
        }

        eventNode.listenOnly<PlayerChatEvent> {

            message = message
                .replace("lag", "high performance")

            setChatFormat {
                Component.text()
                    .append(player.displayName!!)
                    .append(Component.text(": "))
                    .append(Component.text(message))
                    .build()
            }

            if (player.channel == ChatChannel.PARTY) {
                if (player.party == null) {
                    player.sendMessage(
                        Component.text(
                            "Set your channel to global because you are not in a party",
                            NamedTextColor.GOLD
                        )
                    )

                    isCancelled = true
                    player.channel = ChatChannel.GLOBAL
                    return@listenOnly
                }

                recipients.clear()
                recipients.addAll(player.party!!.players)

                setChatFormat {
                    Component.text()
                        .append(partyPrefix)
                        .append(player.displayName!!)
                        .append(Component.text(": "))
                        .append(Component.text(message))
                        .build()
                }
            }

            chatLogger.info("${player.displayName!!.plainText()}: $message")
        }

        if (databaseConfig.enabled) {
            ChannelCommand.register()
            FriendCommand.register()
            MessageCommand.register()
            PartyCommand.register()
            ShrugCommand.register()
            ReplyCommand.register()
        }

        logger.info("[Acquaintance] Initialized!")
    }

    override fun terminate() {
        ChannelCommand.unregister()
        FriendCommand.unregister()
        MessageCommand.unregister()
        PartyCommand.unregister()
        ShrugCommand.unregister()
        ReplyCommand.unregister()

        logger.info("[Acquaintance] Terminated!")
    }

}