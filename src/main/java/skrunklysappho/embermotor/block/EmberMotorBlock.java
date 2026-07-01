package skrunklysappho.embermotor.block;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import skrunklysappho.embermotor.CEMBlockEntityTypes;
import skrunklysappho.embermotor.blockentity.EmberMotorBlockEntity;
import skrunklysappho.embermotor.voxelShape.CEMShapes;

public class EmberMotorBlock extends DirectionalKineticBlock implements IBE<EmberMotorBlockEntity> {

    public EmberMotorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferred = getPreferredFacing(context);
        if ((context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown()) || preferred == null)
            return super.getStateForPlacement(context);
        return defaultBlockState().setValue(FACING, preferred);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter reader, BlockPos pos, PathComputationType type) {
        return false;
    }

    public static final VoxelShaper VOXEL_SHAPE = CEMShapes.shape(3, 1, 3, 13, 14, 13)
            .forDirectional();

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return VOXEL_SHAPE.get(state.getValue(FACING));
    }

    @Override
    public Class<EmberMotorBlockEntity> getBlockEntityClass() {
        return EmberMotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EmberMotorBlockEntity> getBlockEntityType() {
        return CEMBlockEntityTypes.EMBER_MOTOR.get();
    }
}
