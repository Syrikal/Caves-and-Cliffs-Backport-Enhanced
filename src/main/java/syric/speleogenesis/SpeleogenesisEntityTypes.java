package syric.speleogenesis;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import syric.speleogenesis.lush.LushGeneratorEntity;
import syric.speleogenesis.lush.LushSeedEntity;

public class SpeleogenesisEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITIES, Speleogenesis.MODID);

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
    public static final RegistryObject<EntityType<LushGeneratorEntity>> LUSH_GENERATOR = ENTITY_TYPES.register("lush_generator",
            () -> EntityType.Builder.<LushGeneratorEntity>of(LushGeneratorEntity::new, EntityClassification.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("lush_seed"));


    private static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(String entityName, EntityType.Builder<T> builder) {
        return ENTITY_TYPES.register(entityName, () -> builder.build(entityName));
    }

}
