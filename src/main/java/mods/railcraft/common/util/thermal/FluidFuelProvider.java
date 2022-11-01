package mods.railcraft.common.util.thermal;

import mods.railcraft.api.fuel.FuelManager;
import mods.railcraft.common.fluids.FluidHelper;
import mods.railcraft.common.fluids.tanks.StandardTank;
import net.minecraftforge.fluids.FluidStack;

public class FluidFuelProvider implements IFuelProvider {

    private final StandardTank fuelTank;

    public FluidFuelProvider(StandardTank fuelTank) {
        this.fuelTank = fuelTank;
    }

    @Override
    public double getHeatStep() {
        return Thermal.TEMP_STEP;
    }

    @Override
    /*public double getMoreFuel() {
        FluidStack fuel = fuelTank.drain(FluidHelper.BUCKET_VOLUME, false);
        if (fuel == null)
            return 0;

        double heatValue = FuelManager.getBoilerFuelValue(fuel.getFluid());
        if (heatValue > 0) {
            fuelTank.drain(FluidHelper.BUCKET_VOLUME, true);
            if (fuel.amount < FluidHelper.BUCKET_VOLUME)
                heatValue *= (double) fuel.amount / (double) FluidHelper.BUCKET_VOLUME;
        }
        return heatValue;
    }*/
    public double getMoreFuel() {
        FluidStack fuel = fuelTank.drain(1, false);
        if (fuel == null)
            return 0;

        double heatValue = FuelManager.getBoilerFuelValue(fuel.getFluid());
        if (heatValue > 0) {
            fuelTank.drain(1, true);
            heatValue *= (double) fuel.amount / (double) FluidHelper.BUCKET_VOLUME;
        }
        return heatValue;
    }

}
