package com.nutria2.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nutria2.Nutria2;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/** Nube de pensamiento con texto arriba del jugador. */
public class BubbleRender {
	private static final ResourceLocation CLOUD = Nutria2.id("textures/gui/cloud.png");
	private static final ResourceLocation DOT = Nutria2.id("textures/gui/dot.png");
	private static final int FULL_LIGHT = 15728880;

	public static void render(AbstractClientPlayer p, PoseStack pose, MultiBufferSource buffers) {
		String text = ClientBubbles.get(p.getUUID());
		if (text == null) return;
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		List<FormattedCharSequence> lines = font.split(Component.literal(text), 120);
		if (lines.size() > 4) lines = lines.subList(0, 4);
		int textW = 0;
		for (FormattedCharSequence l : lines) textW = Math.max(textW, font.width(l));
		float w = Math.max(70, textW + 34);
		float h = lines.size() * 10 + 24;
		float dotsH = 16;

		pose.pushPose();
		pose.translate(0.0F, p.getBbHeight() + 0.45F, 0.0F);
		pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
		pose.scale(0.025F, -0.025F, 0.025F);

		float top = -(h + dotsH);
		quad(pose, buffers, CLOUD, -w / 2, top, w, h);
		quad(pose, buffers, DOT, -w / 2 + 14, -dotsH + 1, 9, 9);
		quad(pose, buffers, DOT, -w / 2 + 8, -dotsH + 11, 5, 5);

		float y = top + 12;
		for (FormattedCharSequence l : lines) {
			float x = -font.width(l) / 2.0F;
			font.drawInBatch(l, x, y, 0xFF555560, false, pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, FULL_LIGHT);
			y += 10;
		}
		pose.popPose();
	}

	private static void quad(PoseStack pose, MultiBufferSource buffers, ResourceLocation tex, float x, float y, float w, float h) {
		VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
		var last = pose.last();
		float[][] pos = { { x, y }, { x + w, y }, { x + w, y + h }, { x, y + h } };
		float[][] uv = { { 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++) {
			vc.addVertex(last, pos[i][0], pos[i][1], 0.0F)
					.setColor(-1)
					.setUv(uv[i][0], uv[i][1])
					.setOverlay(OverlayTexture.NO_OVERLAY)
					.setLight(FULL_LIGHT)
					.setNormal(last, 0.0F, 0.0F, -1.0F);
		}
	}
}
