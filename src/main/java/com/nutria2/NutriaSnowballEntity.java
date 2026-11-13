package com.nutria2;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Bola de nieve que golpea (efecto de dano) pero NO quita vida ni empuja. */
public class NutriaSnowballEntity extends ThrowableItemProjectile {
	public NutriaSnowballEntity(EntityType<? extends NutriaSnowballEntity> type, Level level) {
		super(type, level);
	}

	public NutriaSnowballEntity(Level level, LivingEntity owner) {
		super(ModEntities.SNOWBALL, owner, level);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.NUTRIA_SNOWBALL;
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		Entity target = result.getEntity();
		// Sin dano y sin empuje: solo el efecto de golpe (parpadeo rojo y sonido)
		if (!this.level().isClientSide && target instanceof LivingEntity living && target != this.getOwner()) {
			this.level().broadcastEntityEvent(living, (byte) 2);
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (!this.level().isClientSide) {
			this.level().broadcastEntityEvent(this, (byte) 3);
			this.discard();
		}
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 3) {
			for (int i = 0; i < 8; i++) {
				this.level().addParticle(ParticleTypes.ITEM_SNOWBALL, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
			}
		} else {
			super.handleEntityEvent(id);
		}
	}
}
