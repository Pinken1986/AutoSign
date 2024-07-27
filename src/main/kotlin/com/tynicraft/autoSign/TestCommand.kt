package com.tynicraft.autoSign

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class TestCommand(private val plugin: AutoSign) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be run by a player.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("Usage: /autosign-test <subcommand> [args...]")
            return true
        }

        when (args[0].toLowerCase()) {
            "autosign" -> handleAutoSignTest(sender, args.drop(1))
            "perk" -> handlePerkTest(sender, args.drop(1))
            else -> {
                sender.sendMessage("Unknown subcommand. Use 'autosign' or 'perk'.")
                return true
            }
        }

        return true
    }

    private fun handleAutoSignTest(player: Player, args: List<String>) {
        if (args.size < 2) {
            player.sendMessage("Usage: /autosign-test autosign <item> <direction>")
            return
        }

        val item = args[0]
        val direction = args[1]

        // Simulate AutoSign creation
        plugin.logger.info("Test AutoSign created by ${player.name}: Moving $item to the $direction")
        player.sendMessage("Test AutoSign created: Moving $item to the $direction")
    }

    private fun handlePerkTest(player: Player, args: List<String>) {
        if (args.size < 3) {
            player.sendMessage("Usage: /autosign-test perk <type> <duration> <cost>")
            return
        }

        val type = args[0]
        val duration = args[1].toLongOrNull()
        val cost = args[2].toDoubleOrNull()

        if (duration == null || cost == null) {
            player.sendMessage("Invalid duration or cost. Please use numbers.")
            return
        }

        // Simulate Perk sign creation
        plugin.logger.info("Test Perk sign created by ${player.name}: $type perk for $duration seconds, cost: $cost")
        player.sendMessage("Test Perk sign created: $type perk for $duration seconds, cost: $cost")
    }
}