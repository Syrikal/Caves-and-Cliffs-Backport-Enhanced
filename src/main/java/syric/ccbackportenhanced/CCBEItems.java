package syric.ccbackportenhanced;

import net.minecraft.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class CCBEItems {
    // create DeferredRegister object
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CCBackportEnhanced.MODID);

    static void register(IEventBus bus) {
        ITEMS.register(bus);
    }


    // register items
//    public static final RegistryObject<Item> DRAGONSEEKER = ITEMS.register("dragonseeker", dragonseekerItem::new);

}
