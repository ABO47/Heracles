package earth.terrarium.heracles.client.widgets.modals;

import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import earth.terrarium.heracles.api.client.WidgetUtils;
import earth.terrarium.heracles.client.widgets.base.BaseModal;
import earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox;
import earth.terrarium.heracles.client.widgets.buttons.ThemedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class EntityVariantLibraryModal extends BaseModal {

    private static final int WIDTH = 420;
    private static final int HEIGHT = 260;
    private static final int ROW_HEIGHT = 18;

    private final EnterableEditBox searchBox;
    private final Button useButton;
    private final Button clearButton;
    private final Consumer<String> onSelect;
    private final EntityType<?> entityType;
    private List<String> allVariants = new ArrayList<>();
    private List<String> filteredVariants = new ArrayList<>();
    private int scroll = 0;
    private int selectedIndex = -1;
    private net.minecraft.world.entity.Entity previewEntity;
    private String previewEntityId = "";
    private long lastRowClickAt = 0L;
    private int lastRowClickIndex = -1;

    public EntityVariantLibraryModal(int screenWidth, int screenHeight, EntityType<?> entityType, String currentVariant, Consumer<String> onSelect) {
        super(screenWidth, screenHeight, WIDTH, HEIGHT, 2);
        this.onSelect = onSelect;
        this.entityType = entityType;
        this.searchBox = addChild(new EnterableEditBox(Minecraft.getInstance().font, this.x + 8, this.y + 8, WIDTH - 40, 14, Component.literal("Search variants...")));
        this.searchBox.setResponder(s -> applyFilter());
        this.searchBox.setEnter(s -> applySelection());
        this.useButton = addChild(ThemedButton.builder(Component.literal("Use"), b -> applySelection())
            .bounds(this.x + WIDTH - 58, this.y + HEIGHT - 20, 50, 14).build());
        this.clearButton = addChild(ThemedButton.builder(Component.literal("Clear"), b -> clearSelection())
            .bounds(this.x + WIDTH - 114, this.y + HEIGHT - 20, 52, 14).build());
        reloadVariants();
        if (currentVariant != null && !currentVariant.isBlank()) {
            int idx = this.filteredVariants.indexOf(currentVariant);
            if (idx >= 0) this.selectedIndex = idx;
        }
        this.useButton.active = this.selectedIndex >= 0;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible) return;
        reloadVariants();
        this.setFocused(this.searchBox);
        this.searchBox.setFocused(true);
        this.searchBox.moveCursorToEnd();
    }

    private void reloadVariants() {
        this.allVariants.clear();
        this.allVariants.add("");
        for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
            if (profession == VillagerProfession.NONE) continue;
            this.allVariants.add(BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession).toString());
        }
        applyFilter();
        this.previewEntity = null;
        this.previewEntityId = "";
    }

    private void applyFilter() {
        String q = this.searchBox.getValue();
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        this.filteredVariants = new ArrayList<>();
        for (String variant : this.allVariants) {
            String name = displayName(variant).toLowerCase(Locale.ROOT);
            if (needle.isBlank() || name.contains(needle)) {
                this.filteredVariants.add(variant);
            }
        }
        if (this.filteredVariants.isEmpty()) this.selectedIndex = -1;
        else if (this.selectedIndex < 0 || this.selectedIndex >= this.filteredVariants.size()) this.selectedIndex = 0;
        int maxScroll = Math.max(0, this.filteredVariants.size() - visibleRows());
        this.scroll = Mth.clamp(this.scroll, 0, maxScroll);
        this.useButton.active = this.selectedIndex >= 0;
    }

    private void applySelection() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.filteredVariants.size()) return;
        this.onSelect.accept(this.filteredVariants.get(this.selectedIndex));
        this.setVisible(false);
    }

    private void clearSelection() {
        this.onSelect.accept("");
        this.setVisible(false);
    }

    private int previewX() {
        return this.x + 8;
    }

    private int previewY() {
        return this.y + 42;
    }

    private int previewW() {
        return 140;
    }

    private int previewH() {
        return HEIGHT - 70;
    }

    private int listX() {
        return previewX() + previewW() + 8;
    }

    private int listY() {
        return previewY();
    }

    private int listW() {
        return WIDTH - (previewW() + 24);
    }

    private int listH() {
        return previewH();
    }

    private int visibleRows() {
        return Math.max(1, listH() / ROW_HEIGHT);
    }

    @Override
    protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        try (CloseablePoseStack pose = new CloseablePoseStack(graphics)) {
            graphics.fill(this.x, this.y, this.x + WIDTH, this.y + HEIGHT, 0xEE121212);
            graphics.fill(this.x + 1, this.y + 1, this.x + WIDTH - 1, this.y + HEIGHT - 1, 0xFF1E1E1E);
            renderChildren(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(this.font, Component.literal("Variant Library"), this.x + 8, this.y + 28, 0xEDEDED, false);

        int px = previewX();
        int py = previewY();
        int pw = previewW();
        int ph = previewH();
        graphics.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xCC1B1B1B);
        graphics.fill(px, py, px + pw, py + ph, 0xAA0D0D0D);

        String selected = this.selectedIndex >= 0 && this.selectedIndex < this.filteredVariants.size() ? this.filteredVariants.get(this.selectedIndex) : "";
        if (Minecraft.getInstance().level != null && this.entityType != null) {
            String currentId = BuiltInRegistries.ENTITY_TYPE.getKey(this.entityType).toString();
            if (this.previewEntity == null || !currentId.equals(this.previewEntityId)) {
                this.previewEntity = this.entityType.create(Minecraft.getInstance().level);
                this.previewEntityId = currentId;
            }
            var entity = this.previewEntity;
            if (entity != null) {
                applyVariant(entity, selected);
                float spin = (System.currentTimeMillis() % 120000L) / 120000.0f * 360.0f;
                WidgetUtils.drawEntityInBox(graphics, px + 6, py + 6, pw - 12, ph - 12, entity, -35.0f + spin);
            }
        }

        int lx = listX();
        int ly = listY();
        int lw = listW();
        int lh = listH();
        graphics.fill(lx - 1, ly - 1, lx + lw + 1, ly + lh + 1, 0xB01A1A1A);
        graphics.fill(lx, ly, lx + lw, ly + lh, 0x8A121212);

        int rows = visibleRows();
        int renderRows = Math.min(rows, Math.max(0, this.filteredVariants.size() - this.scroll));
        for (int i = 0; i < renderRows; i++) {
            int idx = i + this.scroll;
            int rowY = ly + i * ROW_HEIGHT;
            boolean hovered = mouseX >= lx && mouseX < lx + lw && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            boolean selectedRow = idx == this.selectedIndex;
            if (hovered) graphics.fill(lx, rowY, lx + lw, rowY + ROW_HEIGHT - 1, 0x66333333);
            if (selectedRow) graphics.renderOutline(lx, rowY, lw, ROW_HEIGHT - 1, 0xFF8CC9FF);
            graphics.drawString(this.font, displayName(this.filteredVariants.get(idx)), lx + 6, rowY + 4, selectedRow ? 0xFFFFFF : 0xD4D4D4, false);
            graphics.fill(lx + 1, rowY + ROW_HEIGHT - 1, lx + lw - 1, rowY + ROW_HEIGHT, 0x55343434);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible()) return false;
        int lx = listX();
        int ly = listY();
        int lw = listW();
        int lh = listH();
        if (button == 0 && mouseX >= lx && mouseX < lx + lw && mouseY >= ly && mouseY < ly + lh) {
            int idx = (int) ((mouseY - ly) / ROW_HEIGHT) + this.scroll;
            if (idx >= 0 && idx < this.filteredVariants.size()) {
                this.selectedIndex = idx;
                this.useButton.active = true;
                long now = System.currentTimeMillis();
                if (idx == this.lastRowClickIndex && now - this.lastRowClickAt < 250L) {
                    applySelection();
                    return true;
                }
                this.lastRowClickIndex = idx;
                this.lastRowClickAt = now;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (!isVisible()) return false;
        int max = Math.max(0, this.filteredVariants.size() - visibleRows());
        if (max <= 0) return false;
        this.scroll = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(scrollAmount)));
        return true;
    }

    private static void applyVariant(net.minecraft.world.entity.Entity entity, String variantId) {
        if (variantId == null || variantId.isBlank()) return;
        ResourceLocation id = ResourceLocation.tryParse(variantId);
        if (id == null) return;
        VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.getOptional(id).orElse(null);
        if (profession == null) return;
        if (entity instanceof Villager villager) {
            villager.setVillagerData(villager.getVillagerData().setProfession(profession));
        } else if (entity instanceof ZombieVillager zombieVillager) {
            zombieVillager.setVillagerData(zombieVillager.getVillagerData().setProfession(profession));
        }
    }

    private static String displayName(String variant) {
        if (variant == null || variant.isBlank()) return "Default";
        int colon = variant.indexOf(':');
        String base = colon >= 0 ? variant.substring(colon + 1) : variant;
        return base.replace('_', ' ');
    }
}
