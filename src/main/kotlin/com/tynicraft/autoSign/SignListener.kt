package com.tynicraft.autoSign

import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.block.Chest
import org.bukkit.block.data.type.WallSign
import org.bukkit.event.player.PlayerInteractEvent

class SignListener(private val plugin: AutoSign) : Listener {

    companion object {
        private const val GIVE_KEYWORD = "<give>"
        private const val TAKE_KEYWORD = "<take>"
        private const val FURNACE_KEYWORD = "<furnace>"
        private const val PERK_KEYWORD = "[Perk]"
        private const val INVALID_SIGN_FORMAT_MESSAGE = "Invalid AutoSign format. Use <give> or <take> on first line and <furnace> on second line."
        private const val AUTOSIGN_CREATED_MESSAGE = "AutoSign created: %s items %s furnace"
        private const val NO_CHEST_FOUND_MESSAGE = "No chest found behind the sign."
        private const val NO_FURNACE_FOUND_MESSAGE = "No furnace found adjacent to the chest."
    }

    private val perkSign = PerkSign(plugin)

    @EventHandler
    fun onSignChange(event: SignChangeEvent) {
        val player = event.player
        val firstLine = event.getLine(0)
        val secondLine = event.getLine(1)

        if (firstLine.isNullOrEmpty() || secondLine.isNullOrEmpty()) {
            player.sendMessage(INVALID_SIGN_FORMAT_MESSAGE)
            plugin.logger.warning("Invalid sign attempt: null or empty lines")
            return
        }

        when {
            isValidAutoSign(firstLine, secondLine) -> handleAutoSign(event)
            firstLine.equals(PERK_KEYWORD, ignoreCase = true) -> handlePerkSign(event)
            else -> {
                player.sendMessage(INVALID_SIGN_FORMAT_MESSAGE)
                plugin.logger.warning("Invalid sign attempt by ${player.name} at ${event.block.location}")
            }
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock
        if (block?.state is Sign) {
            val sign = block.state as Sign
            if (sign.getLine(0).equals(PERK_KEYWORD, ignoreCase = true)) {
                event.isCancelled = true
                perkSign.activatePerk(event.player, sign)
            }
        }
    }

    private fun isValidAutoSign(firstLine: String, secondLine: String): Boolean {
        return (firstLine.equals(GIVE_KEYWORD, ignoreCase = true) || firstLine.equals(TAKE_KEYWORD, ignoreCase = true)) &&
                secondLine.equals(FURNACE_KEYWORD, ignoreCase = true)
    }

    private fun handleAutoSign(event: SignChangeEvent) {
        val player = event.player
        val signBlock = event.block
        val signData = signBlock.blockData

        if (signData is WallSign) {
            val chestBlock = signBlock.getRelative(signData.facing.oppositeFace)

            if (chestBlock.state is Chest) {
                val furnaceDirection = plugin.findAdjacentFurnace(chestBlock)
                if (furnaceDirection != null) {
                    val isGiveSign = event.getLine(0).equals(GIVE_KEYWORD, ignoreCase = true)
                    val autoSign = AutoSignData(signBlock.location, isGiveSign, furnaceDirection)
                    plugin.addActiveAutoSign(autoSign)

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
    }

    private fun handlePerkSign(event: SignChangeEvent) {
        val player = event.player
        val perkType = event.getLine(1)
        val duration = event.getLine(2)?.split(" ")?.get(0)?.toLongOrNull()
        val cost = event.getLine(3)?.let {
            if (plugin.useExperienceEconomy) {
                it.removeSuffix(" XP").toDoubleOrNull()
            } else {
                it.removePrefix("$").toDoubleOrNull()
            }
        }

        if (perkType != null && duration != null && cost != null) {
            perkSign.createPerkSign(player, event.block.state as Sign, perkType, duration, cost)
        } else {
            player.sendMessage("Invalid perk sign format. Use: [Perk], <type>, <duration> seconds, ${if (plugin.useExperienceEconomy) "<cost> XP" else "$<cost>"}")
        }
    }
}