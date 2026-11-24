package dev.santiagosantofimio.effirromplus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.santiagosantofimio.effirromplus.villager.PriceDistribution;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PriceDistributionTest {
	private static final int ROLLS = 2_000_000;

	@Test
	void defaultsNeverLeaveTheRangeAndAverageForty() {
		PriceDistribution prices = new PriceDistribution(30, 64, 40);
		Random random = new Random(7);
		long total = 0;
		int lowest = Integer.MAX_VALUE;
		int highest = Integer.MIN_VALUE;
		for (int i = 0; i < ROLLS; i++) {
			int price = prices.sample(random::nextDouble);
			lowest = Math.min(lowest, price);
			highest = Math.max(highest, price);
			total += price;
		}
		assertTrue(lowest >= 30, "lowest " + lowest);
		assertTrue(highest <= 64, "highest " + highest);
		assertEquals(40.0, (double) total / ROLLS, 1.0);
	}

	@Test
	void anyAverageInsideTheRangeIsReached() {
		for (int average : new int[] {35, 47, 55}) {
			PriceDistribution prices = new PriceDistribution(30, 64, average);
			Random random = new Random(average);
			long total = 0;
			for (int i = 0; i < ROLLS; i++) {
				total += prices.sample(random::nextDouble);
			}
			assertEquals(average, (double) total / ROLLS, 1.0);
		}
	}

	@Test
	void equalMinimumAndMaximumGiveAConstantPrice() {
		PriceDistribution prices = new PriceDistribution(20, 20, 20);
		assertEquals(20, prices.sample(() -> 0.37));
	}
}
