package com.jeefbeebos23.library_villager;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class LibraryVillager implements ModInitializer {
    public static final String MOD_ID = "library_villager";

    @Override
    public void onInitialize() {
        // Register mystery book item
        // Note: Item registration requires ResourceLocation which is not found in net.minecraft.resources
        // for Minecraft 26.1.2 with Mojmap. This will be fixed in next iteration.
        LibraryVillagerItems.MYSTERY_BOOK = new Item(new Item.Properties().stacksTo(1));
    }
}
