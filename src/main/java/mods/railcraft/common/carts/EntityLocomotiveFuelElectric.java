/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.carts;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.railcraft.api.carts.IRefuelableCart;
import mods.railcraft.api.carts.locomotive.LocomotiveRenderType;
import mods.railcraft.api.fuel.FuelManager;
import mods.railcraft.common.core.RailcraftConfig;
import mods.railcraft.common.fluids.FluidHelper;
import mods.railcraft.common.fluids.FluidItemHelper;
import mods.railcraft.common.fluids.TankManager;
import mods.railcraft.common.fluids.tanks.BoilerFuelTank;
import mods.railcraft.common.gui.EnumGui;
import mods.railcraft.common.gui.GuiHandler;
import mods.railcraft.common.items.ItemTicket;
import mods.railcraft.common.util.inventory.InvTools;
import mods.railcraft.common.util.inventory.wrappers.InventoryMapper;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.sounds.SoundHelper;
import mods.railcraft.common.util.thermal.FluidFuelProvider;
import mods.railcraft.common.util.thermal.ThermalElectricGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntitySmokeFX;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
/**
 *
 * @author CovertJaguar <http://www.railcraft.info/>
 */
public class EntityLocomotiveFuelElectric extends EntityLocomotive implements ISidedInventory, IFluidHandler, IRefuelableCart {
	
    private static final int CHARGE_USE_PER_TICK = 20;
    private static final int CHARGE_PER_REQUEST = 1;
    private static final int CHARGE_USE_PER_REQUEST = CHARGE_USE_PER_TICK * CHARGE_PER_REQUEST;
    //private final ChargeHandler chargeHandler = new ChargeHandler(this, ChargeHandler.Type.USER, MAX_CHARGE);
    
    public static final int SLOT_LIQUID_INPUT = 0;
    public static final int SLOT_LIQUID_OUTPUT = 1;
    private static final int SLOT_TICKET = 2;
    private static final int SLOT_DESTINATION = 3;
    //TODO: SISTEMARE LIMITI ARRAY INVENTRAIO!
    private static final int[] SLOTS = InvTools.buildSlotArray(0, 3);
    private final IInventory invTicket = new InventoryMapper(this, SLOT_TICKET, 2, false);
    
    private static final int TANK_FUEL = 0;
    public static final byte WHITESMOKE_FLAG = 6;
    public static final byte BLACKSMOKE_FLAG = 7;
    
    public ThermalElectricGenerator generator;
    protected BoilerFuelTank tankFuel;
    private TankManager tankManager;
    protected InventoryMapper invFuelInput;
    protected IInventory invFuelOutput = new InventoryMapper(this, SLOT_LIQUID_OUTPUT, 1);
    
    private int update = rand.nextInt();
        
    public EntityLocomotiveFuelElectric(World world) {
        super(world);
    }

    public EntityLocomotiveFuelElectric(World world, double x, double y, double z) {
        super(world, x, y, z);
    }
    
    @Override
    protected void entityInit() {
        super.entityInit();
        tankManager = new TankManager();
        
        tankFuel = new BoilerFuelTank(FluidHelper.BUCKET_VOLUME * 64, null);
        
        tankManager.add(tankFuel);
        
        invFuelInput = new InventoryMapper(this, SLOT_LIQUID_INPUT, 1);
        invFuelInput.setStackSizeLimit(4);
        
        generator = new ThermalElectricGenerator(tankFuel, this);
        
        generator.setFuelProvider(new FluidFuelProvider(tankFuel) {
            @Override
            public double getMoreFuel() {
                if (isShutdown())
                    return 0;
                return super.getMoreFuel();
            }
        });
    }
    
    @Override
    protected ItemStack getCartItemBase() {
        return EnumCart.LOCO_FUELELECTRIC.getCartItem();
    }
    
    @Override
    protected void openGui(EntityPlayer player) {
        GuiHandler.openGui(EnumGui.LOCO_FUELELECTRIC, player, worldObj, this);
    }
    
    @Override
    public String getWhistle() {
        return SoundHelper.SOUND_LOCOMOTIVE_ELECTRIC_WHISTLE;
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();

        if (Game.isHost(worldObj)){
        	update++;
        	
        	//TODO:Controllare le condizioni reali di questo IF
            if (generator.getChargeHandler().getCapacity() >= generator.getChargeHandler().getCharge() || isShutdown()) {
                generator.tick();

                setBlackSmoking(generator.isRunning() && generator.getHeat() > 45 && generator.getHeat() < 55);
                setWhiteSmoking(generator.isRunning() && generator.getHeat() > 50);
            }
            
            if (update % FluidHelper.BUCKET_FILL_TIME == 0)
                FluidHelper.drainContainers(this, this, SLOT_LIQUID_INPUT, SLOT_LIQUID_OUTPUT);

        }else{
        	
            if (isBlackSmoking()){
            	doBlackSmoking();
            }
            
            if (isWhiteSmoking()){
            	doWhiteSmoking();
            }
        }
    }
    
    @Override
    protected IInventory getTicketInventory() {
        return invTicket;
    }
    
