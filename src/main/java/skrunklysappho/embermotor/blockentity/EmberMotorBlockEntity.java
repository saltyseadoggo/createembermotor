package skrunklysappho.embermotor.blockentity;

import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.event.EmberEvent;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.api.tile.IUpgradeable;
import com.rekindled.embers.api.upgrades.UpgradeContext;
import com.rekindled.embers.api.upgrades.UpgradeUtil;
import com.rekindled.embers.power.DefaultEmberCapability;
import com.rekindled.embers.util.sound.ISoundController;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import skrunklysappho.embermotor.CEMBlocks;
import skrunklysappho.embermotor.Config;
import skrunklysappho.embermotor.block.EmberMotorBlock;
import skrunklysappho.embermotor.sound.CEMSounds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

// ISoundController is Embers' helper for playing looping machine sounds
public class EmberMotorBlockEntity extends GeneratingKineticBlockEntity implements ISoundController, IUpgradeable {

    // Grab the motor's output speed, ember consumption and stress capacity values from the config
    // - Note that `speedWhilePowered` is only the *base* speed. Clockwork attenuators can reduce it,
    //   while catalytic plugs can increase it. These multipliers are handled in `lazyTick`
    public static final int speedWhilePowered = Config.outputSpeed;
    public static final double emberCost = Config.emberConsumption;
    public static final float stressCapacity = Config.stressCapacity;

    // Float containing the motor's current speed
    // - Server side, its value is decided by `lazyTick` and used to set the motor's speed
    // - Client side, its value is copied from the `Speed` NBT tag and used to determine if the motor should play its sound
    protected float speedCurrent;
    // Int containing the motor's *upcoming* speed as determined by the `lazyTick` method
    public float speedNew = 0;

    // Variables needed by `ISoundController`
    HashSet<Integer> soundsPlaying = new HashSet<>();
    public static final int SOUND_LOOP = 1;
    public static final int[] SOUND_IDS = new int[]{SOUND_LOOP};

    // Variables used in `lazyTick`
    boolean first = true;
    private List<UpgradeContext> upgrades;

    // Store the face which has the motor's shaft on it. This face cannot receive ember or upgrades
    public Direction shaftFace = this.getBlockState().getValue(EmberMotorBlock.FACING);

    // Whether attached catalytic plugs should consume gas. Set to true by `lazyTick` when the motor is active
    boolean consumeGas = false;

