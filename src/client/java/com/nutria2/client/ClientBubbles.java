package com.nutria2.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** Textos de las nubes y el sonido "turururururu". */
public class ClientBubbles {
	private static final Map<UUID, String> TEXTS = new ConcurrentHashMap<>();
	private static final List<Blip> BLIPS = new ArrayList<>();
	private static final float[] PITCHES = { 1.6F, 1.0F, 1.25F, 1.0F, 1.35F, 1.0F, 1.2F, 1.0F };

	private static class Blip {
		UUID id;
		int remaining;
		int index;
		int wait;
	}

	public static String get(UUID id) {
		return TEXTS.get(id);
	}

	public static void clear() {
		TEXTS.clear();
		BLIPS.clear();
	}

	public static void set(UUID id, String text, boolean sound) {
		if (text.isEmpty()) {
			TEXTS.remove(id);
			return;
		}
		TEXTS.put(id, text);
		if (sound) {
			Blip b = new Blip();
			b.id = id;
			b.remaining = Math.min(Math.max(text.replace(" ", "").length(), 10), 40);
			synchronized (BLIPS) { BLIPS.add(b); }
		}
	}

	/** Se llama cada tick del cliente. Un "tu" cada 2 ticks. */
	public static void tick(Minecraft mc) {
		if (mc.level == null) return;
		synchronized (BLIPS) {
			BLIPS.removeIf(b -> {
				if (b.wait-- > 0) return false;
				b.wait = 1;
				Player p = mc.level.getPlayerByUUID(b.id);
				if (p != null) {
					float pitch = PITCHES[b.index % PITCHES.length] + mc.level.random.nextFloat() * 0.1F;
					mc.level.playLocalSound(p.getX(), p.getY() + 1.5, p.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(),
							SoundSource.PLAYERS, 0.9F, pitch, false);
				}
				b.index++;
				return --b.remaining <= 0;
			});
		}
	}
}
