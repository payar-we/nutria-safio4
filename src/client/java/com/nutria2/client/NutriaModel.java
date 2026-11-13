package com.nutria2.client;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Quaternionf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nutria2.Nutria2;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Modelo de nutria cargado desde nutria_model.json (convertido de nutria_skin.bbmodel). */
public class NutriaModel {
	private static NutriaModel INSTANCE;
	private static boolean failed;

	private static final String[] FACES = { "north", "south", "east", "west", "up", "down" };
	private static final float[][] NORMALS = { { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 } };

	static class Cube {
		float[] from, to, origin, rot;
		float[][] uv = new float[6][];
	}

	static class Group {
		String name;
		float[] origin, rot;
		List<Cube> cubes = new ArrayList<>();
		List<Group> children = new ArrayList<>();
	}

	static class Bone {
		float[][] rot, pos;
	}

	public static class Anim {
		public float length;
		Map<String, Bone> bones = new HashMap<>();
	}

	private final List<Group> roots = new ArrayList<>();
	private final Map<String, Anim> anims = new HashMap<>();
	private float texW = 128, texH = 128;

	public static NutriaModel get() {
		if (INSTANCE == null && !failed) {
			try {
				INSTANCE = load();
			} catch (Throwable t) {
				failed = true;
			}
		}
		return INSTANCE;
	}

	public Anim anim(String name) {
		return anims.get(name);
	}

	private static float[] arr(JsonElement e) {
		JsonArray a = e.getAsJsonArray();
		float[] r = new float[a.size()];
		for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
		return r;
	}

	private static float[][] rows(JsonElement e) {
		if (e == null) return null;
		JsonArray a = e.getAsJsonArray();
		float[][] r = new float[a.size()][];
		for (int i = 0; i < r.length; i++) r[i] = arr(a.get(i));
		return r;
	}

	private static Group parseGroup(JsonObject o) {
		Group g = new Group();
		g.name = o.get("name").getAsString();
		g.origin = arr(o.get("origin"));
		g.rot = arr(o.get("rotation"));
		for (JsonElement ce : o.getAsJsonArray("cubes")) {
			JsonObject co = ce.getAsJsonObject();
			Cube c = new Cube();
			c.from = arr(co.get("from"));
			c.to = arr(co.get("to"));
			c.origin = arr(co.get("origin"));
			c.rot = arr(co.get("rotation"));
			JsonObject faces = co.getAsJsonObject("faces");
			for (int i = 0; i < 6; i++) {
				if (faces.has(FACES[i])) c.uv[i] = arr(faces.get(FACES[i]));
			}
			g.cubes.add(c);
		}
		for (JsonElement ch : o.getAsJsonArray("children")) g.children.add(parseGroup(ch.getAsJsonObject()));
		return g;
	}

	private static NutriaModel load() throws Exception {
		var res = Minecraft.getInstance().getResourceManager().getResource(Nutria2.id("nutria_model.json"));
		if (res.isEmpty()) throw new IllegalStateException("falta nutria_model.json");
		NutriaModel m = new NutriaModel();
		try (Reader r = new InputStreamReader(res.get().open())) {
			JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
			m.texW = root.get("texW").getAsFloat();
			m.texH = root.get("texH").getAsFloat();
			for (JsonElement e : root.getAsJsonArray("root")) m.roots.add(parseGroup(e.getAsJsonObject()));
			for (var entry : root.getAsJsonObject("animations").entrySet()) {
				JsonObject ao = entry.getValue().getAsJsonObject();
				Anim a = new Anim();
				a.length = ao.get("length").getAsFloat();
				for (var be : ao.getAsJsonObject("bones").entrySet()) {
					Bone b = new Bone();
					JsonObject bo = be.getValue().getAsJsonObject();
					b.rot = rows(bo.get("rotation"));
					b.pos = rows(bo.get("position"));
					a.bones.put(be.getKey(), b);
				}
				m.anims.put(entry.getKey(), a);
			}
		}
		return m;
	}

	private static float[] sample(float[][] rows, float t) {
		if (rows == null || rows.length == 0) return null;
		if (t <= rows[0][0]) return new float[] { rows[0][1], rows[0][2], rows[0][3] };
		for (int i = 0; i < rows.length - 1; i++) {
			if (t <= rows[i + 1][0]) {
				float span = rows[i + 1][0] - rows[i][0];
				float f = span <= 0 ? 0 : (t - rows[i][0]) / span;
				return new float[] { Mth.lerp(f, rows[i][1], rows[i + 1][1]), Mth.lerp(f, rows[i][2], rows[i + 1][2]), Mth.lerp(f, rows[i][3], rows[i + 1][3]) };
			}
		}
		float[] l = rows[rows.length - 1];
		return new float[] { l[1], l[2], l[3] };
	}

