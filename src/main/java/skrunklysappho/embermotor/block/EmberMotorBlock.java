package skrunklysappho.embermotor.block;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import skrunklysappho.embermotor.AllBlockEntityTypes;
import skrunklysappho.embermotor.blockentity.EmberMotorBlockEntity;

public class EmberMotorBlock extends DirectionalKineticBlock implements IBE<EmberMotorBlockEntity> {

    public EmberMotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public Class<EmberMotorBlockEntity> getBlockEntityClass() {
        return EmberMotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EmberMotorBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.EMBER_MOTOR.get();
    }
}
