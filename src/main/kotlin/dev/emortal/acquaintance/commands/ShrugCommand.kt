package dev.emortal.acquaintance.commands

import world.cepi.kstom.command.kommand.Kommand

object ShrugCommand : Kommand({

    onlyPlayers

    default {
        player.chat("¯\\_☻_/¯")
    }

}, "shrug")