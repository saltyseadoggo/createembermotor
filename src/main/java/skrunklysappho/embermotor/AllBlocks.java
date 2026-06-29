package skrunklysappho.embermotor;

import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.Create;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.infrastructure.config.CStress;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.MapColor;
import skrunklysappho.embermotor.block.EmberMotorBlock;
import skrunklysappho.embermotor.blockStateGen.EmberMotorBlockStateGen;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

// Register all blocks for the mod
// This code is based on how Create registers its blocks with Registrate. Ngl, I do not fully get how it works

public class AllBlocks {

    static {
        // Set what creative mode tab the motor appears in. Right now it's set to Create's base tab
        CreateEmberMotorMod.REGISTRATE.setCreativeTab(AllCreativeModeTabs.BASE_CREATIVE_TAB);
    }

    public static final BlockEntry<EmberMotorBlock> EMBER_MOTOR =
            CreateEmberMotorMod.REGISTRATE.block("ember_motor", EmberMotorBlock::new)
                    .initialProperties(SharedProperties::stone)
                    // TODO: Set map color to smth more fitting
                    .properties(p -> p.mapColor(MapColor.COLOR_PURPLE).forceSolidOn())
                    .tag(AllBlockTags.SAFE_NBT.tag)
                    .transform(pickaxeOnly())
                    .blockstate(new EmberMotorBlockStateGen()::generate)
                    // TODO: Set stress capacity to smth more reasonable, and make it configurable
                    // transform(CStress.setCapacity(16384.0))
                    //.onRegister(BlockStressValues.setGeneratorSpeed(256, true))
                    .item()
                    .transform(customItemModel())
                    .register();

    public static void register() {
    }
}
