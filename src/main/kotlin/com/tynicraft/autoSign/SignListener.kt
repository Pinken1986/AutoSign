package com.tynicraft.autoSign

import org.bukkit.block.Sign
import org.bukkit.block.data.type.WallSign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.SignChangeEvent

class SignListener(private val plugin: AutoSign) : Listener {

    companion object {
        private const val GIVE_KEYWORD = "<give>"
        private const val TAKE_KEYWORD = "<take>"
        private const val FURNACE_KEYWORD = "<furnace>"
    }

    @EventHandler
    fun onSignChange(event: SignChangeEvent) {
        val player = event.player
        val firstLine = event.getLine(0)
        val secondLine = event.getLine(1)

        if (firstLine == null || secondLine == null) return

        if (isValidAutoSign(firstLine, secondLine)) {
            val signBlock = event.block
            val signData = signBlock.blockData

            if (signData is WallSign) {
                val isGiveSign = firstLine.equals(GIVE_KEYWORD, ignoreCase = true)
                plugin.signManager.addSign(signBlock.location, isGiveSign)
                player.sendMessage("AutoSign created: ${if (isGiveSign) "Giving" else "Taking"} items ${if (isGiveSign) "to" else "from"} furnace")
            } else {
                player.sendMessage("The sign must be attached to a block.")
            }
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        if (block.state is Sign) {
            val sign = block.state as Sign
            if (isValidAutoSign(sign.getLine(0), sign.getLine(1))) {
                plugin.signManager.removeSign(block.location)
                event.player.sendMessage("AutoSign removed.")
            }
        }
    }

    private fun isValidAutoSign(firstLine: String, secondLine: String): Boolean {
        return (firstLine.equals(GIVE_KEYWORD, ignoreCase = true) || firstLine.equals(TAKE_KEYWORD, ignoreCase = true)) &&
                secondLine.equals(FURNACE_KEYWORD, ignoreCase = true)
    }
}