package dev.emortal.acquaintance.commands

import dev.emortal.acquaintance.AcquaintanceExtension
import dev.emortal.acquaintance.RelationshipManager.acceptFriendRequest
import dev.emortal.acquaintance.RelationshipManager.denyFriendRequest
import dev.emortal.acquaintance.RelationshipManager.requestFriend
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import world.cepi.kstom.Manager
import world.cepi.kstom.command.arguments.ArgumentPlayer
import world.cepi.kstom.command.arguments.literal
import world.cepi.kstom.command.kommand.Kommand

object FriendCommand : Kommand({

    onlyPlayers


    default {
        // If in party, show list of members, if not, show help message

        sender.sendMessage("bad usage")
    }

    val user = ArgumentPlayer("user")

    val accept by literal
    val deny by literal
    val remove by literal
    val list by literal

    syntax(user) {
        player.requestFriend(!user)
    }

    syntax(accept, user) {
        val successful = player.acceptFriendRequest(!user)
        if (!successful) {
            player.sendMessage(Component.text("You have no request from that player", NamedTextColor.RED))
            return@syntax
        }
    }

    syntax(deny, user) {
        val successful = player.denyFriendRequest(!user)
        if (!successful) {
            player.sendMessage(Component.text("You have no request from that player", NamedTextColor.RED))
            return@syntax
        }

        player.sendMessage(Component.text("Denied invite from ${(!user).username}!", NamedTextColor.GREEN))
    }

    syntax(remove, user) {

    }

    syntax(list) {
        val message = Component.text()
            .append(Component.text("Your friends:\n", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))

        val friends =
            AcquaintanceExtension.storage.getFriends(player.uuid).mapNotNull { Manager.connection.getPlayer(it) }

        friends.forEach {
            message.append(Component.text(" - ${it.username}\n", NamedTextColor.DARK_PURPLE))
        }

        player.sendMessage(message)
    }

}, "friend", "f")