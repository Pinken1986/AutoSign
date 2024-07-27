package com.tynicraft.autoSign

import org.bukkit.entity.Player

class TimeManager(private val plugin: AutoSign) {
    fun changeTime(player: Player) {
        val world = player.world
        if (world.time < 12000) {
            world.time = 13000 // Night time
            player.sendMessage("Time set to night!")
        } else {
            world.time = 1000 // Day time
            player.sendMessage("Time set to day!")
        }
    }
}