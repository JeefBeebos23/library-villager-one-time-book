# Mystery Book GUI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the anvil-based random enchantment mechanic with a vanilla-style scrollable GUI that lets players pick any level-1 enchantment from a categorized grid.

**Architecture:** `MysteryBookItem.use()` opens `EnchantmentSelectScreen` on the client; clicking a book slot sends `SelectEnchantmentPayload` to the server; the server validates and swaps the Mystery Book in the player's hand with a standard `ENCHANTED_BOOK` carrying `STORED_ENCHANTMENTS` at level 1. `AnvilMenuMixin` is deleted entirely.

**Tech Stack:** Minecraft 26.1.2 (Mojmap), Fabric Loader 0.19.2, Fabric API 0.148.0+26.1.2, Java 25, Gradle

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `src/main/java/com/jeefbeebos23/library_villager/EnchantmentCategories.java` | Static list of 9 categories with glass pane item, center item, and enchantment ID strings |
| Create | `src/main/java/com/jeefbeebos23/library_villager/packet/SelectEnchantmentPayload.java` | `CustomPacketPayload` record carrying the chosen enchantment `Identifier` |
| Create | `src/main/java/com/jeefbeebos23/library_villager/MysteryBookItem.java` | `Item` subclass; `use()` opens the screen on client side |
| Create | `src/main/java/com/jeefbeebos23/library_villager/client/EnchantmentSelectScreen.java` | Client-only GUI — 9-column scrollable grid with separator rows |
| Create | `src/main/java/com/jeefbeebos23/library_villager/client/LibraryVillagerClient.java` | `ClientModInitializer` — registers client packet receiver; exposes `openEnchantScreen()` |
| Modify | `src/main/java/com/jeefbeebos23/library_villager/LibraryVillagerItems.java` | Change `MYSTERY_BOOK` field type from `Item` to `MysteryBookItem` |
| Modify | `src/main/java/com/jeefbeebos23/library_villager/LibraryVillager.java` | Register payload type + server packet handler; instantiate `MysteryBookItem` |
| Modify | `src/main/resources/fabric.mod.json` | Add `client` entrypoint for `LibraryVillagerClient` |
| Delete | `src/main/java/com/jeefbeebos23/library_villager/mixin/AnvilMenuMixin.java` | Entire anvil mechanic replaced |
| Modify | `src/main/resources/library_villager.mixins.json` | Remove `AnvilMenuMixin` from mixins list |

---

## Task 1: EnchantmentCategories

**Files:**
- Create: `src/main/java/com/jeefbeebos23/library_villager/EnchantmentCategories.java`

- [ ] **Step 1: Create the file**

```java
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
```

- [ ] **Step 2: Verify it compiles**

```
cd c:\Users\wbgui\coding_projects\mc_mods\library-villager-one-time-book
.\gradlew.bat classes 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```
git add src/main/java/com/jeefbeebos23/library_villager/EnchantmentCategories.java
git commit -m "feat: add EnchantmentCategories static data"
```

---

## Task 2: SelectEnchantmentPayload + server registration

**Files:**
- Create: `src/main/java/com/jeefbeebos23/library_villager/packet/SelectEnchantmentPayload.java`
- Modify: `src/main/java/com/jeefbeebos23/library_villager/LibraryVillager.java`
- Modify: `src/main/java/com/jeefbeebos23/library_villager/LibraryVillagerItems.java`

- [ ] **Step 1: Create `SelectEnchantmentPayload.java`**

Create the directory `src/main/java/com/jeefbeebos23/library_villager/packet/` first.

```java
package com.jeefbeebos23.library_villager.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectEnchantmentPayload(Identifier enchantmentId) implements CustomPacketPayload {

    public static final Type<SelectEnchantmentPayload> TYPE =
        CustomPacketPayload.createType("library_villager:select_enchantment");

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectEnchantmentPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.enchantmentId().toString()),
            buf -> new SelectEnchantmentPayload(Identifier.parse(buf.readUtf()))
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

- [ ] **Step 2: Update `LibraryVillagerItems.java` to use `MysteryBookItem` type**

Change the field type from `Item` to `MysteryBookItem` (the class doesn't exist yet, but we're preparing the field; it will compile once Task 3 creates `MysteryBookItem`):

