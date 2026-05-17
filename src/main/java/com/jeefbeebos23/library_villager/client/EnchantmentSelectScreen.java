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
    // Panel inner width: 9 slots × 18px + 7px padding each side = 176px (matches vanilla chest width)
    private static final int PANEL_WIDTH = COLS * SLOT_SIZE + 14;
    private static final int PANEL_HEIGHT = VISIBLE_ROWS * SLOT_SIZE + 34;

    // Vanilla-style slot colors
    private static final int COLOR_PANEL      = 0xFFC6C6C6;
    private static final int COLOR_BORDER_LT  = 0xFFFFFFFF; // raised panel top/left highlight
    private static final int COLOR_BORDER_DK  = 0xFF555555; // raised panel bottom/right shadow
    private static final int COLOR_SLOT       = 0xFF8B8B8B; // normal slot interior
    private static final int COLOR_SLOT_HOVER = 0xFF9999BB; // hovered slot interior
    private static final int COLOR_SLOT_LT    = 0xFFFFFFFF; // recessed slot bottom/right
    private static final int COLOR_SLOT_DK    = 0xFF373737; // recessed slot top/left
    private static final int COLOR_FILLER     = 0xFF666666; // empty trailing slot

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
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        extractMenuBackground(g);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        drawPanel(g);

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

                if (entry.enchantmentId() == null && entry.stack().isEmpty()) {
                    drawSlot(g, sx, sy, COLOR_FILLER);
                } else if (entry.enchantmentId() == null) {
                    // Separator glass pane — flat fill matching panel, item rendered on top
                    g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, COLOR_PANEL);
                    g.item(entry.stack(), sx + 1, sy + 1);
                } else {
                    drawSlot(g, sx, sy, hovering ? COLOR_SLOT_HOVER : COLOR_SLOT);
                    g.item(entry.stack(), sx + 1, sy + 1);
                    if (hovering) { hoveredRow = scrollRow + r; hoveredCol = c; }
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

    private void drawPanel(GuiGraphicsExtractor g) {
        // Fill
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_PANEL);
        // Raised border: light top + left, dark bottom + right
        g.fill(panelX, panelY, panelX + PANEL_WIDTH - 1, panelY + 1, COLOR_BORDER_LT);
        g.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT - 1, COLOR_BORDER_LT);
        g.fill(panelX + 1, panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_BORDER_DK);
        g.fill(panelX + PANEL_WIDTH - 1, panelY + 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, COLOR_BORDER_DK);
        // Title
        g.text(font, title, panelX + 8, panelY + 6, 0x404040, false);
    }

    private void drawSlot(GuiGraphicsExtractor g, int x, int y, int fillColor) {
        // Recessed look: dark top+left, light bottom+right
        g.fill(x, y, x + SLOT_SIZE, y + 1, COLOR_SLOT_DK);
        g.fill(x, y + 1, x + 1, y + SLOT_SIZE, COLOR_SLOT_DK);
        g.fill(x + 1, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, COLOR_SLOT_LT);
        g.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE - 1, COLOR_SLOT_LT);
        // Interior
        g.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, fillColor);
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
