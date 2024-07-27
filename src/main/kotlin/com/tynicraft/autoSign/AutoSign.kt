package com.tynicraft.autoSign

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Chest
import org.bukkit.block.Furnace
import org.bukkit.block.data.type.WallSign
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.FurnaceInventory
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class AutoSign : JavaPlugin() {

    val activeAutoSigns = mutableListOf<AutoSignData>()
    private lateinit var moveItemsTask: BukkitTask

    private lateinit var fuelItems: List<Material>
    private lateinit var smeltableOres: List<Material>
    private lateinit var outputItems: List<Material>

    override fun onEnable() {
        saveDefaultConfig()
        loadConfigValues()
        server.pluginManager.registerEvents(SignListener(this), this)
        getCommand("autosign-test")?.setExecutor(TestCommand(this))

        val updateInterval = config.getLong("update_interval", 20L)
        moveItemsTask = server.scheduler.runTaskTimer(this, Runnable { moveItems() }, 0L, updateInterval)

        logger.info("AutoSign has been enabled!")
        if (isDebugMode()) {
            logger.info("Debug mode is enabled")
        }
    }

    override fun onDisable() {
        moveItemsTask.cancel()
        logger.info("AutoSign has been disabled.")
    }

    private fun loadConfigValues() {
        fuelItems = config.getStringList("fuel_items").mapNotNull { getMaterialFromMinecraftId(it) }
        smeltableOres = config.getStringList("smeltable_ores").mapNotNull { getMaterialFromMinecraftId(it) }
        outputItems = config.getStringList("output_items").mapNotNull { getMaterialFromMinecraftId(it) }

        if (isDebugMode()) {
            logger.info("Loaded fuel items: ${fuelItems.joinToString()}")
            logger.info("Loaded smeltable ores: ${smeltableOres.joinToString()}")
            logger.info("Loaded output items: ${outputItems.joinToString()}")
        }
    }

    private fun getMaterialFromMinecraftId(id: String): Material? {
        val key = NamespacedKey.minecraft(id.removePrefix("minecraft:"))
        return Material.getMaterial(key.key.uppercase())
    }

    private fun isDebugMode(): Boolean = config.getBoolean("debug_mode", false)

    fun findAdjacentFurnace(block: Block): BlockFace? {
        val faces = listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
        for (face in faces) {
            val adjacentBlock = block.getRelative(face)
            if (adjacentBlock.type == Material.FURNACE) {
                return face
            }
        }
        return null
    }

    private fun moveItems() {
        if (isDebugMode()) logger.info("Starting moveItems() method")
        for (autoSign in activeAutoSigns) {
            if (isDebugMode()) logger.info("Processing AutoSign at ${autoSign.location}")
            val signBlock = autoSign.location.block
            val signData = signBlock.blockData

            if (signData is WallSign) {
                val chestBlock = signBlock.getRelative(signData.facing.oppositeFace)
                val furnaceBlock = chestBlock.getRelative(autoSign.direction)

                if (chestBlock.state is Chest && furnaceBlock.state is Furnace) {
                    val chest = chestBlock.state as Chest
                    val furnace = furnaceBlock.state as Furnace

                    if (autoSign.isGiveSign) {
                        if (isDebugMode()) logger.info("Processing give sign")
                        moveItemsToFurnace(chest, furnace)
                    } else {
                        if (isDebugMode()) logger.info("Processing take sign")
                        moveItemsFromFurnace(furnace, chest)
                    }
                } else {
                    if (isDebugMode()) logger.warning("Invalid chest or furnace block")
                }
            } else {
                if (isDebugMode()) logger.warning("Sign is not a WallSign")
            }
        }
        if (isDebugMode()) logger.info("Finished moveItems() method")
    }

    private fun moveItemsToFurnace(chest: Chest, furnace: Furnace) {
        val chestInventory = chest.inventory
        val furnaceInventory = furnace.inventory
        val maxItemsPerMove = config.getInt("max_items_per_move", 64)

        var itemsMoved = false

        // Move fuel to furnace
        for (fuelType in fuelItems) {
            if (moveSpecificItemToFurnace(chestInventory, furnaceInventory, fuelType, "fuel", maxItemsPerMove)) {
                itemsMoved = true
                break
            }
        }

        // Move ores to furnace
        for (oreType in smeltableOres) {
            if (moveSpecificItemToFurnace(chestInventory, furnaceInventory, oreType, "smelting", maxItemsPerMove)) {
                itemsMoved = true
                break
            }
        }

        if (itemsMoved) {
            chest.update()
            furnace.update()
            if (isDebugMode()) logger.info("Items moved to furnace and inventories updated")
        } else {
            if (isDebugMode()) logger.info("No items were moved to the furnace")
        }
    }

    private fun moveSpecificItemToFurnace(chestInventory: org.bukkit.inventory.Inventory, furnaceInventory: FurnaceInventory, itemType: Material, slot: String, maxItems: Int): Boolean {
        val furnaceSlot = when (slot) {
            "fuel" -> furnaceInventory.fuel
            "smelting" -> furnaceInventory.smelting
            else -> return false
        }

        for (item in chestInventory.contents) {
            if (item != null && item.type == itemType) {
                val amountToMove = minOf(item.amount, maxItems, 64 - (furnaceSlot?.amount ?: 64))
                if (amountToMove > 0) {
                    val itemToMove = ItemStack(item.type, amountToMove)
                    when (slot) {
                        "fuel" -> furnaceInventory.fuel = itemToMove
                        "smelting" -> furnaceInventory.smelting = itemToMove
                    }
                    item.amount -= amountToMove
                    if (item.amount <= 0) {
                        chestInventory.remove(item)
                    }
                    if (isDebugMode()) logger.info("Moved $amountToMove ${item.type} to furnace $slot")
                    return true
                }
            }
        }
        return false
    }

    private fun moveItemsFromFurnace(furnace: Furnace, chest: Chest) {
        val furnaceInventory = furnace.inventory
        val chestInventory = chest.inventory

        val result = furnaceInventory.result
        if (result != null && result.type != Material.AIR && isOutputItem(result.type)) {
            val amountToMove = minOf(result.amount, getAvailableSpace(chestInventory, result.type))
            if (amountToMove > 0) {
                val itemToMove = ItemStack(result.type, amountToMove)
                chestInventory.addItem(itemToMove)
                result.amount -= amountToMove
                if (result.amount <= 0) {
                    furnaceInventory.result = null
                }
                chest.update()
                furnace.update()
                if (isDebugMode()) logger.info("Moved $amountToMove ${result.type} from furnace to chest")
            } else {
                if (isDebugMode()) logger.info("No space in chest to move items from furnace")
            }
        } else {
            if (isDebugMode()) logger.info("No items to move from furnace to chest")
        }
    }

    private fun getAvailableSpace(inventory: org.bukkit.inventory.Inventory, material: Material): Int {
        var availableSpace = 0
        for (item in inventory.contents) {
            if (item == null) {
                availableSpace += material.maxStackSize
            } else if (item.type == material) {
                availableSpace += material.maxStackSize - item.amount
            }
        }
        return availableSpace
    }

    private fun isOutputItem(material: Material): Boolean = material in outputItems
}