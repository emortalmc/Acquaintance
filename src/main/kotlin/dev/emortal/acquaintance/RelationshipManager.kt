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
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object RelationshipManager {

    internal val partyPrefix = Component.text()
        .append(Component.text("PARTY", TextColor.color(255, 100, 255), TextDecoration.BOLD))
        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))

    internal val friendPrefix = Component.text()
        .append(Component.text("FRIEND", NamedTextColor.GOLD, TextDecoration.BOLD))
        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))

    internal val errorColor = NamedTextColor.RED


    val friendCache = ConcurrentHashMap<UUID, MutableList<UUID>>()

    val chatChannelTag = Tag.Integer("chatChannel")

    var Player.channel: ChatChannel
        get() {
            return ChatChannel.values()[getTag(chatChannelTag) ?: 0]
        }
        set(value) = setTag(chatChannelTag, value.ordinal)

    val partyInviteMap = ConcurrentHashMap<Player, MutableList<Player>>()
    val friendRequestMap = ConcurrentHashMap<Player, MutableList<Player>>()
    val partyMap = ConcurrentHashMap<Player, Party>()

    val Player.party
        get() = partyMap[this]
    val Player.friends
        get() = friendCache[uuid] ?: AcquaintanceExtension.storage.getFriends(uuid)

    fun Player.inviteToParty(player: Player) {
        if (partyInviteMap[player]?.contains(this) == true) {
            this.sendMessage(Component.text("You have already sent an invite to that player", errorColor))
            return
        }

        partyInviteMap[player]!!.add(this)

        if (!partyMap.contains(this)) Party(this)

        player.sendMessage(
            Component.text()
                .append(partyPrefix)
                .append(Component.text(this.username, TextColor.color(255, 100, 255)))
                .append(
                    Component.text(" has invited you their party! ", TextColor.color(235, 0, 235))
                        .append(
                            Component.text("[✔]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/party accept ${this.username}"))
                                .hoverEvent(HoverEvent.showText(Component.text("Accept invite", NamedTextColor.GREEN)))
                        )
                        .append(Component.space())
                        .append(
                            Component.text("[❌]", NamedTextColor.RED, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/party deny ${this.username}"))
                                .hoverEvent(HoverEvent.showText(Component.text("Deny invite", errorColor)))
                        )
                )
        )

        this.sendMessage(
            Component.text()
                .append(partyPrefix)
                .append(Component.text("Invited '${player.username}' to the party!", NamedTextColor.GREEN))
        )
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
            this.sendMessage(Component.text("You have already sent a request to that player", errorColor))
            return
        }

        friendRequestMap[player]!!.add(this)

        player.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text(this.username, TextColor.color(255, 220, 0)))
                .append(
                    Component.text(" wants to be friends! ", TextColor.color(255, 150, 0))
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

        this.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("Sent a friend request to '${player.username}'!", NamedTextColor.GREEN))
        )
    }

    fun Player.acceptFriendRequest(player: Player): Boolean {
        if (!friendRequestMap[this]!!.contains(player)) return false

        friendRequestMap[this]!!.remove(player)

        AcquaintanceExtension.storage.addFriend(this.uuid, player.uuid)
        friendCache[this.uuid]!!.add(player.uuid)

        this.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("You are now friends with '${player.username}'!", NamedTextColor.GREEN))
        )
        player.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("You are now friends with '${this.username}'!", NamedTextColor.GREEN))
        )

        return true
    }

    fun UUID.removeFriend(uuid: UUID) {
        AcquaintanceExtension.storage.removeFriend(this, uuid)
        friendCache[this]!!.remove(uuid)
    }

    fun Player.removeFriend(player: Player) {
        AcquaintanceExtension.storage.removeFriend(this.uuid, player.uuid)
        friendCache[this.uuid]!!.remove(player.uuid)
    }

    fun Player.denyFriendRequest(player: Player): Boolean {
        if (!friendRequestMap[this]!!.contains(player)) return false

        friendRequestMap[this]!!.remove(player)

        return true
    }
}