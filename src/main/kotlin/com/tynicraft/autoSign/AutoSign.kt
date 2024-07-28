package com.tynicraft.autoSign

import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin

class AutoSign : JavaPlugin() {
    lateinit var signManager: SignManager

    override fun onEnable() {
        saveDefaultConfig()
        signManager = SignManager(this)
        loadSigns()
        server.pluginManager.registerEvents(SignListener(this), this)

        signManager.startSignProcessingTask()

        logger.info("AutoSign has been enabled!")
    }

    override fun onDisable() {
        signManager.stopSignProcessingTask()
        saveSigns()
        logger.info("AutoSign has been disabled.")
    }

    private fun loadSigns() {
        val signsConfig = config.getConfigurationSection("signs") ?: return
        for (key in signsConfig.getKeys(false)) {
            val section = signsConfig.getConfigurationSection(key) ?: continue
            val world = server.getWorld(section.getString("world") ?: continue)
            val x = section.getInt("x")
            val y = section.getInt("y")
            val z = section.getInt("z")
            val isGiveSign = section.getBoolean("isGiveSign")
            val location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
            signManager.addSign(location, isGiveSign)
        }
    }

    private fun saveSigns() {
        config.set("signs", null)
        val signsConfig = config.createSection("signs")
        signManager.getActiveSigns().entries.forEachIndexed { index, (location, signData) ->
            val signSection = signsConfig.createSection(index.toString())
            signSection.set("world", location.world?.name)
            signSection.set("x", location.blockX)
            signSection.set("y", location.blockY)
            signSection.set("z", location.blockZ)
            signSection.set("isGiveSign", signData.isGiveSign)
        }
        saveConfig()
    }
}