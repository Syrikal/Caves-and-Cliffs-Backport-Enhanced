package syric.speleogenesis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.SpriteRenderer;
import net.minecraft.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import syric.speleogenesis.Speleogenesis;
import syric.speleogenesis.SpeleogenesisEntityTypes;
import syric.speleogenesis.lush.LushSeedEntity;

@Mod.EventBusSubscriber(modid = Speleogenesis.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvent {

    private ClientModEvent() {

    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
//        RenderingRegistry.registerEntityRenderingHandler(SpeleogenesisEntityTypes.LUSH_SEED.get(), new lushRenderFactory());
        RenderingRegistry.registerEntityRenderingHandler(SpeleogenesisEntityTypes.LUSH_SEED.get(), entity -> new SpriteRenderer<>(entity, Minecraft.getInstance().getItemRenderer()));

    }

    private static class lushRenderFactory implements IRenderFactory<LushSeedEntity> {
        @Override
        public EntityRenderer<? super LushSeedEntity> createRenderFor(EntityRendererManager manager) {
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            return new SpriteRenderer<>(manager, itemRenderer);
        }
    }

}
