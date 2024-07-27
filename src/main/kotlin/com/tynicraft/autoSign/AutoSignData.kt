package com.tynicraft.autoSign

import org.bukkit.Location
import org.bukkit.block.BlockFace

data class AutoSignData(
    val location: Location,
    val isGiveSign: Boolean,
    val direction: BlockFace
)