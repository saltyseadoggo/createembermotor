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
import skrunklysappho.embermotor.block.EmberMotorBlock;

public class EmberMotorBlockEntity extends GeneratingKineticBlockEntity {

    // Store the motor's current generated spinny speed in a variable. By default it's set to 0
    public int generatedSpeed = 0;
    // Set motor's generated speed when it is powered, in RPM
    public static int generatedSpeedWhilePowered = 32;
    // Set ember cost to run the motor per second
    // TODO: Make configurable
    public static final double EMBER_COST = 5.0;

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
        if (!this.remove && cap == EmbersCapabilities.EMBER_CAPABILITY) {
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
    // For some reason the given value gets multiplied by 32 in game, so we divide it by 32 here to compensate
    // TODO: Make configurable
    public float calculateAddedStressCapacity() {
        float capacity = 2048 / 32f;
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
        super.lazyTick();
        if(level.isClientSide()) return;

        // Only generate stress units if enough ember is present to consume
        if (EmberMotorBlockEntity.this.capability.getEmber() >= EMBER_COST) {
            // Consume ember
            EmberMotorBlockEntity.this.capability.removeAmount(EMBER_COST, true);
            // Set generatedSpeed to the configured value for when the motor is running
            generatedSpeed = generatedSpeedWhilePowered;
        }
        else {
            // When insufficient ember is present to run the motor, set generatedSpeed to 0
            generatedSpeed = 0;
        }
        updateGeneratedRotation();
    }
}
