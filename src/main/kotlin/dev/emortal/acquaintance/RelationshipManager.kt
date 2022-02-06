package dev.emortal.acquaintance

import dev.emortal.acquaintance.RelationshipManager.getFriendsAsync
import dev.emortal.acquaintance.channel.ChatChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
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

    internal var separator = Component.text(" | ", NamedTextColor.DARK_GRAY)

    internal val partyPrefix = Component.text()
        .append(Component.text("PARTY", TextColor.color(255, 100, 255), TextDecoration.BOLD))
        .append(separator)

    internal val friendPrefix = Component.text()
        .append(Component.text("FRIEND", NamedTextColor.GOLD, TextDecoration.BOLD))
        .append(separator)

    internal val errorColor = NamedTextColor.RED
    internal val errorDark = TextColor.color(200, 0, 0)

    internal val successColor = NamedTextColor.GREEN
    internal val successDark = TextColor.color(0, 200, 0)

    val friendCache = ConcurrentHashMap<UUID, MutableList<UUID>>()

    val chatChannelTag = Tag.Integer("chatChannel")

    var Player.channel: ChatChannel
        get() {
            return ChatChannel.values()[getTag(chatChannelTag) ?: 0]
        }
        set(value) = setTag(chatChannelTag, value.ordinal)

    val partyInviteMap = ConcurrentHashMap<UUID, MutableList<UUID>>()
    val friendRequestMap = ConcurrentHashMap<UUID, MutableList<UUID>>()
    val partyMap = ConcurrentHashMap<UUID, Party>()

    val Player.party
        get() = partyMap[this.uuid]

    suspend fun Player.getFriendsAsync() =
        friendCache[uuid] ?: AcquaintanceExtension.storage!!.getFriendsAsync(uuid)

    fun Player.inviteToParty(player: Player) {
        if (partyInviteMap[player.uuid]?.contains(this.uuid) == true) {
            this.sendMessage(Component.text("You have already sent an invite to that player", errorColor))
            return
        }

        partyInviteMap[player.uuid]!!.add(this.uuid)

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
        if (!partyInviteMap[this.uuid]!!.contains(player.uuid)) return false

        partyInviteMap[this.uuid]!!.remove(player.uuid)

        if (player.party == null) return false
        player.party!!.add(this)

        return true
    }

    fun Player.denyInvite(player: Player): Boolean {
        if (!partyInviteMap[this.uuid]!!.contains(player.uuid)) return false

        partyInviteMap[this.uuid]!!.remove(player.uuid)

        return true
    }


    // FRIENDS -----

    fun Player.requestFriend(player: Player) = runBlocking {
        if (this@requestFriend.getFriendsAsync().size > 200) {
            this@requestFriend.sendMessage(Component.text("You already have enough friends", errorColor))
            return@runBlocking
        }

        if (friendRequestMap[player.uuid]?.contains(this@requestFriend.uuid) == true) {
            this@requestFriend.sendMessage(Component.text("You have already sent a request to that player", errorColor))
            return@runBlocking
        }

        if (friendRequestMap[this@requestFriend.uuid]?.contains(player.uuid) == true) {
            player.acceptFriendRequest(this@requestFriend)
            return@runBlocking
        }
        friendRequestMap[player.uuid]!!.add(this@requestFriend.uuid)

        player.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text(this@requestFriend.username, TextColor.color(255, 220, 0), TextDecoration.BOLD))
                .append(
                    Component.text(" wants to be friends! ", TextColor.color(255, 150, 0))
                        .append(
                            Component.text("[✔]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/friend accept ${this@requestFriend.username}"))
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
                                .clickEvent(ClickEvent.runCommand("/friend deny ${this@requestFriend.username}"))
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

        this@requestFriend.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("Sent a friend request to ", successDark))
                .append(Component.text(player.username, successColor, TextDecoration.BOLD))
                .append(Component.text("!", successDark))
        )
    }

    fun Player.acceptFriendRequest(player: Player): Boolean {
        if (!friendRequestMap[this.uuid]!!.contains(player.uuid)) return false

        friendRequestMap[this.uuid]!!.remove(player.uuid)

        AcquaintanceExtension.storage!!.addFriend(this.uuid, player.uuid)
        friendCache[this.uuid]?.add(player.uuid)
        friendCache[player.uuid]?.add(this.uuid)

        this.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("You are now friends with ", successDark))
                .append(Component.text(player.username, successColor, TextDecoration.BOLD))
                .append(Component.text("!", successDark))
        )
        player.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("You are now friends with ", successDark))
                .append(Component.text(this.username, successColor, TextDecoration.BOLD))
                .append(Component.text("!", successDark))
        )

        return true
    }

    fun UUID.removeFriend(uuid: UUID) {
        AcquaintanceExtension.storage!!.removeFriend(this, uuid)
        friendCache[this]?.remove(uuid)
        friendCache[uuid]?.remove(this)
    }

    fun Player.removeFriend(player: Player) = uuid.removeFriend(player.uuid)

    fun Player.denyFriendRequest(player: Player): Boolean {
        if (!friendRequestMap[this.uuid]!!.contains(player.uuid)) return false

        friendRequestMap[this.uuid]!!.remove(player.uuid)

        return true
    }
}