package dev.santiagosantofimio.effirromplus.loot;

import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public final class BookDrops {
	public static final Identifier END_CITY_TREASURE = Identifier.withDefaultNamespace("chests/end_city_treasure");
	public static final Identifier FISHING = Identifier.withDefaultNamespace("gameplay/fishing");

	private BookDrops() {
	}

	public static void modify(Identifier tableId, LootContext context, List<ItemStack> drops) {
		EffirromplusConfig config = EffirromplusConfig.get();
		if (tableId.equals(END_CITY_TREASURE)) {
			if (config.endCitiesEnabled() && context.getRandom().nextDouble() < config.endCityChancePerChest()) {
				drops.add(ExtendedBooks.createRandom(context.getLevel().registryAccess(), context.getRandom()));
			}
		} else if (tableId.equals(FISHING)) {
			if (config.fishingEnabled() && context.getRandom().nextDouble() < config.fishingChancePerCatch()) {
				drops.clear();
				drops.add(ExtendedBooks.createRandom(context.getLevel().registryAccess(), context.getRandom()));
			}
		}
	}
}
