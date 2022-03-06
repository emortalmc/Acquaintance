package dev.emortal.acquaintance

import dev.emortal.acquaintance.util.armify
import dev.emortal.immortal.event.PlayerJoinGameEvent
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGame
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.entity.Player
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import java.util.concurrent.ConcurrentHashMap

class Party(var leader: Player) : PacketGroupingAudience {

    private val players: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    var privateGames = false

    init {
        add(leader)
        leader.sendMessage(
            Component.text("Welcome to your party, ", NamedTextColor.GRAY)
                .append(Component.text(leader.username, NamedTextColor.GREEN, TextDecoration.BOLD))
                .armify()
        )

        /*Manager.globalEvent.listenOnly<PlayerJoinGameEvent> {
            val game = getGame()
            players.forEach {
                it.joinGame(game)
            }
        }*/
    }

    fun add(player: Player, sendMessage: Boolean = true) {
        players.add(player)
        RelationshipManager.partyMap[player.uuid] = this
        if (player != leader && sendMessage) sendMessage(
            Component.text()
                .append(Component.text("JOIN PARTY", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.GREEN))
                .append(Component.text(" joined the party", NamedTextColor.GRAY))
        )
    }

    fun remove(player: Player, sendMessage: Boolean = true) {
        if (sendMessage) sendMessage(
            Component.text()
                .append(Component.text("QUIT PARTY", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.RED))
                .append(Component.text(" left the party", NamedTextColor.GRAY))
        )

        players.remove(player)
        if (players.size == 1) leader = players.first()

        RelationshipManager.partyMap.remove(player.uuid)
    }

    fun destroy() {
        sendMessage(Component.text("The party was destroyed", NamedTextColor.RED))
        players.forEach {
            RelationshipManager.partyMap.remove(it.uuid)
        }
        players.clear()
    }

    override fun getPlayers(): MutableCollection<Player> = players

}