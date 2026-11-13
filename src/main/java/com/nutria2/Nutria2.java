package com.nutria2;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Nutria2 implements ModInitializer {
	public static final String MOD_ID = "nutria2";
	public static final int KIND_CREEPER = 0, KIND_VANISH = 1, KIND_NUTRIA_ALL = 2, KIND_NAMES_HIDDEN = 3;
	private static final UUID NIL = new UUID(0, 0);

	private static final Set<UUID> CREEPERS = ConcurrentHashMap.newKeySet();
	private static final Set<UUID> VANISHED = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, String> BUBBLES = new ConcurrentHashMap<>();
	private static volatile boolean nutriaAll = false;
	private static volatile boolean namesHidden = false;

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.register();
		ModItems.register();
		PayloadTypeRegistry.playS2C().register(StatePayload.TYPE, StatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BubblePayload.TYPE, BubblePayload.CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer joined = handler.getPlayer();
			for (UUID u : CREEPERS) send(joined, KIND_CREEPER, u, true);
			for (UUID u : VANISHED) send(joined, KIND_VANISH, u, true);
			if (nutriaAll) send(joined, KIND_NUTRIA_ALL, NIL, true);
			if (namesHidden) send(joined, KIND_NAMES_HIDDEN, NIL, true);
			if (ServerPlayNetworking.canSend(joined, BubblePayload.TYPE)) {
				for (Map.Entry<UUID, String> e : BUBBLES.entrySet()) {
					ServerPlayNetworking.send(joined, new BubblePayload(e.getKey(), e.getValue(), false));
				}
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			setState(server, KIND_CREEPER, handler.getPlayer().getUUID(), false);
			setBubble(server, handler.getPlayer().getUUID(), "", false);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> setState(newP.getServer(), KIND_CREEPER, newP.getUUID(), false));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			CREEPERS.clear();
			VANISHED.clear();
			nutriaAll = false;
			namesHidden = false;
			BUBBLES.clear();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("nutria")
					.requires(s -> s.hasPermission(2))
					.then(Commands.literal("todos")
							.executes(ctx -> setNutriaAll(ctx, true))
							.then(Commands.argument("activo", BoolArgumentType.bool())
									.executes(ctx -> setNutriaAll(ctx, BoolArgumentType.getBool(ctx, "activo"))))));
			dispatcher.register(Commands.literal("nombres")
					.requires(s -> s.hasPermission(2))
					.executes(ctx -> setNames(ctx, !namesHidden))
					.then(Commands.argument("ocultar", BoolArgumentType.bool())
							.executes(ctx -> setNames(ctx, BoolArgumentType.getBool(ctx, "ocultar")))));
			dispatcher.register(Commands.literal("eon")
					.then(Commands.literal("parar").executes(ctx -> eonStop(ctx)))
					.then(Commands.argument("texto", StringArgumentType.greedyString())
							.executes(ctx -> eon(ctx, StringArgumentType.getString(ctx, "texto")))));
			dispatcher.register(Commands.literal("vanish")
					.requires(s -> s.hasPermission(2))
					.executes(ctx -> setVanish(ctx, null))
					.then(Commands.argument("activo", BoolArgumentType.bool())
							.executes(ctx -> setVanish(ctx, BoolArgumentType.getBool(ctx, "activo")))));
		});
	}

	private static int setNutriaAll(CommandContext<CommandSourceStack> ctx, boolean on) {
		MinecraftServer server = ctx.getSource().getServer();
		nutriaAll = on;
		setState(server, KIND_NUTRIA_ALL, NIL, on);
		ctx.getSource().sendSuccess(() -> Component.translatable(on ? "commands.nutria2.nutria_on" : "commands.nutria2.nutria_off"), true);
		return 1;
	}

	private static int setNames(CommandContext<CommandSourceStack> ctx, boolean hide) {
		namesHidden = hide;
		setState(ctx.getSource().getServer(), KIND_NAMES_HIDDEN, NIL, hide);
		ctx.getSource().sendSuccess(() -> Component.translatable(hide ? "commands.nutria2.names_on" : "commands.nutria2.names_off"), true);
		return 1;
	}

	private static int eonStop(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		setBubble(p.getServer(), p.getUUID(), "", false);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.nutria2.eon_stop"), false);
		return 1;
	}

	/** /eon texto  -> nube con el texto sobre el jugador y sonido. (/eon texto parar tambien la quita) */
	private static int eon(CommandContext<CommandSourceStack> ctx, String text) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		String t = text.trim();
		if (t.equalsIgnoreCase("texto parar") || t.equalsIgnoreCase("parar")) return eonStop(ctx);
		if (t.length() > 120) t = t.substring(0, 120);
		setBubble(p.getServer(), p.getUUID(), t, true);
		return 1;
	}

	private static void setBubble(MinecraftServer server, UUID id, String text, boolean sound) {
		if (text.isEmpty()) {
			if (BUBBLES.remove(id) == null) return;
		} else {
			BUBBLES.put(id, text);
		}
		if (server == null) return;
		for (ServerPlayer other : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(other, BubblePayload.TYPE)) {
				ServerPlayNetworking.send(other, new BubblePayload(id, text, sound));
			}
		}
	}

	private static int setVanish(CommandContext<CommandSourceStack> ctx, Boolean value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = ctx.getSource().getPlayerOrException();
		boolean on = value != null ? value : !VANISHED.contains(p.getUUID());
		if (on) {
			p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
		} else {
			p.removeEffect(MobEffects.INVISIBILITY);
		}
		setState(p.getServer(), KIND_VANISH, p.getUUID(), on);
		ctx.getSource().sendSuccess(() -> Component.translatable(on ? "commands.nutria2.vanish_on" : "commands.nutria2.vanish_off"), false);
		return 1;
	}

	/** Cambia el estado de un jugador (creeper) y avisa A TODOS, incluido el mismo jugador. */
	public static boolean toggleCreeper(ServerPlayer p) {
		boolean now = !CREEPERS.contains(p.getUUID());
		setState(p.getServer(), KIND_CREEPER, p.getUUID(), now);
		return now;
	}

	private static void setState(MinecraftServer server, int kind, UUID id, boolean on) {
		boolean changed = true;
		if (kind == KIND_CREEPER) changed = on ? CREEPERS.add(id) : CREEPERS.remove(id);
		else if (kind == KIND_VANISH) changed = on ? VANISHED.add(id) : VANISHED.remove(id);
		if (!changed || server == null) return;
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		for (ServerPlayer other : players) send(other, kind, id, on);
	}

	private static void send(ServerPlayer to, int kind, UUID id, boolean on) {
		if (ServerPlayNetworking.canSend(to, StatePayload.TYPE)) {
			ServerPlayNetworking.send(to, new StatePayload(kind, id, on));
		}
	}
}
