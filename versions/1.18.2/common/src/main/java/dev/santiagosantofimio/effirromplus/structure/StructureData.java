package dev.santiagosantofimio.effirromplus.structure;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class StructureData extends SavedData {
	public static final String NAME = "effirromplus_structure_books";

	private static final Codec<Map<String, StructureRecord>> CODEC = Codec.unboundedMap(Codec.STRING, StructureRecord.CODEC);
	private static final String RECORDS = "records";

	private final Map<String, StructureRecord> records = new HashMap<>();
	private final Map<String, String> structureByChest = new HashMap<>();

	public StructureData() {
	}

	public static StructureData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(StructureData::load, StructureData::new, NAME);
	}

	private static StructureData load(CompoundTag tag) {
		StructureData data = new StructureData();
		Tag stored = tag.get(RECORDS);
		if (stored != null) {
			CODEC.parse(NbtOps.INSTANCE, stored).result().ifPresent(parsed -> {
				data.records.putAll(parsed);
				parsed.forEach((key, record) -> record.chestIds().forEach(chest -> data.structureByChest.put(chest, key)));
			});
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CODEC.encodeStart(NbtOps.INSTANCE, records).result().ifPresent(encoded -> tag.put(RECORDS, encoded));
		return tag;
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
