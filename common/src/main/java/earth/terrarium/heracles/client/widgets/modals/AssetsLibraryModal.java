package earth.terrarium.heracles.client.widgets.modals;

import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import earth.terrarium.heracles.client.handlers.CustomImageManager;
import earth.terrarium.heracles.client.widgets.base.BaseModal;
import earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox;
import earth.terrarium.heracles.client.widgets.buttons.ThemedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class AssetsLibraryModal extends BaseModal {

    private static final int WIDTH = 420;
    private static final int HEIGHT = 260;
    private static final int ROW_HEIGHT = 18;

    private final EnterableEditBox searchBox;
    private final Button useButton;
    private final Button clearButton;
    private final Consumer<String> onSelect;
    private List<String> allAssets = new ArrayList<>();
    private List<String> filteredAssets = new ArrayList<>();
    private int scroll = 0;
    private int selectedIndex = -1;
    private int lastClickedIndex = -1;
    private long lastClickTime = 0L;

    public AssetsLibraryModal(int screenWidth, int screenHeight, Consumer<String> onSelect) {
        super(screenWidth, screenHeight, WIDTH, HEIGHT, 2);
        this.onSelect = onSelect;
        this.searchBox = addChild(new EnterableEditBox(Minecraft.getInstance().font, this.x + 8, this.y + 8, WIDTH - 40, 14, Component.literal("Search assets...")));
        this.searchBox.setResponder(s -> applyFilter());
        this.searchBox.setEnter(s -> applySelection());

        this.useButton = addChild(ThemedButton.builder(Component.literal("Use"), b -> applySelection())
            .bounds(this.x + WIDTH - 58, this.y + HEIGHT - 20, 50, 14).build());
        this.clearButton = addChild(ThemedButton.builder(Component.literal("Clear"), b -> clearSelection())
            .bounds(this.x + WIDTH - 114, this.y + HEIGHT - 20, 52, 14).build());

        reloadAssets();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible) return;
        this.lastClickedIndex = -1;
        this.lastClickTime = 0L;
        reloadAssets();
        this.setFocused(this.searchBox);
        this.searchBox.setFocused(true);
        this.searchBox.moveCursorToEnd();
    }

    private void reloadAssets() {
        this.allAssets = new ArrayList<>(CustomImageManager.listManagedImages());
        applyFilter();
    }

    private void applyFilter() {
        String q = this.searchBox.getValue();
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        this.filteredAssets = new ArrayList<>();
        for (String path : this.allAssets) {
            String fileName = displayName(path);
            if (needle.isBlank() || fileName.toLowerCase(Locale.ROOT).contains(needle)) {
                this.filteredAssets.add(path);
            }
        }
        if (this.filteredAssets.isEmpty()) {
            this.selectedIndex = -1;
        } else if (this.selectedIndex < 0 || this.selectedIndex >= this.filteredAssets.size()) {
            this.selectedIndex = 0;
        }
        int maxScroll = Math.max(0, this.filteredAssets.size() - visibleRows());
        this.scroll = Mth.clamp(this.scroll, 0, maxScroll);
        this.useButton.active = this.selectedIndex >= 0;
    }

    private void applySelection() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.filteredAssets.size()) return;
        String selected = this.filteredAssets.get(this.selectedIndex);
        this.onSelect.accept(selected);
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
        graphics.drawString(this.font, Component.literal("Assets Library"), this.x + 8, this.y + 28, 0xEDEDED, false);

        int px = previewX();
        int py = previewY();
        int pw = previewW();
        int ph = previewH();
        graphics.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, 0xCC1B1B1B);
        graphics.fill(px, py, px + pw, py + ph, 0xAA0D0D0D);

        String selectedPath = this.selectedIndex >= 0 && this.selectedIndex < this.filteredAssets.size()
            ? this.filteredAssets.get(this.selectedIndex)
            : null;
        if (selectedPath != null) {
            ResourceLocation texture = CustomImageManager.getTexture(selectedPath);
            if (texture != null) {
                int inset = 6;
                graphics.blit(texture, px + inset, py + inset, 0, 0, pw - inset * 2, ph - inset * 2, pw - inset * 2, ph - inset * 2);
            }
        } else {
            graphics.drawCenteredString(this.font, Component.literal("No asset selected"), px + pw / 2, py + ph / 2 - 4, 0x8D8D8D);
        }

        int lx = listX();
        int ly = listY();
        int lw = listW();
        int lh = listH();
        graphics.fill(lx - 1, ly - 1, lx + lw + 1, ly + lh + 1, 0xB01A1A1A);
        graphics.fill(lx, ly, lx + lw, ly + lh, 0x8A121212);

        int rows = visibleRows();
        int renderRows = Math.min(rows, Math.max(0, this.filteredAssets.size() - this.scroll));
        for (int i = 0; i < renderRows; i++) {
            int idx = i + this.scroll;
            int rowY = ly + i * ROW_HEIGHT;
            boolean hovered = mouseX >= lx && mouseX < lx + lw && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            boolean selected = idx == this.selectedIndex;
            if (hovered) {
                graphics.fill(lx, rowY, lx + lw, rowY + ROW_HEIGHT - 1, 0x66333333);
            }
            if (selected) {
                graphics.renderOutline(lx, rowY, lw, ROW_HEIGHT - 1, 0xFF8CC9FF);
            }
            String name = displayName(this.filteredAssets.get(idx));
            graphics.drawString(this.font, name, lx + 6, rowY + 4, selected ? 0xFFFFFF : 0xD4D4D4, false);
            graphics.fill(lx + 1, rowY + ROW_HEIGHT - 1, lx + lw - 1, rowY + ROW_HEIGHT, 0x55343434);
        }

        if (this.filteredAssets.isEmpty()) {
            graphics.drawString(this.font, Component.literal("No image files found in assets/pictures"), lx + 6, ly + 6, 0xAAAAAA, false);
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
            if (idx >= 0 && idx < this.filteredAssets.size()) {
                long now = System.currentTimeMillis();
                this.selectedIndex = idx;
                this.useButton.active = true;
                if (this.lastClickedIndex == idx && now - this.lastClickTime < 300L) {
                    applySelection();
                    return true;
                }
                this.lastClickedIndex = idx;
                this.lastClickTime = now;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (!isVisible()) return false;
        int max = Math.max(0, this.filteredAssets.size() - visibleRows());
        if (max <= 0) return false;
        this.scroll = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(scrollAmount)));
        return true;
    }

    private String displayName(String managedPath) {
        if (managedPath == null || managedPath.isBlank()) return "";
        int slash = Math.max(managedPath.lastIndexOf('/'), managedPath.lastIndexOf('\\'));
        return slash >= 0 ? managedPath.substring(slash + 1) : managedPath;
    }
}
