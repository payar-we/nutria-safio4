package com.nutria2.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nutria2.client.BubbleRender;
import com.nutria2.client.ClientState;
import com.nutria2.client.CreeperRender;
import com.nutria2.client.NutriaRender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
	@Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
			at = @At("HEAD"), cancellable = true)
	private void nutria2$render(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose,
			MultiBufferSource buffers, int light, CallbackInfo ci) {
		// vanish: los demas no ven al jugador (el propio jugador si se ve)
		if (ClientState.isVanished(player.getUUID()) && player != Minecraft.getInstance().player) {
			ci.cancel();
			return;
		}
		BubbleRender.render(player, pose, buffers);
		if (ClientState.isCreeper(player.getUUID())) {
			CreeperRender.render(player, partialTick, pose, buffers, light);
			ci.cancel();
			return;
		}
		if (ClientState.nutriaAll && NutriaRender.render(player, partialTick, pose, buffers, light)) {
			ci.cancel();
		}
	}

	@Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
	private void nutria2$hideRight(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
		if (ClientState.isCreeper(player.getUUID()) || ClientState.nutriaAll) ci.cancel();
	}

	@Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
	private void nutria2$hideLeft(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, CallbackInfo ci) {
		if (ClientState.isCreeper(player.getUUID()) || ClientState.nutriaAll) ci.cancel();
	}
}
