package dev.santiagosantofimio.effirromplus.fabric;

import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import dev.santiagosantofimio.effirromplus.platform.GameEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.loader.api.FabricLoader;

public class EffirromplusFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		EffirromplusMod.init(FabricLoader.getInstance().getConfigDir());
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> table.unwrapKey().ifPresent(key -> GameEvents.modifyDrops(key.identifier(), context, drops)));
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> GameEvents.chunkLoad(level, chunk));
		ServerEntityEvents.ENTITY_LOAD.register(GameEvents::entityLoad);
	}
}
