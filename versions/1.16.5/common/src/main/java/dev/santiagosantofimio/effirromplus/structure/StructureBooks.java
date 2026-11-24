package dev.santiagosantofimio.effirromplus.structure;

import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class StructureBooks {
	private static final String LOOT_TABLE = "LootTable";
	private static final int STRING_TAG = 8;

	private StructureBooks() {
	}

	private static boolean isTracked(ResourceLocation table, EffirromplusConfig config) {
		if (!"minecraft".equals(table.getNamespace())) {
			return false;
		}
		String path = table.getPath();
		if (path.startsWith("chests/bastion_")) {
			return config.bastionsEnabled();
		}
		return path.equals("chests/abandoned_mineshaft") && config.mineshaftsEnabled();
	}

	private static final List<StructureFeature<?>> FEATURES = List.of(StructureFeature.BASTION_REMNANT, StructureFeature.MINESHAFT);

	private static String structureKey(StructureStart<?> start) {
		ResourceLocation id = Registry.STRUCTURE_FEATURE.getKey(start.getFeature());
		return id + "@" + start.getChunkX() + "," + start.getChunkZ();
	}

	private static Set<Long> chunksOf(StructureStart<?> start) {
		Set<Long> chunks = new HashSet<>();
		for (StructurePiece piece : start.getPieces()) {
			BoundingBox box = piece.getBoundingBox();
			for (int x = box.x0 >> 4; x <= box.x1 >> 4; x++) {
				for (int z = box.z0 >> 4; z <= box.z1 >> 4; z++) {
					chunks.add(ChunkPos.asLong(x, z));
				}
			}
		}
		return chunks;
	}

	private static Random randomFor(ServerLevel level, String key) {
		return new Random(level.getSeed() * 31L + key.hashCode());
	}

	private static void decideIfReady(ServerLevel level, StructureData data, String key, StructureRecord record) {
		EffirromplusConfig config = EffirromplusConfig.get();
		if (record.decideIfReady(randomFor(level, key), config.secondBookChance(), config.thirdBookChance())) {
			data.setDirty();
		}
	}

	private static ResourceLocation lootTableOf(CompoundTag tag) {
		return tag.contains(LOOT_TABLE, STRING_TAG) ? new ResourceLocation(tag.getString(LOOT_TABLE)) : null;
	}

	public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
		StructureData data = StructureData.get(level);
		long chunkPos = chunk.getPos().toLong();
		Map<String, StructureRecord> touched = new HashMap<>();
		SectionPos section = SectionPos.of(chunk.getPos(), 0);
		for (StructureFeature<?> feature : FEATURES) {
			level.structureFeatureManager().startsForFeature(section, feature).forEach(start -> {
				String key = structureKey(start);
				StructureRecord record = data.record(key, chunksOf(start));
				if (record.chunkLoaded(chunkPos)) {
					data.setDirty();
				}
				touched.put(key, record);
			});
		}
		for (BlockPos pos : chunk.getBlockEntitiesPos()) {
			BlockEntity blockEntity = chunk.getBlockEntity(pos);
			if (blockEntity instanceof RandomizableContainerBlockEntity) {
				ResourceLocation table = lootTableOf(blockEntity.save(new CompoundTag()));
				if (table != null) {
					register(level, data, pos, "b:" + pos.asLong(), table, touched);
				}
			}
		}
		touched.forEach((key, record) -> decideIfReady(level, data, key, record));
	}

	public static void onEntityLoad(Entity entity, ServerLevel level) {
		if (entity instanceof MinecartChest cart) {
			ResourceLocation table = lootTableOf(cart.saveWithoutId(new CompoundTag()));
			if (table != null) {
				StructureData data = StructureData.get(level);
				Map<String, StructureRecord> touched = new HashMap<>();
				register(level, data, cart.blockPosition(), "e:" + cart.getUUID(), table, touched);
				touched.forEach((key, record) -> decideIfReady(level, data, key, record));
			}
		}
	}

	private static void register(ServerLevel level, StructureData data, BlockPos pos, String chestId, ResourceLocation table,
			Map<String, StructureRecord> touched) {
		if (!isTracked(table, EffirromplusConfig.get()) || data.recordOfChest(chestId) != null) {
			return;
		}
		for (StructureFeature<?> feature : FEATURES) {
			StructureStart<?> start = level.structureFeatureManager().getStructureAt(pos, true, feature);
			if (start.isValid()) {
				String key = structureKey(start);
				StructureRecord record = data.record(key, chunksOf(start));
				data.registerChest(key, record, chestId);
				touched.put(key, record);
				return;
			}
		}
	}

	public static void onLoot(ResourceLocation tableId, LootContext context, List<ItemStack> drops) {
		EffirromplusConfig config = EffirromplusConfig.get();
		if (!isTracked(tableId, config)) {
			return;
		}
		ServerLevel level = context.getLevel();
		StructureData data = StructureData.get(level);
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
			drops.add(ExtendedBooks.create(book == StructureRecord.UNBREAKING_BOOK));
		}
	}

	private static String chestId(ServerLevel level, StructureData data, LootContext context) {
		Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
		if (origin == null) {
			return null;
		}
		String blockId = "b:" + new BlockPos(origin).asLong();
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
