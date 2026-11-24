package dev.santiagosantofimio.effirromplus.test;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import dev.santiagosantofimio.effirromplus.loot.BookDrops;
import dev.santiagosantofimio.effirromplus.platform.GameEvents;
import dev.santiagosantofimio.effirromplus.structure.StructureData;
import dev.santiagosantofimio.effirromplus.villager.LibrarianListings;
import java.util.List;
import java.util.Optional;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class LoaderBridgeTests {
	public static final String NAME = "loader_bridge";

	private LoaderBridgeTests() {
	}

	private static Holder<Enchantment> enchantment(ServerLevel level, ResourceKey<Enchantment> key) {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	private static LootContext chestContext(ServerLevel level, long seed) {
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
		return new LootContext.Builder(params).withOptionalRandomSource(RandomSource.create(seed)).create(Optional.empty());
	}

	private static boolean exceedsVanilla(ItemStack stack) {
		ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		for (ItemEnchantments group : List.of(stack.getEnchantments(), stored)) {
			for (Holder<Enchantment> holder : group.keySet()) {
				int level = group.getLevel(holder);
				if (holder.is(Enchantments.UNBREAKING) && level > 3 || holder.is(Enchantments.EFFICIENCY) && level > 5) {
					return true;
				}
			}
		}
		return false;
	}

	private static int levelOf(ItemStack stack, ServerLevel level, ResourceKey<Enchantment> key) {
		ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		return Math.max(stored.getLevel(enchantment(level, key)), stack.getEnchantments().getLevel(enchantment(level, key)));
	}

	private static ItemStack book(ServerLevel level, ResourceKey<Enchantment> key, int enchantLevel) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		book.enchant(enchantment(level, key), enchantLevel);
		return book;
	}

	@SuppressWarnings("removal")
	private static ItemStack anvil(GameTestHelper helper, ItemStack left, ItemStack right) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), BlockPos.ZERO));
		menu.getSlot(0).set(left);
		menu.getSlot(1).set(right);
		menu.createResult();
		return menu.getSlot(2).getItem();
	}


	private static boolean librarianOffersExtendedBook(ServerLevel level, GameTestHelper helper, int villagers) {
		int found = 0;
		for (int i = 0; i < villagers; i++) {
			Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			villager.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
			villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.LIBRARIAN).withLevel(1));
			for (MerchantOffer offer : villager.getOffers()) {
				if (ExtendedBooks.isExtendedBook(offer.getResult())) {
					int price = offer.getItemCostA().count();
					helper.assertTrue(price >= 30 && price <= 64, "librarian book price " + price);
					helper.assertTrue(offer.getItemCostA().item().value() == Items.EMERALD, "librarian book is not paid in emeralds");
					helper.assertTrue(offer.getItemCostB().isPresent() && offer.getItemCostB().get().item().value() == Items.BOOK, "librarian book does not also want a book");
					helper.assertValueEqual(offer.getMaxUses(), 12, "librarian book uses");
					helper.assertValueEqual(offer.getXp(), 5, "librarian book xp");
					found++;
				}
			}
		}
		return found > 0;
	}

	public static void run(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		EffirromplusConfig.use(EffirromplusConfig.defaults());

		int[] chunkLoads = {0};
		int[] cartLoads = {0};
		GameEvents.CHUNK_LOAD.register((loaded, chunk) -> chunkLoads[0]++);
		GameEvents.ENTITY_LOAD.register((entity, loaded) -> cartLoads[0] += entity instanceof MinecartChest ? 1 : 0);

		helper.assertValueEqual(enchantment(level, Enchantments.UNBREAKING).value().getMaxLevel(), 4, "unbreaking maximum");
		helper.assertValueEqual(enchantment(level, Enchantments.EFFICIENCY).value().getMaxLevel(), 6, "efficiency maximum");
		helper.assertValueEqual(enchantment(level, Enchantments.SHARPNESS).value().getMaxLevel(), 5, "sharpness maximum");

		HolderLookup.RegistryLookup<Enchantment> all = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		for (ItemStack target : List.of(new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.BOOK))) {
			for (int cost = 1; cost <= 60; cost++) {
				for (EnchantmentInstance instance : EnchantmentHelper.getAvailableEnchantmentResults(cost, target, all.listElements().map(reference -> (Holder<Enchantment>) reference))) {
					helper.assertTrue(!(instance.enchantment().is(Enchantments.UNBREAKING) && instance.level() > 3), "MIXIN table: unbreaking " + instance.level());
					helper.assertTrue(!(instance.enchantment().is(Enchantments.EFFICIENCY) && instance.level() > 5), "MIXIN table: efficiency " + instance.level());
				}
			}
		}

		var function = EnchantRandomlyFunction.randomEnchantment()
				.withOneOf(HolderSet.direct(enchantment(level, Enchantments.UNBREAKING), enchantment(level, Enchantments.EFFICIENCY)))
				.allowingIncompatibleEnchantments()
				.build();
		LootContext context = chestContext(level, 5L);
		boolean reachedUnbreakingIII = false;
		boolean reachedEfficiencyV = false;
		for (int i = 0; i < 2000; i++) {
			ItemStack result = function.apply(new ItemStack(Items.BOOK), context);
			helper.assertTrue(!exceedsVanilla(result), "MIXIN random loot: " + result.get(DataComponents.STORED_ENCHANTMENTS));
			reachedUnbreakingIII |= levelOf(result, level, Enchantments.UNBREAKING) == 3;
			reachedEfficiencyV |= levelOf(result, level, Enchantments.EFFICIENCY) == 5;
		}
		helper.assertTrue(reachedUnbreakingIII && reachedEfficiencyV, "MIXIN random loot never reached the vanilla maximums");

		helper.assertValueEqual(levelOf(anvil(helper, book(level, Enchantments.EFFICIENCY, 5), book(level, Enchantments.EFFICIENCY, 5)), level, Enchantments.EFFICIENCY), 6, "anvil efficiency V + V");
		helper.assertValueEqual(levelOf(anvil(helper, book(level, Enchantments.UNBREAKING, 3), book(level, Enchantments.UNBREAKING, 3)), level, Enchantments.UNBREAKING), 4, "anvil unbreaking III + III");
		helper.assertValueEqual(levelOf(anvil(helper, book(level, Enchantments.EFFICIENCY, 6), book(level, Enchantments.EFFICIENCY, 6)), level, Enchantments.EFFICIENCY), 6, "anvil efficiency VI + VI");

		JsonObject values = EffirromplusConfig.defaults();
		values.getAsJsonObject("endCities").addProperty("chancePerChest", 1.0);
		EffirromplusConfig.use(values);
		BlockPos chestPos = new BlockPos(1, 1, 1);
		helper.setBlock(chestPos, Blocks.CHEST);
		ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
		chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, BookDrops.END_CITY_TREASURE), 7L);
		chest.unpackLootTable(null);
		boolean chestHasBook = false;
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			chestHasBook |= ExtendedBooks.isExtendedBook(chest.getItem(slot));
		}
		helper.assertTrue(chestHasBook, "MODIFY_DROPS: the end city chest got no book");
		EffirromplusConfig.use(EffirromplusConfig.defaults());

		boolean registered = false;
		for (VillagerTrades.ItemListing listing : VillagerTrades.TRADES.get(VillagerProfession.LIBRARIAN).get(1)) {
			registered |= LibrarianListings.listings().stream().anyMatch(ours -> ours.getClass() == listing.getClass());
		}
		helper.assertTrue(registered, "TRADES: the loader did not add the librarian listings");
		if (!level.enabledFeatures().contains(FeatureFlags.TRADE_REBALANCE)) {
			helper.assertTrue(librarianOffersExtendedBook(level, helper, 400), "TRADES: no novice librarian offered an extended book");
		}

		helper.assertTrue(level.getDataStorage().computeIfAbsent(StructureData.TYPE) != null, "SavedData: structure data unavailable");

		BlockPos far = helper.absolutePos(new BlockPos(0, 0, 0)).offset(2_000_000, 0, 2_000_000);
		level.getChunk(far.getX() >> 4, far.getZ() >> 4);
		helper.assertTrue(chunkLoads[0] > 0, "CHUNK_LOAD never fired for a new chunk");

		MinecartChest cart = EntityType.CHEST_MINECART.create(level, EntitySpawnReason.COMMAND);
		cart.setPos(helper.absoluteVec(new Vec3(3.5, 1.0, 3.5)));
		level.addFreshEntity(cart);
		helper.assertTrue(cartLoads[0] > 0, "ENTITY_LOAD never fired for a chest minecart");
		cart.discard();

		helper.succeed();
	}
}
