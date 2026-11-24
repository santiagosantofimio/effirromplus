package dev.santiagosantofimio.effirromplus.test;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import dev.santiagosantofimio.effirromplus.loot.BookDrops;
import dev.santiagosantofimio.effirromplus.structure.StructureBooks;
import dev.santiagosantofimio.effirromplus.structure.StructureData;
import dev.santiagosantofimio.effirromplus.structure.StructureRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class EffirromplusGameTests implements FabricGameTest {
	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new GameTestAssertException(message);
		}
	}

	private static void same(Object actual, Object expected, String what) {
		check(expected.equals(actual), what + ": expected " + expected + " but was " + actual);
	}

	private static LootContext chestContext(ServerLevel level, long seed, Vec3 origin) {
		return new LootContext.Builder(level).withRandom(new Random(seed)).withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.CHEST);
	}

	private static boolean exceedsVanilla(ItemStack stack) {
		for (var group : List.of(EnchantmentHelper.getEnchantments(stack), EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack)))) {
			if (group.getOrDefault(Enchantments.UNBREAKING, 0) > 3 || group.getOrDefault(Enchantments.BLOCK_EFFICIENCY, 0) > 5) {
				return true;
			}
		}
		return false;
	}

	private static int levelOf(ItemStack stack, Enchantment enchantment) {
		int stored = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack)).getOrDefault(enchantment, 0);
		return Math.max(stored, EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack));
	}

	private static ItemStack book(Enchantment enchantment, int level) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, level));
		return book;
	}

	private static ItemStack anvil(ServerLevel level, ItemStack left, ItemStack right) {
		ServerPlayer player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.nameUUIDFromBytes("effirromplus-test".getBytes()), "effirromplus-test"));
		AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(level, BlockPos.ZERO));
		menu.getSlot(0).set(left);
		menu.getSlot(1).set(right);
		menu.createResult();
		return menu.getSlot(2).getItem();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void onlyTheTwoMaximumsChange(GameTestHelper helper) {
		same(Enchantments.UNBREAKING.getMaxLevel(), 4, "unbreaking maximum");
		same(Enchantments.BLOCK_EFFICIENCY.getMaxLevel(), 6, "efficiency maximum");
		same(Enchantments.SHARPNESS.getMaxLevel(), 5, "sharpness maximum");
		same(Enchantments.BLOCK_FORTUNE.getMaxLevel(), 3, "fortune maximum");
		same(Enchantments.ALL_DAMAGE_PROTECTION.getMaxLevel(), 4, "protection maximum");
		same(Enchantments.MENDING.getMaxLevel(), 1, "mending maximum");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void enchantingTableNeverGivesTheNewLevels(GameTestHelper helper) {
		boolean sawUnbreakingIII = false;
		boolean sawEfficiencyV = false;
		for (ItemStack target : List.of(new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.BOOK))) {
			for (int cost = 1; cost <= 60; cost++) {
				for (EnchantmentInstance instance : EnchantmentHelper.getAvailableEnchantmentResults(cost, target, true)) {
					check(!(instance.enchantment == Enchantments.UNBREAKING && instance.level > 3), "table offered unbreaking " + instance.level);
					check(!(instance.enchantment == Enchantments.BLOCK_EFFICIENCY && instance.level > 5), "table offered efficiency " + instance.level);
					sawUnbreakingIII |= instance.enchantment == Enchantments.UNBREAKING && instance.level == 3;
					sawEfficiencyV |= instance.enchantment == Enchantments.BLOCK_EFFICIENCY && instance.level == 5;
				}
			}
		}
		check(sawUnbreakingIII, "table never offered unbreaking III");
		check(sawEfficiencyV, "table never offered efficiency V");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void enchantedToolsFromTheTableStayWithinVanilla(GameTestHelper helper) {
		Random random = new Random(11L);
		for (int i = 0; i < 4000; i++) {
			ItemStack tool = new ItemStack(i % 2 == 0 ? Items.DIAMOND_PICKAXE : Items.BOOK);
			ItemStack result = EnchantmentHelper.enchantItem(random, tool, 1 + random.nextInt(60), true);
			check(!exceedsVanilla(result), "table gave " + EnchantmentHelper.getEnchantments(result) + " " + EnchantedBookItem.getEnchantments(result));
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void randomLootEnchantsStayWithinVanilla(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootContext context = chestContext(level, 5L, Vec3.ZERO);
		var function = EnchantRandomlyFunction.randomEnchantment().withEnchantment(Enchantments.UNBREAKING).withEnchantment(Enchantments.BLOCK_EFFICIENCY).build();
		boolean sawUnbreakingIII = false;
		boolean sawEfficiencyV = false;
		for (int i = 0; i < 3000; i++) {
			ItemStack result = function.apply(new ItemStack(Items.BOOK), context);
			check(!exceedsVanilla(result), "random loot gave " + EnchantedBookItem.getEnchantments(result));
			sawUnbreakingIII |= levelOf(result, Enchantments.UNBREAKING) == 3;
			sawEfficiencyV |= levelOf(result, Enchantments.BLOCK_EFFICIENCY) == 5;
		}
		check(sawUnbreakingIII && sawEfficiencyV, "random loot never reached the vanilla maximums");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anvilCombinesUpToTheNewMaximums(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		same(levelOf(anvil(level, book(Enchantments.BLOCK_EFFICIENCY, 5), book(Enchantments.BLOCK_EFFICIENCY, 5)), Enchantments.BLOCK_EFFICIENCY), 6, "efficiency V + V");
		same(levelOf(anvil(level, book(Enchantments.UNBREAKING, 3), book(Enchantments.UNBREAKING, 3)), Enchantments.UNBREAKING), 4, "unbreaking III + III");
		same(levelOf(anvil(level, book(Enchantments.BLOCK_EFFICIENCY, 6), book(Enchantments.BLOCK_EFFICIENCY, 6)), Enchantments.BLOCK_EFFICIENCY), 6, "efficiency VI + VI");
		same(levelOf(anvil(level, book(Enchantments.UNBREAKING, 4), book(Enchantments.UNBREAKING, 4)), Enchantments.UNBREAKING), 4, "unbreaking IV + IV");
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		pickaxe.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
		same(levelOf(anvil(level, pickaxe, book(Enchantments.BLOCK_EFFICIENCY, 5)), Enchantments.BLOCK_EFFICIENCY), 6, "pickaxe efficiency V + book V");
		same(levelOf(anvil(level, new ItemStack(Items.DIAMOND_PICKAXE), ExtendedBooks.create(false)), Enchantments.BLOCK_EFFICIENCY), 6, "efficiency VI book on a pickaxe");
		same(levelOf(anvil(level, new ItemStack(Items.DIAMOND_SWORD), ExtendedBooks.create(true)), Enchantments.UNBREAKING), 4, "unbreaking IV book on a sword");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void noviceLibrariansOfferTheBooksAtTheRightPrice(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int found = 0;
		for (int i = 0; i < 400; i++) {
			Villager villager = EntityType.VILLAGER.create(level);
			villager.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
			villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.LIBRARIAN).setLevel(1));
			for (MerchantOffer offer : villager.getOffers()) {
				if (ExtendedBooks.isExtendedBook(offer.getResult())) {
					int price = offer.getBaseCostA().getCount();
					check(price >= 30 && price <= 64, "librarian book price " + price);
					check(offer.getBaseCostA().is(Items.EMERALD), "librarian book is not paid in emeralds");
					check(offer.getCostB().is(Items.BOOK), "librarian book does not also want a book");
					same(offer.getMaxUses(), 12, "librarian book uses");
					same(offer.getXp(), 5, "librarian book xp");
					found++;
				}
			}
		}
		check(found > 0, "no novice librarian offered an extended book");
		helper.succeed();
	}

	private static void useConfig(String section, String key, Object value) {
		JsonObject values = EffirromplusConfig.defaults();
		if (value instanceof Boolean flag) {
			values.getAsJsonObject(section).addProperty(key, flag);
		} else {
			values.getAsJsonObject(section).addProperty(key, (Number) value);
		}
		EffirromplusConfig.use(values);
	}

	private static int books(ServerLevel level, ResourceLocation table, int rolls, long seed) {
		Random random = new Random(seed);
		int count = 0;
		for (int i = 0; i < rolls; i++) {
			List<ItemStack> drops = new ArrayList<>(List.of(new ItemStack(Items.COD)));
			BookDrops.modify(table, chestContext(level, random.nextLong(), Vec3.ZERO), drops);
			count += (int) drops.stream().filter(ExtendedBooks::isExtendedBook).count();
		}
		return count;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void endCityChestsGiveBooksWithTheConfiguredChance(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		try {
			EffirromplusConfig.use(EffirromplusConfig.defaults());
			int rolls = 40000;
			double rate = (double) books(level, BookDrops.END_CITY_TREASURE, rolls, 21L) / rolls;
			check(Math.abs(rate - 0.10) < 0.01, "end city rate " + rate);
			same(books(level, new ResourceLocation("chests/simple_dungeon"), 2000, 1L), 0, "books in an unrelated chest");
			useConfig("endCities", "enabled", false);
			same(books(level, BookDrops.END_CITY_TREASURE, 2000, 2L), 0, "books with end cities switched off");
			useConfig("endCities", "chancePerChest", 1.0);
			same(books(level, BookDrops.END_CITY_TREASURE, 100, 3L), 100, "books with chance 1");
		} finally {
			EffirromplusConfig.use(EffirromplusConfig.defaults());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void fishingGivesBooksRarelyInsteadOfTheCatch(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		try {
			EffirromplusConfig.use(EffirromplusConfig.defaults());
			int rolls = 400000;
			double rate = (double) books(level, BookDrops.FISHING, rolls, 33L) / rolls;
			check(Math.abs(rate - 0.003) < 0.0008, "fishing rate " + rate);
			useConfig("fishing", "chancePerCatch", 1.0);
			List<ItemStack> drops = new ArrayList<>(List.of(new ItemStack(Items.COD)));
			BookDrops.modify(BookDrops.FISHING, chestContext(level, 1L, Vec3.ZERO), drops);
			same(drops.size(), 1, "drops after a book catch");
			check(ExtendedBooks.isExtendedBook(drops.get(0)), "the catch was not replaced by a book");
			useConfig("fishing", "enabled", false);
			same(books(level, BookDrops.FISHING, 1000, 4L), 0, "books with fishing switched off");
		} finally {
			EffirromplusConfig.use(EffirromplusConfig.defaults());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void realChestsGetTheEndCityBookWhenTheyFillTheirLoot(GameTestHelper helper) {
		try {
			useConfig("endCities", "chancePerChest", 1.0);
			BlockPos pos = new BlockPos(1, 1, 1);
			helper.setBlock(pos, Blocks.CHEST);
			ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(pos);
			chest.clearContent();
			chest.setLootTable(BookDrops.END_CITY_TREASURE, 7L);
			chest.unpackLootTable(null);
			boolean found = false;
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				found |= ExtendedBooks.isExtendedBook(chest.getItem(slot));
			}
			check(found, "the end city chest did not get a book");
		} finally {
			EffirromplusConfig.use(EffirromplusConfig.defaults());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void structureChestsReceiveTheirPlannedBookOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		EffirromplusConfig.use(EffirromplusConfig.defaults());
		StructureData data = StructureData.get(level);
		String key = "minecraft:bastion_remnant@" + System.nanoTime() + ",test";
		StructureRecord record = data.record(key, Set.of(1L));
		int offset = (int) (System.nanoTime() & 0xFFFFF) * 8;
		BlockPos first = new BlockPos(offset, 60, 5);
		BlockPos second = new BlockPos(offset + 2, 60, 5);
		data.registerChest(key, record, "b:" + first.asLong());
		data.registerChest(key, record, "b:" + second.asLong());
		record.chunkLoaded(1L);
		check(record.decideIfReady(new Random(5), 0.0, 0.0), "structure was not decided");
		BlockPos bookedPos = record.bookOf("b:" + first.asLong()) != StructureRecord.NO_BOOK ? first : second;
		BlockPos plainPos = bookedPos.equals(first) ? second : first;
		ResourceLocation table = new ResourceLocation("chests/bastion_treasure");
		List<ItemStack> withBook = new ArrayList<>();
		StructureBooks.onLoot(table, chestContext(level, 1L, Vec3.atCenterOf(bookedPos)), withBook);
		same(withBook.size(), 1, "drops of the chosen chest");
		check(ExtendedBooks.isExtendedBook(withBook.get(0)), "the chosen chest did not get an extended book");
		List<ItemStack> again = new ArrayList<>();
		StructureBooks.onLoot(table, chestContext(level, 1L, Vec3.atCenterOf(bookedPos)), again);
		same(again.size(), 0, "the chosen chest gave a second book");
		List<ItemStack> without = new ArrayList<>();
		StructureBooks.onLoot(table, chestContext(level, 1L, Vec3.atCenterOf(plainPos)), without);
		same(without.size(), 0, "the other chest got a book");
		helper.succeed();
	}
}
