package skrunklysappho.embermotor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = CreateEmberMotorMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue EMBER_CONSUMPTION = BUILDER
            .comment("Amount of ember consumed by the ember motor per second")
            .defineInRange("emberConsumption", 5.0, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue OUTPUT_SPEED = BUILDER
            .comment("Rotation speed output by the motor, in RPM")
            .defineInRange("outputSpeed", 32, 0, 256);

    private static final ForgeConfigSpec.IntValue STRESS_CAPACITY = BUILDER
            .comment("Stress capacity provided by the motor at one RPM")
            .comment("In game, the actual stress capacity will be this multiplied by outputSpeed")
            .defineInRange("stressCapacity", 64, 0, Integer.MAX_VALUE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static double emberConsumption;
    public static int outputSpeed;
    public static int stressCapacity;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        emberConsumption = EMBER_CONSUMPTION.get();
        outputSpeed = OUTPUT_SPEED.get();
        stressCapacity = STRESS_CAPACITY.get();
    }
}
