package dev.santiagosantofimio.effirromplus.enchantment;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

public final class VanillaLevels {
	public static final String UNBREAKING_KEY = "enchantment.minecraft.unbreaking";
	public static final String EFFICIENCY_KEY = "enchantment.minecraft.efficiency";

	private static final Map<String, Integer> VANILLA_MAX = Map.of(UNBREAKING_KEY, 3, EFFICIENCY_KEY, 5);

	private VanillaLevels() {
	}

	public static int cap(String descriptionKey, int maxLevel) {
		Integer vanillaMax = VANILLA_MAX.get(descriptionKey);
		return vanillaMax == null ? maxLevel : Math.min(maxLevel, vanillaMax);
	}

	public static int cap(Enchantment enchantment) {
		int maxLevel = enchantment.getMaxLevel();
		if (enchantment.description().getContents() instanceof TranslatableContents translatable) {
			return cap(translatable.getKey(), maxLevel);
		}
		return maxLevel;
	}

	public static void trim(int cost, List<EnchantmentInstance> results) {
		ListIterator<EnchantmentInstance> iterator = results.listIterator();
		while (iterator.hasNext()) {
			EnchantmentInstance instance = iterator.next();
			Enchantment enchantment = instance.enchantment().value();
			int cap = cap(enchantment);
			if (instance.level() <= cap) {
				continue;
			}
			int level = cap;
			while (level >= enchantment.getMinLevel() && !(cost >= enchantment.getMinCost(level) && cost <= enchantment.getMaxCost(level))) {
				level--;
			}
			if (level < enchantment.getMinLevel()) {
				iterator.remove();
			} else {
				iterator.set(new EnchantmentInstance(instance.enchantment(), level));
			}
		}
	}
}