	/** El PoseStack debe estar en la base de los pies, mirando como el jugador, con escala 1/16 ya aplicada. */
	public void render(PoseStack pose, VertexConsumer vc, int light, int overlay, Anim anim, float time, float headYaw, float headPitch) {
		for (Group g : roots) renderGroup(g, pose, vc, light, overlay, anim, time, headYaw, headPitch);
	}

	private void renderGroup(Group g, PoseStack pose, VertexConsumer vc, int light, int overlay, Anim anim, float time, float headYaw, float headPitch) {
		float rx = g.rot[0], ry = g.rot[1], rz = g.rot[2];
		float px = 0, py = 0, pz = 0;
		if (anim != null) {
			Bone b = anim.bones.get(g.name);
			if (b != null) {
				float[] r = sample(b.rot, time);
				if (r != null) { rx += r[0]; ry += r[1]; rz += r[2]; }
				float[] p = sample(b.pos, time);
				if (p != null) { px = p[0]; py = p[1]; pz = p[2]; }
			}
		}
		if (g.name.equals("head")) {
			ry += -headYaw;
			rx += -headPitch;
		}
		pose.pushPose();
		pose.translate(px, py, pz);
		pose.translate(g.origin[0], g.origin[1], g.origin[2]);
		pose.mulPose(new Quaternionf().rotationZYX(rz * Mth.DEG_TO_RAD, ry * Mth.DEG_TO_RAD, rx * Mth.DEG_TO_RAD));
		pose.translate(-g.origin[0], -g.origin[1], -g.origin[2]);
		for (Cube c : g.cubes) renderCube(c, pose, vc, light, overlay);
		for (Group ch : g.children) renderGroup(ch, pose, vc, light, overlay, anim, time, headYaw, headPitch);
		pose.popPose();
	}

	private void renderCube(Cube c, PoseStack pose, VertexConsumer vc, int light, int overlay) {
		pose.pushPose();
		pose.translate(c.origin[0], c.origin[1], c.origin[2]);
		pose.mulPose(new Quaternionf().rotationZYX(c.rot[2] * Mth.DEG_TO_RAD, c.rot[1] * Mth.DEG_TO_RAD, c.rot[0] * Mth.DEG_TO_RAD));
		pose.translate(-c.origin[0], -c.origin[1], -c.origin[2]);
		float x1 = c.from[0], y1 = c.from[1], z1 = c.from[2], x2 = c.to[0], y2 = c.to[1], z2 = c.to[2];
		for (int f = 0; f < 6; f++) {
			float[] uv = c.uv[f];
			if (uv == null) continue;
			float[][] v = corners(f, x1, y1, z1, x2, y2, z2);
			float[][] t = { { uv[0], uv[1] }, { uv[2], uv[1] }, { uv[2], uv[3] }, { uv[0], uv[3] } };
			var last = pose.last();
			float[] n = NORMALS[f];
			for (int i = 0; i < 4; i++) {
				vc.addVertex(last, v[i][0], v[i][1], v[i][2])
						.setColor(-1)
						.setUv(t[i][0] / texW, t[i][1] / texH)
						.setOverlay(overlay)
						.setLight(light)
						.setNormal(last, n[0], n[1], n[2]);
			}
		}
		pose.popPose();
	}

	/** Esquinas de cada cara en orden: arriba-izq, arriba-der, abajo-der, abajo-izq (vistas desde afuera). */
	private static float[][] corners(int face, float x1, float y1, float z1, float x2, float y2, float z2) {
		return switch (face) {
			case 0 -> new float[][] { { x2, y2, z1 }, { x1, y2, z1 }, { x1, y1, z1 }, { x2, y1, z1 } }; // norte
			case 1 -> new float[][] { { x1, y2, z2 }, { x2, y2, z2 }, { x2, y1, z2 }, { x1, y1, z2 } }; // sur
			case 2 -> new float[][] { { x2, y2, z2 }, { x2, y2, z1 }, { x2, y1, z1 }, { x2, y1, z2 } }; // este
			case 3 -> new float[][] { { x1, y2, z1 }, { x1, y2, z2 }, { x1, y1, z2 }, { x1, y1, z1 } }; // oeste
			case 4 -> new float[][] { { x1, y2, z1 }, { x2, y2, z1 }, { x2, y2, z2 }, { x1, y2, z2 } }; // arriba
			default -> new float[][] { { x1, y1, z2 }, { x2, y1, z2 }, { x2, y1, z1 }, { x1, y1, z1 } }; // abajo
		};
	}
}
