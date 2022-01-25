package dev.emortal.acquaintance

import dev.emortal.acquaintance.RelationshipManager.channel
import dev.emortal.acquaintance.RelationshipManager.friendPrefix
import dev.emortal.acquaintance.RelationshipManager.getFriendsAsync
import dev.emortal.acquaintance.RelationshipManager.party
import dev.emortal.acquaintance.RelationshipManager.partyPrefix
import dev.emortal.acquaintance.channel.ChatChannel
import dev.emortal.acquaintance.commands.ChannelCommand
import dev.emortal.acquaintance.commands.FriendCommand
import dev.emortal.acquaintance.commands.PartyCommand
import dev.emortal.acquaintance.config.DatabaseConfig
import dev.emortal.acquaintance.db.MySQLStorage
import dev.emortal.acquaintance.db.Storage
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.config.ConfigHelper.noPrettyPrintFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.AreaEffectCloudMeta
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.Task
import org.slf4j.LoggerFactory
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class AcquaintanceExtension : Extension() {

    companion object {
        val leavingTasks = ConcurrentHashMap<Player, Task>()
        var storage: Storage? = null

        val chatLogger = LoggerFactory.getLogger("Chat")

        val chatHologramMap = hashMapOf<Player, Pair<Entity, Task>>()

        var databaseConfig = DatabaseConfig()
        val databaseConfigPath = Path.of("./acquaintance.json")

        var playerCache = hashMapOf<String, String>()
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
                //playerCache[player.username] = player.uuid.toString()
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

            // TODO: Leave parties after a while
        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            if (isFirstSpawn) {
                RelationshipManager.partyInviteMap[player.uuid] = mutableListOf()
                RelationshipManager.friendRequestMap[player.uuid] = mutableListOf()
            }
        }

        eventNode.listenOnly<PlayerChatEvent> {

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

            chatHologramMap[player]?.first?.remove()
            chatHologramMap[player]?.second?.cancel()
            chatHologramMap.remove(player)

            val entity = Entity(EntityType.AREA_EFFECT_CLOUD)
            val meta = entity.entityMeta as AreaEffectCloudMeta

            val playerName = player.displayName ?: Component.text(player.username)

            entity.updateViewableRule { recipients.contains(it) }
            meta.radius = 0f
            meta.isHasNoGravity = true
            meta.customName = playerName.append(Component.text(": ")).append(Component.text(if (message.length > 20) message.take(20) + "..." else message))
            meta.isCustomNameVisible = true

            entity.setInstance(player.instance!!, player.position)
                .thenRun {
                    player.addPassenger(entity)

                    val task = Manager.scheduler.buildTask {
                        entity.remove()
                        chatHologramMap.remove(player)
                    }.delay(Duration.ofSeconds(3)).schedule()
                    chatHologramMap[player] = Pair(entity, task)
                }

            recipients.forEach {
                it.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 2f), Sound.Emitter.self())
            }
        }

        if (databaseConfig.enabled) {
            ChannelCommand.register()
            FriendCommand.register()
            PartyCommand.register()
        }

        logger.info("[Acquaintance] Initialized!")
    }

    override fun terminate() {
        if (databaseConfig.enabled) {
            ChannelCommand.unregister()
            FriendCommand.unregister()
            PartyCommand.unregister()
        }

        logger.info("[Acquaintance] Terminated!")
    }

}