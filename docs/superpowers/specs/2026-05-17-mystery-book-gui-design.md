# Mystery Book GUI — Design Spec

**Mod:** library-villager-one-time-book  
**Minecraft:** 26.1.2 (Mojmap, Fabric Loader 0.19.2, Fabric API 0.148.0+26.1.2, Java 25)  
**Replaces:** AnvilMenuMixin (random anvil enchant mechanic — deleted entirely)

---

## Goal

When a player right-clicks a Mystery Book, a vanilla-style inventory GUI opens listing all enchantments grouped by equipment category. The player clicks their chosen enchantment; the Mystery Book in their hand is replaced with a standard level-1 `ENCHANTED_BOOK` (identical to one found in a chest or traded for). No randomness. No anvil required for the selection step.

---

## Architecture

### Flow

1. Player right-clicks Mystery Book → `MysteryBookItem.use()` fires on both sides
2. Client-side: opens `EnchantmentSelectScreen`
3. Player clicks an enchanted book slot → client sends `SelectEnchantmentPayload` to server
4. Server validates the enchantment exists in the registry, replaces the Mystery Book in the player's active hand with a standard `ENCHANTED_BOOK` with `STORED_ENCHANTMENTS` at level 1
5. Screen closes on client

### Files created

| File | Purpose |
|---|---|
| `MysteryBookItem.java` | Extends `Item`; overrides `use()` to open screen on client side |
| `EnchantmentCategories.java` | Static hardcoded list of categories with color, center item, enchantment IDs |
| `packet/SelectEnchantmentPayload.java` | `CustomPacketPayload` carrying the chosen `Identifier` to the server |
| `client/EnchantmentSelectScreen.java` | The GUI — extends `Screen`, renders the grouped enchantment grid |
| `client/LibraryVillagerClient.java` | `ClientModInitializer` — registers the client packet receiver that opens the screen |

### Files modified

| File | Change |
|---|---|
| `LibraryVillager.java` | Register `SelectEnchantmentPayload` type; register server-side packet handler |
| `LibraryVillagerItems.java` | Change `MYSTERY_BOOK` field type from `Item` to `MysteryBookItem` |
| `fabric.mod.json` | Add `client` entrypoint: `com.jeefbeebos23.library_villager.client.LibraryVillagerClient` |
| `library_villager.mixins.json` | Remove `AnvilMenuMixin` from the mixins list |

### Files deleted

| File | Reason |
|---|---|
| `mixin/AnvilMenuMixin.java` | Entire anvil mechanic is replaced |

---

## EnchantmentCategories

Hardcoded at compile time. Each category has:
- A `DyeColor` for the glass pane separator row
- An `Item` for the center slot of the separator row
- An ordered `List<String>` of Mojang enchantment namespaced IDs

At screen-open time, each ID is looked up in the live registry. IDs not present in the registry (e.g. removed by datapack) are silently skipped — their slot simply does not appear.

### Category table

| Category | Glass | Center item | Enchantment IDs (`minecraft:*`) |
|---|---|---|---|
| Sword | CYAN | `minecraft:wooden_sword` | sharpness, smite, bane_of_arthropods, knockback, fire_aspect, looting, sweeping_edge, unbreaking, mending |
| Mace | ORANGE | `minecraft:mace` | density, breach, wind_burst, smite, bane_of_arthropods, fire_aspect, knockback, looting, unbreaking, mending |
| Tools | LIME | `minecraft:iron_pickaxe` | efficiency, fortune, silk_touch, unbreaking, mending |
| Armor | BLUE | `minecraft:iron_chestplate` | protection, fire_protection, blast_protection, projectile_protection, thorns, respiration, aqua_affinity, feather_falling, depth_strider, frost_walker, soul_speed, swift_sneak, unbreaking, mending |
| Bow | PURPLE | `minecraft:bow` | power, punch, flame, infinity, unbreaking, mending |
| Crossbow | PINK | `minecraft:crossbow` | multishot, piercing, quick_charge, unbreaking, mending |
| Trident | LIGHT_BLUE | `minecraft:trident` | loyalty, impaling, riptide, channeling, unbreaking, mending |
| Fishing Rod | BROWN | `minecraft:fishing_rod` | luck_of_the_sea, lure, unbreaking, mending |
| Curses | RED | `minecraft:barrier` | binding_curse, vanishing_curse |

