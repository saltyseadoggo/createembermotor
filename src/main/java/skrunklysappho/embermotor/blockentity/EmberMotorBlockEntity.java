package skrunklysappho.embermotor.blockentity;

import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
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

import java.util.HashSet;

// ISoundController is Embers' helper for playing looping machine sounds
public class EmberMotorBlockEntity extends GeneratingKineticBlockEntity implements ISoundController {

    // Grab the motor's output speed, ember consumption and stress capacity values from the config
    public static final int speedWhilePowered = Config.outputSpeed;
    public static final double emberCost = Config.emberConsumption;
    public static final float stressCapacity = Config.stressCapacity;

    // Float containing the motor's current speed
    protected float speedCurrent;
    // Int containing the motor's *upcoming* speed as determined by the `lazyTick` method
    public int speedNew = 0;

    // Variables needed by `ISoundController`
    HashSet<Integer> soundsPlaying = new HashSet<>();
    public static final int SOUND_LOOP = 1;
    public static final int[] SOUND_IDS = new int[]{SOUND_LOOP};

    // Variable used in `lazyTick`
    boolean first = true;

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
        if (!this.remove && side != this.getBlockState().getValue(EmberMotorBlock.FACING) && cap == EmbersCapabilities.EMBER_CAPABILITY) {
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
        nbt.putFloat("Speed", speedCurrent);
    }

    // Override NBT data reading method from GeneratingKineticBlockEntity to set motor's ember amount from NBT data
    // This is needed to prevent the motor from losing all of its ember upon exiting and reentering the world
    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        capability.deserializeNBT(nbt);
        // Set speedCurrent to whatever the motor's NBT data says its rotation speed actually is
        // - While speedCurrent exists on both the client and server, the code in lazyTick and updateGeneratedRotation
        //   that sets it only runs on the server, so the client's copy stays at zero
        // - This prevents shouldPlaySound from detecting when the motor is running to play the motor's sound,
        //   and the goggles tooltip from showing the correct SU value
        // - However, the server automatically stores the speed value in the motor's NBT data, so we can use that
        //   to update the client's copy of speedCurrent and make those things work correctly~
        speedCurrent = nbt.getFloat("Speed");
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

            // If the motor has enough ember, consume some to run the motor. If not, stop the motor
            if (EmberMotorBlockEntity.this.capability.getEmber() >= emberCost) {
                if (!level.isClientSide) EmberMotorBlockEntity.this.capability.removeAmount(emberCost, true);
                speedNew = speedWhilePowered;
            } else {
                speedNew = 0;
            }
            // Update the motor's speed to whatever we determined it should be, either `speedWhilePowered` or zero
            updateGeneratedRotation(speedNew);
        }

        // Now on the client side, call `ISoundController`'s method to check if the motor started or stopped
        // and start/stop its looping sound accordingly
        // - Code adapted from Embers' hearth coil, which does all sound processing only on the client
        else {
            EmberMotorBlockEntity.this.handleSound();
        }
    }

    // Set the motor's current speed to the new speed value determined by `lazyTick`
    // - If we use `motorSpeedNew` in the `getGeneratedSpeed` method, it introduces a bug where the motor's stress capacity
    //   increases after leaving and reentering the world. Adding this middle step fixes the bug, though I don't know why
    // - Code adapted from Create Crafts & Additions' electric motor
    public void updateGeneratedRotation(int newSpeed) {
        speedCurrent = newSpeed;
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
}
