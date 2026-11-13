package com.nutria2;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Nube de texto sobre un jugador. text vacio = quitar la nube. */
public record BubblePayload(UUID player, String text, boolean sound) implements CustomPacketPayload {
	public static final Type<BubblePayload> TYPE = new Type<>(Nutria2.id("bubble"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BubblePayload> CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, BubblePayload::player,
			ByteBufCodecs.stringUtf8(200), BubblePayload::text,
			ByteBufCodecs.BOOL, BubblePayload::sound,
			BubblePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
