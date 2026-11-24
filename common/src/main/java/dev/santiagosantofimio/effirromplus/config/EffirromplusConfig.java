package dev.santiagosantofimio.effirromplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EffirromplusConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final int MAX_STACK = 64;

	private static EffirromplusConfig current = fromJson(defaults());

	private final JsonObject values;

	private EffirromplusConfig(JsonObject values) {
		this.values = values;
	}

	public static EffirromplusConfig get() {
		return current;
	}

	public static void use(JsonObject values) {
		current = fromJson(values);
	}

	public static EffirromplusConfig fromJson(JsonObject values) {
		return new EffirromplusConfig(values);
	}

	public static JsonObject defaults() {
		JsonObject root = new JsonObject();

		JsonObject villagers = new JsonObject();
		villagers.addProperty("enabled", true);
		villagers.addProperty("minPrice", 30);
		villagers.addProperty("maxPrice", 64);
		villagers.addProperty("averagePrice", 40);
		villagers.addProperty("maxUses", 12);
		root.add("villagers", villagers);

		JsonObject structures = new JsonObject();
		structures.addProperty("bastions", true);
		structures.addProperty("ancientCities", true);
		structures.addProperty("mineshafts", true);
		structures.addProperty("secondBookChance", 0.15);
		structures.addProperty("thirdBookChance", 0.03);
		root.add("structures", structures);

		JsonObject endCities = new JsonObject();
		endCities.addProperty("enabled", true);
		endCities.addProperty("chancePerChest", 0.10);
		root.add("endCities", endCities);

		JsonObject fishing = new JsonObject();
		fishing.addProperty("enabled", true);
		fishing.addProperty("chancePerCatch", 0.003);
		root.add("fishing", fishing);

		return root;
	}

	public static void load(Path configDir) {
		Path path = configDir.resolve(EffirromplusMod.MOD_ID + ".json");
		JsonObject defaults = defaults();
		JsonObject values = new JsonObject();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				JsonElement parsed = JsonParser.parseReader(reader);
				if (!parsed.isJsonObject()) {
					throw new JsonParseException("Root must be a JSON object");
				}
				values = parsed.getAsJsonObject();
			} catch (IOException | JsonParseException e) {
				EffirromplusMod.LOGGER.error("Could not read {}; it will not be overwritten", path, e);
				current = fromJson(defaults);
				return;
			}
		}
		if (JsonMerge.addMissing(values, defaults) || !Files.exists(path)) {
			write(path, values);
		}
		current = fromJson(values);
	}

	private static void write(Path path, JsonObject values) {
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(values, writer);
			}
		} catch (IOException e) {
			EffirromplusMod.LOGGER.error("Could not write {}", path, e);
		}
	}

	public boolean villagersEnabled() {
		return bool("villagers", "enabled");
	}

	public int minPrice() {
		return Math.clamp(integer("villagers", "minPrice"), 1, MAX_STACK);
	}

	public int maxPrice() {
		return Math.clamp(integer("villagers", "maxPrice"), minPrice(), MAX_STACK);
	}

	public int averagePrice() {
		return Math.clamp(integer("villagers", "averagePrice"), minPrice(), maxPrice());
	}

	public int tradeMaxUses() {
		return Math.max(1, integer("villagers", "maxUses"));
	}

	public boolean bastionsEnabled() {
		return bool("structures", "bastions");
	}

	public boolean ancientCitiesEnabled() {
		return bool("structures", "ancientCities");
	}

	public boolean mineshaftsEnabled() {
		return bool("structures", "mineshafts");
	}

	public double secondBookChance() {
		return probability("structures", "secondBookChance");
	}

	public double thirdBookChance() {
		return probability("structures", "thirdBookChance");
	}

	public boolean endCitiesEnabled() {
		return bool("endCities", "enabled");
	}

	public double endCityChancePerChest() {
		return probability("endCities", "chancePerChest");
	}

	public boolean fishingEnabled() {
		return bool("fishing", "enabled");
	}

	public double fishingChancePerCatch() {
		return probability("fishing", "chancePerCatch");
	}

	private JsonElement find(String section, String key) {
		JsonElement sectionValue = values.get(section);
		if (sectionValue != null && sectionValue.isJsonObject()) {
			JsonElement value = sectionValue.getAsJsonObject().get(key);
			if (value != null && value.isJsonPrimitive()) {
				return value;
			}
		}
		return defaults().getAsJsonObject(section).get(key);
	}

	private boolean bool(String section, String key) {
		try {
			return find(section, key).getAsBoolean();
		} catch (RuntimeException e) {
			return defaults().getAsJsonObject(section).get(key).getAsBoolean();
		}
	}

	private int integer(String section, String key) {
		try {
			return find(section, key).getAsInt();
		} catch (RuntimeException e) {
			return defaults().getAsJsonObject(section).get(key).getAsInt();
		}
	}

	private double probability(String section, String key) {
		double value;
		try {
			value = find(section, key).getAsDouble();
		} catch (RuntimeException e) {
			value = defaults().getAsJsonObject(section).get(key).getAsDouble();
		}
		return Math.clamp(value, 0.0, 1.0);
	}
}
