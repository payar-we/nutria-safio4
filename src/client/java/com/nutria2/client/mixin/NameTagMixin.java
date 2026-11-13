package com.nutria2.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nutria2.client.ClientState;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

@Mixin(LivingEntityRenderer.class)
public class NameTagMixin {
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
	private void nutria2$hideNames(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ClientState.namesHidden && entity instanceof Player) {
			cir.setReturnValue(false);
		}
	}
}
