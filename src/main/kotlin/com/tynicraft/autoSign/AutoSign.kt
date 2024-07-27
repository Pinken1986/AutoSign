package com.tynicraft.autoSign

import org.bukkit.plugin.java.JavaPlugin
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.permission.Permission
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.Material

class AutoSign : JavaPlugin() {
    lateinit var permissionManager: Permission
    lateinit var perkManager: PerkManager
    lateinit var weatherManager: WeatherManager
    lateinit var timeManager: TimeManager
    private var economyManager: Economy? = null
    var useExperienceEconomy: Boolean = false

    private val _activeAutoSigns = mutableListOf<AutoSignData>()
    val activeAutoSigns: List<AutoSignData> get() = _activeAutoSigns.toList()

    override fun onEnable() {
        saveDefaultConfig()
        loadConfigValues()
        setupVault()

        perkManager = PerkManager(this)
        weatherManager = WeatherManager(this)
        timeManager = TimeManager(this)

        server.pluginManager.registerEvents(SignListener(this), this)
        getCommand("autosign-test")?.setExecutor(TestCommand(this))

        // Start the perk manager task
        perkManager.startPerkCheckerTask()

        logger.info("AutoSign has been enabled!")
        if (useExperienceEconomy) {
            logger.info("Using experience-based economy as a fallback.")
        }
    }

    override fun onDisable() {
        perkManager.stopPerkCheckerTask()
        logger.info("AutoSign has been disabled.")
    }

    private fun setupVault() {
        if (!setupPermissions()) {
            logger.severe("Failed to setup Vault permissions. Disabling plugin.")
            server.pluginManager.disablePlugin(this)
            return
        }

        if (!setupEconomy()) {
            logger.warning("No economy plugin found. Using experience-based economy as fallback.")
            useExperienceEconomy = true
        }
    }

    private fun setupPermissions(): Boolean {
        val rsp = server.servicesManager.getRegistration(Permission::class.java)
        if (rsp != null) {
            permissionManager = rsp.provider
            return true
        }
        return false
    }

    private fun setupEconomy(): Boolean {
        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        if (rsp != null) {
            economyManager = rsp.provider
            return true
        }
        return false
    }

    fun hasEnoughCurrency(player: org.bukkit.entity.Player, amount: Double): Boolean {
        return if (useExperienceEconomy) {
            val expRequired = amount.toInt()
            player.totalExperience >= expRequired
        } else {
            economyManager?.has(player, amount) ?: false
        }
    }

    fun withdrawCurrency(player: org.bukkit.entity.Player, amount: Double) {
        if (useExperienceEconomy) {
            val expRequired = amount.toInt()
            player.totalExperience -= expRequired
        } else {
            economyManager?.withdrawPlayer(player, amount)
        }
    }

    private fun loadConfigValues() {
        // Load configuration values here
    }

    fun isDebugMode(): Boolean {
        return config.getBoolean("debug_mode", false)
    }

    fun findAdjacentFurnace(block: Block): BlockFace? {
        for (face in BlockFace.values()) {
            if (block.getRelative(face).type == Material.FURNACE) {
                return face
            }
        }
        return null
    }

    fun addActiveAutoSign(autoSign: AutoSignData) {
        _activeAutoSigns.add(autoSign)
    }
}