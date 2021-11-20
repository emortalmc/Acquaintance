package dev.emortal.acquaintance.commands

import dev.emortal.acquaintance.RelationshipManager
import dev.emortal.acquaintance.RelationshipManager.acceptInvite
import dev.emortal.acquaintance.RelationshipManager.denyInvite
import dev.emortal.acquaintance.RelationshipManager.errorColor
import dev.emortal.acquaintance.RelationshipManager.inviteToParty
import dev.emortal.acquaintance.RelationshipManager.party
import dev.emortal.acquaintance.util.armify
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.command.builder.arguments.ArgumentWord
import world.cepi.kstom.Manager
import world.cepi.kstom.command.arguments.literal
import world.cepi.kstom.command.kommand.Kommand

object PartyCommand : Kommand({

    onlyPlayers

    default {
        // If in party, show list of members, if not, show help message

        if (player.party == null) {
            player.sendMessage(
                Component.text(
                    "Invite others to your party with /party <username>",
                    NamedTextColor.LIGHT_PURPLE
                )
            )
            return@default
        } else {
            player.chat("/party list")
            return@default
        }
    }

    val user = ArgumentWord("user")

    val accept by literal
    val deny by literal
    val leave by literal
    val list by literal
    val kick by literal
    val destroy by literal

    syntax(list) {
        if (player.party == null) {
            player.sendMessage(Component.text("You are not in a party", errorColor))
            return@syntax
        }

        val message = Component.text()
            .append(Component.text("Players in party:", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
            .append(Component.text(" ${player.party!!.players.size}\n", NamedTextColor.DARK_GRAY))

        player.party!!.players.forEach {
            message.append(Component.text("\n - ", NamedTextColor.DARK_GRAY))
            message.append(Component.text(it.username, NamedTextColor.GRAY))
        }

        player.sendMessage(message.armify())
    }

    syntax(destroy) {
        if (player.party == null) {
            player.sendMessage(Component.text("You are not in a party", errorColor))
            return@syntax
        }

        player.party?.destroy()
    }

    syntax(leave) {
        if (player.party == null) {
            player.sendMessage(Component.text("You are not in a party", errorColor))
            return@syntax
        }

        player.party?.remove(player)
    }

    syntax(accept, user) {
        val user = Manager.connection.getPlayer((!user))

        if (user == null) {
            player.sendMessage(Component.text("That player is not online", errorColor))
            return@syntax
        }

        val successful = player.acceptInvite(user)
        if (!successful) {
            player.sendMessage(Component.text("You have no invite from '${user.username}'", errorColor))
            return@syntax
        }
    }

    syntax(deny, user) {
        val user = Manager.connection.getPlayer((!user))

        if (user == null) {
            player.sendMessage(Component.text("That player is not online", errorColor))
            return@syntax
        }

        val successful = player.denyInvite(user)
        if (!successful) {
            player.sendMessage(Component.text("You have no invite from '${user.username}'", errorColor))
            return@syntax
        }

        player.sendMessage(Component.text("Denied invite from ${user.username}!", NamedTextColor.GREEN))
    }

    syntax(kick, user) {
        if (player.party == null) {
            player.sendMessage(Component.text("You are not in a party", errorColor))
            return@syntax
        }

        val userPlayer = Manager.connection.getPlayer((!user))

        val playerToKick = player.party!!.players.firstOrNull {
            it == userPlayer || it.username.contentEquals(!user, true)
        }

        if (playerToKick == null) {
            player.sendMessage(Component.text("Couldn't find '${!user}' in your party", errorColor))
            return@syntax
        }

        playerToKick.sendMessage(Component.text("You were kicked from ", errorColor))
        player.party?.remove(playerToKick)
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

        if (amountOfInvites > 4) {
            player.sendMessage(Component.text("You are sending too many invites", errorColor))
            return@syntax
        }

        player.inviteToParty(user)
    }

}, "party", "p")