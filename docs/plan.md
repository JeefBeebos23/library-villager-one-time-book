# Library Villager One-Time Book — Implementation Plan

**Minecraft version:** 26.1.2 (snapshot)  
**Mappings:** Mojmap  
**Fabric Loader:** 0.19.2  
**Fabric API:** 0.148.0+26.1.2  
**Java:** 25  
**Loom:** 1.16-SNAPSHOT  
**Group / Mod ID:** `com.jeefbeebos23` / `library_villager`

---

## Goal

Librarian villagers offer enchanted book trades that restock indefinitely. This mod replaces those trades with a single one-time "Mystery Book" trade. When the Mystery Book is used in an anvil, it applies a random level-1 enchantment to the item in the left slot and is consumed. The librarian can never restock that trade.

---

## Architecture

### Custom item: `MysteryBookItem`
- Extends `Item`, stack size 1
- Registered at init time via `ModInitializer` using `Registry.register(BuiltInRegistries.ITEM, ...)`
- Held in a constants class `LibraryVillagerItems` (same pattern as `HopperProperties` in rotatable-hoppers)
- Needs an item model JSON + texture (can reuse `enchanted_book` texture with a custom tint/overlay, or just use `enchanted_book`)

### Trade replacement: `VillagerMixin`
- `@Mixin(Villager.class)`
- `@Inject` into `updateMerchantData()` at `TAIL`
- If the villager's profession is `VillagerProfession.LIBRARIAN`, iterate `this.getOffers()` and replace any `MerchantOffer` whose result `.is(Items.ENCHANTED_BOOK)` with a new `MerchantOffer` that has the mystery book as result and `maxUses = 1`
- Use `@Accessor` mixin on `MerchantOffer` to read the private fields `baseCostA`, `costB`, `xp`, `priceMultiplier` needed to reconstruct the offer

### Restock prevention: `MerchantOfferMixin`
- `@Mixin(MerchantOffer.class)`
- `@Inject` into `resetUses()` at `HEAD`, cancellable
- Cancel if `((MerchantOffer)(Object)this).getResult().is(LibraryVillagerItems.MYSTERY_BOOK)`
- This fires whenever `Villager.restock()` tries to reset any offer — our check silently skips mystery book offers

### Anvil handling: `AnvilMenuMixin`
- `@Mixin(AnvilMenu.class)`
- `@Inject` into `createResult()` at `HEAD`, cancellable (with `CallbackInfo`, not returnable — `createResult()` is void)
- `@Shadow` the `inputSlots` field from `ItemCombinerMenu` (parent class)
- Check: if `inputSlots.getItem(1).is(LibraryVillagerItems.MYSTERY_BOOK)` and `inputSlots.getItem(0)` is not empty
- Pick a random level-1 enchantment from the enchantment registry
- Apply it to the item in slot 0, set it as the result, set XP cost
- Cancel vanilla `createResult()` logic
- Also `@Inject` into `onTake()` at `TAIL` to consume the mystery book (set slot 1 to `ItemStack.EMPTY`)

### Accessor: `MerchantOfferAccessor`
- `@Mixin(MerchantOffer.class)` interface-style accessor
- `@Accessor` methods for: `getBaseCostA() -> ItemCost`, `getCostB() -> Optional<ItemCost>`, `getXp() -> int`, `getPriceMultiplier() -> float`
- Needed by `VillagerMixin` to reconstruct the replacement offer

---

## File Structure

```
src/main/java/com/jeefbeebos23/library_villager/
├── LibraryVillager.java           — ModInitializer, registers item
├── LibraryVillagerItems.java      — public static final MYSTERY_BOOK constant
└── mixin/
    ├── MerchantOfferAccessor.java — @Mixin accessor interface for MerchantOffer fields
    ├── VillagerMixin.java         — trade replacement in updateMerchantData()
    ├── MerchantOfferMixin.java    — restock prevention in resetUses()
    └── AnvilMenuMixin.java        — mystery book anvil logic in createResult() + onTake()

src/main/resources/
├── fabric.mod.json
├── library_villager.mixins.json
└── assets/library_villager/
    ├── models/item/mystery_book.json
    └── textures/item/mystery_book.png  (or reuse enchanted_book via parent model)
```

---

## Task 1 — Project scaffold

