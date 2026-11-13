package com.nutria2;

import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** kind: 0 = creeper, 1 = vanish, 2 = todos nutrias (player se ignora). */
public record StatePayload(int kind, UUID player, boolean on) implements CustomPacketPayload {
	public static final Type<StatePayload> TYPE = new Type<>(Nutria2.id("state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, StatePayload::kind,
			UUIDUtil.STREAM_CODEC, StatePayload::player,
			ByteBufCodecs.BOOL, StatePayload::on,
			StatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
