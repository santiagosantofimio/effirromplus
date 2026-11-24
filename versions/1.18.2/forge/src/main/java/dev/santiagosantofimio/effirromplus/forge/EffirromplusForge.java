package dev.santiagosantofimio.effirromplus.forge;

import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import dev.santiagosantofimio.effirromplus.platform.GameEvents;
import dev.santiagosantofimio.effirromplus.villager.LibrarianListings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(EffirromplusMod.MOD_ID)
public final class EffirromplusForge {
	private static final DeferredRegister<GlobalLootModifierSerializer<?>> LOOT_MODIFIERS =
			DeferredRegister.create(ForgeRegistries.Keys.LOOT_MODIFIER_SERIALIZERS, EffirromplusMod.MOD_ID);

	static {
		LOOT_MODIFIERS.register("game_events", GameEventsLootModifier.Serializer::new);
	}

	public EffirromplusForge() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		LOOT_MODIFIERS.register(modBus);
		EffirromplusMod.init(FMLPaths.CONFIGDIR.get());

		IEventBus bus = MinecraftForge.EVENT_BUS;
		bus.addListener((VillagerTradesEvent event) -> {
			if (event.getType() == VillagerProfession.LIBRARIAN) {
				event.getTrades().get(1).addAll(LibrarianListings.listings());
			}
		});
		bus.addListener((ChunkEvent.Load event) -> {
			if (event.getWorld() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
				GameEvents.chunkLoad(level, chunk);
			}
		});
		bus.addListener(EventPriority.LOWEST, (EntityJoinWorldEvent event) -> {
			if (!event.isCanceled() && event.getWorld() instanceof ServerLevel level) {
				GameEvents.entityLoad(event.getEntity(), level);
			}
		});
	}
}
