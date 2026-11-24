package dev.santiagosantofimio.effirromplus.test;

import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.structure.StructureData;
import dev.santiagosantofimio.effirromplus.structure.StructureRecord;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

public final class SelfTest {
	private static final String TAG = "[selftest] ";
	private static final int SETTLE_TICKS = 40;

	private static final class Target {
		private final ResourceKey<Level> dimension;
		private final StructureFeature<?> feature;
		private final String name;
		private final int radius;
		private final BlockPos origin;

		private Target(ResourceKey<Level> dimension, StructureFeature<?> feature, String name, int radius, BlockPos origin) {
			this.dimension = dimension;
			this.feature = feature;
			this.name = name;
			this.radius = radius;
			this.origin = origin;
		}
	}

	private static final class Loaded {
		private final Target target;
		private final ServerLevel level;
		private final StructureStart<?> start;
		private final List<ChunkPos> chunks;
		private int waited;

		private Loaded(Target target, ServerLevel level, StructureStart<?> start, List<ChunkPos> chunks) {
			this.target = target;
			this.level = level;
			this.start = start;
			this.chunks = chunks;
		}
	}

	private static final List<Target> TARGETS = List.of(
			new Target(Level.NETHER, StructureFeature.BASTION_REMNANT, "bastion_remnant", 100, BlockPos.ZERO),
			new Target(Level.OVERWORLD, StructureFeature.MINESHAFT, "mineshaft", 100, BlockPos.ZERO),
			new Target(Level.OVERWORLD, StructureFeature.MINESHAFT, "mineshaft", 100, new BlockPos(3000, 0, 3000)),
			new Target(Level.OVERWORLD, StructureFeature.MINESHAFT, "mineshaft", 100, new BlockPos(-3000, 0, 3000)),
			new Target(Level.OVERWORLD, StructureFeature.MINESHAFT, "mineshaft", 100, new BlockPos(3000, 0, -3000)),
			new Target(Level.OVERWORLD, StructureFeature.MINESHAFT, "mineshaft", 100, new BlockPos(-3000, 0, -3000)));

	private final List<String> failures = new ArrayList<>();
	private int index;
	private Loaded loaded;

	private static void log(String message) {
		EffirromplusMod.LOGGER.info(TAG + message);
	}

	public boolean tick(MinecraftServer server) {
		if (index >= TARGETS.size()) {
			log(failures.isEmpty() ? "RESULT PASS" : "RESULT FAIL " + failures);
			return true;
		}
		Target target = TARGETS.get(index);
		if (loaded == null) {
			loaded = load(server, target);
			if (loaded == null) {
				index++;
			}
			return false;
		}
		if (loaded.waited < SETTLE_TICKS) {
			loaded.waited++;
			return false;
		}
		try {
			evaluate(loaded);
		} finally {
			Loaded done = loaded;
			done.chunks.forEach(chunk -> done.level.setChunkForced(chunk.x, chunk.z, false));
			loaded = null;
			index++;
		}
		return false;
	}

	private Loaded load(MinecraftServer server, Target target) {
		ServerLevel level = server.getLevel(target.dimension);
		BlockPos at = level.findNearestMapFeature(target.feature, target.origin, target.radius, false);
		if (at == null) {
			log(target.name + ": none found within " + target.radius + " chunks, skipped");
			return null;
		}
		level.getChunk(at.getX() >> 4, at.getZ() >> 4);
		List<StructureStart<?>> starts = level.structureFeatureManager().startsForFeature(SectionPos.of(new ChunkPos(at), 0), target.feature)
				.collect(Collectors.toList());
		StructureStart<?> start = starts.isEmpty() ? StructureStart.INVALID_START : starts.get(0);
		if (!start.isValid()) {
			failures.add(target.name + ": no structure start at " + at);
			return null;
		}
		BoundingBox box = start.getBoundingBox();
		List<ChunkPos> chunks = new ArrayList<>();
		for (int x = (box.x0 >> 4) - 1; x <= (box.x1 >> 4) + 1; x++) {
			for (int z = (box.z0 >> 4) - 1; z <= (box.z1 >> 4) + 1; z++) {
				chunks.add(new ChunkPos(x, z));
				level.setChunkForced(x, z, true);
			}
		}
		log(target.name + ": found at " + at + ", loading " + chunks.size() + " chunks");
		return new Loaded(target, level, start, chunks);
	}

