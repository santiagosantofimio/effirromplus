package dev.santiagosantofimio.effirromplus;

import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import dev.santiagosantofimio.effirromplus.loot.BookDrops;
import dev.santiagosantofimio.effirromplus.platform.GameEvents;
import dev.santiagosantofimio.effirromplus.structure.StructureBooks;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class EffirromplusMod {
	public static final String MOD_ID = "effirromplus";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	private EffirromplusMod() {
	}

	public static void init(Path configDir) {
		EffirromplusConfig.load(configDir);
		GameEvents.MODIFY_DROPS.register(BookDrops::modify);
		GameEvents.MODIFY_DROPS.register(StructureBooks::onLoot);
		GameEvents.CHUNK_LOAD.register(StructureBooks::onChunkLoad);
		GameEvents.ENTITY_LOAD.register(StructureBooks::onEntityLoad);
		LOGGER.info("Effirromplus loaded");
	}
}
