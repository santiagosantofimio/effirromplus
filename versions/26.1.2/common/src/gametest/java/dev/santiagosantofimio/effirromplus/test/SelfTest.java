package dev.santiagosantofimio.effirromplus.test;

import com.mojang.datafixers.util.Pair;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

public final class SelfTest {
	private static final String TAG = "[selftest] ";
	private static final int SETTLE_TICKS = 40;

	private record Target(ResourceKey<Level> dimension, String structure, int radius) {
	}

	private record Loaded(Target target, ServerLevel level, StructureStart start, List<ChunkPos> chunks, int waited) {
	}

	private static final List<Target> TARGETS = List.of(
			new Target(Level.NETHER, "bastion_remnant", 100),
			new Target(Level.OVERWORLD, "mineshaft", 100),
			new Target(Level.OVERWORLD, "ancient_city", 200));

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
		if (loaded.waited() < SETTLE_TICKS) {
			loaded = new Loaded(loaded.target(), loaded.level(), loaded.start(), loaded.chunks(), loaded.waited() + 1);
			return false;
		}
		try {
			evaluate(loaded);
		} finally {
			loaded.chunks().forEach(chunk -> loaded.level().setChunkForced(chunk.x(), chunk.z(), false));
			loaded = null;
			index++;
		}
		return false;
	}

	private Loaded load(MinecraftServer server, Target target) {
		ServerLevel level = server.getLevel(target.dimension());
		Holder<Structure> structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
				.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace(target.structure())));
		Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
				.findNearestMapStructure(level, HolderSet.direct(structure), BlockPos.ZERO, target.radius(), false);
		if (found == null) {
			log(target.structure() + ": none found within " + target.radius() + " chunks, skipped");
			return null;
		}
		BlockPos at = found.getFirst();
		level.getChunk(at.getX() >> 4, at.getZ() >> 4);
		List<StructureStart> starts = level.structureManager().startsForStructure(ChunkPos.containing(at), candidate -> candidate == structure.value());
		StructureStart start = starts.isEmpty() ? StructureStart.INVALID_START : starts.getFirst();
		if (!start.isValid()) {
			failures.add(target.structure() + ": no structure start at " + at);
			return null;
		}
		BoundingBox box = start.getBoundingBox();
		List<ChunkPos> chunks = new ArrayList<>();
		for (int x = (box.minX() >> 4) - 1; x <= (box.maxX() >> 4) + 1; x++) {
			for (int z = (box.minZ() >> 4) - 1; z <= (box.maxZ() >> 4) + 1; z++) {
				chunks.add(new ChunkPos(x, z));
				level.setChunkForced(x, z, true);
			}
		}
		log(target.structure() + ": found at " + at + ", loading " + chunks.size() + " chunks");
		return new Loaded(target, level, start, chunks, 0);
	}

	private static java.util.function.Predicate<Holder<Structure>> structureTag(ServerLevel level, String name) {
		return holder -> holder.unwrapKey().map(key -> key.identifier().getPath().startsWith(name)).orElse(false);
	}

	private void evaluate(Loaded context) {
		String name = context.target().structure();
		ServerLevel level = context.level();
		StructureData data = level.getDataStorage().computeIfAbsent(StructureData.TYPE);
		Map<String, StructureRecord> records = new HashMap<>();
		List<RandomizableContainerBlockEntity> chests = new ArrayList<>();
		List<String> chestIds = new ArrayList<>();
		for (ChunkPos position : context.chunks()) {
			LevelChunk chunk = level.getChunk(position.x(), position.z());
			for (BlockPos pos : chunk.getBlockEntitiesPos()) {
				BlockEntity blockEntity = chunk.getBlockEntity(pos);
				if (blockEntity instanceof RandomizableContainerBlockEntity chest && chest.getLootTable() != null) {
					String id = "b:" + pos.asLong();
					StructureRecord record = data.recordOfChest(id);
					if (record != null && data.keyOf(id).contains(name)) {
						records.put(data.keyOf(id), record);
						chests.add(chest);
						chestIds.add(id);
					}
				}
			}
		}
		BoundingBox box = context.start().getBoundingBox();
		List<MinecartChest> carts = level.getEntities(EntityTypeTest.forClass(MinecartChest.class),
				new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1), cart -> true);
		List<MinecartChest> registeredCarts = new ArrayList<>();
		for (MinecartChest cart : carts) {
			log(name + ": cart " + cart.getUUID() + " at " + cart.blockPosition() + " table=" + cart.getContainerLootTable() + " pieceAt=" + level.structureManager().getStructureWithPieceAt(cart.blockPosition(), structureTag(level, name)).isValid());
		}
		for (MinecartChest cart : carts) {
			String id = "e:" + cart.getUUID();
			StructureRecord record = data.recordOfChest(id);
			if (record != null && data.keyOf(id).contains(name)) {
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
			cart.unpackChestVehicleLootTable(null);
			for (int slot = 0; slot < cart.getContainerSize(); slot++) {
				ItemStack stack = cart.getItem(slot);
				found += ExtendedBooks.isExtendedBook(stack) ? 1 : 0;
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
