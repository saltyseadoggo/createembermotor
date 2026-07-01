package skrunklysappho.embermotor.sound;

import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.util.sound.MachineSound;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.RegistryObject;
import skrunklysappho.embermotor.CreateEmberMotorMod;

public class CEMSounds {

    public static final RegistryObject<SoundEvent> MOTOR_HUM = registerSound("block.motor_hum");

    public static RegistryObject<SoundEvent> registerSound(String name) {
        return RegistryManager.SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CreateEmberMotorMod.MODID, name)));
    }

    public static void register() {
    }

    // Method needed by Embers' fancy means of playing looping machine sounds
    @OnlyIn(Dist.CLIENT)
    public static void playMachineSound(BlockEntity tile, int id, SoundEvent soundIn, SoundSource categoryIn, boolean repeat, float volume, float pitch, float xIn, float yIn, float zIn) {
        Minecraft.getInstance().getSoundManager().play(new MachineSound(tile, id, soundIn, categoryIn, repeat, volume, pitch, xIn, yIn, zIn));
    }
}
