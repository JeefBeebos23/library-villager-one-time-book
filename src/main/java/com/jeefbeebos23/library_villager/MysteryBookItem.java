package com.jeefbeebos23.library_villager;

import com.jeefbeebos23.library_villager.client.LibraryVillagerClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MysteryBookItem extends Item {

    public MysteryBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            openScreenClient();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Environment(EnvType.CLIENT)
    private static void openScreenClient() {
        LibraryVillagerClient.openEnchantScreen();
    }
}
