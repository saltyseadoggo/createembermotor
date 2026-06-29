package skrunklysappho.embermotor.blockentity;

import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.power.DefaultEmberCapability;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import skrunklysappho.embermotor.AllBlocks;
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

    // Set motor's generated spinny speed in the correct direction based on which way the motor block is facing
    @Override
    public float getGeneratedSpeed() {
        if (!AllBlocks.EMBER_MOTOR.has(getBlockState()))
            return 0;
        return convertToDirection(generatedSpeed, getBlockState().getValue(EmberMotorBlock.FACING));
    }

    @Override
    public void initialize() {
        super.initialize();
        if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
            updateGeneratedRotation();
    }

    public void serverTick(Level level, BlockPos pos, BlockState state, EmberMotorBlockEntity blockEntity) {
        // Only generate stress units if enough ember is present to consume
        if (blockEntity.capability.getEmber() >= EMBER_COST) {
            // Consume ember
            blockEntity.capability.removeAmount(EMBER_COST, true);
            // Set generatedSpeed to the configured value for when the motor is running
            generatedSpeed = generatedSpeedWhilePowered;
        }
        else {
            // When insufficient ember is present to run the motor, set generatedSpeed to 0
            generatedSpeed = 0;
        }
    }
}