- [ ] Copy `build.gradle`, `gradle.properties`, `settings.gradle`, `gradlew`, `gradlew.bat`, `gradle/` from rotatable-hoppers
- [ ] Edit `gradle.properties`: set `mod_version=1.0.0`, `archives_base_name=library-villager`, `mod_id=library_villager`
- [ ] Edit `settings.gradle`: set `rootProject.name = "library-villager-one-time-book"`
- [ ] Create `src/main/resources/fabric.mod.json`:
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
    "main": ["com.jeefbeebos23.library_villager.LibraryVillager"]
  },
  "mixins": ["library_villager.mixins.json"],
  "depends": {
    "fabricloader": ">=0.19.0",
    "fabric-api": "*",
    "minecraft": "~26.1.2"
  }
}
```
- [ ] Create `src/main/resources/library_villager.mixins.json`:
```json
{
  "required": true,
  "package": "com.jeefbeebos23.library_villager.mixin",
  "compatibilityLevel": "JAVA_25",
  "mixins": [
    "MerchantOfferAccessor",
    "MerchantOfferMixin",
    "VillagerMixin",
    "AnvilMenuMixin"
  ],
  "injectors": { "defaultRequire": 1 }
}
```
- [ ] Run `.\gradlew.bat build` (expect compile errors — no source yet) — verify Gradle config works: `BUILD FAILED` on compile, not on configuration
- [ ] Commit: `chore: scaffold project`

---

## Task 2 — Item registration

- [ ] Create `LibraryVillagerItems.java`:
```java
package com.jeefbeebos23.library_villager;

import net.minecraft.world.item.Item;

public final class LibraryVillagerItems {
    public static Item MYSTERY_BOOK;
    private LibraryVillagerItems() {}
}
```
- [ ] Create `LibraryVillager.java`:
```java
package com.jeefbeebos23.library_villager;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;

public class LibraryVillager implements ModInitializer {
    public static final String MOD_ID = "library_villager";

    @Override
    public void onInitialize() {
        LibraryVillagerItems.MYSTERY_BOOK = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "mystery_book"),
            new Item(new Item.Properties().stacksTo(1))
        );
    }
}
```
- [ ] Create `src/main/resources/assets/library_villager/models/item/mystery_book.json`:
```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "minecraft:item/enchanted_book"
  }
}
```
  (No custom texture needed for MVP — reuses vanilla enchanted book look)
- [ ] Run `.\gradlew.bat build` — expect compile errors only from missing mixin classes
- [ ] Commit: `feat: register MysteryBook item`

---

## Task 3 — MerchantOffer accessor

- [ ] Create `MerchantOfferAccessor.java`:
```java
package com.jeefbeebos23.library_villager.mixin;

import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Optional;

@Mixin(MerchantOffer.class)
public interface MerchantOfferAccessor {
    @Accessor("baseCostA")
    ItemCost getBaseCostA();

    @Accessor("costB")
    Optional<ItemCost> getCostB();

    @Accessor("xp")
    int getXp();

    @Accessor("priceMultiplier")
    float getPriceMultiplier();
}
```
  Note: Field names are Mojmap. If names don't compile, check the decompiled `MerchantOffer` class in the Loom cache for exact field names.
- [ ] Run `.\gradlew.bat build` — should compile (only accessor, no logic yet)
- [ ] Commit: `feat: add MerchantOffer accessor`

---

## Task 4 — Restock prevention

- [ ] Create `MerchantOfferMixin.java`:
```java
package com.jeefbeebos23.library_villager.mixin;

import com.jeefbeebos23.library_villager.LibraryVillagerItems;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantOffer.class)
public class MerchantOfferMixin {

    @Inject(method = "resetUses", at = @At("HEAD"), cancellable = true)
    private void preventMysteryBookRestock(CallbackInfo ci) {
        if (((MerchantOffer)(Object)this).getResult().is(LibraryVillagerItems.MYSTERY_BOOK)) {
            ci.cancel();
        }
    }
}
```
- [ ] Run `.\gradlew.bat build` — should compile
- [ ] Commit: `feat: prevent mystery book trade from restocking`

---

## Task 5 — Trade replacement

- [ ] Create `VillagerMixin.java`:
```java
package com.jeefbeebos23.library_villager.mixin;

