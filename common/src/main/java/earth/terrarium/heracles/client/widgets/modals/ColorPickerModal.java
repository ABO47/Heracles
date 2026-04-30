package earth.terrarium.heracles.client.widgets.modals;

import earth.terrarium.heracles.client.widgets.base.BaseModal;
import earth.terrarium.heracles.client.widgets.buttons.ThemedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class ColorPickerModal extends BaseModal {

    private static final int WIDTH = 336;
    private static final int HEIGHT = 248;

    private static final int SV_X = 10;
    private static final int SV_Y = 26;
    private static final int SV_W = 214;
    private static final int SV_H = 166;

    private static final int HUE_X = 232;
    private static final int HUE_Y = 26;
    private static final int HUE_W = 16;
    private static final int HUE_H = 166;

    private final IntConsumer callback;
    private final Button saveColorButton;

    private float hue;
    private float saturation;
    private float value;

    private boolean draggingSv = false;
    private boolean draggingHue = false;
    private static final List<Integer> SAVED_COLORS = new ArrayList<>(List.of(
        0xF2D13F, 0xFF5555, 0x55FF55, 0x55AAFF, 0xB577FF
    ));

    public ColorPickerModal(int screenWidth, int screenHeight, int initialColor, IntConsumer callback) {
        super(screenWidth, screenHeight, WIDTH, HEIGHT, 2);
        this.callback = callback;

        float[] hsv = rgbToHsv(initialColor);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];

        addChild(ThemedButton.builder(Component.literal("Apply"), button -> {
            if (this.callback != null) {
                this.callback.accept(getSelectedColor());
            }
            setVisible(false);
        }).bounds(this.x + 10, this.y + HEIGHT - 20, 64, 14).build());

        addChild(ThemedButton.builder(Component.literal("Cancel"), button -> setVisible(false))
            .bounds(this.x + 78, this.y + HEIGHT - 20, 64, 14)
            .build());

        this.saveColorButton = addChild(ThemedButton.builder(Component.literal("Save"), button -> saveCurrentColor())
            .bounds(this.x + 270, this.y + 66, 54, 14)
            .build());
    }

    @Override
    protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(this.x + 4, this.y + 2, this.x + WIDTH - 4, this.y + HEIGHT - 4, 0xDD181818);
        graphics.fill(this.x + 5, this.y + 3, this.x + WIDTH - 5, this.y + HEIGHT - 5, 0xDD232323);

        renderSvBox(graphics);
        renderHueBar(graphics);

        int previewColor = 0xFF000000 | getSelectedColor();
        int previewX = this.x + 270;
        int previewY = this.y + 22;
        graphics.fill(previewX, previewY, previewX + 54, previewY + 24, 0xFF000000);
        graphics.fill(previewX + 1, previewY + 1, previewX + 53, previewY + 23, previewColor);

        String hex = String.format("#%06X", getSelectedColor());
        graphics.drawString(this.font, hex, previewX, previewY + 30, 0xFFFFFFFF, false);
        renderSavedSwatches(graphics, previewX, previewY + 90, mouseX, mouseY);
        this.saveColorButton.active = true;

        renderChildren(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(this.font, "Pick Color", this.x + 10, this.y + 6, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "S/V", this.x + SV_X, this.y + 18, 0xFFB8B8B8, false);
        graphics.drawString(this.font, "H", this.x + HUE_X + 2, this.y + 18, 0xFFB8B8B8, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible()) return false;
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        if (button != 0) return true;

        if (!isMouseOver(mouseX, mouseY)) {
            setVisible(false);
            return true;
        }

        if (isInSv(mouseX, mouseY)) {
            this.draggingSv = true;
            updateSv(mouseX, mouseY);
            return true;
        }
        if (isInHue(mouseX, mouseY)) {
            this.draggingHue = true;
            updateHue(mouseY);
            return true;
        }
        if (handleSavedSwatchClick(mouseX, mouseY)) {
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isVisible()) return false;
        if (this.draggingSv) {
            updateSv(mouseX, mouseY);
            return true;
        }
        if (this.draggingHue) {
            updateHue(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingSv = false;
        this.draggingHue = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderSvBox(GuiGraphics graphics) {
        int bx = this.x + SV_X;
        int by = this.y + SV_Y;

        for (int dx = 0; dx < SV_W; dx += 2) {
            float s = dx / (float) Math.max(1, SV_W - 1);
            for (int dy = 0; dy < SV_H; dy += 2) {
                float v = 1.0f - dy / (float) Math.max(1, SV_H - 1);
                int color = 0xFF000000 | hsvToRgb(this.hue, s, v);
                graphics.fill(bx + dx, by + dy, bx + Math.min(SV_W, dx + 2), by + Math.min(SV_H, dy + 2), color);
            }
        }

        int markerX = bx + Math.round(this.saturation * (SV_W - 1));
        int markerY = by + Math.round((1.0f - this.value) * (SV_H - 1));
        graphics.fill(markerX - 2, markerY - 2, markerX + 3, markerY + 3, 0xFF000000);
        graphics.fill(markerX - 1, markerY - 1, markerX + 2, markerY + 2, 0xFFFFFFFF);
    }

    private void renderHueBar(GuiGraphics graphics) {
        int bx = this.x + HUE_X;
        int by = this.y + HUE_Y;

        for (int dy = 0; dy < HUE_H; dy++) {
            float h = dy / (float) Math.max(1, HUE_H - 1);
            int color = 0xFF000000 | hsvToRgb(h, 1.0f, 1.0f);
            graphics.fill(bx, by + dy, bx + HUE_W, by + dy + 1, color);
        }

        int markerY = by + Math.round(this.hue * (HUE_H - 1));
        graphics.fill(bx - 2, markerY - 1, bx + HUE_W + 2, markerY + 1, 0xFFFFFFFF);
    }

    private boolean isInSv(double mouseX, double mouseY) {
        int bx = this.x + SV_X;
        int by = this.y + SV_Y;
        return mouseX >= bx && mouseX <= bx + SV_W && mouseY >= by && mouseY <= by + SV_H;
    }

    private boolean isInHue(double mouseX, double mouseY) {
        int bx = this.x + HUE_X;
        int by = this.y + HUE_Y;
        return mouseX >= bx && mouseX <= bx + HUE_W && mouseY >= by && mouseY <= by + HUE_H;
    }

    private void updateSv(double mouseX, double mouseY) {
        int bx = this.x + SV_X;
        int by = this.y + SV_Y;
        this.saturation = Mth.clamp((float) ((mouseX - bx) / (double) Math.max(1, SV_W - 1)), 0.0f, 1.0f);
        this.value = 1.0f - Mth.clamp((float) ((mouseY - by) / (double) Math.max(1, SV_H - 1)), 0.0f, 1.0f);
    }

    private void updateHue(double mouseY) {
        int by = this.y + HUE_Y;
        this.hue = Mth.clamp((float) ((mouseY - by) / (double) Math.max(1, HUE_H - 1)), 0.0f, 1.0f);
    }

    private void saveCurrentColor() {
        int color = getSelectedColor() & 0xFFFFFF;
        if (SAVED_COLORS.contains(color)) return;
        SAVED_COLORS.add(0, color);
        while (SAVED_COLORS.size() > 14) {
            SAVED_COLORS.remove(SAVED_COLORS.size() - 1);
        }
    }

    private void renderSavedSwatches(GuiGraphics graphics, int startX, int startY, int mouseX, int mouseY) {
        graphics.drawString(this.font, "Saved", startX, startY - 10, 0xFFBEBEBE, false);
        int cell = 14;
        int cols = 3;
        for (int i = 0; i < SAVED_COLORS.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            int x = startX + col * (cell + 4);
            int y = startY + row * (cell + 4);
            int color = 0xFF000000 | SAVED_COLORS.get(i);
            boolean hovered = mouseX >= x && mouseX < x + cell && mouseY >= y && mouseY < y + cell;
            graphics.fill(x, y, x + cell, y + cell, hovered ? 0xFFFFFFFF : 0xFF6E6E6E);
            graphics.fill(x + 1, y + 1, x + cell - 1, y + cell - 1, color);
        }
    }

    private boolean handleSavedSwatchClick(double mouseX, double mouseY) {
        int startX = this.x + 270;
        int startY = this.y + 112;
        int cell = 14;
        int cols = 3;
        for (int i = 0; i < SAVED_COLORS.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            int x = startX + col * (cell + 4);
            int y = startY + row * (cell + 4);
            if (mouseX >= x && mouseX < x + cell && mouseY >= y && mouseY < y + cell) {
                int color = SAVED_COLORS.get(i);
                float[] hsv = rgbToHsv(color);
                this.hue = hsv[0];
                this.saturation = hsv[1];
                this.value = hsv[2];
                return true;
            }
        }
        return false;
    }

    private int getSelectedColor() {
        return hsvToRgb(this.hue, this.saturation, this.value);
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        int sector = (int) Math.floor(h);
        float f = h - sector;
        float p = value * (1.0f - saturation);
        float q = value * (1.0f - saturation * f);
        float t = value * (1.0f - saturation * (1.0f - f));

        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> {
                r = value;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = value;
                b = p;
            }
            case 2 -> {
                r = p;
                g = value;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = value;
            }
            case 4 -> {
                r = t;
                g = p;
                b = value;
            }
            default -> {
                r = value;
                g = p;
                b = q;
            }
        }

        int ri = Mth.clamp((int) Math.round(r * 255.0f), 0, 255);
        int gi = Mth.clamp((int) Math.round(g * 255.0f), 0, 255);
        int bi = Mth.clamp((int) Math.round(b * 255.0f), 0, 255);
        return (ri << 16) | (gi << 8) | bi;
    }

    private static float[] rgbToHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h;
        if (delta == 0.0f) {
            h = 0.0f;
        } else if (max == r) {
            h = ((g - b) / delta) % 6.0f;
        } else if (max == g) {
            h = ((b - r) / delta) + 2.0f;
        } else {
            h = ((r - g) / delta) + 4.0f;
        }
        h /= 6.0f;
        if (h < 0.0f) h += 1.0f;

        float s = max == 0.0f ? 0.0f : delta / max;
        float v = max;
        return new float[]{h, s, v};
    }
}
