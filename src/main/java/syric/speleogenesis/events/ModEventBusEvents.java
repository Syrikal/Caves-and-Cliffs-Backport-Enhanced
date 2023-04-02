package syric.speleogenesis.events;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import syric.speleogenesis.Speleogenesis;
import syric.speleogenesis.events.loot.LushSeedAdditionModifier;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(modid = Speleogenesis.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void registerModifierSerializers(@Nonnull final RegistryEvent.Register<GlobalLootModifierSerializer<?>>
                                                           event) {
        event.getRegistry().registerAll(

                new LushSeedAdditionModifier.Serializer().setRegistryName
                        (new ResourceLocation(Speleogenesis.MODID,"lush_seed_in_dungeon")),
                new LushSeedAdditionModifier.Serializer().setRegistryName
                        (new ResourceLocation(Speleogenesis.MODID,"lush_seed_in_jungle_temple")),
                new LushSeedAdditionModifier.Serializer().setRegistryName
                        (new ResourceLocation(Speleogenesis.MODID,"lush_seed_in_mineshaft"))
        );
    }
}