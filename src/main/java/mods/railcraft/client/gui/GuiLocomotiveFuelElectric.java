/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.client.gui;

import mods.railcraft.common.carts.EntityLocomotiveFuelElectric;
import net.minecraft.entity.player.InventoryPlayer;
import mods.railcraft.common.gui.containers.ContainerLocomotiveFuelElectric;
import net.minecraft.entity.player.EntityPlayer;

public class GuiLocomotiveFuelElectric extends GuiLocomotive {

    private final EntityLocomotiveFuelElectric loco;
    private final EntityPlayer player;

    public GuiLocomotiveFuelElectric(InventoryPlayer inv, EntityLocomotiveFuelElectric loco) {
        super(inv, loco, ContainerLocomotiveFuelElectric.make(inv, loco), "fuelelectric", "gui_locomotive_fuelelectric.png", 205, true);
        this.loco = loco;
        this.player = inv.player;
    }
}
