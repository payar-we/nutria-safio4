package com.nutria2.client;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientState {
	private static final Set<UUID> CREEPERS = ConcurrentHashMap.newKeySet();
	private static final Set<UUID> VANISHED = ConcurrentHashMap.newKeySet();
	public static volatile boolean nutriaAll = false;
	public static volatile boolean namesHidden = false;

	public static void apply(int kind, UUID id, boolean on) {
		switch (kind) {
			case 0 -> { if (on) CREEPERS.add(id); else CREEPERS.remove(id); }
			case 1 -> { if (on) VANISHED.add(id); else VANISHED.remove(id); }
			case 2 -> nutriaAll = on;
			case 3 -> namesHidden = on;
			default -> { }
		}
	}

	public static boolean isCreeper(UUID id) { return CREEPERS.contains(id); }
	public static boolean isVanished(UUID id) { return VANISHED.contains(id); }

	public static void clear() {
		CREEPERS.clear();
		VANISHED.clear();
		nutriaAll = false;
		namesHidden = false;
		ClientBubbles.clear();
	}
}
