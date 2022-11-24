package syric.ccbackportenhanced;

import com.blackgear.cavesandcliffs.common.blocks.BlockMaterials;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class CCBEBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CCBackportEnhanced.MODID);

    // register item
    public static final RegistryObject<Block> GLOWBERRY_SACK = register("glowberry_sack",
            () -> new Block(AbstractBlock.Properties.of(Material.WOOL)
                    .strength(0.5F).lightLevel(s -> 14).sound(SoundType.WOOL)), ItemGroup.TAB_DECORATIONS);

    //Registration Methods
    static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    private static <T extends Block> RegistryObject<T> registerNoItem(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }
    private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, ItemGroup tab) {
        RegistryObject<T> ret = registerNoItem(name, block);
        CCBEItems.ITEMS.register(name, () -> new BlockItem(ret.get(), new Item.Properties().tab(tab)));
        return ret;
    }


}
