package dev.santiagosantofimio.effirromplus.structure;

import com.mojang.serialization.Codec;
import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class StructureData extends SavedData {
	public static final Codec<StructureData> CODEC = Codec.unboundedMap(Codec.STRING, StructureRecord.CODEC)
			.xmap(StructureData::new, data -> data.records);

	public static final SavedDataType<StructureData> TYPE = new SavedDataType<>(
			EffirromplusMod.MOD_ID + "_structure_books",
			(Supplier<StructureData>) StructureData::new,
			CODEC,
			DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final Map<String, StructureRecord> records = new HashMap<>();
	private final Map<String, String> structureByChest = new HashMap<>();

	public StructureData() {
	}

	private StructureData(Map<String, StructureRecord> records) {
		this.records.putAll(records);
		records.forEach((key, record) -> record.chestIds().forEach(chest -> structureByChest.put(chest, key)));
	}

	public StructureRecord record(String key, Set<Long> expectedChunks) {
		return records.computeIfAbsent(key, ignored -> {
			setDirty();
			return StructureRecord.expecting(expectedChunks);
		});
	}

	public StructureRecord recordOfChest(String chestId) {
		String key = structureByChest.get(chestId);
		return key == null ? null : records.get(key);
	}

	public String keyOf(String chestId) {
		return structureByChest.get(chestId);
	}

	public boolean registerChest(String key, StructureRecord record, String chestId) {
		if (structureByChest.containsKey(chestId)) {
			return false;
		}
		structureByChest.put(chestId, key);
		record.register(chestId);
		setDirty();
		return true;
	}
}
