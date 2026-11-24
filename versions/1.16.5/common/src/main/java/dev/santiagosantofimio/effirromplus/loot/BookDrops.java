package dev.santiagosantofimio.effirromplus.loot;

import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public final class BookDrops {
	public static final ResourceLocation END_CITY_TREASURE = new ResourceLocation("chests/end_city_treasure");
	public static final ResourceLocation FISHING = new ResourceLocation("gameplay/fishing");

	private BookDrops() {
	}

	public static void modify(ResourceLocation tableId, LootContext context, List<ItemStack> drops) {
		EffirromplusConfig config = EffirromplusConfig.get();
		if (tableId.equals(END_CITY_TREASURE)) {
			if (config.endCitiesEnabled() && context.getRandom().nextDouble() < config.endCityChancePerChest()) {
				drops.add(ExtendedBooks.createRandom(context.getRandom()));
			}
		} else if (tableId.equals(FISHING)) {
			if (config.fishingEnabled() && context.getRandom().nextDouble() < config.fishingChancePerCatch()) {
				drops.clear();
				drops.add(ExtendedBooks.createRandom(context.getRandom()));
			}
		}
	}
}
