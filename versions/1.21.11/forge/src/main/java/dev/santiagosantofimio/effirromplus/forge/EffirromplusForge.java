package dev.santiagosantofimio.effirromplus.forge;

import com.mojang.serialization.MapCodec;
import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import dev.santiagosantofimio.effirromplus.platform.GameEvents;
import dev.santiagosantofimio.effirromplus.villager.LibrarianListings;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(EffirromplusMod.MOD_ID)
public class EffirromplusForge {
	private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
			DeferredRegister.create(ForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, EffirromplusMod.MOD_ID);

	public EffirromplusForge(FMLJavaModLoadingContext context) {
		EffirromplusMod.init(FMLPaths.CONFIGDIR.get());
		LOOT_MODIFIERS.register("game_events", () -> GameEventsLootModifier.CODEC);
		LOOT_MODIFIERS.register(context.getModBusGroup());

		VillagerTradesEvent.BUS.addListener(event -> {
			if (event.getType() == VillagerProfession.LIBRARIAN) {
				event.getTrades().get(1).addAll(LibrarianListings.listings());
			}
		});
		ChunkEvent.Load.BUS.addListener(event -> {
			if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
				GameEvents.chunkLoad(level, chunk);
			}
		});
		EntityJoinLevelEvent.BUS.addListener(Priority.LOWEST, event -> {
			if (event.getLevel() instanceof ServerLevel level) {
				GameEvents.entityLoad(event.getEntity(), level);
			}
		});
	}
}
