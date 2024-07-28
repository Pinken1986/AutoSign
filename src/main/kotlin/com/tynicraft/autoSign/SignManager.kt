package com.tynicraft.autoSign

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.block.Furnace
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask

class SignManager(private val plugin: AutoSign) {
    private val activeSigns = mutableMapOf<Location, SignData>()
    private var processingTask: BukkitTask? = null

    fun addSign(location: Location, isGiveSign: Boolean) {
        activeSigns[location] = SignData(isGiveSign)
    }

    fun removeSign(location: Location) {
        activeSigns.remove(location)
    }

    fun hasActiveSign(location: Location): Boolean {
        return activeSigns.containsKey(location)
    }

    fun getActiveSigns(): Map<Location, SignData> = activeSigns

    fun startSignProcessingTask() {
        processingTask = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            processAllSigns()
        }, 0L, 20L) // Run every second
    }

    fun stopSignProcessingTask() {
        processingTask?.cancel()
    }

    private fun processAllSigns() {
        for ((location, signData) in activeSigns) {
            processSign(location, signData)
        }
    }

    private fun processSign(location: Location, signData: SignData) {
        val signBlock = location.block
        val chestBlock = signBlock.getRelative(signData.chestDirection)
        val furnaceBlock = chestBlock.getRelative(signData.furnaceDirection)

        if (chestBlock.state is Chest && furnaceBlock.state is Furnace) {
            val chest = chestBlock.state as Chest
            val furnace = furnaceBlock.state as Furnace

            if (signData.isGiveSign) {
                moveItemsToFurnace(chest, furnace)
            } else {
                moveItemsFromFurnace(furnace, chest)
            }
        }
    }

    private fun moveItemsToFurnace(chest: Chest, furnace: Furnace) {
        val chestInventory = chest.inventory
        val furnaceInventory = furnace.inventory

        // Move fuel
        for (item in chestInventory.contents) {
            if (item != null && isFuel(item)) {
                val fuelSlot = furnaceInventory.getFuel()
                if (fuelSlot == null || fuelSlot.type == Material.AIR) {
                    furnaceInventory.fuel = item
                    chestInventory.remove(item)
                    break
                }
            }
        }

        // Move smeltable items
        for (item in chestInventory.contents) {
            if (item != null && isSmeltable(item)) {
                val smeltSlot = furnaceInventory.getSmelting()
                if (smeltSlot == null || smeltSlot.type == Material.AIR) {
                    furnaceInventory.smelting = item
                    chestInventory.remove(item)
                    break
                }
            }
        }
    }

    private fun moveItemsFromFurnace(furnace: Furnace, chest: Chest) {
        val furnaceInventory = furnace.inventory
        val chestInventory = chest.inventory

        val result = furnaceInventory.result
        if (result != null && result.type != Material.AIR) {
            val leftover = chestInventory.addItem(result)
            if (leftover.isEmpty()) {
                furnaceInventory.result = null
            } else {
                furnaceInventory.result = leftover[0]
            }
        }
    }

    private fun isFuel(item: ItemStack): Boolean {
        // Implement logic to check if the item is a valid fuel
        return item.type == Material.COAL || item.type == Material.CHARCOAL
    }

    private fun isSmeltable(item: ItemStack): Boolean {
        // Implement logic to check if the item is smeltable
        return item.type == Material.IRON_ORE || item.type == Material.GOLD_ORE
    }

    data class SignData(
        val isGiveSign: Boolean,
        val chestDirection: org.bukkit.block.BlockFace = org.bukkit.block.BlockFace.NORTH,
        val furnaceDirection: org.bukkit.block.BlockFace = org.bukkit.block.BlockFace.EAST
    )
}