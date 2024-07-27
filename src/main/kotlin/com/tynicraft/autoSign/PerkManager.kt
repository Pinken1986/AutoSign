package com.tynicraft.autoSign

import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class PerkManager(private val plugin: AutoSign) {
    private val activeFlyPerks = mutableMapOf<Player, Long>()
    private val activeGodPerks = mutableMapOf<Player, Long>()
    private val activeSafetyNets = mutableMapOf<Player, BukkitTask>()
    private lateinit var perkCheckerTask: BukkitTask

    fun activateFly(player: Player, duration: Long) {
        player.allowFlight = true
        player.isFlying = true
        activeFlyPerks[player] = System.currentTimeMillis() + (duration * 1000)
        player.sendMessage("Fly mode activated for $duration seconds!")
    }

    fun activateGod(player: Player, duration: Long) {
        player.isInvulnerable = true
        activeGodPerks[player] = System.currentTimeMillis() + (duration * 1000)
        player.sendMessage("God mode activated for $duration seconds!")
    }

    fun startPerkCheckerTask() {
        perkCheckerTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            checkPerks()
        }, 20L, 20L) // Run every second
    }

    fun stopPerkCheckerTask() {
        if (::perkCheckerTask.isInitialized) {
            perkCheckerTask.cancel()
        }
        // Cancel all active safety nets
        activeSafetyNets.values.forEach { it.cancel() }
        activeSafetyNets.clear()
    }

    private fun checkPerks() {
        val currentTime = System.currentTimeMillis()

        activeFlyPerks.entries.removeIf { (player, endTime) ->
            if (currentTime >= endTime) {
                deactivateFly(player)
                true
            } else false
        }

        activeGodPerks.entries.removeIf { (player, endTime) ->
            if (currentTime >= endTime) {
                deactivateGod(player)
                true
            } else false
        }
    }

    private fun deactivateFly(player: Player) {
        player.allowFlight = false
        player.isFlying = false
        player.sendMessage("Your fly perk has expired.")

        if (plugin.config.getBoolean("perk_system.fly.safety_net.enabled", true)) {
            applySafetyNet(player)
        }
    }

    private fun deactivateGod(player: Player) {
        player.isInvulnerable = false
        player.sendMessage("Your god mode perk has expired.")
    }

    private fun applySafetyNet(player: Player) {
        val slowFallingLevel = plugin.config.getInt("perk_system.fly.safety_net.slow_falling_level", 1)
        val maxDuration = plugin.config.getInt("perk_system.fly.safety_net.max_duration", 30)

        player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, 20 * maxDuration, slowFallingLevel - 1))
        player.sendMessage("Safety net activated! You have Slow Falling for $maxDuration seconds.")

        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (player.isOnGround()) {  // Changed to method call
                removeSafetyNet(player)
            }
        }, 20L, 20L) // Check every second

        activeSafetyNets[player] = task
    }

    private fun removeSafetyNet(player: Player) {
        player.removePotionEffect(PotionEffectType.SLOW_FALLING)
        activeSafetyNets[player]?.cancel()
        activeSafetyNets.remove(player)
        player.sendMessage("Safety net deactivated. You're now on the ground.")
    }

    @Suppress("unused")
    fun hasFlyPerk(player: Player): Boolean {
        return activeFlyPerks.containsKey(player)
    }

    @Suppress("unused")
    fun hasGodPerk(player: Player): Boolean {
        return activeGodPerks.containsKey(player)
    }

    @Suppress("unused")
    fun getRemainingFlyTime(player: Player): Long {
        val endTime = activeFlyPerks[player] ?: return 0
        return (endTime - System.currentTimeMillis()) / 1000
    }

    @Suppress("unused")
    fun getRemainingGodTime(player: Player): Long {
        val endTime = activeGodPerks[player] ?: return 0
        return (endTime - System.currentTimeMillis()) / 1000
    }
}