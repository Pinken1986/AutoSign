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

        if (args.size < 2) {
            sender.sendMessage("Usage: /autosign-test <item> <direction>")
            return true
        }

        val item = args[0]
        val direction = args[1]

        // Simulate sign creation
        plugin.logger.info("Test AutoSign created by ${sender.name}: Moving $item to the $direction")
        sender.sendMessage("Test AutoSign created: Moving $item to the $direction")

        // Here you would call your AutoSign creation logic
        // For now, we'll just log it

        return true
    }
}