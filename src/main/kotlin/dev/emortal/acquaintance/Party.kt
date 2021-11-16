package dev.emortal.acquaintance

import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.entity.Player
import world.cepi.kstom.adventure.sendMiniMessage
import java.util.concurrent.ConcurrentHashMap

class Party(var leader: Player) : PacketGroupingAudience {

    private val players: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    var privateGames = false
        set(value) {
            sendMiniMessage("<light_purple>Private games ${if (value) "enabled" else "disabled"}")
            field = value
        }

    init {
        add(leader)
        leader.sendMiniMessage("<green>Welcome to your party! Use /party invite to invite players!")
    }

    fun add(player: Player, sendMessage: Boolean = true) {
        players.add(player)
        RelationshipManager.partyMap[player] = this
        if (player != leader && sendMessage) sendMiniMessage("${player.username} joined the party")
    }

    fun remove(player: Player, sendMessage: Boolean = true) {
        if (sendMessage) sendMiniMessage("${player.username} left the party")

        players.remove(player)
        if (players.size == 1) leader = players.first()

        RelationshipManager.partyMap.remove(player)
    }

    override fun getPlayers(): MutableCollection<Player> = players

}