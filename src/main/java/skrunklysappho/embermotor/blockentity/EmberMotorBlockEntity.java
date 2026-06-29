package skrunklysappho.embermotor.blockentity;

import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.power.DefaultEmberCapability;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import skrunklysappho.embermotor.CEMBlocks;
import skrunklysappho.embermotor.Config;
import skrunklysappho.embermotor.block.EmberMotorBlock;

public class EmberMotorBlockEntity extends GeneratingKineticBlockEntity {

    // Store the motor's current generated spinny speed in a variable. By default it's set to 0
    public int generatedSpeed = 0;
    // Set motor's generated speed when it is powered, in RPM
    public static final int outputSpeedWhilePowered = Config.outputSpeed;
    // Set ember cost to run the motor per second
    public static final double emberConsumption = Config.emberConsumption;
    // Set stress capacity of the motor
    public static final float stressCapacity = Config.stressCapacity;

    // Constructor needed by GeneratingKineticBlockEntity
    public EmberMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // Set motor's ember capacity
        capability.setEmberCapacity(1000);
        // Set lazy tick rate so the motor only consumes ember & updates its speed once every second
        // Lazy ticking functionality comes from Create's `SmartBlockEntity` class
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
        return convertToDirection(generatedSpeed, getBlockState().getValue(EmberMotorBlock.FACING));
    }

    // More kinetic speed shit from Create's creative motor
    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
            updateGeneratedRotation();
    }

    // Give the motor stress capacity. The value I chose is equal to the stress capacity of four large water wheels
    // For some reason the given value gets multiplied by the motor's speed, so we divide it by the speed to compensate
    // TODO: Make configurable
    public float calculateAddedStressCapacity() {
        float capacity = stressCapacity / outputSpeedWhilePowered;
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
    }

    // Every second, check if there's enough ember to consume and if so, spin the motor
    // `lazyTick` method comes from Create's `SmartBlockEntity` class
    public void lazyTick() {
        // Call `lazyTick` method in the class we extend, just in case
        super.lazyTick();

        // Only generate stress units if enough ember is present to consume
        if (EmberMotorBlockEntity.this.capability.getEmber() >= emberConsumption) {
            // Consume ember (only on the server side. I'm guessing trying to do this on the client side
            // would cause a crash when playing on a server, but I can't test that rn)
            if (!level.isClientSide) EmberMotorBlockEntity.this.capability.removeAmount(emberConsumption, true);
            // Set generatedSpeed to the configured value for when the motor is running
            generatedSpeed = outputSpeedWhilePowered;
        }
        else {
            // When insufficient ember is present to run the motor, set generatedSpeed to 0
            generatedSpeed = 0;
        }
        // This needs to run on the client for the goggles tooltip to show the correct SU generation for the motor
        updateGeneratedRotation();
    }
}
