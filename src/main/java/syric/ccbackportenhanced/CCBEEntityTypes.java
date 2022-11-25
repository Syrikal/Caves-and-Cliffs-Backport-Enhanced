package syric.ccbackportenhanced;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class CCBEEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, CCBackportEnhanced.MODID);

    static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }


    // register items
//    public static final RegistryObject<EntityType<LushSeedEntity>> LUSH_SEED =
//            registerEntity("lush_seed",
//                    EntityType.Builder.<LushSeedEntity>of(LushSeedEntity::new, EntityClassification.MISC)
//                            .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));

    public static final RegistryObject<EntityType<LushSeedEntity>> LUSH_SEED = ENTITY_TYPES.register("lush_seed",
            () -> EntityType.Builder.<LushSeedEntity>of(LushSeedEntity::new, EntityClassification.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("lush_seed"));


    private static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(String entityName, EntityType.Builder<T> builder) {
        return ENTITY_TYPES.register(entityName, () -> builder.build(entityName));
    }

}
