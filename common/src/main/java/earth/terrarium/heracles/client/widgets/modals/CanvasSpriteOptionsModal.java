package earth.terrarium.heracles.client.widgets.modals;

import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import earth.terrarium.heracles.client.handlers.DisplayConfig;
import earth.terrarium.heracles.client.widgets.GridOpacitySlider;
import earth.terrarium.heracles.client.widgets.base.BaseModal;
import earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

public class CanvasSpriteOptionsModal extends BaseModal {

    private static final int WIDTH = 280;
    private static final int HEIGHT = 110;

    private final DisplayConfig.CanvasSprite sprite;
    private final IntConsumer onOpacityChanged;
    private final GridOpacitySlider opacitySlider;
    private final EnterableEditBox opacityField;

    public CanvasSpriteOptionsModal(int screenWidth, int screenHeight, DisplayConfig.CanvasSprite sprite, IntConsumer onOpacityChanged) {
        super(screenWidth, screenHeight, WIDTH, HEIGHT, 2);
        this.sprite = sprite;
        this.onOpacityChanged = onOpacityChanged;
        this.opacityField = addChild(new EnterableEditBox(this.font, this.x + 208, this.y + 54, 54, 14, Component.nullToEmpty("")));
        this.opacityField.setMaxLength(3);
        this.opacityField.setValue(String.valueOf(Math.max(0, sprite == null ? 100 : sprite.opacity())));
        this.opacityField.setResponder(s -> applyOpacityField(false));
        this.opacityField.setEnter(s -> applyOpacityField(true));
        this.opacitySlider = addChild(new GridOpacitySlider(this.x + 10, this.y + 56, 190, 12, sprite == null ? 100 : sprite.opacity(), v -> {
            int value = Math.max(0, Mth.clamp(v, 0, 100));
            this.opacityField.setValue(String.valueOf(value));
            if (this.onOpacityChanged != null) {
                this.onOpacityChanged.accept(value);
            }
        }));
    }

    @Override
    protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        try (CloseablePoseStack ignored = new CloseablePoseStack(graphics)) {
            graphics.fill(this.x, this.y, this.x + WIDTH, this.y + HEIGHT, 0xEE121212);
            graphics.fill(this.x + 1, this.y + 1, this.x + WIDTH - 1, this.y + HEIGHT - 1, 0xFF1E1E1E);
            renderChildren(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(this.font, Component.translatable("contextmenu.heracles.canvas_image_options"), this.x + 10, this.y + 10, 0xFFFFFF, false);
        String path = this.sprite == null ? "" : this.sprite.path();
        graphics.drawString(this.font, Component.literal(shortName(path)), this.x + 10, this.y + 28, 0xC6C6C6, false);
        int value = this.opacitySlider == null ? 100 : this.opacitySlider.getValue();
        graphics.drawString(this.font, Component.translatable("contextmenu.heracles.canvas_image_opacity", value), this.x + 10, this.y + 44, 0xE5E5E5, false);
    }

    private void applyOpacityField(boolean normalize) {
        int value = parsePercent(this.opacityField == null ? "" : this.opacityField.getValue());
        if (value < 0) return;
        if (this.opacitySlider != null) this.opacitySlider.setValue(value);
        if (this.onOpacityChanged != null) this.onOpacityChanged.accept(value);
        if (normalize && this.opacityField != null) this.opacityField.setValue(String.valueOf(value));
    }

    private static int parsePercent(String value) {
        if (value == null) return -1;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return -1;
        try {
            int parsed = Integer.parseInt(trimmed);
            return Math.max(0, Math.min(100, parsed));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String shortName(String path) {
        if (path == null || path.isBlank()) return "unknown";
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