import com.jeefbeebos23.library_villager.LibraryVillagerItems;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
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

    @Inject(method = "updateMerchantData", at = @At("TAIL"))
    private void replaceBookTradesWithMysteryBook(CallbackInfo ci) {
        Villager villager = (Villager)(Object)this;
        if (villager.getVillagerData().getProfession() != VillagerProfession.LIBRARIAN) return;

        MerchantOffers offers = villager.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (!offer.getResult().is(Items.ENCHANTED_BOOK)) continue;

            MerchantOfferAccessor acc = (MerchantOfferAccessor) offer;
            MerchantOffer replacement = new MerchantOffer(
                acc.getBaseCostA(),
                acc.getCostB(),
                new ItemStack(LibraryVillagerItems.MYSTERY_BOOK),
                1,                        // maxUses = 1
                acc.getXp(),
                acc.getPriceMultiplier()
            );
            offers.set(i, replacement);
        }
    }
}
```
  Note: Verify exact method name `updateMerchantData` at compile time. If it doesn't exist, the mixin will fail with `@Inject requires 1` — search the decompiled Villager class for a void method called when profession/level changes that calls `setOffers()` or similar.
- [ ] Run `.\gradlew.bat build` — should compile
- [ ] Commit: `feat: replace enchanted book trades with mystery book (maxUses=1)`

---

## Task 6 — Anvil handling

- [ ] Create `AnvilMenuMixin.java`:
```java
package com.jeefbeebos23.library_villager.mixin;

import com.jeefbeebos23.library_villager.LibraryVillagerItems;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentLevelEntry;
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

    @Shadow protected abstract Level getLevel();

    protected AnvilMenuMixin(Object type, int syncId, Object playerInventory, Object input) {
        super(null, syncId, null, null);
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void handleMysteryBook(CallbackInfo ci) {
        ItemStack target = this.inputSlots.getItem(0);
        ItemStack book   = this.inputSlots.getItem(1);
        if (!book.is(LibraryVillagerItems.MYSTERY_BOOK)) return;
        if (target.isEmpty()) return;

        Level level = this.getLevel();
        Registry<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Holder.Reference<Enchantment>> candidates = registry.listElements()
            .filter(h -> h.value().getMaxLevel() == 1)
            .collect(Collectors.toList());

        if (candidates.isEmpty()) return;

        Holder<Enchantment> chosen = candidates.get(level.random.nextInt(candidates.size()));
        ItemStack result = target.copy();
        EnchantmentHelper.updateEnchantments(result, mutable -> mutable.set(chosen, 1));

        this.resultSlots.setItem(0, result);
        this.cost.set(1);
        ci.cancel();
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void consumeMysteryBook(net.minecraft.world.entity.player.Player player, ItemStack stack, CallbackInfo ci) {
        if (this.inputSlots.getItem(1).is(LibraryVillagerItems.MYSTERY_BOOK)) {
            this.inputSlots.setItem(1, ItemStack.EMPTY);
        }
    }
}
```
  Notes:
  - `EnchantmentHelper.updateEnchantments` is the Mojmap method for modifying enchantments on a copy. If it doesn't compile, look for `addEnchantment(Holder, int)` on `ItemStack` or use `EnchantedBookItem.addEnchantment()` on the result stack.
  - `this.cost` is the XP cost `IntDataHolder` field on `ItemCombinerMenu` — verify field name at compile time.
  - The dummy constructor is required because `AnvilMenu` extends `ItemCombinerMenu` — use `@Pseudo` or verify the right super-constructor signature.
  - `inputSlots` and `resultSlots` are `protected` fields from `ItemCombinerMenu`.
- [ ] Run `.\gradlew.bat build` — fix any compile errors against the actual class structure
- [ ] Commit: `feat: apply random level-1 enchantment from Mystery Book in anvil`

---

## Task 7 — Build, deploy, push

- [ ] `$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.3.9-hotspot"; .\gradlew.bat build`
- [ ] Copy JAR: `Copy-Item build\libs\library-villager-1.0.0.jar "$env:APPDATA\.minecraft\mods\" -Force`
- [ ] Test in-game: find a librarian, trade for the mystery book, use it in an anvil
- [ ] Copy JAR to project root: `Copy-Item build\libs\library-villager-1.0.0.jar . -Force`
- [ ] Set up `download` orphan branch (same pattern as other mods): JAR + README + .gitignore only
- [ ] Commit source on `master`, push both branches

---

## Known unknowns to verify at compile time

1. **`Villager.updateMerchantData()`** — exact Mojmap method name for when a librarian's trade offers are (re)generated. Search `Villager.class` for `setOffers` or `VillagerTrades.PROFESSION_TO_LEVELED_TRADE` usage.
2. **`MerchantOffer` field names** — `baseCostA`, `costB`, `xp`, `priceMultiplier` are the expected Mojmap names but verify against the decompiled class.
3. **`AnvilMenu.createResult()`** — verify method exists with this exact name (Mojmap); in Yarn it is `updateResult()`.
4. **`EnchantmentHelper.updateEnchantments()`** — verify the correct API for setting a new enchantment on an existing stack's enchantment data component.
5. **`ItemCombinerMenu` field visibility** — `inputSlots`, `resultSlots`, `cost` should be `protected`; if not, use `@Shadow` accessors.
