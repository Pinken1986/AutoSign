package com.tynicraft.autoSign

import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.block.Chest
import org.bukkit.block.Furnace
import org.bukkit.block.data.type.WallSign
import org.bukkit.block.BlockFace

class SignListener(private val plugin: AutoSign) : Listener {

    companion object {
        private const val GIVE_KEYWORD = "<give>"
        private const val TAKE_KEYWORD = "<take>"
        private const val FURNACE_KEYWORD = "<furnace>"
        private const val INVALID_SIGN_FORMAT_MESSAGE = "Invalid AutoSign format. Use <give> or <take> on first line and <furnace> on second line."
        private const val AUTOSIGN_CREATED_MESSAGE = "AutoSign created: %s items %s furnace"
        private const val NO_CHEST_FOUND_MESSAGE = "No chest found behind the sign."
        private const val NO_FURNACE_FOUND_MESSAGE = "No furnace found adjacent to the chest."
    }

    @EventHandler
    fun onSignChange(event: SignChangeEvent) {
        val player = event.player

        val firstLine = event.getLine(0)
        val secondLine = event.getLine(1)

        if (firstLine == null || secondLine == null) {
            player.sendMessage(INVALID_SIGN_FORMAT_MESSAGE)
            plugin.logger.warning("Invalid AutoSign attempt: null lines")
            return
        }

        if (isValidAutoSign(firstLine, secondLine)) {
            val signBlock = event.block
            val signData = signBlock.blockData

            if (signData is WallSign) {
                val chestBlock = signBlock.getRelative(signData.facing.oppositeFace)

                if (chestBlock.state is Chest) {
                    val furnaceDirection = plugin.findAdjacentFurnace(chestBlock)
                    if (furnaceDirection != null) {
                        val isGiveSign = firstLine.equals(GIVE_KEYWORD, ignoreCase = true)
                        val autoSign = AutoSignData(signBlock.location, isGiveSign, furnaceDirection)
                        plugin.activeAutoSigns.add(autoSign)

                        val actionWord = if (isGiveSign) "Giving" else "Taking"
                        val directionWord = if (isGiveSign) "to" else "from"
                        player.sendMessage(AUTOSIGN_CREATED_MESSAGE.format(actionWord, directionWord))
                        plugin.logger.info("AutoSign created by ${player.name} at ${event.block.location}: $actionWord items $directionWord furnace")
                    } else {
                        player.sendMessage(NO_FURNACE_FOUND_MESSAGE)
                        plugin.logger.warning("No furnace found for AutoSign by ${player.name} at ${event.block.location}")
                    }
                } else {
                    player.sendMessage(NO_CHEST_FOUND_MESSAGE)
                    plugin.logger.warning("No chest found for AutoSign by ${player.name} at ${event.block.location}")
                }
            } else {
                player.sendMessage("The sign must be attached to a chest.")
                plugin.logger.warning("Invalid sign placement by ${player.name} at ${event.block.location}")
            }
        } else {
            player.sendMessage(INVALID_SIGN_FORMAT_MESSAGE)
            plugin.logger.warning("Invalid AutoSign attempt by ${player.name} at ${event.block.location}")
        }
    }

    private fun isValidAutoSign(firstLine: String, secondLine: String): Boolean {
        return (firstLine.equals(GIVE_KEYWORD, ignoreCase = true) || firstLine.equals(TAKE_KEYWORD, ignoreCase = true)) &&
                secondLine.equals(FURNACE_KEYWORD, ignoreCase = true)
    }
}