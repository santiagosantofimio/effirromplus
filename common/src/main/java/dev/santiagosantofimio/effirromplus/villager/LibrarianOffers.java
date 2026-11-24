package dev.santiagosantofimio.effirromplus.villager;

import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

public final class LibrarianOffers {
	private LibrarianOffers() {
	}

	public static @Nullable MerchantOffer adjust(@Nullable MerchantOffer offer, RandomSource random) {
		if (offer == null || !ExtendedBooks.isExtendedBook(offer.getResult())) {
			return offer;
		}
		EffirromplusConfig config = EffirromplusConfig.get();
		if (!config.villagersEnabled()) {
			return null;
		}
		PriceDistribution prices = new PriceDistribution(config.minPrice(), config.maxPrice(), config.averagePrice());
		int price = prices.sample(random::nextDouble);
		ItemCost emeralds = new ItemCost(Items.EMERALD, price);
		return new MerchantOffer(emeralds, offer.getItemCostB(), offer.getResult(), config.tradeMaxUses(), offer.getXp(), offer.getPriceMultiplier());
	}
}
