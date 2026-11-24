package dev.santiagosantofimio.effirromplus.structure;

import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.RandomizableContainer;

public final class StructureBooks {
	private static final Set<String> STRUCTURES = Set.of("bastion_remnant", "ancient_city", "mineshaft", "mineshaft_mesa");

	private StructureBooks() {
	}

	private static boolean isTracked(Identifier table, EffirromplusConfig config) {
		if (!"minecraft".equals(table.getNamespace())) {
			return false;
		}
		String path = table.getPath();
		if (path.startsWith("chests/bastion_")) {
			return config.bastionsEnabled();
		}
		if (path.equals("chests/ancient_city") || path.equals("chests/ancient_city_ice_box")) {
			return config.ancientCitiesEnabled();
		}
		return path.equals("chests/abandoned_mineshaft") && config.mineshaftsEnabled();
	}

	private static String structureKey(ServerLevel level, StructureStart start) {
		Identifier id = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
		ChunkPos origin = start.getChunkPos();
		return id + "@" + origin.x() + "," + origin.z();
	}

	private static boolean isTrackedStructure(ServerLevel level, Structure structure) {
		Identifier id = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(structure);
		return id != null && "minecraft".equals(id.getNamespace()) && STRUCTURES.contains(id.getPath());
	}

	private static Set<Long> chunksOf(StructureStart start) {
		Set<Long> chunks = new HashSet<>();
		for (StructurePiece piece : start.getPieces()) {
			var box = piece.getBoundingBox();
			for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
				for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
					chunks.add(ChunkPos.pack(x, z));
				}
			}
		}
		return chunks;
	}

	private static RandomGenerator randomFor(ServerLevel level, String key) {
		return new java.util.Random(level.getSeed() * 31L + key.hashCode());
	}

	private static StructureData data(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(StructureData.TYPE);
	}

	private static void decideIfReady(ServerLevel level, String key, StructureRecord record) {
		EffirromplusConfig config = EffirromplusConfig.get();
		if (record.decideIfReady(randomFor(level, key), config.secondBookChance(), config.thirdBookChance())) {
			data(level).setDirty();
		}
	}

	public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
		StructureData data = data(level);
		long chunkPos = chunk.getPos().pack();
		Map<String, StructureRecord> touched = new java.util.HashMap<>();
		for (Map.Entry<Structure, LongSet> reference : chunk.getAllReferences().entrySet()) {
			if (!isTrackedStructure(level, reference.getKey())) {
				continue;
			}
			level.structureManager().fillStartsForStructure(reference.getKey(), reference.getValue(), start -> {
				String key = structureKey(level, start);
				StructureRecord record = data.record(key, chunksOf(start));
				if (record.chunkLoaded(chunkPos)) {
					data.setDirty();
				}
				touched.put(key, record);
			});
		}
		for (BlockPos pos : chunk.getBlockEntitiesPos()) {
			BlockEntity blockEntity = chunk.getBlockEntity(pos);
			if (blockEntity instanceof RandomizableContainer container && container.getLootTable() != null) {
				register(level, data, pos, "b:" + pos.asLong(), container.getLootTable(), touched);
			}
		}
		touched.forEach((key, record) -> decideIfReady(level, key, record));
	}

	public static void onEntityLoad(Entity entity, ServerLevel level) {
		if (entity instanceof MinecartChest cart && cart.getContainerLootTable() != null) {
			Map<String, StructureRecord> touched = new java.util.HashMap<>();
			register(level, data(level), cart.blockPosition(), "e:" + cart.getUUID(), cart.getContainerLootTable(), touched);
			touched.forEach((key, record) -> decideIfReady(level, key, record));
		}
	}

	private static void register(ServerLevel level, StructureData data, BlockPos pos, String chestId, ResourceKey<LootTable> table,
			Map<String, StructureRecord> touched) {
		if (!isTracked(table.identifier(), EffirromplusConfig.get()) || data.recordOfChest(chestId) != null) {
			return;
		}
		StructureStart start = level.structureManager().getStructureWithPieceAt(pos, holder -> isTrackedStructure(level, holder.value()));
		if (!start.isValid()) {
			return;
		}
		String key = structureKey(level, start);
		StructureRecord record = data.record(key, chunksOf(start));
		data.registerChest(key, record, chestId);
		touched.put(key, record);
	}

	public static void onLoot(Identifier tableId, LootContext context, List<ItemStack> drops) {
		EffirromplusConfig config = EffirromplusConfig.get();
		if (!isTracked(tableId, config)) {
			return;
		}
		ServerLevel level = context.getLevel();
		StructureData data = data(level);
		String chestId = chestId(level, data, context);
		if (chestId == null) {
			return;
		}
		StructureRecord record = data.recordOfChest(chestId);
		if (record == null) {
			return;
		}
		String key = data.keyOf(chestId);
		int book = record.open(chestId, randomFor(level, key), config.secondBookChance(), config.thirdBookChance());
		data.setDirty();
		if (book != StructureRecord.NO_BOOK) {
			drops.add(ExtendedBooks.create(level.registryAccess(), book == StructureRecord.UNBREAKING_BOOK));
		}
	}

	private static String chestId(ServerLevel level, StructureData data, LootContext context) {
		Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
		if (origin == null) {
			return null;
		}
		String blockId = "b:" + BlockPos.containing(origin).asLong();
		if (data.recordOfChest(blockId) != null) {
			return blockId;
		}
		for (MinecartChest cart : level.getEntitiesOfClass(MinecartChest.class, new AABB(origin, origin).inflate(0.5))) {
			String cartId = "e:" + cart.getUUID();
			if (data.recordOfChest(cartId) != null) {
				return cartId;
			}
		}
		return null;
	}
}
