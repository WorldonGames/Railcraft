
package mods.railcraft.common.util.thermal;

import mods.railcraft.api.electricity.IElectricMinecart;
import mods.railcraft.common.fluids.tanks.StandardTank;
import mods.railcraft.common.gui.widgets.IIndicatorController;
import mods.railcraft.common.gui.widgets.IndicatorController;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

public class ThermalElectricGenerator implements IElectricMinecart{

    public double cycleTime;
    public double currentItemCycleTime;
    protected boolean isRunning = false;
    protected byte runCycle;
    private final StandardTank tankFuel;
    private double heat = Thermal.TEMP_AMBIENT;
    private double maxHeat = Thermal.TEMP_MAX;
    private double efficiencyModifier = 1;
    protected int engineTPC = 30;
    private IFuelProvider fuelProvider; 
    private EntityMinecart cartProvider;
    private final ChargeHandler chargeHandler = new ChargeHandler(cartProvider, ChargeHandler.Type.SOURCE, 15000.0D);
    
    public ThermalElectricGenerator(StandardTank tankFuel, EntityMinecart cartProvider) {
        this.tankFuel = tankFuel;
        this.cartProvider = cartProvider;
    }

    public ThermalElectricGenerator setFuelProvider(IFuelProvider fuelProvider) {
        this.fuelProvider = fuelProvider;
        return this;
    }

	public ChargeHandler getChargeHandler() {
		return chargeHandler;
	}
	
	public int getEngineTPC() {
        return engineTPC;
    }
	
    public int TPCtoRPM(int TPC) {
    	return 1500-(TPC*20);
    }
    public int RPMtoTPC(int RPM) {
    	return 75-(RPM/20);
    }
	
    public ThermalElectricGenerator setEngineTPC(int ticks) {
        this.engineTPC = ticks;
        return this;
    }

    public ThermalElectricGenerator setEfficiencyModifier(double modifier) {
        this.efficiencyModifier = modifier;
        return this;
    }

    public ThermalElectricGenerator setMaxHeat(double maxHeat) {
        this.maxHeat = maxHeat;
        return this;
    }

    public double getMaxHeat() {
        return maxHeat;
    }

    public double getHeatStep() {
        if (fuelProvider != null)
            return fuelProvider.getHeatStep();
        return Thermal.TEMP_STEP;
    }

    public void reset() {
        heat = Thermal.TEMP_AMBIENT;
    }

    public double getHeat() {
        return heat;
    }

    public void setHeat(double heat) {
        this.heat = heat;
        if (this.heat < Thermal.TEMP_AMBIENT)
            this.heat = Thermal.TEMP_AMBIENT;
    }    
    
    public double getHeatLevel() {
        return heat / getMaxHeat();
    }
    
    public void revUp(int TPC) {
    	setEngineTPC(Math.max(getEngineTPC() - TPC, 10));
    }
    
    public void revDown(int TPC) {
    	setEngineTPC(Math.min(getEngineTPC() + TPC, 30));
    }
    
    public void increaseHeat() {
        double max = getMaxHeat() - 5;
        if (heat == max)
            return;
        double change = (Thermal.TEMP_MAX - heat) * (Thermal.TEMP_STEP/12.5);
        heat += change;
        heat = Math.min(heat, max);
    }

    public void reduceHeat() {
        if (heat == Thermal.TEMP_AMBIENT)
            return;
        double change = (heat - Thermal.TEMP_AMBIENT) * (Thermal.TEMP_STEP/12.5);
        heat -= change;
        heat = Math.max(heat, Thermal.TEMP_AMBIENT);
    }

    public boolean isRunning() {
        return isRunning;
    }

    /*public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }*/

    public boolean hasFuel() {
        return cycleTime > 0;
    }

    /*public int getRunProgressScaled(int i) {
        if (!isRunning())
            return 0;
        int scale = (int) ((cycleTime / currentItemCycleTime) * i);
        scale = Math.max(0, scale);
        scale = Math.min(i, scale);
        return scale;
    }*/

