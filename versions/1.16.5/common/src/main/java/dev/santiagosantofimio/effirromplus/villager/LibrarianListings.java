package dev.santiagosantofimio.effirromplus.villager;

import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import java.util.List;
import java.util.Random;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

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
		public MerchantOffer getOffer(Entity trader, Random random) {
			EffirromplusConfig config = EffirromplusConfig.get();
			if (!config.villagersEnabled()) {
				return null;
			}
			PriceDistribution prices = new PriceDistribution(config.minPrice(), config.maxPrice(), config.averagePrice());
			ItemStack emeralds = new ItemStack(Items.EMERALD, prices.sample(random::nextDouble));
			return new MerchantOffer(emeralds, new ItemStack(Items.BOOK), ExtendedBooks.create(unbreaking), config.tradeMaxUses(), XP, PRICE_MULTIPLIER);
		}
	}
}
