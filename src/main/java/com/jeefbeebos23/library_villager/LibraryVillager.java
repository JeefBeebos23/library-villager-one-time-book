package com.jeefbeebos23.library_villager;

import com.jeefbeebos23.library_villager.packet.SelectEnchantmentPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class LibraryVillager implements ModInitializer {
    public static final String MOD_ID = "library_villager";

    @Override
    public void onInitialize() {
        ResourceKey<Item> mysteryBookKey = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "mystery_book")
        );
        LibraryVillagerItems.MYSTERY_BOOK = Registry.register(
            BuiltInRegistries.ITEM,
            mysteryBookKey,
            new MysteryBookItem(new MysteryBookItem.Properties().setId(mysteryBookKey).stacksTo(1))
        );

        PayloadTypeRegistry.serverboundPlay().register(
            SelectEnchantmentPayload.TYPE,
            SelectEnchantmentPayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(SelectEnchantmentPayload.TYPE, (payload, context) -> {
            var player = context.player();
            context.server().execute(() -> {
                InteractionHand hand;
                if (player.getMainHandItem().is(LibraryVillagerItems.MYSTERY_BOOK)) {
                    hand = InteractionHand.MAIN_HAND;
                } else if (player.getOffhandItem().is(LibraryVillagerItems.MYSTERY_BOOK)) {
                    hand = InteractionHand.OFF_HAND;
                } else {
                    return;
                }
                player.level().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .get(payload.enchantmentId())
                    .ifPresent(holder -> {
                        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        mutable.set(holder, 1);
                        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
                        player.setItemInHand(hand, book);
                    });
            });
        });
    }
}
