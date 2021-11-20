package dev.emortal.acquaintance.commands

import dev.emortal.acquaintance.RelationshipManager.channel
import dev.emortal.acquaintance.RelationshipManager.errorColor
import dev.emortal.acquaintance.RelationshipManager.party
import dev.emortal.acquaintance.channel.ChatChannel
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.arguments.ArgumentString
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand

object ChannelCommand : Kommand({

    onlyPlayers


    default {
        player.sendMessage(Component.text("You are in channel ${player.channel.name}", NamedTextColor.GOLD))
    }

    val channel = ArgumentString("channel").suggest {
        ChatChannel.values().map { it.name }
    }

    syntax(channel) {
        val chatChannelEnum = ChatChannel.valueOf(!channel)
        if (chatChannelEnum == ChatChannel.PARTY && player.party == null) {
            player.sendMessage(Component.text("You are not in a party", errorColor))
            return@syntax
        }

        player.channel = chatChannelEnum

        player.sendMessage(Component.text("Set channel to ${!channel}", NamedTextColor.GOLD))
    }

}, "channel", "ch")