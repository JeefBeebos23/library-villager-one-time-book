package com.jeefbeebos23.library_villager.mixin;

import com.jeefbeebos23.library_villager.LibraryVillagerItems;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow private DataSlot cost;

    protected AnvilMenuMixin() {
        super(null, 0, null, null, null);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void handleMysteryBook(CallbackInfo ci) {
        ItemStack target = this.inputSlots.getItem(0);
        ItemStack book = this.inputSlots.getItem(1);
        if (!book.is(LibraryVillagerItems.MYSTERY_BOOK)) return;
        if (target.isEmpty()) return;

        Level level = this.player.level();
        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Holder.Reference<Enchantment>> candidates = registry.listElements()
            .collect(Collectors.toList());

        if (candidates.isEmpty()) return;

        Holder<Enchantment> chosen = candidates.get(level.getRandom().nextInt(candidates.size()));
        ItemStack result = target.copy();
        EnchantmentHelper.updateEnchantments(result, mutable -> mutable.set(chosen, 1));

        this.resultSlots.setItem(0, result);
        this.cost.set(1);
        ci.cancel();
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void consumeMysteryBook(Player player, ItemStack stack, CallbackInfo ci) {
        if (this.inputSlots.getItem(1).is(LibraryVillagerItems.MYSTERY_BOOK)) {
            this.inputSlots.setItem(1, ItemStack.EMPTY);
        }
    }
}
