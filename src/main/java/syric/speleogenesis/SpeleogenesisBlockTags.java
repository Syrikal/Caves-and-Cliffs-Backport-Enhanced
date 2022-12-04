package syric.speleogenesis;

import net.minecraft.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;
import org.lwjgl.system.CallbackI;

public class SpeleogenesisBlockTags {
    public static final ITag.INamedTag<Block> CAVE_DECORATIONS = register("cave_decorations");
    public static final ITag.INamedTag<Block> DRIPLEAF = register("dripleaf");
    public static final ITag.INamedTag<Block> GLOW_LICHEN_PLACEMENT = register("glow_lichen_placement");



    public SpeleogenesisBlockTags() {
    }

    public static void init() {
    }

    private static ITag.INamedTag<Block> register(String path) {
        return BlockTags.createOptional((new ResourceLocation(Speleogenesis.MODID, path)));
    }

    private static ITag.INamedTag<Block> registerForge(String path) {
        return BlockTags.createOptional((new ResourceLocation("forge", path)));
    }

//    private static Tags.IOptionalNamedTag<Block> register(String id, String path) {
//        return BlockTags.createOptional(new ResourceLocation(id, path));
//    }
}

