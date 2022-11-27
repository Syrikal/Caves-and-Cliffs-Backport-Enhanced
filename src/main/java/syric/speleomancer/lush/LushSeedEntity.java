package syric.speleomancer.lush;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import syric.speleomancer.SpeleomancerEntityTypes;
import syric.speleomancer.SpeleomancerItems;

import static syric.speleomancer.Speleomancer.chatPrint;

public class LushSeedEntity extends ProjectileItemEntity {

    public LushSeedEntity(EntityType<? extends ProjectileItemEntity> type, World world) {
        super(type, world);
    }

    public LushSeedEntity(World world, LivingEntity entity) {
        super(EntityType.ENDER_PEARL, entity, world);
    }

    public LushSeedEntity(World world, double d1, double d2, double d3) {
        super(SpeleomancerEntityTypes.LUSH_SEED.get(), d1, d2, d3, world);
    }

    @Override
    protected Item getDefaultItem() {
        return SpeleomancerItems.LUSH_SEED.get();
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

    protected void onHit(RayTraceResult result) {
        super.onHit(result);
        if (!this.level.isClientSide) {
            LushGeneratorEntity lushGenerator = SpeleomancerEntityTypes.LUSH_GENERATOR.get().create(this.level);
            assert lushGenerator != null;
            lushGenerator.moveTo(this.getX(), this.getY(), this.getZ(), this.yRot, 0.0F);
            this.level.addFreshEntity(lushGenerator);
            lushGenerator.setTraceResult(result);
            chatPrint("Lush Seed Explodes", this.level);

            this.level.broadcastEntityEvent(this, (byte)3);
            this.remove();
        }
    }

}
