package com.jeefbeebos23.library_villager.mixin;

import com.jeefbeebos23.library_villager.LibraryVillagerItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public class VillagerMixin {

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void replaceBookTradesWithMysteryBook(ServerLevel level, CallbackInfo ci) {
        Villager villager = (Villager)(Object)this;
        if (!villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) return;

        MerchantOffers offers = villager.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (!offer.getResult().is(Items.ENCHANTED_BOOK)) continue;

            MerchantOffer replacement = new MerchantOffer(
                offer.getItemCostA(),
                offer.getItemCostB(),
                new ItemStack(LibraryVillagerItems.MYSTERY_BOOK),
                1,
                offer.getXp(),
                offer.getPriceMultiplier()
            );
            offers.set(i, replacement);
        }
    }
}
