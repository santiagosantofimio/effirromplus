package dev.santiagosantofimio.effirromplus.mixin;

import net.minecraft.world.item.enchantment.DiggingEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DiggingEnchantment.class)
public class DiggingEnchantmentMixin {
	@Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
	private void raiseMaximum(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(6);
	}
}
