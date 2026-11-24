package dev.santiagosantofimio.effirromplus.forge;

import com.google.gson.JsonObject;
import dev.santiagosantofimio.effirromplus.platform.GameEvents;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;

public class GameEventsLootModifier extends LootModifier {
	public GameEventsLootModifier(LootItemCondition[] conditions) {
		super(conditions);
	}

	@Override
	protected List<ItemStack> doApply(List<ItemStack> loot, LootContext context) {
		GameEvents.modifyDrops(context.getQueriedLootTableId(), context, loot);
		return loot;
	}

	public static class Serializer extends GlobalLootModifierSerializer<GameEventsLootModifier> {
		@Override
		public GameEventsLootModifier read(ResourceLocation location, JsonObject object, LootItemCondition[] conditions) {
			return new GameEventsLootModifier(conditions);
		}

		@Override
		public JsonObject write(GameEventsLootModifier instance) {
			return makeConditions(instance.conditions);
		}
	}
}
