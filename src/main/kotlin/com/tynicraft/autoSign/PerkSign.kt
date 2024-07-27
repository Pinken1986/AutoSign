package com.tynicraft.autoSign

import org.bukkit.block.Sign
import org.bukkit.entity.Player

class PerkSign(private val plugin: AutoSign) {
    fun createPerkSign(player: Player, sign: Sign, perkType: String, duration: Long, cost: Double) {
        if (plugin.permissionManager.has(player, "autosign.create.perksign")) {
            sign.setLine(0, "[Perk]")
            sign.setLine(1, perkType)
            sign.setLine(2, "$duration seconds")
            sign.setLine(3, if (plugin.useExperienceEconomy) "${cost.toInt()} XP" else "$${cost}")
            sign.update()
            player.sendMessage("Perk sign created successfully!")
        } else {
            player.sendMessage("You don't have permission to create perk signs.")
        }
    }

    fun activatePerk(player: Player, sign: Sign) {
        val perkType = sign.getLine(1)
        val duration = sign.getLine(2).split(" ")[0].toLongOrNull() ?: return
        val costString = sign.getLine(3)
        val cost = if (plugin.useExperienceEconomy) {
            costString.removeSuffix(" XP").toDoubleOrNull() ?: return
        } else {
            costString.removePrefix("$").toDoubleOrNull() ?: return
        }

        if (!plugin.hasEnoughCurrency(player, cost)) {
            player.sendMessage("You don't have enough ${if (plugin.useExperienceEconomy) "experience" else "money"} to purchase this perk.")
            return
        }

        plugin.withdrawCurrency(player, cost)

        when (perkType.toLowerCase()) {
            "fly" -> plugin.perkManager.activateFly(player, duration)
            "god" -> plugin.perkManager.activateGod(player, duration)
            "weather" -> plugin.weatherManager.changeWeather(player)
            "time" -> plugin.timeManager.changeTime(player)
            else -> player.sendMessage("Unknown perk type.")
        }
    }
}