package dev.santiagosantofimio.effirromplus.forge.test;

import dev.santiagosantofimio.effirromplus.EffirromplusMod;
import dev.santiagosantofimio.effirromplus.test.LoaderBridgeTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(EffirromplusMod.MOD_ID)
public final class EffirromplusForgeTests {
	private EffirromplusForgeTests() {
	}

	@GameTest(template = "empty", timeoutTicks = 400)
	public static void loaderBridge(GameTestHelper helper) {
		LoaderBridgeTests.run(helper);
	}
}
