package syric.speleomancer.lush;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;
import syric.speleomancer.SpeleomancerItems;

public class LushSeedItem extends Item {

    public LushSeedItem(Properties properties) {
        super(properties);
    }

    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        world.playSound((PlayerEntity)null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
        if (!world.isClientSide) {
            LushSeedEntity lushSeedEntity = new LushSeedEntity(world, player);
//            lushSeedEntity.setItem(itemstack);
            lushSeedEntity.setItem(new ItemStack(SpeleomancerItems.LUSH_SEED.get()));
            lushSeedEntity.shootFromRotation(player, player.xRot, player.yRot, 0.0F, 1.5F, 0.0F);
            world.addFreshEntity(lushSeedEntity);
        }

//        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.abilities.instabuild) {
            itemstack.shrink(1);
        }

        return ActionResult.sidedSuccess(itemstack, world.isClientSide());
    }
}
