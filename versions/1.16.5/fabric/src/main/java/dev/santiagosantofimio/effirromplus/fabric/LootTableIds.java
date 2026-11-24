package dev.santiagosantofimio.effirromplus.fabric;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

public final class LootTableIds {
	private static final Map<LootTable, ResourceLocation> IDS = Collections.synchronizedMap(new WeakHashMap<>());

	private LootTableIds() {
	}

	public static void remember(Map<ResourceLocation, LootTable> tables) {
		tables.forEach((id, table) -> IDS.put(table, id));
	}

	public static void remember(ResourceLocation id, LootTable table) {
		if (table != LootTable.EMPTY) {
			IDS.put(table, id);
		}
	}

	public static ResourceLocation of(LootTable table) {
		return IDS.get(table);
	}
}
