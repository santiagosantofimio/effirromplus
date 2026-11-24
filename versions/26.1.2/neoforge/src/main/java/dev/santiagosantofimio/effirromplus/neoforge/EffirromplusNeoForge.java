package dev.santiagosantofimio.effirromplus.neoforge;

import com.mojang.serialization.MapCodec;
import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import dev.santiagosantofimio.effirromplus.platform.GameEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(EffirromplusMod.MOD_ID)
public class EffirromplusNeoForge {
	private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
			DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, EffirromplusMod.MOD_ID);

	public EffirromplusNeoForge(IEventBus modBus) {
		EffirromplusMod.init(FMLPaths.CONFIGDIR.get());
		LOOT_MODIFIERS.register("game_events", () -> GameEventsLootModifier.CODEC);
		LOOT_MODIFIERS.register(modBus);

		NeoForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
			if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
				GameEvents.chunkLoad(level, chunk);
			}
		});
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				GameEvents.entityLoad(event.getEntity(), level);
			}
		});
	}
}
