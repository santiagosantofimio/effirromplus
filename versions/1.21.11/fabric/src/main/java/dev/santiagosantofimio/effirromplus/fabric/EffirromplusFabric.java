package dev.santiagosantofimio.effirromplus.fabric;

import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import dev.santiagosantofimio.effirromplus.platform.GameEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import dev.santiagosantofimio.effirromplus.villager.LibrarianListings;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.fabricmc.loader.api.FabricLoader;

public class EffirromplusFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		EffirromplusMod.init(FabricLoader.getInstance().getConfigDir());
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> table.unwrapKey().ifPresent(key -> GameEvents.modifyDrops(key.identifier(), context, drops)));
		TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, 1, listings -> listings.addAll(LibrarianListings.listings()));
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk) -> GameEvents.chunkLoad(level, chunk));
		ServerEntityEvents.ENTITY_LOAD.register(GameEvents::entityLoad);
	}
}
