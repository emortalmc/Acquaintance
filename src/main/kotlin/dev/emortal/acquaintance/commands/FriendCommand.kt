package dev.emortal.acquaintance.commands

import dev.emortal.acquaintance.AcquaintanceExtension
import dev.emortal.acquaintance.RelationshipManager
import dev.emortal.acquaintance.RelationshipManager.acceptFriendRequest
import dev.emortal.acquaintance.RelationshipManager.denyFriendRequest
import dev.emortal.acquaintance.RelationshipManager.errorColor
import dev.emortal.acquaintance.RelationshipManager.friendPrefix
import dev.emortal.acquaintance.RelationshipManager.friends
import dev.emortal.acquaintance.RelationshipManager.removeFriend
import dev.emortal.acquaintance.RelationshipManager.requestFriend
import dev.emortal.acquaintance.util.armify
import dev.emortal.immortal.game.GameManager.game
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
                .append(Component.text("Accepted friend request from '${user.username}'!", NamedTextColor.GREEN))
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
            player.sendMessage(Component.text("You have no request from '${user.username}'", errorColor))
            return@syntax
        }

        player.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("Denied friend request from '${user.username}'!", NamedTextColor.GREEN))
        )
    }

    syntax(remove, user) {
        val userPlayer = Manager.connection.getPlayer(!user)
        if (userPlayer == null) {
            player.sendMessage(Component.text("That player is not online", errorColor))
            return@syntax
        }

        val friends = AcquaintanceExtension.storage!!.getFriends(player.uuid)

        val playerToRemove = friends.firstOrNull {
            it == userPlayer.uuid || AcquaintanceExtension.playerCache[it.toString()].contentEquals(!user, true)
        }

        if (playerToRemove == null) {
            player.sendMessage(Component.text("Couldn't find '${!user}' in your friends list", errorColor))
            return@syntax
        }
        if (!friends.contains(playerToRemove)) {
            player.sendMessage(
                Component.text(
                    "You are not friends with '${!user}'",
                    errorColor
                )
            )
        }

        player.uuid.removeFriend(playerToRemove)

        player.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(
                    Component.text(
                        "Removed friend '${AcquaintanceExtension.playerCache[playerToRemove.toString()]}'!",
                        NamedTextColor.GREEN
                    )
                )
        )
    }

    syntax(list) {
        val friends = player.friends
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
                    "It's quiet here, use /friend <username> to get more friends!",
                    NamedTextColor.GRAY,
                    TextDecoration.ITALIC
                )
            )
        }

        val message = Component.text()
            .append(Component.text("Your friends: ", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text("●${onlineFriends.size}", NamedTextColor.GREEN))
            .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
            .append(Component.text("●${friends.size - onlineFriends.size}\n", NamedTextColor.GRAY))
            .append(listComponent)

        player.sendMessage(message.armify())
    }

    syntax(user) {
        val user = Manager.connection.getPlayer((!user))

        if (user == null) {
            player.sendMessage(Component.text("That player is not online", errorColor))
            return@syntax
        }

        if (user.uuid == player.uuid) {
            player.sendMessage(Component.text("Are you really that lonely?", errorColor))
            return@syntax
        }

        val amountOfInvites = RelationshipManager.partyInviteMap[player]?.size ?: 0

        if (amountOfInvites > 2) {
            player.sendMessage(Component.text("You are sending too many friend requests", errorColor))
            return@syntax
        }

        if (AcquaintanceExtension.storage!!.getFriends(player.uuid).contains(user.uuid)) {
            player.sendMessage(Component.text("You are already friends with '${user.username}'", errorColor))
            return@syntax
        }

        player.requestFriend(user)
    }

}, "friend", "f")