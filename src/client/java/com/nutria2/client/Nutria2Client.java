package com.nutria2.client;

import com.nutria2.BubblePayload;
import com.nutria2.ModEntities;
import com.nutria2.StatePayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class Nutria2Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.SNOWBALL, ThrownItemRenderer::new);
		ClientPlayNetworking.registerGlobalReceiver(StatePayload.TYPE,
				(payload, context) -> ClientState.apply(payload.kind(), payload.player(), payload.on()));
		ClientPlayNetworking.registerGlobalReceiver(BubblePayload.TYPE,
				(payload, context) -> ClientBubbles.set(payload.player(), payload.text(), payload.sound()));
		ClientTickEvents.END_CLIENT_TICK.register(ClientBubbles::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.clear());
	}
}
