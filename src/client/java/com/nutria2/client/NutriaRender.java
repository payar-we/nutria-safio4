package com.nutria2.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nutria2.Nutria2;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja al jugador como la nutria de nutria_skin.bbmodel. */
public class NutriaRender {
	private static final ResourceLocation TEXTURE = Nutria2.id("textures/entity/nutria.png");

	/** @return false si no se pudo cargar el modelo (se queda el jugador normal). */
	public static boolean render(AbstractClientPlayer p, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		NutriaModel model = NutriaModel.get();
		if (model == null) return false;

		float bodyYaw = Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot);
		float headYaw = Mth.rotLerp(partialTick, p.yHeadRotO, p.yHeadRot);
		float pitch = Mth.lerp(partialTick, p.xRotO, p.getXRot());
		float speed = p.walkAnimation.speed(partialTick);
		float pos = p.walkAnimation.position(partialTick);
		float age = p.tickCount + partialTick;

		NutriaModel.Anim anim = null;
		float time = 0;
		if (p.isSwimming() || (p.isInWater() && speed > 0.02F)) {
			anim = model.anim("Nado");
			if (anim != null) time = (age / 20.0F) % anim.length;
		} else if (p.isCrouching()) {
			anim = model.anim("Agachado");
			if (anim != null) time = (age / 20.0F) % anim.length;
		} else if (speed > 0.02F) {
			boolean run = p.isSprinting();
			anim = model.anim(run ? "Correr" : "Caminar");
			if (anim != null) time = ((pos / (run ? 4.5F : 6.0F)) % 1.0F + 1.0F) % 1.0F * anim.length;
		}

		pose.pushPose();
		pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
		pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
		var vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		model.render(pose, vc, light, OverlayTexture.NO_OVERLAY, anim, time, headYaw - bodyYaw, pitch);
		pose.popPose();
		return true;
	}
}
