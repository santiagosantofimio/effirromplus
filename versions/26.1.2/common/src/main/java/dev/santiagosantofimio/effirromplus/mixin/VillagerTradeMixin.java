package dev.santiagosantofimio.effirromplus.mixin;

import dev.santiagosantofimio.effirromplus.villager.LibrarianOffers;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerTrade.class)
public class VillagerTradeMixin {
	@Inject(method = "getOffer", at = @At("RETURN"), cancellable = true)
	private void adjustExtendedBookOffer(LootContext lootContext, CallbackInfoReturnable<MerchantOffer> cir) {
		cir.setReturnValue(LibrarianOffers.adjust(cir.getReturnValue(), lootContext.getRandom()));
	}
}
