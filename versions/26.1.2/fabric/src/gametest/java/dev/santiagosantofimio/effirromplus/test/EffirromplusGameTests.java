package dev.santiagosantofimio.effirromplus.test;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import dev.santiagosantofimio.effirromplus.loot.BookDrops;
import dev.santiagosantofimio.effirromplus.structure.StructureBooks;
import dev.santiagosantofimio.effirromplus.structure.StructureData;
import dev.santiagosantofimio.effirromplus.structure.StructureRecord;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;

public class EffirromplusGameTests {
	private static Holder<Enchantment> enchantment(ServerLevel level, ResourceKey<Enchantment> key) {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	private static LootContext chestContext(ServerLevel level, RandomSource random) {
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
		return new LootContext.Builder(params).withOptionalRandomSource(random).create(Optional.empty());
	}

	private static boolean exceedsVanilla(ItemStack stack) {
		ItemEnchantments enchantments = stack.getEnchantments();
		ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		for (ItemEnchantments group : List.of(enchantments, stored)) {
			for (Holder<Enchantment> holder : group.keySet()) {
				int level = group.getLevel(holder);
				if (holder.is(Enchantments.UNBREAKING) && level > 3 || holder.is(Enchantments.EFFICIENCY) && level > 5) {
					return true;
				}
			}
		}
		return false;
	}

	private static ItemStack anvil(GameTestHelper helper, ItemStack left, ItemStack right) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), BlockPos.ZERO));
		menu.getSlot(0).set(left);
		menu.getSlot(1).set(right);
		menu.createResult();
		return menu.getSlot(2).getItem();
	}

	private static ItemStack book(ServerLevel level, ResourceKey<Enchantment> key, int enchantLevel) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		book.enchant(enchantment(level, key), enchantLevel);
		return book;
	}

	private static int levelOf(ItemStack stack, ServerLevel level, ResourceKey<Enchantment> key) {
		ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		return Math.max(stored.getLevel(enchantment(level, key)), stack.getEnchantments().getLevel(enchantment(level, key)));
	}

	@GameTest
	public void onlyTheTwoMaximumsChange(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertValueEqual(enchantment(level, Enchantments.UNBREAKING).value().getMaxLevel(), 4, "unbreaking maximum");
		helper.assertValueEqual(enchantment(level, Enchantments.EFFICIENCY).value().getMaxLevel(), 6, "efficiency maximum");
		helper.assertValueEqual(enchantment(level, Enchantments.SHARPNESS).value().getMaxLevel(), 5, "sharpness maximum");
		helper.assertValueEqual(enchantment(level, Enchantments.FORTUNE).value().getMaxLevel(), 3, "fortune maximum");
		helper.assertValueEqual(enchantment(level, Enchantments.PROTECTION).value().getMaxLevel(), 4, "protection maximum");
		helper.assertValueEqual(enchantment(level, Enchantments.MENDING).value().getMaxLevel(), 1, "mending maximum");
		helper.succeed();
	}

	@GameTest
	public void enchantingTableNeverGivesTheNewLevels(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HolderLookup.RegistryLookup<Enchantment> all = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		boolean sawUnbreakingIII = false;
		boolean sawEfficiencyV = false;
		for (ItemStack target : List.of(new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.BOOK))) {
			for (int cost = 1; cost <= 60; cost++) {
				for (EnchantmentInstance instance : EnchantmentHelper.getAvailableEnchantmentResults(cost, target, all.listElements().map(reference -> (Holder<Enchantment>) reference))) {
					int enchantLevel = instance.level();
					helper.assertTrue(!(instance.enchantment().is(Enchantments.UNBREAKING) && enchantLevel > 3), "table offered unbreaking " + enchantLevel);
					helper.assertTrue(!(instance.enchantment().is(Enchantments.EFFICIENCY) && enchantLevel > 5), "table offered efficiency " + enchantLevel);
					sawUnbreakingIII |= instance.enchantment().is(Enchantments.UNBREAKING) && enchantLevel == 3;
					sawEfficiencyV |= instance.enchantment().is(Enchantments.EFFICIENCY) && enchantLevel == 5;
				}
			}
		}
		helper.assertTrue(sawUnbreakingIII, "table never offered unbreaking III");
		helper.assertTrue(sawEfficiencyV, "table never offered efficiency V");
		helper.succeed();
	}

	@GameTest
	public void enchantedToolsFromTheTableStayWithinVanilla(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		RandomSource random = RandomSource.create(11L);
		for (int i = 0; i < 4000; i++) {
			ItemStack tool = new ItemStack(i % 2 == 0 ? Items.DIAMOND_PICKAXE : Items.BOOK);
			ItemStack result = EnchantmentHelper.enchantItem(random, tool, 1 + random.nextInt(60), level.registryAccess(), Optional.empty());
			helper.assertTrue(!exceedsVanilla(result), "table gave " + result.getEnchantments() + " " + result.get(DataComponents.STORED_ENCHANTMENTS));
		}
		helper.succeed();
	}

	@GameTest
	public void randomLootEnchantsStayWithinVanilla(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootContext context = chestContext(level, RandomSource.create(5L));
		var function = EnchantRandomlyFunction.randomEnchantment()
				.withOneOf(HolderSet.direct(enchantment(level, Enchantments.UNBREAKING), enchantment(level, Enchantments.EFFICIENCY)))
				.allowingIncompatibleEnchantments()
				.build();
		boolean sawUnbreakingIII = false;
		boolean sawEfficiencyV = false;
		for (int i = 0; i < 3000; i++) {
			ItemStack result = function.apply(new ItemStack(Items.BOOK), context);
			helper.assertTrue(!exceedsVanilla(result), "random loot gave " + result.get(DataComponents.STORED_ENCHANTMENTS));
			sawUnbreakingIII |= levelOf(result, level, Enchantments.UNBREAKING) == 3;
			sawEfficiencyV |= levelOf(result, level, Enchantments.EFFICIENCY) == 5;
		}
		helper.assertTrue(sawUnbreakingIII && sawEfficiencyV, "random loot never reached the vanilla maximums");
		helper.succeed();
	}

	@GameTest
	public void anvilCombinesUpToTheNewMaximums(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertValueEqual(levelOf(anvil(helper, book(level, Enchantments.EFFICIENCY, 5), book(level, Enchantments.EFFICIENCY, 5)), level, Enchantments.EFFICIENCY), 6, "efficiency V + V");
		helper.assertValueEqual(levelOf(anvil(helper, book(level, Enchantments.UNBREAKING, 3), book(level, Enchantments.UNBREAKING, 3)), level, Enchantments.UNBREAKING), 4, "unbreaking III + III");
		helper.assertValueEqual(levelOf(anvil(helper, book(level, Enchantments.EFFICIENCY, 6), book(level, Enchantments.EFFICIENCY, 6)), level, Enchantments.EFFICIENCY), 6, "efficiency VI + VI");
		helper.assertValueEqual(levelOf(anvil(helper, book(level, Enchantments.UNBREAKING, 4), book(level, Enchantments.UNBREAKING, 4)), level, Enchantments.UNBREAKING), 4, "unbreaking IV + IV");
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		pickaxe.enchant(enchantment(level, Enchantments.EFFICIENCY), 5);
		helper.assertValueEqual(levelOf(anvil(helper, pickaxe, book(level, Enchantments.EFFICIENCY, 5)), level, Enchantments.EFFICIENCY), 6, "pickaxe efficiency V + book V");
		ItemStack plain = new ItemStack(Items.DIAMOND_PICKAXE);
		helper.assertValueEqual(levelOf(anvil(helper, plain, ExtendedBooks.create(level.registryAccess(), false)), level, Enchantments.EFFICIENCY), 6, "efficiency VI book on a pickaxe");
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		helper.assertValueEqual(levelOf(anvil(helper, sword, ExtendedBooks.create(level.registryAccess(), true)), level, Enchantments.UNBREAKING), 4, "unbreaking IV book on a sword");
		helper.succeed();
	}

	@GameTest
	public void librarianTradesAreNoviceOffersWithTheRightPrice(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HolderLookup.RegistryLookup<VillagerTrade> trades = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
		Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(1, 1, 1));
		for (String name : List.of("unbreaking_iv", "efficiency_vi")) {
			Identifier id = Identifier.fromNamespaceAndPath("effirromplus", "librarian/1/" + name);
			Holder<VillagerTrade> trade = trades.getOrThrow(ResourceKey.create(Registries.VILLAGER_TRADE, id));
			helper.assertTrue(deliversTag(level, id), name + " is not added to the level 1 librarian trades by any data pack");
			RandomSource random = RandomSource.create(3L);
			long total = 0;
			int rolls = 20000;
			for (int i = 0; i < rolls; i++) {
				LootParams params = new LootParams.Builder(level)
						.withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
						.withParameter(LootContextParams.THIS_ENTITY, villager)
						.withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
						.create(LootContextParamSets.VILLAGER_TRADE);
				MerchantOffer offer = trade.value().getOffer(new LootContext.Builder(params).withOptionalRandomSource(random).create(Optional.empty()));
				helper.assertTrue(offer != null, name + " produced no offer");
				helper.assertTrue(ExtendedBooks.isExtendedBook(offer.getResult()), name + " result is not the extended book");
				helper.assertTrue(offer.getItemCostA().item().value() == Items.EMERALD, name + " is not paid in emeralds");
				int price = offer.getItemCostA().count();
				helper.assertTrue(price >= 30 && price <= 64, name + " price out of range: " + price);
				helper.assertTrue(offer.getItemCostB().isPresent() && offer.getItemCostB().get().item().value() == Items.BOOK, name + " does not also want a book");
				helper.assertValueEqual(offer.getMaxUses(), 12, name + " uses");
				helper.assertValueEqual(offer.getXp(), 5, name + " xp");
				total += price;
			}
			double average = (double) total / rolls;
			helper.assertTrue(Math.abs(average - 40.0) <= 1.0, name + " average price " + average);
		}
		helper.succeed();
	}

	private static boolean deliversTag(ServerLevel level, Identifier trade) {
		Identifier tagFile = Identifier.withDefaultNamespace("tags/villager_trade/librarian/level_1.json");
		for (Resource resource : level.getServer().getResourceManager().getResourceStack(tagFile)) {
			try (BufferedReader reader = resource.openAsReader()) {
				if (reader.lines().anyMatch(line -> line.contains(trade.toString()))) {
					return true;
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		return false;
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

	private static int books(ServerLevel level, Identifier table, int rolls, long seed) {
		RandomSource random = RandomSource.create(seed);
		int count = 0;
		for (int i = 0; i < rolls; i++) {
			List<ItemStack> drops = new ArrayList<>(List.of(new ItemStack(Items.COD)));
			BookDrops.modify(table, chestContext(level, random), drops);
			count += (int) drops.stream().filter(ExtendedBooks::isExtendedBook).count();
		}
		return count;
	}

	@GameTest
	public void endCityChestsGiveBooksWithTheConfiguredChance(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		try {
			EffirromplusConfig.use(EffirromplusConfig.defaults());
			int rolls = 40000;
			double rate = (double) books(level, BookDrops.END_CITY_TREASURE, rolls, 21L) / rolls;
			helper.assertTrue(Math.abs(rate - 0.10) < 0.01, "end city rate " + rate);
			helper.assertValueEqual(books(level, Identifier.withDefaultNamespace("chests/simple_dungeon"), 2000, 1L), 0, "books in an unrelated chest");
			useConfig("endCities", "enabled", false);
			helper.assertValueEqual(books(level, BookDrops.END_CITY_TREASURE, 2000, 2L), 0, "books with end cities switched off");
			useConfig("endCities", "chancePerChest", 1.0);
			helper.assertValueEqual(books(level, BookDrops.END_CITY_TREASURE, 100, 3L), 100, "books with chance 1");
		} finally {
			EffirromplusConfig.use(EffirromplusConfig.defaults());
		}
		helper.succeed();
	}

	@GameTest
	public void fishingGivesBooksRarelyInsteadOfTheCatch(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		try {
			EffirromplusConfig.use(EffirromplusConfig.defaults());
			int rolls = 400000;
			double rate = (double) books(level, BookDrops.FISHING, rolls, 33L) / rolls;
			helper.assertTrue(Math.abs(rate - 0.003) < 0.0008, "fishing rate " + rate);
			useConfig("fishing", "chancePerCatch", 1.0);
			List<ItemStack> drops = new ArrayList<>(List.of(new ItemStack(Items.COD)));
			BookDrops.modify(BookDrops.FISHING, chestContext(level, RandomSource.create(1L)), drops);
			helper.assertValueEqual(drops.size(), 1, "drops after a book catch");
			helper.assertTrue(ExtendedBooks.isExtendedBook(drops.getFirst()), "the catch was not replaced by a book");
			useConfig("fishing", "enabled", false);
			helper.assertValueEqual(books(level, BookDrops.FISHING, 1000, 4L), 0, "books with fishing switched off");
		} finally {
			EffirromplusConfig.use(EffirromplusConfig.defaults());
		}
		helper.succeed();
	}

	@GameTest
	public void realChestsGetTheEndCityBookWhenTheyFillTheirLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		try {
			useConfig("endCities", "chancePerChest", 1.0);
			BlockPos pos = new BlockPos(1, 1, 1);
			helper.setBlock(pos, Blocks.CHEST);
			ChestBlockEntity chest = helper.getBlockEntity(pos, ChestBlockEntity.class);
			chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, BookDrops.END_CITY_TREASURE), 7L);
			chest.unpackLootTable(null);
			boolean found = false;
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				found |= ExtendedBooks.isExtendedBook(chest.getItem(slot));
			}
			helper.assertTrue(found, "the end city chest did not get a book");
		} finally {
			EffirromplusConfig.use(EffirromplusConfig.defaults());
		}
		helper.succeed();
	}

	@GameTest
	public void structureChestsReceiveTheirPlannedBookOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		EffirromplusConfig.use(EffirromplusConfig.defaults());
		StructureData data = level.getDataStorage().computeIfAbsent(StructureData.TYPE);
		String key = "minecraft:bastion_remnant@" + helper.absolutePos(BlockPos.ZERO).getX() + ",test";
		StructureRecord record = data.record(key, Set.of(1L));
		BlockPos first = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos second = helper.absolutePos(new BlockPos(3, 1, 1));
		data.registerChest(key, record, "b:" + first.asLong());
		data.registerChest(key, record, "b:" + second.asLong());
		record.chunkLoaded(1L);
		helper.assertTrue(record.decideIfReady(new java.util.Random(5), 0.0, 0.0), "structure was not decided");
		String booked = record.bookOf("b:" + first.asLong()) != StructureRecord.NO_BOOK ? "b:" + first.asLong() : "b:" + second.asLong();
		BlockPos bookedPos = booked.equals("b:" + first.asLong()) ? first : second;
		BlockPos plainPos = bookedPos.equals(first) ? second : first;
		Identifier table = Identifier.withDefaultNamespace("chests/bastion_treasure");
		List<ItemStack> withBook = new ArrayList<>();
		StructureBooks.onLoot(table, atChest(level, bookedPos), withBook);
		helper.assertValueEqual(withBook.size(), 1, "drops of the chosen chest");
		helper.assertTrue(ExtendedBooks.isExtendedBook(withBook.getFirst()), "the chosen chest did not get an extended book");
		List<ItemStack> again = new ArrayList<>();
		StructureBooks.onLoot(table, atChest(level, bookedPos), again);
		helper.assertValueEqual(again.size(), 0, "the chosen chest gave a second book");
		List<ItemStack> without = new ArrayList<>();
		StructureBooks.onLoot(table, atChest(level, plainPos), without);
		helper.assertValueEqual(without.size(), 0, "the other chest got a book");
		helper.succeed();
	}

	private static LootContext atChest(ServerLevel level, BlockPos pos) {
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).create(LootContextParamSets.CHEST);
		return new LootContext.Builder(params).withOptionalRandomSource(RandomSource.create(1L)).create(Optional.empty());
	}
}
