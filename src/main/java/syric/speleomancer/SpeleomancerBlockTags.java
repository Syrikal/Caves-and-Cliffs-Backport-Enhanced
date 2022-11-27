package syric.speleomancer;

import net.minecraft.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;

public class SpeleomancerBlockTags {
    public static final ITag.INamedTag<Block> CAVE_DECORATIONS = register("cave_decorations");
    public static final ITag.INamedTag<Block> MOSS_FLOOR_DECORATIONS = register("cave_decorations");
    public static final ITag.INamedTag<Block> DRIPLEAF = register("cave_decorations");
    public static final ITag.INamedTag<Block> CEILING_DECORATIONS = register("cave_decorations");



    public SpeleomancerBlockTags() {
    }

    public static void init() {
    }

    private static ITag.INamedTag<Block> register(String path) {
        return BlockTags.bind((new ResourceLocation("ccbackportenhanced", path)).toString());
    }

    private static Tags.IOptionalNamedTag<Block> register(String id, String path) {
        return BlockTags.createOptional(new ResourceLocation(id, path));
    }
}

