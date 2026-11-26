package dev.santiagosantofimio.effirromplus.test;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

public class EffirromplusClientGameTests implements FabricClientGameTest {
	private static List<String> tooltipOf(ClientGameTestContext context, int slot) {
		List<String> lines = new ArrayList<>();
		context.runOnClient(client -> {
			ItemStack stack = client.player.getInventory().getItem(slot);
			for (Component line : stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.Default.NORMAL)) {
				lines.add(line.getString());
			}
		});
		return lines;
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getServer().runCommand("gamemode survival @a");
			singleplayer.getServer().runCommand("give @a minecraft:enchanted_book[stored_enchantments={\"minecraft:unbreaking\":4}] 1");
			singleplayer.getServer().runCommand("give @a minecraft:enchanted_book[stored_enchantments={\"minecraft:efficiency\":6}] 1");
			context.waitTicks(220);

			List<String> unbreaking = tooltipOf(context, 0);
			List<String> efficiency = tooltipOf(context, 1);
			if (unbreaking.stream().noneMatch(line -> line.contains("Unbreaking IV"))) {
				throw new AssertionError("client tooltip for the first book: " + unbreaking);
			}
			if (efficiency.stream().noneMatch(line -> line.contains("Efficiency VI"))) {
				throw new AssertionError("client tooltip for the second book: " + efficiency);
			}

			context.getInput().pressKey(options -> options.keyHotbarSlots[0]);
			context.waitTicks(10);
			context.takeScreenshot("effirromplus_unbreaking_iv_book");
			context.getInput().pressKey(options -> options.keyHotbarSlots[1]);
			context.waitTicks(10);
			context.takeScreenshot("effirromplus_efficiency_vi_book");
		}
	}
}
