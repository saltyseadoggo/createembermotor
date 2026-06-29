package skrunklysappho.embermotor.blockentity;

import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.power.DefaultEmberCapability;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
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
    // Set ember cost to run the motor per tick
    // TODO: Make configurable
    public static final double EMBER_COST = 0.5;

    // Constructor needed by GeneratingKineticBlockEntity
    public EmberMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // Set motor's ember capacity
        capability.setEmberCapacity(1000);
    }

    // Give motor the ember capability so it can store, receive and consume ember
    public IEmberCapability capability = new DefaultEmberCapability() {
        @Override
        public void onContentsChanged() {
            super.onContentsChanged();
            EmberMotorBlockEntity.this.setChanged();
        }
    };

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (!this.remove && cap == EmbersCapabilities.EMBER_CAPABILITY) {
            return capability.getCapability(cap, side);
        }
        return super.getCapability(cap, side);
    }

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

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
            updateGeneratedRotation();
    }

    // Give the motor stress capacity. The value I chose is equal to the stress capacity of four large water wheels
    // TODO: Make configurable
    public float calculateAddedStressCapacity() {
        float capacity = 2048f;
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    public void tick() {
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
