package dev.santiagosantofimio.effirromplus.neoforge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.santiagosantofimio.effirromplus.platform.GameEvents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class GameEventsLootModifier extends LootModifier {
	public static final MapCodec<GameEventsLootModifier> CODEC =
			RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(instance, GameEventsLootModifier::new));

	public GameEventsLootModifier(LootItemCondition[] conditions, int priority) {
		super(conditions, priority);
	}

	@Override
	protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		GameEvents.modifyDrops(context.getQueriedLootTableId(), context, generatedLoot);
		return generatedLoot;
	}

	@Override
	public MapCodec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
}
