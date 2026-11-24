package dev.santiagosantofimio.effirromplus.test;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import dev.santiagosantofimio.effirromplus.book.ExtendedBooks;
import dev.santiagosantofimio.effirromplus.config.EffirromplusConfig;
import dev.santiagosantofimio.effirromplus.loot.BookDrops;
import dev.santiagosantofimio.effirromplus.platform.GameEvents;
import dev.santiagosantofimio.effirromplus.structure.StructureData;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.vehicle.MinecartChest;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class LoaderBridgeTests {
	private LoaderBridgeTests() {
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new GameTestAssertException(message);
		}
	}

	private static void same(Object actual, Object expected, String what) {
		check(expected.equals(actual), what + ": expected " + expected + " but was " + actual);
	}

	private static LootContext chestContext(ServerLevel level, long seed) {
		return new LootContext.Builder(level).withRandom(new Random(seed)).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
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
		return EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack)).getOrDefault(enchantment, 0);
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

	public static void run(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		EffirromplusConfig.use(EffirromplusConfig.defaults());

		int[] chunkLoads = {0};
		int[] cartLoads = {0};
		GameEvents.CHUNK_LOAD.register((loaded, chunk) -> chunkLoads[0]++);
		GameEvents.ENTITY_LOAD.register((entity, loaded) -> cartLoads[0] += entity instanceof MinecartChest ? 1 : 0);

		same(Enchantments.UNBREAKING.getMaxLevel(), 4, "unbreaking maximum");
		same(Enchantments.BLOCK_EFFICIENCY.getMaxLevel(), 6, "efficiency maximum");
		same(Enchantments.SHARPNESS.getMaxLevel(), 5, "sharpness maximum");

		for (ItemStack target : List.of(new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.BOOK))) {
			for (int cost = 1; cost <= 60; cost++) {
				for (EnchantmentInstance instance : EnchantmentHelper.getAvailableEnchantmentResults(cost, target, true)) {
					check(!(instance.enchantment == Enchantments.UNBREAKING && instance.level > 3), "MIXIN table: unbreaking " + instance.level);
					check(!(instance.enchantment == Enchantments.BLOCK_EFFICIENCY && instance.level > 5), "MIXIN table: efficiency " + instance.level);
				}
			}
		}

		var function = EnchantRandomlyFunction.randomEnchantment().withEnchantment(Enchantments.UNBREAKING).withEnchantment(Enchantments.BLOCK_EFFICIENCY).build();
		LootContext context = chestContext(level, 5L);
		boolean reachedUnbreakingIII = false;
		boolean reachedEfficiencyV = false;
		for (int i = 0; i < 2000; i++) {
			ItemStack result = function.apply(new ItemStack(Items.BOOK), context);
			check(!exceedsVanilla(result), "MIXIN random loot: " + EnchantedBookItem.getEnchantments(result));
			reachedUnbreakingIII |= levelOf(result, Enchantments.UNBREAKING) == 3;
			reachedEfficiencyV |= levelOf(result, Enchantments.BLOCK_EFFICIENCY) == 5;
		}
		check(reachedUnbreakingIII && reachedEfficiencyV, "MIXIN random loot never reached the vanilla maximums");

		same(levelOf(anvil(level, book(Enchantments.BLOCK_EFFICIENCY, 5), book(Enchantments.BLOCK_EFFICIENCY, 5)), Enchantments.BLOCK_EFFICIENCY), 6, "anvil efficiency V + V");
		same(levelOf(anvil(level, book(Enchantments.UNBREAKING, 3), book(Enchantments.UNBREAKING, 3)), Enchantments.UNBREAKING), 4, "anvil unbreaking III + III");
		same(levelOf(anvil(level, book(Enchantments.BLOCK_EFFICIENCY, 6), book(Enchantments.BLOCK_EFFICIENCY, 6)), Enchantments.BLOCK_EFFICIENCY), 6, "anvil efficiency VI + VI");

		JsonObject values = EffirromplusConfig.defaults();
		values.getAsJsonObject("endCities").addProperty("chancePerChest", 1.0);
		EffirromplusConfig.use(values);
		BlockPos chestPos = new BlockPos(1, 1, 1);
		helper.setBlock(chestPos, Blocks.CHEST);
		ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(chestPos);
		chest.clearContent();
		chest.setLootTable(BookDrops.END_CITY_TREASURE, 7L);
		chest.unpackLootTable(null);
		boolean chestHasBook = false;
		for (int slot = 0; slot < chest.getContainerSize(); slot++) {
			chestHasBook |= ExtendedBooks.isExtendedBook(chest.getItem(slot));
		}
		check(chestHasBook, "MODIFY_DROPS: the end city chest got no book");
		EffirromplusConfig.use(EffirromplusConfig.defaults());

		int offered = 0;
		for (int i = 0; i < 400; i++) {
			Villager villager = EntityType.VILLAGER.create(level);
			villager.setPos(helper.absoluteVec(new Vec3(1.5, 1.0, 1.5)));
			villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.LIBRARIAN).setLevel(1));
			for (MerchantOffer offer : villager.getOffers()) {
				if (ExtendedBooks.isExtendedBook(offer.getResult())) {
					int price = offer.getBaseCostA().getCount();
					check(price >= 30 && price <= 64, "TRADES: price " + price);
					offered++;
				}
			}
		}
		check(offered > 0, "TRADES: no novice librarian offered an extended book");

		check(StructureData.get(level) != null, "SavedData: structure data unavailable");

		BlockPos far = helper.absolutePos(BlockPos.ZERO).offset(2_000_000, 0, 2_000_000);
		LevelChunk chunk = level.getChunk(far.getX() >> 4, far.getZ() >> 4);
		check(chunk != null && chunkLoads[0] > 0, "CHUNK_LOAD never fired for a new chunk");

		MinecartChest cart = EntityType.CHEST_MINECART.create(level);
		cart.setPos(helper.absoluteVec(new Vec3(3.5, 1.0, 3.5)));
		level.addFreshEntity(cart);
		check(cartLoads[0] > 0, "ENTITY_LOAD never fired for a chest minecart");
		cart.discard();

		helper.succeed();
	}
}
