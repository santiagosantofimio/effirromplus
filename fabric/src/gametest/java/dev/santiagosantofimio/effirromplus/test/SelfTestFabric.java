package dev.santiagosantofimio.effirromplus.test;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class SelfTestFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		if (!Boolean.getBoolean("effirromplus.selftest")) {
			return;
		}
		SelfTest[] running = {null};
		ServerLifecycleEvents.SERVER_STARTED.register(server -> running[0] = new SelfTest());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (running[0] != null && running[0].tick(server)) {
				running[0] = null;
				server.halt(false);
			}
		});
	}
}