    // Constructor needed by GeneratingKineticBlockEntity
    public EmberMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // Set motor's ember capacity
        capability.setEmberCapacity(1000);
        // Set lazy tick rate so the motor only consumes ember & updates its speed once every second
        // - Lazy ticking functionality comes from Create's `SmartBlockEntity` class
        setLazyTickRate(20);
    }

    // Give motor the ember capability so it can store, receive and consume ember
    public IEmberCapability capability = new DefaultEmberCapability() {
        @Override
        public void onContentsChanged() {
            super.onContentsChanged();
            EmberMotorBlockEntity.this.setChanged();
        }
    };

    // More capability shit taken from Embers' mechanical pump
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        // The facing check prevents ember from being pushed into the motor through the front
        // (where the shaft is) by not giving that side the ember capability
        if (!this.remove && side != shaftFace && cap == EmbersCapabilities.EMBER_CAPABILITY) {
            return capability.getCapability(cap, side);
        }
        return super.getCapability(cap, side);
    }

    // More capability shit taken from Embers' mechanical pump
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        capability.invalidate();
    }

    // Set motor's generated spinny speed in the correct direction based on which way the motor block is facing
    @Override
    public float getGeneratedSpeed() {
        if (!CEMBlocks.EMBER_MOTOR.has(getBlockState()))
            return 0;
        return convertToDirection(speedCurrent, getBlockState().getValue(EmberMotorBlock.FACING));
    }

    // More kinetic speed shit from Create's creative motor
    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
            updateGeneratedRotation();
    }

    // Give the motor the stress capacity value designated in the config file
    public float calculateAddedStressCapacity() {
        float capacity = stressCapacity;
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    // Override NBT data writing method from GeneratingKineticBlockEntity to save motor's ember amount to NBT data
    // This is needed to prevent the motor from losing all of its ember upon exiting and reentering the world
    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        capability.writeToNBT(nbt);
    }

    // Override NBT data reading method from GeneratingKineticBlockEntity to set motor's ember amount from NBT data
    // This is needed to prevent the motor from losing all of its ember upon exiting and reentering the world
    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        capability.deserializeNBT(nbt);
        if (level != null && level.isClientSide) {
            // Set speedCurrent to whatever the motor's NBT data says its rotation speed actually is
            // - While speedCurrent exists on both the client and server, the code in lazyTick and
            //   updateGeneratedRotation that sets it only runs on the server, so the client's copy stays at zero
            // - This prevents shouldPlaySound from detecting when the motor is running to play the motor's sound,
            //   and the goggles tooltip from showing the correct SU value
            // - However, the server automatically stores the speed value in the motor's NBT data, so we can use that
            //   to update the client's copy of speedCurrent and make those things work correctly~
            speedCurrent = nbt.getFloat("Speed");
            // Now call `ISoundController`'s method to start/stop the looping sound if the motor started or stopped~
            // - This can't be done in lazyTick or the sound and motor will not start/stop in sync
            // - It also must only run on the server side or else relogging will cause the motor to lose all of its ember
            //   and its network to overstress with a negative stress value if it had machines attached
            EmberMotorBlockEntity.this.handleSound();
        }
    }

    // Every second, consume ember to make the motor spin only if there is enough ember to consume
    // - `lazyTick` method comes from Create's `SmartBlockEntity` class
    // - Code adapted from Create Crafts & Additions' electric motor
    public void lazyTick() {
        // If level is somehow null, bail now to avoid a crash
        if(level == null) return;
        // Only run the following code on the server side, as it involves setting the motor's data
        if(!level.isClientSide) {

            // Code block taken from Create Crafts & Additions' electric motor
            // Without it, the network's stress as seen on a stressometer or in `/data get block` will be NaN
            if (first) {
                speedCurrent = speedNew;
                updateGeneratedRotation();
                first = false;
            }

            // Store this specific motor's block entity so we can access it easier
            EmberMotorBlockEntity blockEntity = EmberMotorBlockEntity.this;
            // Get and store the current Embers upgrades attached to this motor
            upgrades = UpgradeUtil.getUpgrades(level, blockEntity.worldPosition, getUpgradeSlots());
            // Determine what the motor's ember cost and speed while powered will be after being modified by upgrades
            double emberCostAfterUpgrades = UpgradeUtil.getTotalEmberConsumption(blockEntity, emberCost, blockEntity.upgrades);
            float speedWhilePowered_AfterUpgrades = (float) (speedWhilePowered * UpgradeUtil.getTotalSpeedModifier(blockEntity, blockEntity.upgrades));

            // Check if motor has enough ember to meet its cost
            // Also check if a clockwork attenuator has set the motor's speed to zero
            if (blockEntity.capability.getEmber() >= emberCostAfterUpgrades && speedWhilePowered_AfterUpgrades != 0) {
                // Consume ember and set motor's speed
                blockEntity.capability.removeAmount(emberCostAfterUpgrades, true);
                speedNew = speedWhilePowered_AfterUpgrades;
                // If catalytic plug is present, tell it to consume gas
                consumeGas = true;
                // If mini boiler is present, tell it to boil water
                UpgradeUtil.throwEvent(blockEntity, new EmberEvent(blockEntity, EmberEvent.EnumType.CONSUME, emberCostAfterUpgrades), blockEntity.upgrades);
            } else {
                // Stop motor, and stop catalytic plug(s) from consuming gas
                speedNew = 0;
                consumeGas = false;
            }
            // Update the motor's speed to whatever we determined it should be, either `speedWhilePowered` or zero
            updateGeneratedRotation(speedNew);
        }
    }

    // Make attached catalytic plug upgrades consume gas while the motor is running
    // - Referencing Embers' machines like the mechanical pump, this must be handled every tick rather than every lazy tick
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && consumeGas) {
            EmberMotorBlockEntity blockEntity = EmberMotorBlockEntity.this;
            UpgradeUtil.doWork(blockEntity, blockEntity.upgrades);
        }
    }

    // Set the motor's current speed to the new speed value determined by `lazyTick`
    // - If we use `motorSpeedNew` in the `getGeneratedSpeed` method, it introduces a bug where the motor's stress capacity
    //   increases after leaving and reentering the world. Adding this middle step fixes the bug, though I don't know why
    // - Code adapted from Create Crafts & Additions' electric motor
    public void updateGeneratedRotation(float newSpeed) {
        speedCurrent = newSpeed;
        super.updateGeneratedRotation();
    }

    // Method needed to implement Embers' ISoundController for looping sounds.
    // Code adapted from Embers' field chart to play the motor's looping sound
    @Override
    public void playSound(int id) {
        switch (id) {
            case SOUND_LOOP:
                CEMSounds.playMachineSound(this, SOUND_LOOP, CEMSounds.MOTOR_HUM.get(), SoundSource.BLOCKS, true, 1.0f, 1.0f, worldPosition.getX() + 0.5f, worldPosition.getY() + 0.5f, worldPosition.getZ() + 0.5f);
                break;
        }
        soundsPlaying.add(id);
    }

    // Method needed to implement Embers' ISoundController for looping sounds.
    // Has no functionality specific to the ember motor. Code copied from Embers' field chart
    @Override
    public void stopSound(int id) {
        soundsPlaying.remove(id);
    }

    // Method needed to implement Embers' ISoundController for looping sounds.
    // Has no functionality specific to the ember motor. Code copied from Embers' field chart
    @Override
    public boolean isSoundPlaying(int id) {
        return soundsPlaying.contains(id);
    }

    // Method needed to implement Embers' ISoundController for looping sounds.
    // Has no functionality specific to the ember motor. Code copied from Embers' field chart
    @Override
    public int[] getSoundIDs() {
        return SOUND_IDS;
    }

    // Used by `handleSound` to determine when the looping sound should play. Here we say it should if the motor is spinning
    @Override
    public boolean shouldPlaySound(int id) {
        return speedCurrent != 0;
    }

    // Allow Embers upgrades to be attached to all sides except the one with the shaft
    @Override
    public boolean isSideUpgradeSlot(Direction face) {
        return face != shaftFace;
    }

    // Create an array containing every face that upgrades can be attached to
    public Direction[] getUpgradeSlots() {
        Direction[] allFaces = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
        List<Direction> upgradeSlotsList = new ArrayList<>();
        for (Direction face : allFaces) {
            if (face != shaftFace) {
                upgradeSlotsList.add(face);
            }
        }
        Direction[] upgradeSlots = new Direction[upgradeSlotsList.size()];
        upgradeSlotsList.toArray(upgradeSlots);
        return upgradeSlots;
    }
}
