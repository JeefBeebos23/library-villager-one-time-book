package com.jeefbeebos23.library_villager;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public final class EnchantmentCategories {

    public record Category(Item glassPane, Item centerItem, List<String> enchantmentIds) {}

    public static final List<Category> ALL = List.of(
        new Category(Items.CYAN_STAINED_GLASS_PANE, Items.IRON_SWORD, List.of(
            "minecraft:sharpness", "minecraft:smite", "minecraft:bane_of_arthropods",
            "minecraft:knockback", "minecraft:fire_aspect", "minecraft:looting",
            "minecraft:sweeping_edge", "minecraft:unbreaking", "minecraft:mending")),
        new Category(Items.ORANGE_STAINED_GLASS_PANE, Items.MACE, List.of(
            "minecraft:density", "minecraft:breach", "minecraft:wind_burst",
            "minecraft:smite", "minecraft:bane_of_arthropods", "minecraft:fire_aspect",
            "minecraft:knockback", "minecraft:looting", "minecraft:unbreaking", "minecraft:mending")),
        new Category(Items.LIME_STAINED_GLASS_PANE, Items.IRON_PICKAXE, List.of(
            "minecraft:efficiency", "minecraft:fortune", "minecraft:silk_touch",
            "minecraft:unbreaking", "minecraft:mending")),
        new Category(Items.BLUE_STAINED_GLASS_PANE, Items.IRON_CHESTPLATE, List.of(
            "minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection",
            "minecraft:projectile_protection", "minecraft:thorns", "minecraft:respiration",
            "minecraft:aqua_affinity", "minecraft:feather_falling", "minecraft:depth_strider",
            "minecraft:frost_walker", "minecraft:soul_speed", "minecraft:swift_sneak",
            "minecraft:unbreaking", "minecraft:mending")),
        new Category(Items.PURPLE_STAINED_GLASS_PANE, Items.BOW, List.of(
            "minecraft:power", "minecraft:punch", "minecraft:flame", "minecraft:infinity",
            "minecraft:unbreaking", "minecraft:mending")),
        new Category(Items.PINK_STAINED_GLASS_PANE, Items.CROSSBOW, List.of(
            "minecraft:multishot", "minecraft:piercing", "minecraft:quick_charge",
            "minecraft:unbreaking", "minecraft:mending")),
        new Category(Items.LIGHT_BLUE_STAINED_GLASS_PANE, Items.TRIDENT, List.of(
            "minecraft:loyalty", "minecraft:impaling", "minecraft:riptide", "minecraft:channeling",
            "minecraft:unbreaking", "minecraft:mending")),
        new Category(Items.BROWN_STAINED_GLASS_PANE, Items.FISHING_ROD, List.of(
            "minecraft:luck_of_the_sea", "minecraft:lure", "minecraft:unbreaking", "minecraft:mending")),
        new Category(Items.RED_STAINED_GLASS_PANE, Items.BARRIER, List.of(
            "minecraft:binding_curse", "minecraft:vanishing_curse"))
    );

    private EnchantmentCategories() {}
}
