package dev.santiagosantofimio.effirromplus.forge.test;

import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import dev.santiagosantofimio.effirromplus.test.SelfTest;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = EffirromplusMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SelfTestForge {
	private static SelfTest running;

	private SelfTestForge() {
	}

	@SubscribeEvent
	public static void started(ServerStartedEvent event) {
		if (Boolean.getBoolean("effirromplus.selftest")) {
			running = new SelfTest();
		}
	}

	@SubscribeEvent
	public static void tick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END && running != null && running.tick(ServerLifecycleHooks.getCurrentServer())) {
			running = null;
			ServerLifecycleHooks.getCurrentServer().halt(false);
		}
	}
}
