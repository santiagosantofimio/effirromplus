package dev.santiagosantofimio.effirromplus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.santiagosantofimio.effirromplus.enchantment.VanillaLevels;
import org.junit.jupiter.api.Test;

class VanillaLevelsTest {
	@Test
	void unbreakingIsCappedAtThree() {
		assertEquals(3, VanillaLevels.cap(VanillaLevels.UNBREAKING_KEY, 4));
	}

	@Test
	void efficiencyIsCappedAtFive() {
		assertEquals(5, VanillaLevels.cap(VanillaLevels.EFFICIENCY_KEY, 6));
	}

	@Test
	void otherEnchantmentsKeepTheirMaximum() {
		assertEquals(5, VanillaLevels.cap("enchantment.minecraft.sharpness", 5));
		assertEquals(3, VanillaLevels.cap("enchantment.minecraft.fortune", 3));
	}

	@Test
	void aLowerMaximumIsNeverRaised() {
		assertEquals(2, VanillaLevels.cap(VanillaLevels.UNBREAKING_KEY, 2));
	}
}
