package com.tynicraft.autoSign

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class AutoSign : JavaPlugin() {
    lateinit var signManager: SignManager

    companion object {
        const val DATAPACK_VERSION = 1 // Increment this when you make changes to the datapack
    }

    override fun onEnable() {
        saveDefaultConfig()
        signManager = SignManager(this)
        loadSigns()
        server.pluginManager.registerEvents(SignListener(this), this)

        signManager.startSignProcessingTask()

        updateDatapack()

        // Register command
        getCommand("createdatapack")?.setExecutor { sender, _, _, _ ->
            if (sender.hasPermission("autosign.createdatapack")) {
                updateDatapack()
                sender.sendMessage("AutoSign datapack updated successfully!")
            } else {
                sender.sendMessage("You don't have permission to use this command.")
            }
            true
        }

        // Add scoreboard objective for the datapack
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "scoreboard objectives add autosign dummy")

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

    private fun updateDatapack() {
        val worldContainer = Bukkit.getWorldContainer()
        val worldName = Bukkit.getWorlds()[0].name
        val datapackFolder = File(worldContainer, "$worldName/datapacks/AutoSign")

        val versionFile = File(datapackFolder, "version.txt")
        val currentVersion = if (versionFile.exists()) versionFile.readText().toIntOrNull() ?: 0 else 0

        if (currentVersion < DATAPACK_VERSION) {
            logger.info("Updating AutoSign datapack to version $DATAPACK_VERSION")
            datapackFolder.deleteRecursively()
            createDatapack(datapackFolder)
            versionFile.writeText(DATAPACK_VERSION.toString())
        } else {
            logger.info("AutoSign datapack is up to date (version $currentVersion)")
        }
    }

    private fun createDatapack(datapackFolder: File) {
        // Create datapack folder structure
        datapackFolder.mkdirs()
        File(datapackFolder, "data/autosign/functions").mkdirs()
        File(datapackFolder, "data/minecraft/tags/functions").mkdirs()

        // Create pack.mcmeta
        File(datapackFolder, "pack.mcmeta").writeText(
            """
            {
              "pack": {
                "pack_format": 10,
                "description": "AutoSign Datapack"
              }
            }
            """.trimIndent()
        )

        // Create tick.json
        File(datapackFolder, "data/minecraft/tags/functions/tick.json").writeText(
            """
            {
              "values": [
                "autosign:tick"
              ]
            }
            """.trimIndent()
        )

        // Create tick.mcfunction
        File(datapackFolder, "data/autosign/functions/tick.mcfunction").writeText(
            """
            # Run setup for new signs
            execute as @e[type=minecraft:item_frame,tag=!autosign_processed] at @s run function autosign:setup
            # Process give signs
            execute as @e[type=minecraft:item_frame,tag=autosign_give] at @s run function autosign:give_to_furnace
            # Process take signs
            execute as @e[type=minecraft:item_frame,tag=autosign_take] at @s run function autosign:take_from_furnace
            """.trimIndent()
        )

        // Create setup.mcfunction
        File(datapackFolder, "data/autosign/functions/setup.mcfunction").writeText(
            """
            # Check for give sign
            execute if block ~ ~ ~ #minecraft:wall_signs{Text1:'{"text":"<give>"}',Text2:'{"text":"<furnace>"}'} run tag @s add autosign_give
            # Check for take sign
            execute if block ~ ~ ~ #minecraft:wall_signs{Text1:'{"text":"<take>"}',Text2:'{"text":"<furnace>"}'} run tag @s add autosign_take
            # Mark as processed
            tag @s add autosign_processed
            # Notify player
            execute if entity @s[tag=autosign_give] run tellraw @a[distance=..5] {"text":"AutoSign (Give) created!", "color":"green"}
            execute if entity @s[tag=autosign_take] run tellraw @a[distance=..5] {"text":"AutoSign (Take) created!", "color":"green"}
            """.trimIndent()
        )

        // Create give_to_furnace.mcfunction
        File(datapackFolder, "data/autosign/functions/give_to_furnace.mcfunction").writeText(
            """
            # Check all adjacent blocks for chest and furnace
            function autosign:check_adjacent_blocks

            # Transfer fuel (example with coal)
            execute if score #chest_found autosign matches 1 if score #furnace_found autosign matches 1 run execute as @e[tag=autosign_chest,limit=1,sort=nearest] at @s if data block ~ ~ ~ {Items:[{id:"minecraft:coal"}]} as @e[tag=autosign_furnace,limit=1,sort=nearest] at @s if block ~ ~ ~ minecraft:furnace{Items:[{Slot:1b,id:"minecraft:air"}]} run item replace block ~ ~ ~ container.1 from entity @e[tag=autosign_chest,limit=1,sort=nearest] container.0
            execute if score #chest_found autosign matches 1 if score #furnace_found autosign matches 1 run execute as @e[tag=autosign_chest,limit=1,sort=nearest] at @s if data block ~ ~ ~ {Items:[{id:"minecraft:coal"}]} as @e[tag=autosign_furnace,limit=1,sort=nearest] at @s if block ~ ~ ~ minecraft:furnace{Items:[{Slot:1b,id:"minecraft:coal"}]} run item replace block ~ ~ ~ container.0 with minecraft:air

            # Transfer smeltable item (example with iron ore)
            execute if score #chest_found autosign matches 1 if score #furnace_found autosign matches 1 run execute as @e[tag=autosign_chest,limit=1,sort=nearest] at @s if data block ~ ~ ~ {Items:[{id:"minecraft:iron_ore"}]} as @e[tag=autosign_furnace,limit=1,sort=nearest] at @s if block ~ ~ ~ minecraft:furnace{Items:[{Slot:0b,id:"minecraft:air"}]} run item replace block ~ ~ ~ container.0 from entity @e[tag=autosign_chest,limit=1,sort=nearest] container.0
            execute if score #chest_found autosign matches 1 if score #furnace_found autosign matches 1 run execute as @e[tag=autosign_chest,limit=1,sort=nearest] at @s if data block ~ ~ ~ {Items:[{id:"minecraft:iron_ore"}]} as @e[tag=autosign_furnace,limit=1,sort=nearest] at @s if block ~ ~ ~ minecraft:furnace{Items:[{Slot:0b,id:"minecraft:iron_ore"}]} run item replace block ~ ~ ~ container.0 with minecraft:air

            # Clean up tags
            tag @e[tag=autosign_chest] remove autosign_chest
            tag @e[tag=autosign_furnace] remove autosign_furnace
            scoreboard players reset #chest_found autosign
            scoreboard players reset #furnace_found autosign
            """.trimIndent()
        )

        // Create take_from_furnace.mcfunction
        File(datapackFolder, "data/autosign/functions/take_from_furnace.mcfunction").writeText(
            """
            # Check all adjacent blocks for chest and furnace
            function autosign:check_adjacent_blocks

            # Transfer smelted item
            execute if score #chest_found autosign matches 1 if score #furnace_found autosign matches 1 run execute as @e[tag=autosign_furnace,limit=1,sort=nearest] at @s if block ~ ~ ~ minecraft:furnace{Items:[{Slot:2b}]} as @e[tag=autosign_chest,limit=1,sort=nearest] at @s run item replace block ~ ~ ~ container.0 from block ~ ~ ~ container.2
            execute if score #chest_found autosign matches 1 if score #furnace_found autosign matches 1 run execute as @e[tag=autosign_furnace,limit=1,sort=nearest] at @s if block ~ ~ ~ minecraft:furnace{Items:[{Slot:2b}]} run item replace block ~ ~ ~ container.2 with minecraft:air

            # Clean up tags
            tag @e[tag=autosign_chest] remove autosign_chest
            tag @e[tag=autosign_furnace] remove autosign_furnace
            scoreboard players reset #chest_found autosign
            scoreboard players reset #furnace_found autosign
            """.trimIndent()
        )

        // Create check_adjacent_blocks.mcfunction
        File(datapackFolder, "data/autosign/functions/check_adjacent_blocks.mcfunction").writeText(
            """
            # Reset scores
            scoreboard players set #chest_found autosign 0
            scoreboard players set #furnace_found autosign 0

            # Check for chest and furnace in all directions
            execute if block ~ ~-1 ~ #minecraft:chests run scoreboard players set #chest_found autosign 1
            execute if block ~ ~1 ~ #minecraft:chests run scoreboard players set #chest_found autosign 1
            execute if block ~1 ~ ~ #minecraft:chests run scoreboard players set #chest_found autosign 1
            execute if block ~-1 ~ ~ #minecraft:chests run scoreboard players set #chest_found autosign 1
            execute if block ~ ~ ~1 #minecraft:chests run scoreboard players set #chest_found autosign 1
            execute if block ~ ~ ~-1 #minecraft:chests run scoreboard players set #chest_found autosign 1

            execute if block ~ ~-1 ~ minecraft:furnace run scoreboard players set #furnace_found autosign 1
            execute if block ~ ~1 ~ minecraft:furnace run scoreboard players set #furnace_found autosign 1
            execute if block ~1 ~ ~ minecraft:furnace run scoreboard players set #furnace_found autosign 1
            execute if block ~-1 ~ ~ minecraft:furnace run scoreboard players set #furnace_found autosign 1
            execute if block ~ ~ ~1 minecraft:furnace run scoreboard players set #furnace_found autosign 1
            execute if block ~ ~ ~-1 minecraft:furnace run scoreboard players set #furnace_found autosign 1

            # Tag the chest and furnace entities for easier reference
            execute if score #chest_found autosign matches 1 run execute positioned ~ ~-1 ~ if block ~ ~ ~ #minecraft:chests run summon minecraft:marker ~ ~ ~ {Tags:["autosign_chest"]}
            execute if score #chest_found autosign matches 1 run execute positioned ~ ~1 ~ if block ~ ~ ~ #minecraft:chests run summon minecraft:marker ~ ~ ~ {Tags:["autosign_chest"]}
            execute if score #chest_found autosign matches 1 run execute positioned ~1 ~ ~ if block ~ ~ ~ #minecraft:chests run summon minecraft:marker ~ ~ ~ {Tags:["autosign_chest"]}
            execute if score #chest_found autosign matches 1 run execute positioned ~-1 ~ ~ if block ~ ~ ~ #minecraft:chests run summon minecraft:marker ~ ~ ~ {Tags:["autosign_chest"]}
            execute if score #chest_found autosign matches 1 run execute positioned ~ ~ ~1 if block ~ ~ ~ #minecraft:chests run summon minecraft:marker ~ ~ ~ {Tags:["autosign_chest"]}
            execute if score #chest_found autosign matches 1 run execute positioned ~ ~ ~-1 if block ~ ~ ~ #minecraft:chests run summon minecraft:marker ~ ~ ~ {Tags:["autosign_chest"]}

            execute if score #furnace_found autosign matches 1 run execute positioned ~ ~-1 ~ if block ~ ~ ~ minecraft:furnace run summon minecraft:marker ~ ~ ~ {Tags:["autosign_furnace"]}
            execute if score #furnace_found autosign matches 1 run execute positioned ~ ~1 ~ if block ~ ~ ~ minecraft:furnace run summon minecraft:marker ~ ~ ~ {Tags:["autosign_furnace"]}
            execute if score #furnace_found autosign matches 1 run execute positioned ~1 ~ ~ if block ~ ~ ~ minecraft:furnace run summon minecraft:marker ~ ~ ~ {Tags:["autosign_furnace"]}
            execute if score #furnace_found autosign matches 1 run execute positioned ~-1 ~ ~ if block ~ ~ ~ minecraft:furnace run summon minecraft:marker ~ ~ ~ {Tags:["autosign_furnace"]}
            execute if score #furnace_found autosign matches 1 run execute positioned ~ ~ ~1 if block ~ ~ ~ minecraft:furnace run summon minecraft:marker ~ ~ ~ {Tags:["autosign_furnace"]}
            execute if score #furnace_found autosign matches 1 run execute positioned ~ ~ ~-1 if block ~ ~ ~ minecraft:furnace run summon minecraft:marker ~ ~ ~ {Tags:["autosign_furnace"]}
            """.trimIndent()
        )

        // Reload datapacks
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "reload")

        logger.info("AutoSign datapack created and loaded!")
    }
}