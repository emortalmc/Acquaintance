package dev.emortal.acquaintance.commands

import dev.emortal.acquaintance.RelationshipManager
import dev.emortal.acquaintance.RelationshipManager.getFriendsAsync
import dev.emortal.acquaintance.RelationshipManager.separator
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.command.builder.arguments.ArgumentStringArray
import net.minestom.server.command.builder.arguments.ArgumentWord
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.Manager
import world.cepi.kstom.command.kommand.Kommand

object MessageCommand : Kommand({

    onlyPlayers

    val user = ArgumentWord("user")
    val message = ArgumentStringArray("message")

    syntaxSuspending(Dispatchers.IO, user, message) {
        val playerToMessage = Manager.connection.getPlayer(!user)
        val message = !message

        if (playerToMessage == null) {
            player.sendMessage(Component.text("That player is not online", RelationshipManager.errorDark))
            return@syntaxSuspending
        }

        if (!player.getFriendsAsync().contains(playerToMessage.uuid)) {
            player.sendMessage(
                Component.text()
                    .append(Component.text("You are not friends with ", RelationshipManager.errorDark))
                    .append(Component.text(playerToMessage.username, RelationshipManager.errorColor, TextDecoration.BOLD))

            )
            return@syntaxSuspending
        }

        val meComponent = Component.text("ME", NamedTextColor.AQUA, TextDecoration.BOLD)
        val arrowComponent = Component.text(" → ", NamedTextColor.GRAY)
        val messageComponent = Component.text(message.joinToString(separator = " "))

        playerToMessage.sendMessage(
            Component.text()
                .append(Component.text("[", NamedTextColor.DARK_AQUA))
                .append(player.displayName!!)
                .append(arrowComponent)
                .append(meComponent)
                .append(Component.text("] ", NamedTextColor.DARK_AQUA))
                .append(messageComponent)
        )
        player.sendMessage(
            Component.text()
                .append(Component.text("[", NamedTextColor.DARK_AQUA))
                .append(meComponent)
                .append(arrowComponent)
                .append(playerToMessage.displayName!!)
                .append(Component.text("] ", NamedTextColor.DARK_AQUA))
                .append(messageComponent)
        )

        playerToMessage.playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 1f))
    }

}, "message", "msg")