    @Override
    protected void applyDrag() {
        motionX *= getDrag();
        motionY *= 0.0D;
        motionZ *= getDrag();

        LocoSpeed speed = getSpeed();
        if (isRunning()) {
            float force = RailcraftConfig.locomotiveHorsepower() * 0.007F;
            switch (speed) {
                case REVERSE:
                    force = -force;
                    break;
            }
            double yaw = rotationYaw * Math.PI / 180D;
            motionX += Math.cos(yaw) * force;
            motionZ += Math.sin(yaw) * force;
        }

        if (speed != LocoSpeed.MAX) {
            float limit = 0.4f;
            switch (speed) {
                case SLOWEST:
                case REVERSE:
                    limit = 0.1f;
                    break;
                case SLOWER:
                    limit = 0.2f;
                    break;
                case SLOW:
                    limit = 0.3f;
                    break;
            }
            motionX = Math.copySign(Math.min(Math.abs(motionX), limit), motionX);
            motionZ = Math.copySign(Math.min(Math.abs(motionZ), limit), motionZ);
        }
    }
    
    @Override
    public int getMoreGoJuice() {

    	if (generator.getChargeHandler().getCharge() > CHARGE_USE_PER_REQUEST) {
    		generator.getChargeHandler().removeCharge(CHARGE_USE_PER_REQUEST);
	        return CHARGE_PER_REQUEST;
	    }
        return 0;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound data) {
        super.writeEntityToNBT(data);
        tankManager.writeTanksToNBT(data);
        generator.writeToNBT(data);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data);
    	tankManager.readTanksFromNBT(data);
    	generator.readFromNBT(data);
    }

    @Override
    public int getSizeInventory() {
        return 4;
    }
    
    @Override
    public float getOptimalDistance(EntityMinecart cart) {
        return 0.92f;
    }
    
    @Override
    public LocomotiveRenderType getRenderType() {
        return LocomotiveRenderType.FUELELECTRIC;
    }
        
    public boolean isBlackSmoking() {
        return getFlag(BLACKSMOKE_FLAG);
    } 
    private void setBlackSmoking(boolean smoke) {
        if (getFlag(BLACKSMOKE_FLAG) != smoke)
            setFlag(BLACKSMOKE_FLAG, smoke);
    }
    public boolean isWhiteSmoking() {
        return getFlag(WHITESMOKE_FLAG);
    } 
    private void setWhiteSmoking(boolean smoke) {
        if (getFlag(WHITESMOKE_FLAG) != smoke)
            setFlag(WHITESMOKE_FLAG, smoke);
    }
    
    @SideOnly(Side.CLIENT)
    private void doBlackSmoking() {
    	double rads = this.renderYaw * Math.PI / 180.0D;
    	float offset = 0.35F;
    	for (int i = 0; i < 5; i++) {
    		//worldObj.spawnParticle("largesmoke", posX - Math.cos(rads) * offset, posY + 0.7f, posZ -  Math.sin(rads) * offset, 0, 0.75F, 0);
        	EntitySmokeFX blacksmokefx = new EntitySmokeFX(this.worldObj, posX - Math.cos(rads) * offset, posY + 1.1f, posZ - Math.sin(rads) * offset, 0, 0.0075F, 0, 2F);
    		blacksmokefx.setAlphaF(0.66F);
    		Minecraft.getMinecraft().effectRenderer.addEffect(blacksmokefx);
        }
    }
    @SideOnly(Side.CLIENT)
    private void doWhiteSmoking() {
    	double rads = renderYaw * Math.PI / 180D;
    	float offset = 0.35F;
    	for (int i = 0; i < 3; i++) {
    		EntitySmokeFX whitesmokefx = new EntitySmokeFX(this.worldObj, posX - Math.cos(rads) * offset, posY + 1.1f, posZ - Math.sin(rads) * offset, 0, 0.001F, 0, 2F);
    		float color = (float) Math.min(Math.random() + 0.65F, 1);
    		whitesmokefx.setRBGColorF(color, color, color);
    		whitesmokefx.setAlphaF(0.165F);
    		Minecraft.getMinecraft().effectRenderer.addEffect(whitesmokefx);
    	}
    }

    @Override
    public ICartType getCartType() {
        return EnumCart.LOCO_FUELELECTRIC;
    }
    
    public TankManager getTankManager() {
        return tankManager;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int var1) {
        return SLOTS;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot == SLOT_TICKET;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        switch (slot) {
	        case SLOT_LIQUID_INPUT:
	            FluidStack fluidStack = FluidItemHelper.getFluidStackInContainer(stack);
	            if (fluidStack != null && fluidStack.amount > FluidHelper.BUCKET_VOLUME)
	                return false;
	            if (FuelManager.getBoilerFuelValue(FluidItemHelper.getFluidInContainer(stack)) > 0)
	            	return true;
	        case SLOT_TICKET:
	            return ItemTicket.FILTER.matches(stack);
	        default:
	            return false;
        }
    }
        
    @Override
    public boolean needsRefuel() {
        FluidStack fuel = tankFuel.getFluid();
        if (fuel == null || fuel.amount < tankFuel.getCapacity() / 3)
            return true;
        return false;
    }
    
    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return tankFuel.fill(resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        if (fluid == null) return false;
        if (FuelManager.getBoilerFuelValue(fluid) > 0) return true;
        return false;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection direction) {
        return tankManager.getTankInfo();
    }

}
