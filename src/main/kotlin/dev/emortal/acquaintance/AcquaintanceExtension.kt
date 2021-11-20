package dev.emortal.acquaintance

import dev.emortal.acquaintance.RelationshipManager.channel
import dev.emortal.acquaintance.RelationshipManager.friendPrefix
import dev.emortal.acquaintance.RelationshipManager.party
import dev.emortal.acquaintance.RelationshipManager.partyPrefix
import dev.emortal.acquaintance.channel.ChatChannel
import dev.emortal.acquaintance.commands.ChannelCommand
import dev.emortal.acquaintance.commands.FriendCommand
import dev.emortal.acquaintance.commands.PartyCommand
import dev.emortal.acquaintance.config.ConfigurationHelper
import dev.emortal.acquaintance.db.MariaStorage
import dev.emortal.acquaintance.db.Storage
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.timer.Task
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class AcquaintanceExtension : Extension() {

    companion object {
        val leavingTasks = ConcurrentHashMap<Player, Task>()
        val storage: Storage = MariaStorage()

        var playerCache = hashMapOf<String, String>()
        val playerCacheConfigPath = Path.of("./playerCache.json")
    }

    override fun initialize() {
        playerCache = ConfigurationHelper.initConfigFile(playerCacheConfigPath, playerCache)

        eventNode.listenOnly<PlayerLoginEvent> {
            Audience.audience(storage.getFriends(player.uuid).mapNotNull { Manager.connection.getPlayer(it) })
                .sendMessage(
                    Component.text()
                        .append(friendPrefix)
                        .append(Component.text(player.username, NamedTextColor.GREEN))
                        .append(Component.text(" joined the server", NamedTextColor.GRAY))
                )

            if (playerCache[player.uuid.toString()] == player.username) return@listenOnly
            playerCache[player.uuid.toString()] = player.username
            ConfigurationHelper.writeObjectToPath(playerCacheConfigPath, playerCache)
        }

        eventNode.listenOnly<PlayerDisconnectEvent> {
            RelationshipManager.partyInviteMap.remove(player)
            RelationshipManager.friendRequestMap.remove(player)

            Audience.audience(storage.getFriends(player.uuid).mapNotNull { Manager.connection.getPlayer(it) })
                .sendMessage(
                    Component.text()
                        .append(friendPrefix)
                        .append(Component.text(player.username, NamedTextColor.RED))
                        .append(Component.text(" left the server", NamedTextColor.GRAY))
                )
        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            if (isFirstSpawn) {
                RelationshipManager.partyInviteMap[player] = mutableListOf()
                RelationshipManager.friendRequestMap[player] = mutableListOf()
            }
        }

        eventNode.listenOnly<PlayerChatEvent> {
            setChatFormat {
                Component.text()
                    .append(Component.text("${player.username}: ", NamedTextColor.WHITE))
                    .append(Component.text(message, NamedTextColor.GRAY))
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

                    player.channel = ChatChannel.GLOBAL
                    return@listenOnly
                }

                recipients.clear()
                recipients.addAll(player.party!!.players)

                setChatFormat {
                    Component.text()
                        .append(partyPrefix)
                        .append(Component.text("${player.username}: ", NamedTextColor.WHITE))
                        .append(Component.text(message, NamedTextColor.GRAY))
                        .build()
                }
            }
        }

        eventNode.listenOnly<PlayerDisconnectEvent> {
            RelationshipManager.partyInviteMap.remove(player)

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

            // TODO: Leave parties after a while
        }

        ChannelCommand.register()
        FriendCommand.register()
        PartyCommand.register()

        logger.info("[Acquaintance] Initialized!")
    }

    override fun terminate() {
        ChannelCommand.unregister()
        FriendCommand.unregister()
        PartyCommand.unregister()

        storage.connection.close()

        logger.info("[Acquaintance] Terminated!")
    }

}