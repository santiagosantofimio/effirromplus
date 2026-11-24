package dev.santiagosantofimio.effirromplus.platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public final class GameEvents {
	public static final Event<ModifyDrops> MODIFY_DROPS = new Event<>();
	public static final Event<ChunkLoad> CHUNK_LOAD = new Event<>();
	public static final Event<EntityLoad> ENTITY_LOAD = new Event<>();

	private GameEvents() {
	}

	public static final class Event<T> {
		private final List<T> listeners = new CopyOnWriteArrayList<>();

		public void register(T listener) {
			listeners.add(listener);
		}

		public List<T> listeners() {
			return listeners;
		}
	}

	public interface ModifyDrops {
		void modify(Identifier tableId, LootContext context, List<ItemStack> drops);
	}

	public interface ChunkLoad {
		void onLoad(ServerLevel level, LevelChunk chunk);
	}

	public interface EntityLoad {
		void onLoad(Entity entity, ServerLevel level);
	}

	public static void chunkLoad(ServerLevel level, LevelChunk chunk) {
		CHUNK_LOAD.listeners().forEach(listener -> listener.onLoad(level, chunk));
	}

	public static void entityLoad(Entity entity, ServerLevel level) {
		ENTITY_LOAD.listeners().forEach(listener -> listener.onLoad(entity, level));
	}

	public static void modifyDrops(Identifier tableId, LootContext context, List<ItemStack> drops) {
		MODIFY_DROPS.listeners().forEach(listener -> listener.modify(tableId, context, drops));
	}
}
