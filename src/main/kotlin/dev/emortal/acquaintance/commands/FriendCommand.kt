package dev.emortal.acquaintance.commands

import dev.emortal.acquaintance.AcquaintanceExtension
import dev.emortal.acquaintance.RelationshipManager
import dev.emortal.acquaintance.RelationshipManager.acceptFriendRequest
import dev.emortal.acquaintance.RelationshipManager.denyFriendRequest
import dev.emortal.acquaintance.RelationshipManager.friendPrefix
import dev.emortal.acquaintance.RelationshipManager.removeFriend
import dev.emortal.acquaintance.RelationshipManager.requestFriend
import dev.emortal.acquaintance.RelationshipManager.successColor
import dev.emortal.acquaintance.RelationshipManager.errorColor
import dev.emortal.acquaintance.RelationshipManager.errorDark
import dev.emortal.acquaintance.RelationshipManager.getFriendsAsync
import dev.emortal.acquaintance.RelationshipManager.successDark
import dev.emortal.acquaintance.util.armify
import dev.emortal.immortal.game.GameManager.game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.command.builder.arguments.ArgumentWord
import world.cepi.kstom.Manager
import world.cepi.kstom.command.arguments.literal
import world.cepi.kstom.command.kommand.Kommand

object FriendCommand : Kommand({

    onlyPlayers

    val user = ArgumentWord("user")

    val accept by literal
    val deny by literal
    val remove by literal
    val list by literal

    syntax(accept, user) {
        val user = Manager.connection.getPlayer((!user))

        if (user == null) {
            player.sendMessage(Component.text("That player is not online", errorColor))
            return@syntax
        }

        val successful = player.acceptFriendRequest(user)
        if (!successful) {
            player.sendMessage(Component.text("You have no request from '${user.username}'", errorColor))
            return@syntax
        }

        player.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("Accepted friend request from ", successDark))
                .append(Component.text(user.username, successColor, TextDecoration.BOLD))
                .append(Component.text("!", successDark))
        )
    }

    syntax(deny, user) {
        val user = Manager.connection.getPlayer((!user))

        if (user == null) {
            player.sendMessage(Component.text("That player is not online", errorColor))
            return@syntax
        }

        val successful = player.denyFriendRequest(user)
        if (!successful) {
            player.sendMessage(
                Component.text()
                    .append(Component.text("You have no request from ", errorDark))
                    .append(Component.text(user.username, errorColor, TextDecoration.BOLD))

            )
            return@syntax
        }

        player.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("Denied friend request from ", successDark))
                .append(Component.text(user.username, successColor, TextDecoration.BOLD))
                .append(Component.text("!", successDark))
        )
    }

    syntaxSuspending(Dispatchers.IO, remove, user) {
        val friends = AcquaintanceExtension.storage!!.getFriendsAsync(player.uuid)

        val playerToRemove = friends.firstOrNull {
            AcquaintanceExtension.playerCache[it.toString()].contentEquals(!user, true)
        }

        if (playerToRemove == null) {
            player.sendMessage(
                Component.text()
                    .append(Component.text("You are not friends with ", errorDark))
                    .append(Component.text(!user, errorColor, TextDecoration.BOLD))
            )
            return@syntaxSuspending
        }

        player.uuid.removeFriend(playerToRemove)

        player.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("Removed ", successDark))
                .append(Component.text(AcquaintanceExtension.playerCache[playerToRemove.toString()]!!, successColor, TextDecoration.BOLD))
                .append(Component.text("!", successDark))
        )
    }

    syntaxSuspending(Dispatchers.IO, list) {
        val friends = player.getFriendsAsync()
        val onlineFriends = friends.mapNotNull { Manager.connection.getPlayer(it) }

        val listComponent = Component.text()

        friends.sortedBy { Manager.connection.getPlayer(it) == null }.forEach {
            val itPlayer = Manager.connection.getPlayer(it)

            listComponent.append(Component.text("\n - ", NamedTextColor.DARK_GRAY))
            listComponent.append(
                Component.text(
                    AcquaintanceExtension.playerCache[it.toString()] ?: "??",
                    if (itPlayer == null) NamedTextColor.GRAY else NamedTextColor.GREEN
                )
            )
            if (itPlayer != null) {
                val playingGame = itPlayer.game?.gameTypeInfo?.gameName

                listComponent.append(
                    Component.text(" (playing ${playingGame})", NamedTextColor.DARK_GRAY)
                )
            }

        }

        if (friends.isEmpty()) {
            listComponent.append(
                Component.text(
                    "\nIt's quiet here, use /friend <username> to add friends!",
                    NamedTextColor.GRAY,
                    TextDecoration.ITALIC
                )
            )
        }

        val specialMessage = when (onlineFriends.size) {
            69 -> "(nice)"
            else -> ""
        }

        val offlineFriendCount = friends.size - onlineFriends.size

        val message = Component.text()
            .append(Component.text("Your friends: ", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("●${onlineFriends.size} $specialMessage", NamedTextColor.GREEN))

        if (offlineFriendCount != 0) {
            message
                .append(Component.text("| ", NamedTextColor.DARK_GRAY))
                .append(Component.text("●${offlineFriendCount}\n", NamedTextColor.GRAY))
        }

        message.append(listComponent)

        player.sendMessage(message.armify())
    }

    syntaxSuspending(Dispatchers.IO, user) {
        val user = Manager.connection.getPlayer((!user))

        if (user == null) {
            player.sendMessage(Component.text("That player is not online", errorColor))
            return@syntaxSuspending
        }

        if (user.uuid == player.uuid) {
            player.sendMessage(Component.text("Are you really that lonely?", errorColor))
            return@syntaxSuspending
        }

        val amountOfInvites = RelationshipManager.partyInviteMap[player.uuid]?.size ?: 0

        if (amountOfInvites > 2) {
            player.sendMessage(Component.text("You are sending too many friend requests", errorColor))
            return@syntaxSuspending
        }

        if (AcquaintanceExtension.storage!!.getFriendsAsync(player.uuid).contains(user.uuid)) {
            player.sendMessage(Component.text("You are already friends with '${user.username}'", errorColor))
            return@syntaxSuspending
        }

        player.requestFriend(user)
    }

}, "friend", "f")