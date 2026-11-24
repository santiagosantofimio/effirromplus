package dev.santiagosantofimio.effirromplus.book;

import java.util.Map;
import java.util.Random;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;

public final class ExtendedBooks {
	public static final int UNBREAKING_LEVEL = 4;
	public static final int EFFICIENCY_LEVEL = 6;

	private ExtendedBooks() {
	}

	public static ItemStack create(boolean unbreaking) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		Enchantment enchantment = unbreaking ? Enchantments.UNBREAKING : Enchantments.BLOCK_EFFICIENCY;
		EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, unbreaking ? UNBREAKING_LEVEL : EFFICIENCY_LEVEL));
		return book;
	}

	public static ItemStack createRandom(Random random) {
		return create(random.nextBoolean());
	}

	public static boolean isExtendedBook(ItemStack stack) {
		if (!stack.is(Items.ENCHANTED_BOOK)) {
			return false;
		}
		Map<Enchantment, Integer> stored = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack));
		return stored.getOrDefault(Enchantments.UNBREAKING, 0) >= UNBREAKING_LEVEL
				|| stored.getOrDefault(Enchantments.BLOCK_EFFICIENCY, 0) >= EFFICIENCY_LEVEL;
	}
}
