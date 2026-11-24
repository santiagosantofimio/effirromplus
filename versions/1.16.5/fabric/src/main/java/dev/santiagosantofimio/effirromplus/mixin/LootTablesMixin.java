package dev.santiagosantofimio.effirromplus.mixin;

import com.google.gson.JsonElement;
import dev.santiagosantofimio.effirromplus.fabric.LootTableIds;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootTables.class)
public class LootTablesMixin {
	@Shadow
	private Map<ResourceLocation, LootTable> tables;

	@Inject(method = "apply", at = @At("RETURN"))
	private void rememberTableIds(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
		LootTableIds.remember(tables);
	}

	@Inject(method = "get", at = @At("RETURN"))
	private void rememberRequestedId(ResourceLocation id, CallbackInfoReturnable<LootTable> cir) {
		LootTableIds.remember(id, cir.getReturnValue());
	}
}
