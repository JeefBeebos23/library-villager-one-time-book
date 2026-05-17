package com.jeefbeebos23.library_villager.client;

import com.jeefbeebos23.library_villager.EnchantmentCategories;
import com.jeefbeebos23.library_villager.packet.SelectEnchantmentPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class EnchantmentSelectScreen extends Screen {

    private static final int COLS = 9;
    private static final int SLOT_SIZE = 18;
    private static final int VISIBLE_ROWS = 6;
    private static final int PANEL_WIDTH = COLS * SLOT_SIZE + 14;
    private static final int PANEL_HEIGHT = VISIBLE_ROWS * SLOT_SIZE + 34;

    private record SlotEntry(ItemStack stack, Identifier enchantmentId) {}

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
            SlotEntry[] sepRow = new SlotEntry[COLS];
            for (int i = 0; i < COLS; i++) {
                var pane = i == 4 ? category.centerItem() : category.glassPane();
                sepRow[i] = new SlotEntry(new ItemStack(pane), null);
            }
            rows.add(sepRow);

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
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xA0000000);
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFC6C6C6);
        g.text(font, title, panelX + 7, panelY + 6, 0x404040, false);

        int slotAreaTop = panelY + 17;
        int hoveredRow = -1, hoveredCol = -1;

        for (int r = 0; r < VISIBLE_ROWS && (scrollRow + r) < totalRows; r++) {
            SlotEntry[] row = rows.get(scrollRow + r);
            for (int c = 0; c < COLS; c++) {
                int sx = panelX + 7 + c * SLOT_SIZE;
                int sy = slotAreaTop + r * SLOT_SIZE;
                SlotEntry entry = row[c];
                boolean hovering = mouseX >= sx && mouseX < sx + SLOT_SIZE
                                && mouseY >= sy && mouseY < sy + SLOT_SIZE;

                if (entry.stack().isEmpty()) {
                    g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFF666666);
                } else if (entry.enchantmentId() != null) {
                    g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, hovering ? 0xFF9999BB : 0xFF8B8B8B);
                }

                if (!entry.stack().isEmpty()) {
                    g.item(entry.stack(), sx + 1, sy + 1);
                }

                if (hovering && entry.enchantmentId() != null) {
                    hoveredRow = scrollRow + r;
                    hoveredCol = c;
                }
            }
        }

        if (totalRows > VISIBLE_ROWS) {
            String indicator = "scroll ▼";
            g.text(font, indicator,
                panelX + PANEL_WIDTH / 2 - font.width(indicator) / 2,
                panelY + PANEL_HEIGHT - 12, 0x404040, false);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);

        if (hoveredRow >= 0) {
            SlotEntry entry = rows.get(hoveredRow)[hoveredCol];
            g.setTooltipForNextFrame(font, entry.stack(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int delta = scrollY > 0 ? -1 : 1;
        scrollRow = Math.max(0, Math.min(scrollRow + delta, Math.max(0, totalRows - VISIBLE_ROWS)));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int slotAreaTop = panelY + 17;
        double mouseX = event.x();
        double mouseY = event.y();
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
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
