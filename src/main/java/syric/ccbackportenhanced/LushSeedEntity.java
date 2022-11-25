package syric.ccbackportenhanced;

import com.blackgear.cavesandcliffs.core.CavesAndCliffs;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.CallbackI;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LushSeedEntity extends ProjectileItemEntity {

    public LushSeedEntity(EntityType<? extends ProjectileItemEntity> type, World world) {
        super(type, world);
    }

    public LushSeedEntity(World world, LivingEntity entity) {
        super(EntityType.ENDER_PEARL, entity, world);
    }

    public LushSeedEntity(World world, double d1, double d2, double d3) {
        super(CCBEEntityTypes.LUSH_SEED.get(), d1, d2, d3, world);
    }

    @Override
    protected Item getDefaultItem() {
        return CCBEItems.LUSH_SEED.get();
    }


    @OnlyIn(Dist.CLIENT)
    private IParticleData getParticle() {
        return new ItemParticleData(ParticleTypes.ITEM, this.getItemRaw());
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte b) {
        if (b == 3) {
            IParticleData iparticledata = this.getParticle();

            for(int i = 0; i < 8; ++i) {
                this.level.addParticle(iparticledata, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }

    }

    protected void onHitEntity(EntityRayTraceResult traceResult) {
        super.onHitEntity(traceResult);
        Entity entity = traceResult.getEntity();
        int i = 0;
        entity.hurt(DamageSource.thrown(this, this.getOwner()), (float)i);
    }

    protected void onHit(RayTraceResult traceResult) {
        super.onHit(traceResult);

        if (!this.level.isClientSide) {

            chatPrint("Lush Seed Explodes", this.level);
            explode(traceResult);

            this.level.broadcastEntityEvent(this, (byte)3);
            this.remove();
        }

    }

    public static void chatPrint(String input, Entity entity) {
        if (entity instanceof PlayerEntity) {
            String[] outputSplit = input.split("\n");
            for (String i : outputSplit) {
                PlayerEntity player = (PlayerEntity) entity;
                player.displayClientMessage(ITextComponent.nullToEmpty(i), false);
            }
        }
    }
    public static void chatPrint(String input, World level) {
        List<? extends PlayerEntity> playerList = level.players();
        for (PlayerEntity player : playerList) {
            chatPrint(input, player);
        }
    }


    public void explode(RayTraceResult traceResult) {

        //Find out whether it's hit a ceiling or a floor.
        Vector3i posvector = new Vector3i(traceResult.getLocation().x, traceResult.getLocation().y, traceResult.getLocation().z);
        BlockPos hitPos = new BlockPos(posvector);
        World world = this.level;
        int balance = 0; //Each solid block above adds 1, each below adds 2.
        for (int i = -3; i <3; i++) {
            if (i >= 0 && world.getBlockState(hitPos.above(i)).isSolidRender(world, hitPos.above(i))) {
                balance ++;
                world.setBlock(hitPos.above(i), Blocks.LAPIS_BLOCK.defaultBlockState(), 3);
            } else if (i < 0 && world.getBlockState(hitPos.above(i)).isSolidRender(world, hitPos.above(i))) {
                balance --;
                world.setBlock(hitPos.above(i), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            }
        }
        boolean hitCeiling = balance > 0;
        chatPrint("HitPos: " + hitPos.getX() + ", " + hitPos.getY() + ", " + hitPos.getZ(), world);
        chatPrint("Hit ceiling?: " + hitCeiling, world);
        //Find the height of the ceiling, or if it's outdoors.

        int ceilingHeight = 0;
        boolean outdoors = false;
        if (!hitCeiling) {
            boolean stop = false;
            int distance = 0;
            while (!stop) {
                if (world.getBlockState(hitPos.above(distance)).isSolidRender(world, hitPos.above(distance))) {
                    stop = true;
                    ceilingHeight = distance;
                } else if (distance < 100) {
                    distance++;
                } else {
                    stop = true;
                    outdoors = true;
                }
            }
        } else {
            boolean stop = false;
            int distance = 1;
            while (!stop) {
                if (world.getBlockState(hitPos.below(distance)).isSolidRender(world, hitPos.below(distance))) {
                    stop = true;
                    ceilingHeight = distance-1;
                } else if (distance < 100) {
                    distance++;
                } else {
                    stop = true;
                    outdoors = true;
                }
            }
        }
        chatPrint("Ceiling Height: " + ceilingHeight, world);

        if (outdoors) {
            //Plant an azalea tree
            return;
        }


        //Choose a block four blocks vertically from the surface, or halfway between floor and ceiling.

        int originY = 0;
        if (ceilingHeight > 8) {
            originY = hitCeiling ? hitPos.getY() - 4 : hitPos.getY() + 4;
        } else {
            originY = hitCeiling ? hitPos.getY() - ceilingHeight/2: hitPos.getY() + ceilingHeight/2;
        }

        BlockPos OriginPos = new BlockPos(hitPos.getX(), originY, hitPos.getZ());
        chatPrint("Origin pos : " + hitPos.getX() + ", " + originY + ", " + hitPos.getZ(), world);
        this.level.addParticle(ParticleTypes.HAPPY_VILLAGER, hitPos.getX(), originY, hitPos.getZ(), 0.0D, 0.0D, 0.0D);



        //Spread pattern with volume 4200, only air and other transparent blocks
        SpreadPattern spread = new SpreadPattern(world, OriginPos, 4200);
        Map<BlockPos, Double> blocks = spread.blockMap();

        //Remove everything over 10 blocks away
        for (Map.Entry<BlockPos, Double> entry : blocks.entrySet()) {
            if (entry.getValue() > 10) {
                blocks.remove(entry.getKey());
            }
        }

        //Create a map of all replaceable blocks adjacent to the blocks in the map
        ConcurrentHashMap<BlockPos, Integer> replacementCandidateMap = new ConcurrentHashMap<BlockPos, Integer>();
        for (Map.Entry<BlockPos, Double> entry : blocks.entrySet()) {
            for (Direction direction : Direction.values()) {
                if (world.getBlockState(entry.getKey().relative(direction)).is(CavesAndCliffs))
            }
        }


        //Split into ceiling, floor, floorcorner, and wall maps


        //Decide where water goes


        //In those areas, replace floor with water and floorcorner with clay

        //When you add water, add chance of sprouting a dripleaf and chance of spawning an axolotl


        //Elsewhere, replace floor with patches of moss.


        //When you replace stuff with moss, add chances of azalea, flowering azalea, grass, tall grass, and moss carpets.


        //Remove patches from the ceiling map to be stone (make smaller map that includes them for later)

        //Replace the rest of the ceiling with moss

        //When you place moss, add chance of glowberry vine of random length

        //Afterwards, chance of spore blossoms and glowberry vines everywhere (including removed patches)


        //Add vines to the ceiling and walls


        //Something something spawn it all in a ripple? Just record the distance


        //Lingering particles everywhere?


    }







}
