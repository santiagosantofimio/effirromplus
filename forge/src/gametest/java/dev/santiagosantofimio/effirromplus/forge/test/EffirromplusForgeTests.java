package dev.santiagosantofimio.effirromplus.forge.test;

import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import dev.santiagosantofimio.effirromplus.test.LoaderBridgeTests;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import dev.santiagosantofimio.effirromplus.test.SelfTest;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = EffirromplusMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EffirromplusForgeTests {
	private EffirromplusForgeTests() {
	}

	@SubscribeEvent
	public static void register(RegisterEvent event) {
		startSelfTest();
		event.register(Registries.TEST_FUNCTION, helper -> helper.register(
				Identifier.fromNamespaceAndPath(EffirromplusMod.MOD_ID, LoaderBridgeTests.NAME), (Consumer<GameTestHelper>) LoaderBridgeTests::run));
	}

	private static boolean selfTestStarted;

	private static void startSelfTest() {
		if (selfTestStarted || !Boolean.getBoolean("effirromplus.selftest")) {
			return;
		}
		selfTestStarted = true;
		SelfTest[] running = {null};
		ServerStartedEvent.BUS.addListener(event -> running[0] = new SelfTest());
		TickEvent.ServerTickEvent.Post.BUS.addListener(event -> {
			if (running[0] != null && running[0].tick(event.server())) {
				running[0] = null;
				event.server().halt(false);
			}
		});
	}
}
