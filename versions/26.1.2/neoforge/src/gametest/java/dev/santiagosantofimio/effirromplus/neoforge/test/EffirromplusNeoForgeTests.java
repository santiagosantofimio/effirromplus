package dev.santiagosantofimio.effirromplus.neoforge.test;

import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import dev.santiagosantofimio.effirromplus.test.LoaderBridgeTests;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import dev.santiagosantofimio.effirromplus.test.SelfTest;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(EffirromplusMod.MOD_ID)
public class EffirromplusNeoForgeTests {
	public EffirromplusNeoForgeTests(IEventBus modBus) {
		DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, EffirromplusMod.MOD_ID);
		functions.register(LoaderBridgeTests.NAME, () -> LoaderBridgeTests::run);
		functions.register(modBus);
		if (Boolean.getBoolean("effirromplus.selftest")) {
			SelfTest[] running = {null};
			NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> running[0] = new SelfTest());
			NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
				if (running[0] != null && running[0].tick(event.getServer())) {
					running[0] = null;
					event.getServer().halt(false);
				}
			});
		}
	}
}