	private static boolean hasLootTable(BlockEntity blockEntity) {
		CompoundTag tag = blockEntity.save(new CompoundTag());
		return tag.contains("LootTable", 8);
	}

	private void evaluate(Loaded context) {
		String name = context.target.name;
		ServerLevel level = context.level;
		StructureData data = StructureData.get(level);
		String expectedKey = "minecraft:" + name + "@" + context.start.getChunkX() + "," + context.start.getChunkZ();
		Map<String, StructureRecord> records = new HashMap<>();
		List<RandomizableContainerBlockEntity> chests = new ArrayList<>();
		List<String> chestIds = new ArrayList<>();
		for (ChunkPos position : context.chunks) {
			LevelChunk chunk = level.getChunk(position.x, position.z);
			for (BlockPos pos : chunk.getBlockEntitiesPos()) {
				BlockEntity blockEntity = chunk.getBlockEntity(pos);
				if (blockEntity instanceof RandomizableContainerBlockEntity && hasLootTable(blockEntity)) {
					String id = "b:" + pos.asLong();
					StructureRecord record = data.recordOfChest(id);
					if (record != null && data.keyOf(id).equals(expectedKey)) {
						records.put(data.keyOf(id), record);
						chests.add((RandomizableContainerBlockEntity) blockEntity);
						chestIds.add(id);
					}
				}
			}
		}
		BoundingBox box = context.start.getBoundingBox();
		List<MinecartChest> carts = level.getEntitiesOfClass(MinecartChest.class,
				new AABB(box.x0, box.y0, box.z0, box.x1 + 1, box.y1 + 1, box.z1 + 1));
		List<MinecartChest> registeredCarts = new ArrayList<>();
		for (MinecartChest cart : carts) {
			String id = "e:" + cart.getUUID();
			StructureRecord record = data.recordOfChest(id);
			if (record != null && data.keyOf(id).equals(expectedKey)) {
				records.put(data.keyOf(id), record);
				registeredCarts.add(cart);
				chestIds.add(id);
			}
		}
		int registered = chests.size() + registeredCarts.size();
		if (registered == 0) {
			log(name + ": no registered chests (blocks " + chests.size() + ", carts " + carts.size() + "), inconclusive");
			return;
		}
		Set<String> known = new HashSet<>(chestIds);
		int planned = 0;
		for (Map.Entry<String, StructureRecord> entry : records.entrySet()) {
			StructureRecord record = entry.getValue();
			if (!record.isDecided()) {
				failures.add(name + ": " + entry.getKey() + " not decided before opening any chest (complete=" + record.isComplete() + ")");
			}
			for (String id : record.chestIds()) {
				if (known.contains(id) && record.bookOf(id) != StructureRecord.NO_BOOK) {
					planned++;
				}
			}
		}
		int found = 0;
		for (RandomizableContainerBlockEntity chest : chests) {
			chest.unpackLootTable(null);
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				found += ExtendedBooks.isExtendedBook(chest.getItem(slot)) ? 1 : 0;
			}
		}
		for (MinecartChest cart : registeredCarts) {
			cart.unpackLootTable(null);
			for (int slot = 0; slot < cart.getContainerSize(); slot++) {
				found += ExtendedBooks.isExtendedBook(cart.getItem(slot)) ? 1 : 0;
			}
		}
		log(name + ": records=" + records.keySet() + " chests=" + chests.size() + " carts=" + registeredCarts.size()
				+ " plannedBooks=" + planned + " foundBooks=" + found);
		if (planned < 1) {
			failures.add(name + ": the structure has no planned book");
		}
		if (found != planned) {
			failures.add(name + ": planned " + planned + " books but opened chests had " + found);
		}
	}
}
