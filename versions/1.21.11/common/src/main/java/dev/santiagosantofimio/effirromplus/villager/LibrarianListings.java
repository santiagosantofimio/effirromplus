package dev.santiagosantofimio.effirromplus.villager;

import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

public final class LibrarianListings {
	private static final int XP = 5;
	private static final float PRICE_MULTIPLIER = 0.2f;

	private LibrarianListings() {
	}

	public static List<VillagerTrades.ItemListing> listings() {
		return List.of(new BookListing(true), new BookListing(false));
	}

	private record BookListing(boolean unbreaking) implements VillagerTrades.ItemListing {
		@Override
		public @Nullable MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random) {
			EffirromplusConfig config = EffirromplusConfig.get();
			if (!config.villagersEnabled()) {
				return null;
			}
			PriceDistribution prices = new PriceDistribution(config.minPrice(), config.maxPrice(), config.averagePrice());
			ItemCost emeralds = new ItemCost(Items.EMERALD, prices.sample(random::nextDouble));
			return new MerchantOffer(emeralds, Optional.of(new ItemCost(Items.BOOK)), ExtendedBooks.create(level.registryAccess(), unbreaking),
					config.tradeMaxUses(), XP, PRICE_MULTIPLIER);
		}
	}
}