---

## EnchantmentSelectScreen

Extends `net.minecraft.client.gui.screens.Screen`. Client-side only (`@Environment(EnvType.CLIENT)`).

### Layout

- **Background:** standard inventory texture (`textures/gui/container/inventory.png`) scaled to fit, or a generic dark panel rendered with `GuiGraphics.fill`
- **Scroll area:** 9 columns × visible rows; content scrolls vertically with mouse wheel
- **Row types:**
  - **Separator row:** all 9 slots are glass pane items of the category color; the center slot (index 4) holds the category representative item
  - **Enchant rows:** slots filled left-to-right with rendered `ENCHANTED_BOOK` ItemStacks (with `STORED_ENCHANTMENTS` set so the tooltip shows the enchantment name); unfilled trailing slots in the last row of a category are darkened empty slots
- **Visible height:** 6 rows (fits in a standard double-chest-height panel)
- **Scroll:** mouse wheel moves the view by 1 row; scroll is clamped to valid range
- **Tooltip:** hovering an enchanted book slot shows the standard Minecraft item tooltip (enchantment name)
- **Click:** left-clicking an enchanted book slot sends `SelectEnchantmentPayload` with that enchantment's `Identifier` and closes the screen
- **Escape / close:** closes the screen with no effect

### Rendering approach

Use `GuiGraphics.renderItem()` for all item rendering (glass panes and enchanted books). Use `GuiGraphics.renderTooltip()` on hover. Slot highlight drawn with `GuiGraphics.fill()` on mouse-over.

---

## SelectEnchantmentPayload

```
record SelectEnchantmentPayload(Identifier enchantmentId) implements CustomPacketPayload
```

- `TYPE`: `CustomPacketPayload.Type<SelectEnchantmentPayload>` with id `library_villager:select_enchantment`
- `STREAM_CODEC`: reads/writes a single `Identifier` via `Identifier.STREAM_CODEC`
- Registered in `LibraryVillager.onInitialize()` via `PayloadTypeRegistry.playC2S().register(...)`

---

## Server-side packet handler

Registered in `LibraryVillager.onInitialize()` via `ServerPlayNetworking.registerGlobalReceiver(...)`.

On receipt:
1. Look up the enchantment `Holder` from the server's registry: `serverPlayer.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(enchantmentId)` — if absent, ignore the packet silently
2. Verify the player's active hand holds a Mystery Book — if not, ignore silently (prevents abuse)
3. Build the result: `new ItemStack(Items.ENCHANTED_BOOK)`, then `EnchantmentHelper.updateEnchantments(result, m -> m.set(holder, 1))`
4. Find which hand holds the Mystery Book: check `MAIN_HAND` first, then `OFF_HAND`; replace that hand's item with the result

---

## MysteryBookItem

```java
public class MysteryBookItem extends Item {
    public MysteryBookItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            openScreen(); // delegates to EnvType.CLIENT helper
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
```

The `openScreen()` call is isolated in `LibraryVillagerClient` (or a thin `@Environment(EnvType.CLIENT)` inner helper) to prevent `Minecraft` class from loading on dedicated servers.

---

## What is NOT changing

- `VillagerMixin` — trade replacement still works exactly as before
- `MerchantOfferMixin` — restock prevention unchanged
- `MerchantOfferAccessor` — unchanged (placeholder, stays registered)
- Item registration — same `Identifier`, same stack size; only the class changes from `Item` to `MysteryBookItem`
- Item model JSON — unchanged
