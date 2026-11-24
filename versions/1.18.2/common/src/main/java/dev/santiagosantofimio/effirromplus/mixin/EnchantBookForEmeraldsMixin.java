package dev.santiagosantofimio.effirromplus.mixin;

import dev.santiagosantofimio.effirromplus.enchantment.VanillaLevels;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$EnchantBookForEmeralds")
public class EnchantBookForEmeraldsMixin {
	@Redirect(
			method = "getOffer",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I")
	)
	private int vanillaMaxLevel(Enchantment enchantment) {
		return VanillaLevels.cap(enchantment);
	}
}
