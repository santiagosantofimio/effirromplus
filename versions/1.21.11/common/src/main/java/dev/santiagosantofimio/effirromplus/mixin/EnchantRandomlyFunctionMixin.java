package dev.santiagosantofimio.effirromplus.mixin;

import dev.santiagosantofimio.effirromplus.enchantment.VanillaLevels;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantRandomlyFunction.class)
public class EnchantRandomlyFunctionMixin {
	@Redirect(
			method = "enchantItem",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I")
	)
	private static int vanillaMaxLevel(Enchantment enchantment) {
		return VanillaLevels.cap(enchantment);
	}
}