```java
package com.jeefbeebos23.library_villager;

public final class LibraryVillagerItems {
    public static MysteryBookItem MYSTERY_BOOK;
    private LibraryVillagerItems() {}
}
```

- [ ] **Step 3: Update `LibraryVillager.java`** to register the payload type and server handler

Replace the entire file:

```java
package com.jeefbeebos23.library_villager;

import com.jeefbeebos23.library_villager.packet.SelectEnchantmentPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.component.DataComponents;

public class LibraryVillager implements ModInitializer {
    public static final String MOD_ID = "library_villager";

    @Override
    public void onInitialize() {
        LibraryVillagerItems.MYSTERY_BOOK = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "mystery_book"),
            new MysteryBookItem(new MysteryBookItem.Properties().stacksTo(1))
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
```

Note: This won't compile until Task 3 creates `MysteryBookItem.java`.

- [ ] **Step 4: Commit (after Task 3 compiles)**

Wait — commit this together with Task 3 since `MysteryBookItem` must exist first. Skip standalone commit here; commit after Task 3 Step 3 instead.

---

## Task 3: MysteryBookItem

**Files:**
- Create: `src/main/java/com/jeefbeebos23/library_villager/MysteryBookItem.java`

- [ ] **Step 1: Create `MysteryBookItem.java`**

```java
package com.jeefbeebos23.library_villager;

import com.jeefbeebos23.library_villager.client.LibraryVillagerClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

    @Environment(EnvType.CLIENT)
    private static void openScreenClient() {
        LibraryVillagerClient.openEnchantScreen();
    }
}
```

Note: `LibraryVillagerClient.openEnchantScreen()` doesn't exist yet; it will be created in Task 5. The `@Environment(EnvType.CLIENT)` annotation ensures this method body — and the `LibraryVillagerClient` class reference — never loads on a dedicated server.

- [ ] **Step 2: Compile to verify Tasks 2 and 3 together**

