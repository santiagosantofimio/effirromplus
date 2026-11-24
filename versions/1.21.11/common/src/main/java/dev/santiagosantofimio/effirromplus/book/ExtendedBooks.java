package dev.santiagosantofimio.effirromplus.book;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantments;

public final class ExtendedBooks {
	public static final int UNBREAKING_LEVEL = 4;
	public static final int EFFICIENCY_LEVEL = 6;

	private ExtendedBooks() {
	}

	public static ItemStack create(HolderLookup.Provider registries, boolean unbreaking) {
		ResourceKey<Enchantment> key = unbreaking ? Enchantments.UNBREAKING : Enchantments.EFFICIENCY;
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		book.enchant(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), unbreaking ? UNBREAKING_LEVEL : EFFICIENCY_LEVEL);
		return book;
	}

	public static ItemStack createRandom(HolderLookup.Provider registries, RandomSource random) {
		return create(registries, random.nextBoolean());
	}

	public static boolean isExtendedBook(ItemStack stack) {
		if (!stack.is(Items.ENCHANTED_BOOK)) {
			return false;
		}
		ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
		if (stored == null) {
			return false;
		}
		for (Holder<Enchantment> enchantment : stored.keySet()) {
			int level = stored.getLevel(enchantment);
			if (enchantment.is(Enchantments.UNBREAKING) && level >= UNBREAKING_LEVEL) {
				return true;
			}
			if (enchantment.is(Enchantments.EFFICIENCY) && level >= EFFICIENCY_LEVEL) {
				return true;
			}
		}
		return false;
	}
}
