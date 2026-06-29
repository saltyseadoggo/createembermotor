package skrunklysappho.embermotor;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import skrunklysappho.embermotor.blockentity.EmberMotorBlockEntity;
import skrunklysappho.embermotor.kineticBlockEntityRenderer.EmberMotorRenderer;

// Register all block entities for the mod
// This code is based on how Create registers its block entities with Registrate. Ngl, I do not fully get how it works

public class CEMBlockEntityTypes {

    public static final BlockEntityEntry<EmberMotorBlockEntity> EMBER_MOTOR = CreateEmberMotorMod.REGISTRATE
        .blockEntity("ember_motor", EmberMotorBlockEntity::new)
        .visual(() -> OrientedRotatingVisual.of(AllPartialModels.SHAFT_HALF), false)
        .validBlocks(CEMBlocks.EMBER_MOTOR)
        .renderer(() -> EmberMotorRenderer::new)
        .register();

    public static void register() {
    }
}
