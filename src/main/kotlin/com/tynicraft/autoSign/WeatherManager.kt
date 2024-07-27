package com.tynicraft.autoSign

import org.bukkit.entity.Player

class WeatherManager(private val plugin: AutoSign) {
    fun changeWeather(player: Player) {
        val world = player.world
        if (world.hasStorm()) {
            world.setStorm(false)
            world.isThundering = false
            player.sendMessage("Weather changed to clear!")
        } else {
            world.setStorm(true)
            world.isThundering = true
            player.sendMessage("Weather changed to stormy!")
        }
    }
}