    private boolean addFuel() {
        if (fuelProvider == null)
            return false;
        double fuel = fuelProvider.getMoreFuel();
        if (fuel <= 0)
            return false;
        cycleTime += fuel;
        currentItemCycleTime = cycleTime;
        return true;
    }

    public double getFuelPerCycle() {
        double fuel_inefficiency = 1 + (100/getHeat()) - (100/Thermal.TEMP_MAX);
        double fuel = Thermal.FUEL_PER_ENGINE_CYCLE * fuel_inefficiency;
        return fuel;
    }

    public void tick() {
        runCycle++;
        if (runCycle >= getEngineTPC()) {
            runCycle = 0;
            double fuelNeeded = getFuelPerCycle();
            while (cycleTime < fuelNeeded) {
                boolean addedFuel = addFuel();
                if (!addedFuel)
                    break;
            }
            isRunning = cycleTime >= fuelNeeded;
            if (isRunning)
                cycleTime -= fuelNeeded;
            
            if(getHeat() > 50)
            	generateElectricity();
        }

        if (isRunning)
            increaseHeat();
        else
            reduceHeat();
    }

    public int generateElectricity() {
        if (!isRunning())
            return 0;
        
        double maxCapacity = chargeHandler.getCapacity();
        double charge = chargeHandler.getCharge();
        long chargeLevel = Math.round((charge/maxCapacity)*100);
        int actualTPC = getEngineTPC();
        
        if((maxCapacity - Thermal.RF_PER_CYCLE) >= charge)
        	chargeHandler.addCharge(Thermal.RF_PER_CYCLE);
        else
        	chargeHandler.addCharge(maxCapacity-charge);
        
        if(actualTPC <= 30) {
        	if(90 < chargeLevel)
        		revDown(3);
        	if(80 < chargeLevel && chargeLevel < 90)
        		revDown(2);
        	if(70 < chargeLevel && chargeLevel < 80)
        		revDown(1);
        }
        
        if(actualTPC >= 10) {
        	if(chargeLevel < 40)
        		revUp(3);
        	if(40 < chargeLevel && chargeLevel < 50)
        		revUp(2);
        	if(50 < chargeLevel && chargeLevel < 60)
        		revUp(1);
        }
        return 1;
    }

    public void writeToNBT(NBTTagCompound data) {
        data.setFloat("heat", (float) heat);
        data.setFloat("maxHeat", (float) maxHeat);
        data.setInteger("engineTPC", (int) engineTPC);
        data.setFloat("cycleTime", (float) cycleTime);
        data.setFloat("currentItemCycleTime", (float) currentItemCycleTime);
        chargeHandler.writeToNBT(data);
    }

    public void readFromNBT(NBTTagCompound data) {
        heat = data.getFloat("heat");
        maxHeat = data.getFloat("maxHeat");
        engineTPC = data.getInteger("engineTPC");
        cycleTime = data.getFloat("cycleTime");
        currentItemCycleTime = data.getFloat("currentItemCycleTime");
        chargeHandler.readFromNBT(data);
    }

    public final IIndicatorController HeatIndicator = new HeatIndicator();

    private class HeatIndicator extends IndicatorController {

        @Override
        protected void refreshToolTip() {
            tip.text = String.format("%.0f\u00B0C", getHeat());
        }

        @Override
        public int getScaledLevel(int size) {
            return (int) ((getHeat() - Thermal.TEMP_AMBIENT) * size / (getMaxHeat() - Thermal.TEMP_AMBIENT));
        }

    }
    
    public final IIndicatorController RPMIndicator = new RPMIndicator();

    private class RPMIndicator extends IndicatorController {

        @Override
        protected void refreshToolTip() {
        	float RPM = TPCtoRPM(getEngineTPC());
            tip.text = String.format("%.0f RPM", RPM);
        }

        @Override
        public int getScaledLevel(int size) {
            return (int) ((TPCtoRPM(getEngineTPC()) - 800) * size / (1400 - 800));
        }

    }
}