```
.\gradlew.bat classes 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` (once `LibraryVillagerClient` stub exists from Task 5, or accept a compile error here for `LibraryVillagerClient` — that's resolved in Task 5)

If the compile fails only because `LibraryVillagerClient` is missing, that's expected. Proceed.

- [ ] **Step 3: Commit Tasks 2 and 3 together**

```
git add src/main/java/com/jeefbeebos23/library_villager/packet/SelectEnchantmentPayload.java
git add src/main/java/com/jeefbeebos23/library_villager/MysteryBookItem.java
git add src/main/java/com/jeefbeebos23/library_villager/LibraryVillager.java
git add src/main/java/com/jeefbeebos23/library_villager/LibraryVillagerItems.java
git commit -m "feat: add SelectEnchantmentPayload, MysteryBookItem, server handler"
```

---

## Task 4: EnchantmentSelectScreen

**Files:**
- Create: `src/main/java/com/jeefbeebos23/library_villager/client/EnchantmentSelectScreen.java`

This is the largest file. It builds the scrollable 9-column grid at render time.

- [ ] **Step 1: Create `client/` directory and `EnchantmentSelectScreen.java`**

```java
package com.jeefbeebos23.library_villager.client;

import com.jeefbeebos23.library_villager.EnchantmentCategories;
import com.jeefbeebos23.library_villager.packet.SelectEnchantmentPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class EnchantmentSelectScreen extends Screen {

    private static final int COLS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int VISIBLE_ROWS = 6;
    private static final int PANEL_WIDTH = COLS * SLOT_SIZE + 14; // slots + border padding
    private static final int PANEL_HEIGHT = VISIBLE_ROWS * SLOT_SIZE + 34; // rows + title + footer

    // Each entry is either a separator row (9 slots) or an enchantment slot
    private record SlotEntry(ItemStack stack, Identifier enchantmentId) {}
    // null enchantmentId = non-clickable (separator or empty filler)

    private final List<SlotEntry[]> rows = new ArrayList<>();
    private int scrollRow = 0;
    private int totalRows = 0;

    private int panelX, panelY;

    public EnchantmentSelectScreen() {
        super(Component.literal("Mystery Book — Choose Enchantment"));
    }

    @Override
    protected void init() {
        rows.clear();
        var registryAccess = Minecraft.getInstance().level.registryAccess();
        var enchantRegistry = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);

        for (var category : EnchantmentCategories.ALL) {
            // Separator row
            SlotEntry[] sepRow = new SlotEntry[COLS];
            for (int i = 0; i < COLS; i++) {
                Item pane = i == 4 ? category.centerItem() : category.glassPane();
                sepRow[i] = new SlotEntry(new ItemStack(pane), null);
            }
            rows.add(sepRow);

            // Collect valid enchantment stacks for this category
            List<SlotEntry> enchants = new ArrayList<>();
            for (String id : category.enchantmentIds()) {
                Identifier rid = Identifier.parse(id);
                Optional<Holder.Reference<Enchantment>> holder = enchantRegistry.get(rid);
                if (holder.isEmpty()) continue;
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                mutable.set(holder.get(), 1);
                book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
                enchants.add(new SlotEntry(book, rid));
            }

            // Pack into rows of 9, padding trailing slots with null entries
            for (int i = 0; i < enchants.size(); i += COLS) {
                SlotEntry[] row = new SlotEntry[COLS];
                for (int j = 0; j < COLS; j++) {
                    row[j] = (i + j < enchants.size()) ? enchants.get(i + j) : new SlotEntry(ItemStack.EMPTY, null);
                }
                rows.add(row);
            }
        }

        totalRows = rows.size();
        scrollRow = 0;
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        // Dark background
        g.fill(0, 0, width, height, 0xA0000000);

        // Panel background
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFC6C6C6);

        // Title
        g.drawString(font, title, panelX + 7, panelY + 6, 0x404040, false);

        int slotAreaTop = panelY + 17;

        // Render visible rows
        int hoveredRow = -1, hoveredCol = -1;
        for (int r = 0; r < VISIBLE_ROWS && (scrollRow + r) < totalRows; r++) {
            SlotEntry[] row = rows.get(scrollRow + r);
            for (int c = 0; c < COLS; c++) {
                int sx = panelX + 7 + c * SLOT_SIZE;
                int sy = slotAreaTop + r * SLOT_SIZE;
                SlotEntry entry = row[c];
                boolean hovering = mouseX >= sx && mouseX < sx + SLOT_SIZE
                                && mouseY >= sy && mouseY < sy + SLOT_SIZE;

                // Slot background
                if (entry.stack().isEmpty()) {
                    // Darkened empty filler
                    g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFF666666);
                } else if (entry.enchantmentId() != null) {
                    // Clickable enchant slot
                    g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, hovering ? 0xFF9999BB : 0xFF8B8B8B);
                } else {
                    // Separator — no special bg needed beyond item rendering
                }

                // Item
                if (!entry.stack().isEmpty()) {
                    g.renderItem(entry.stack(), sx + 1, sy + 1);
                }

                if (hovering && entry.enchantmentId() != null) {
                    hoveredRow = scrollRow + r;
                    hoveredCol = c;
                }
            }
        }

        // Scroll indicator (simple text)
        if (totalRows > VISIBLE_ROWS) {
            String indicator = "▲ scroll ▼";
            g.drawString(font, indicator, panelX + PANEL_WIDTH / 2 - font.width(indicator) / 2,
                panelY + PANEL_HEIGHT - 12, 0x404040, false);
        }

        super.render(g, mouseX, mouseY, delta);

        // Tooltip for hovered slot
        if (hoveredRow >= 0) {
            SlotEntry entry = rows.get(hoveredRow)[hoveredCol];
            g.renderTooltip(font, entry.stack(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int delta = scrollY > 0 ? -1 : 1;
        scrollRow = Math.max(0, Math.min(scrollRow + delta, Math.max(0, totalRows - VISIBLE_ROWS)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int slotAreaTop = panelY + 17;
        for (int r = 0; r < VISIBLE_ROWS && (scrollRow + r) < totalRows; r++) {
            SlotEntry[] row = rows.get(scrollRow + r);
            for (int c = 0; c < COLS; c++) {
                int sx = panelX + 7 + c * SLOT_SIZE;
                int sy = slotAreaTop + r * SLOT_SIZE;
                if (mouseX >= sx && mouseX < sx + SLOT_SIZE && mouseY >= sy && mouseY < sy + SLOT_SIZE) {
                    SlotEntry entry = row[c];
                    if (entry.enchantmentId() != null) {
                        ClientPlayNetworking.send(new SelectEnchantmentPayload(entry.enchantmentId()));
                        onClose();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
```

- [ ] **Step 2: Commit**

```
git add src/main/java/com/jeefbeebos23/library_villager/client/EnchantmentSelectScreen.java
git commit -m "feat: add EnchantmentSelectScreen scrollable GUI"
```

---

## Task 5: LibraryVillagerClient + fabric.mod.json

**Files:**
- Create: `src/main/java/com/jeefbeebos23/library_villager/client/LibraryVillagerClient.java`
- Modify: `src/main/resources/fabric.mod.json`

- [ ] **Step 1: Create `LibraryVillagerClient.java`**

```java
package com.jeefbeebos23.library_villager.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class LibraryVillagerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // No client packet receiver needed — server swaps the item directly.
        // The screen opens from MysteryBookItem.use() on the client.
    }

    public static void openEnchantScreen() {
        Minecraft.getInstance().setScreen(new EnchantmentSelectScreen());
    }
}
```

- [ ] **Step 2: Update `fabric.mod.json`** to add the client entrypoint

Replace the `entrypoints` block only:

```json
{
  "schemaVersion": 1,
  "id": "library_villager",
  "version": "1.0.0",
  "name": "Library Villager One-Time Book",
  "description": "Librarian villagers give a one-time Mystery Book instead of restocking enchanted book trades.",
  "authors": ["JeefBeebos23"],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": ["com.jeefbeebos23.library_villager.LibraryVillager"],
    "client": ["com.jeefbeebos23.library_villager.client.LibraryVillagerClient"]
  },
  "mixins": ["library_villager.mixins.json"],
  "depends": {
    "fabricloader": ">=0.19.0",
    "fabric-api": "*",
    "minecraft": "~26.1.2"
  }
}
```

- [ ] **Step 3: Build the full project**

```
.\gradlew.bat build 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` with a JAR produced in `build/libs/`.

Fix any compilation errors before committing.

- [ ] **Step 4: Commit**

```
git add src/main/java/com/jeefbeebos23/library_villager/client/LibraryVillagerClient.java
git add src/main/resources/fabric.mod.json
git commit -m "feat: add LibraryVillagerClient entrypoint, register client"
```

---

## Task 6: Delete AnvilMenuMixin + update mixins list

**Files:**
- Delete: `src/main/java/com/jeefbeebos23/library_villager/mixin/AnvilMenuMixin.java`
- Modify: `src/main/resources/library_villager.mixins.json`

- [ ] **Step 1: Delete `AnvilMenuMixin.java`**

```
git rm src/main/java/com/jeefbeebos23/library_villager/mixin/AnvilMenuMixin.java
```

- [ ] **Step 2: Remove `AnvilMenuMixin` from `library_villager.mixins.json`**

The updated file:

```json
{
  "required": true,
  "package": "com.jeefbeebos23.library_villager.mixin",
  "compatibilityLevel": "JAVA_25",
  "mixins": [
    "MerchantOfferAccessor",
    "MerchantOfferMixin",
    "VillagerMixin"
  ],
  "injectors": { "defaultRequire": 1 }
}
```

- [ ] **Step 3: Build to confirm clean compile**

```
.\gradlew.bat build 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```
git add src/main/resources/library_villager.mixins.json
git commit -m "chore: delete AnvilMenuMixin, remove from mixin registry"
```

---

## Task 7: Build, deploy, push to GitHub

- [ ] **Step 1: Final build**

```
.\gradlew.bat build 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`. JAR is at `build/libs/library-villager-1.0.0.jar` (or similar).

- [ ] **Step 2: Copy JAR to mods folder**

```powershell
Copy-Item "build\libs\library-villager-1.0.0.jar" "C:\Users\wbgui\AppData\Roaming\.minecraft\mods\" -Force
```

- [ ] **Step 3: Update JAR on download branch**

The repo uses an orphan `download` branch for distributing the built JAR.

```
git checkout download
git rm library-villager-*.jar
Copy-Item "c:\Users\wbgui\coding_projects\mc_mods\library-villager-one-time-book\build\libs\library-villager-1.0.0.jar" .
git add library-villager-1.0.0.jar
git commit -m "release: mystery book GUI v1.1.0"
git checkout main
```

- [ ] **Step 4: Push both branches**

```
git push origin main
git push origin download
```

- [ ] **Step 5: Verify**

Confirm `main` and `download` branches pushed successfully on GitHub.
