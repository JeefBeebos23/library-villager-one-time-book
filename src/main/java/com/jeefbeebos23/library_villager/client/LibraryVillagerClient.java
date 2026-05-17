package com.jeefbeebos23.library_villager.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class LibraryVillagerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
    }

    public static void openEnchantScreen() {
        Minecraft.getInstance().setScreen(new EnchantmentSelectScreen());
    }
}
