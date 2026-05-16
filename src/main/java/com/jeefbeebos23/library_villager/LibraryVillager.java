package com.jeefbeebos23.library_villager;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class LibraryVillager implements ModInitializer {
    public static final String MOD_ID = "library_villager";

    @Override
    public void onInitialize() {
        LibraryVillagerItems.MYSTERY_BOOK = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "mystery_book"),
            new Item(new Item.Properties().stacksTo(1))
        );
    }
}
