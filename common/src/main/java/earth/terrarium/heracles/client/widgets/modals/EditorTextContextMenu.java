package earth.terrarium.heracles.client.widgets.modals;

import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import earth.terrarium.heracles.client.widgets.base.BaseWidget;
import earth.terrarium.heracles.client.widgets.base.TemporaryWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class EditorTextContextMenu extends BaseWidget implements TemporaryWidget {

    private static final int ITEM_HEIGHT = 18;
    private static final int MIN_WIDTH = 120;

    private final int x;
    private final int y;
    private final List<MenuItem> items = new ArrayList<>();
    private int width;
    private int height;
    private boolean visible = true;

    public EditorTextContextMenu(int x, int y, List<MenuItem> items) {
        this.x = x;
        this.y = y;
        this.items.addAll(items);

        int maxLabelWidth = 0;
        for (MenuItem item : this.items) {
            maxLabelWidth = Math.max(maxLabelWidth, this.font.width(item.label.getString()));
        }
        this.width = Math.max(MIN_WIDTH, maxLabelWidth + 14);
        this.height = 6 + this.items.size() * ITEM_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int displayHeight = Math.min(this.height, Math.max(32, screenH - 10));
        int effectiveX = this.x;
        int effectiveY = this.y;
        if (effectiveX + this.width > screenW) effectiveX = Math.max(5, screenW - this.width - 5);
        if (effectiveY + displayHeight > screenH) effectiveY = Math.max(5, this.y - displayHeight);

        try (CloseablePoseStack pose = new CloseablePoseStack(graphics)) {
            pose.translate(0, 0, 320);
            graphics.fill(effectiveX, effectiveY, effectiveX + this.width, effectiveY + displayHeight, 0xEE222222);
            graphics.fill(effectiveX + 1, effectiveY + 1, effectiveX + this.width - 1, effectiveY + displayHeight - 1, 0xFF2B2B2B);

            int totalHeight = this.items.size() * ITEM_HEIGHT;
            int maxScroll = Math.max(0, totalHeight - (displayHeight - 8));
            int scroll = 0;
            int startIndex = scroll / ITEM_HEIGHT;
            int offsetPixels = scroll % ITEM_HEIGHT;
            int visibleCount = (displayHeight - 8) / ITEM_HEIGHT + 1;
            int iy = effectiveY + 4;

            for (int i = startIndex; i < Math.min(this.items.size(), startIndex + visibleCount); i++) {
                MenuItem item = this.items.get(i);
                int itemY = iy + (i - startIndex) * ITEM_HEIGHT - offsetPixels;
                if (mouseX >= effectiveX && mouseX < effectiveX + this.width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                    graphics.fill(effectiveX + 2, itemY, effectiveX + this.width - 2, itemY + ITEM_HEIGHT, 0xAAFFFFFF);
                }
                graphics.drawString(this.font, item.label, effectiveX + 6, itemY + (ITEM_HEIGHT - 9) / 2, 0xFFFFFFFF, false);
            }
        }
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public int depth() {
        return 100000;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.visible) return false;

        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int displayHeight = Math.min(this.height, Math.max(32, screenH - 10));
        int effectiveX = this.x;
        int effectiveY = this.y;
        if (effectiveX + this.width > screenW) effectiveX = Math.max(5, screenW - this.width - 5);
        if (effectiveY + displayHeight > screenH) effectiveY = Math.max(5, this.y - displayHeight);
        return mouseX >= effectiveX && mouseX < effectiveX + this.width && mouseY >= effectiveY && mouseY < effectiveY + displayHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;

        if (button == 0) {
            if (isMouseOver(mouseX, mouseY)) {
                int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                int displayHeight = Math.min(this.height, Math.max(32, screenH - 10));
                int effectiveX = this.x;
                int effectiveY = this.y;
                if (effectiveX + this.width > Minecraft.getInstance().getWindow().getGuiScaledWidth()) {
                    effectiveX = Math.max(5, Minecraft.getInstance().getWindow().getGuiScaledWidth() - this.width - 5);
                }
                if (effectiveY + displayHeight > screenH) {
                    effectiveY = Math.max(5, this.y - displayHeight);
                }
                int relativeY = (int) mouseY - (effectiveY + 4);
                int index = relativeY / ITEM_HEIGHT;
                if (index >= 0 && index < this.items.size()) {
                    try {
                        this.items.get(index).action.run();
                    } catch (Exception ignored) {
                    }
                }
            }
            this.visible = false;
            return true;
        }

        if (button == 1) {
            this.visible = false;
            return true;
        }
        return false;
    }

    public record MenuItem(Component label, Runnable action) {
    }
}
