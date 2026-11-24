package dev.santiagosantofimio.effirromplus.mixin;

import dev.santiagosantofimio.effirromplus.fabric.LootTableIds;
import dev.santiagosantofimio.effirromplus.platform.GameEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootTable.class)
public abstract class LootTableMixin {
	private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

	@Inject(method = "getRandomItemsRaw", at = @At("HEAD"), cancellable = true)
	private void modifyDrops(LootContext context, Consumer<ItemStack> output, CallbackInfo ci) {
		int[] depth = DEPTH.get();
		if (depth[0] > 0) {
			return;
		}
		ResourceLocation id = LootTableIds.of((LootTable) (Object) this);
		if (id == null) {
			return;
		}
		List<ItemStack> drops = new ArrayList<>();
		depth[0]++;
		try {
			((LootTable) (Object) this).getRandomItemsRaw(context, drops::add);
		} finally {
			depth[0]--;
		}
		GameEvents.modifyDrops(id, context, drops);
		drops.forEach(output);
		ci.cancel();
	}
}
