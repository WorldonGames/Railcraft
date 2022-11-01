
package mods.railcraft.common.gui.containers;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.railcraft.api.electricity.IElectricMinecart;
import mods.railcraft.common.carts.EntityLocomotiveFuelElectric;
import mods.railcraft.common.fluids.TankManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;
import mods.railcraft.common.gui.widgets.IndicatorWidget;
import mods.railcraft.common.gui.slots.SlotFluidContainerFilled;
import mods.railcraft.common.gui.slots.SlotOutput;
import mods.railcraft.common.gui.widgets.ChargeIndicator;
import mods.railcraft.common.gui.widgets.FluidGaugeWidget;

public class ContainerLocomotiveFuelElectric extends ContainerLocomotive {

    private final EntityLocomotiveFuelElectric loco;
    private final IElectricMinecart.ChargeHandler chargeHandler;
    private final ChargeIndicator chargeIndicator;
    private double lastCharge;
    private double lastHeat;
    private double lastRPM = 0;

    private ContainerLocomotiveFuelElectric(InventoryPlayer playerInv, EntityLocomotiveFuelElectric loco) {
        super(playerInv, loco, 205);
        this.loco = loco;
        this.chargeHandler = loco.generator.getChargeHandler();
        this.chargeIndicator = new ChargeIndicator(loco.generator.getChargeHandler().getCapacity());
    }

    public static ContainerLocomotiveFuelElectric make(InventoryPlayer playerInv, EntityLocomotiveFuelElectric loco) {
        ContainerLocomotiveFuelElectric con = new ContainerLocomotiveFuelElectric(playerInv, loco);
        con.init();
        return con;
    }
    
    private int getEngineRPM() {
		int engineRPM = 0;
		double heat = loco.generator.getHeat();
		
    	if(loco.generator.isRunning() && heat >= 35){
    		if(heat < 50) {
    			engineRPM = (int) Math.round(Math.pow(2*heat - 70, 2));
    		}else{
    			engineRPM = loco.generator.TPCtoRPM(loco.generator.getEngineTPC());
    		}
    	}
    	
    	return engineRPM;
    }
    
    @Override
    public void defineSlotsAndWidgets() {
        addWidget(new FluidGaugeWidget(loco.getTankManager().get(0), 116, 23, 176, 0, 16, 47));
        
        addWidget(new IndicatorWidget(chargeIndicator, 17, 19, 176, 104, 16, 53, true));
        addWidget(new IndicatorWidget(loco.generator.RPMIndicator, 40, 25, 176, 61, 6, 43));
        addWidget(new IndicatorWidget(loco.generator.HeatIndicator, 49, 25, 183, 61, 6, 43));
        
        addSlot(new SlotFluidContainerFilled(loco, 0, 143, 21));
        addSlot(new SlotOutput(loco, 1, 143, 56));
    }

    @Override
    public void addCraftingToCrafters(ICrafting icrafting) {
        super.addCraftingToCrafters(icrafting);
        TankManager tMan = loco.getTankManager();
        if (tMan != null) {
            tMan.initGuiData(this, icrafting, 0);
        }
        icrafting.sendProgressBarUpdate(this, 20, (int) Math.round(chargeHandler.getCharge()));
        icrafting.sendProgressBarUpdate(this, 22, (int) Math.round(loco.generator.getHeat()));
        icrafting.sendProgressBarUpdate(this, 23, (int) getEngineRPM());
    }

    @Override
    public void sendUpdateToClient() {
        super.sendUpdateToClient();
        TankManager tMan = loco.getTankManager();
        if (tMan != null) {
            tMan.updateGuiData(this, crafters, 0);
        }
        
        int engineRPM = getEngineRPM();
        double engineHeat = loco.generator.getHeat();
        
        for (int var1 = 0; var1 < this.crafters.size(); ++var1) {
            ICrafting var2 = (ICrafting) this.crafters.get(var1);

            if (this.lastCharge != chargeHandler.getCharge())
                var2.sendProgressBarUpdate(this, 21, (int) Math.round(chargeHandler.getCharge()));
            
            if (this.lastHeat != engineHeat)
                var2.sendProgressBarUpdate(this, 22, (int) Math.round(engineHeat));
            
            if (this.lastRPM != engineRPM)
            	var2.sendProgressBarUpdate(this, 23, engineRPM);
        }
        
        this.lastHeat = engineHeat;
        this.lastRPM = engineRPM;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        super.updateProgressBar(id, value);
        TankManager tMan = loco.getTankManager();
        if (tMan != null)
            tMan.processGuiUpdate(id, value);

        switch (id) {
            case 20:
                chargeIndicator.setCharge(value);
                break;
            case 21:
                chargeIndicator.updateCharge(value);
                break;
            case 22:
                loco.generator.setHeat(value);
                break;
            case 23:
                loco.generator.setEngineTPC(loco.generator.RPMtoTPC(value));
                break;
        }
    }

}
