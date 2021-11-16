package dev.emortal.acquaintance.commands

import dev.emortal.acquaintance.RelationshipManager.acceptInvite
import dev.emortal.acquaintance.RelationshipManager.denyInvite
import dev.emortal.acquaintance.RelationshipManager.inviteToParty
import dev.emortal.acquaintance.RelationshipManager.party
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import world.cepi.kstom.command.arguments.ArgumentPlayer
import world.cepi.kstom.command.arguments.literal
import world.cepi.kstom.command.kommand.Kommand

object PartyCommand : Kommand({

    onlyPlayers


    default {
        // If in party, show list of members, if not, show help message

        sender.sendMessage("bad usage")
    }

    val user = ArgumentPlayer("user")

    val accept by literal
    val deny by literal
    val leave by literal
    val list by literal

    syntax(list) {
        if (player.party == null) {
            player.sendMessage(Component.text("You are not in a party", NamedTextColor.RED))
            return@syntax
        }

        val message = Component.text()
            .append(Component.text("Players in party:\n", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
        player.party!!.players.forEach {
            message.append(Component.text(" - ${it.username}\n", NamedTextColor.DARK_PURPLE))
        }

        player.sendMessage(message)
    }

    syntax(user) {
        player.inviteToParty(!user)
    }

    syntax(leave) {
        if (player.party == null) {
            player.sendMessage(Component.text("You are not in a party", NamedTextColor.RED))
            return@syntax
        }

        player.party?.remove(player)
    }

    syntax(accept, user) {
        val successful = player.acceptInvite(!user)
        if (!successful) {
            player.sendMessage(Component.text("You have no invite from that player", NamedTextColor.RED))
            return@syntax
        }
    }

    syntax(deny, user) {
        val successful = player.denyInvite(!user)
        if (!successful) {
            player.sendMessage(Component.text("You have no invite from that player", NamedTextColor.RED))
            return@syntax
        }

        player.sendMessage(Component.text("Denied invite from ${(!user).username}!", NamedTextColor.GREEN))
    }

}, "party", "p")