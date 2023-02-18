package syric.speleogenesis.events.loot;

import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

public class LushSeedAdditionModifier extends LootModifier {
    private final Item addition;

    protected LushSeedAdditionModifier(ILootCondition[] conditionsIn, Item addition) {
        super(conditionsIn);
        this.addition = addition;
    }

    @Nonnull
    @Override
    protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
        // generatedLoot is the loot that would be dropped, if we wouldn't add or replace
        // anything!
        if (context.getRandom().nextFloat() > 0.9) {
            generatedLoot.add(new ItemStack(addition, 1));
        }
        if (context.getRandom().nextFloat() > 0.9) {
            generatedLoot.add(new ItemStack(addition, 1));
        }
        if (context.getRandom().nextFloat() > 0.9) {
            generatedLoot.add(new ItemStack(addition, 1));
        }
        if (context.getRandom().nextFloat() > 0.9) {
            generatedLoot.add(new ItemStack(addition, 1));
        }
        return generatedLoot;
    }

    public static class Serializer extends GlobalLootModifierSerializer<LushSeedAdditionModifier> {

        @Override
        public LushSeedAdditionModifier read(ResourceLocation name, JsonObject object, ILootCondition[] conditionsIn) {
            Item addition = ForgeRegistries.ITEMS.getValue(
                    new ResourceLocation(JSONUtils.getAsString(object, "addition")));
            return new LushSeedAdditionModifier(conditionsIn, addition);
        }

        @Override
        public JsonObject write(LushSeedAdditionModifier instance) {
            JsonObject json = makeConditions(instance.conditions);
            json.addProperty("addition", Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(instance.addition)).toString());
            return json;
        }
    }
}
