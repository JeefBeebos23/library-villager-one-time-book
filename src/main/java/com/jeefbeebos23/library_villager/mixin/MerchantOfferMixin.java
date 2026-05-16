package com.jeefbeebos23.library_villager.mixin;

import com.jeefbeebos23.library_villager.LibraryVillagerItems;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantOffer.class)
public class MerchantOfferMixin {

    @Inject(method = "resetUses", at = @At("HEAD"), cancellable = true)
    private void preventMysteryBookRestock(CallbackInfo ci) {
        if (((MerchantOffer)(Object)this).getResult().is(LibraryVillagerItems.MYSTERY_BOOK)) {
            ci.cancel();
        }
    }
}
