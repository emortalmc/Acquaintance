package dev.emortal.acquaintance

import dev.emortal.acquaintance.channel.ChatChannel
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag
import java.util.concurrent.ConcurrentHashMap

object RelationshipManager {

    val chatChannelTag = Tag.Integer("chatChannel")

    var Player.channel: ChatChannel
        get() {
            return ChatChannel.values()[getTag(chatChannelTag) ?: 0]
        }
        set(value) = setTag(chatChannelTag, value.ordinal)

    val partyInviteMap = ConcurrentHashMap<Player, MutableList<Player>>().withDefault { mutableListOf() }
    val friendRequestMap = ConcurrentHashMap<Player, MutableList<Player>>().withDefault { mutableListOf() }
    val partyMap = ConcurrentHashMap<Player, Party>()

    val Player.party
        get() = partyMap[this]

    fun Player.inviteToParty(player: Player) {
        if (partyInviteMap[player]?.contains(this) == true) {
            this.sendMessage(Component.text("You have already sent an invite to that player", NamedTextColor.RED))
            return
        }

        partyInviteMap[player]!!.add(this)

        if (!partyMap.contains(this)) Party(this)

        player.sendMessage(
            Component.text()
                .append(Component.text(this.username, TextColor.color(255, 90, 255)))
                .append(
                    Component.text(" has invited you their party! ", TextColor.color(200, 0, 200))
                        .append(
                            Component.text("[✔]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/party accept ${this.username}"))
                                .hoverEvent(HoverEvent.showText(Component.text("Accept invite", NamedTextColor.GREEN)))
                        )
                        .append(Component.space())
                        .append(
                            Component.text("[❌]", NamedTextColor.RED, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/party deny ${this.username}"))
                                .hoverEvent(HoverEvent.showText(Component.text("Deny invite", NamedTextColor.RED)))
                        )
                )
        )

        this.sendMessage(Component.text("Invited ${player.username} to the party"))
    }

    fun Player.acceptInvite(player: Player): Boolean {
        if (!partyInviteMap[this]!!.contains(player)) return false

        partyInviteMap[this]!!.remove(player)

        if (player.party == null) return false
        player.party!!.add(this)

        return true
    }

    fun Player.denyInvite(player: Player): Boolean {
        if (!partyInviteMap[this]!!.contains(player)) return false

        partyInviteMap[this]!!.remove(player)

        return true
    }


    // FRIENDS -----

    fun Player.requestFriend(player: Player) {
        if (friendRequestMap[player]?.contains(this) == true) {
            this.sendMessage(Component.text("You have already sent a request to that player", NamedTextColor.RED))
            return
        }

        friendRequestMap[player]!!.add(this)

        player.sendMessage(
            Component.text()
                .append(Component.text(this.username, TextColor.color(255, 90, 255)))
                .append(
                    Component.text(" wants to be friends! ", TextColor.color(200, 0, 200))
                        .append(
                            Component.text("[✔]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/friend accept ${this.username}"))
                                .hoverEvent(
                                    HoverEvent.showText(
                                        Component.text(
                                            "Accept friend request",
                                            NamedTextColor.GREEN
                                        )
                                    )
                                )
                        )
                        .append(Component.space())
                        .append(
                            Component.text("[❌]", NamedTextColor.RED, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/friend deny ${this.username}"))
                                .hoverEvent(
                                    HoverEvent.showText(
                                        Component.text(
                                            "Deny friend request",
                                            NamedTextColor.RED
                                        )
                                    )
                                )
                        )
                )
        )

        this.sendMessage(Component.text("Sent a friend request to ${player.username}"))
    }

    fun Player.acceptFriendRequest(player: Player): Boolean {
        if (!friendRequestMap[this]!!.contains(player)) return false

        friendRequestMap[this]!!.remove(player)

        AcquaintanceExtension.storage.addFriend(this.uuid, player.uuid)

        return true
    }

    fun Player.denyFriendRequest(player: Player): Boolean {
        if (!friendRequestMap[this]!!.contains(player)) return false

        friendRequestMap[this]!!.remove(player)

        return true
    }
}