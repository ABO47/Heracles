package earth.terrarium.heracles.client.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

public class GridOpacitySlider extends Button {

    private int value; 
    private boolean dragging = false;
    private final Consumer<Integer> onChange;

    public GridOpacitySlider(int x, int y, int width, int height, int initialValue, Consumer<Integer> onChange) {
        super(x, y, width, height, Component.empty(), b -> {}, Button.DEFAULT_NARRATION);
        this.value = Mth.clamp(initialValue, 0, 100);
        this.onChange = onChange;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int tx = this.getX();
        int ty = this.getY();
        int tw = this.getWidth();
        int th = this.getHeight();

        
        int trackY = ty + th / 2 - 2;
        graphics.fill(tx, trackY, tx + tw, trackY + 4, 0x44000000);

        int fillW = (int) ((tw - 6) * (value / 100.0f));
        if (fillW > 0) {
            graphics.fill(tx + 1, trackY + 1, tx + 1 + fillW, trackY + 3, 0x55FFFFFF);
        }
        
        int knobX = tx + Mth.clamp((int) ((tw - 6) * (value / 100.0f)), 0, tw - 6);
        int knobW = 6;
        graphics.fill(knobX, ty, knobX + knobW, ty + th, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active) return false;
        if (isMouseOver(mouseX, mouseY)) {
            updateValueFromMouse(mouseX);
            this.dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragging) {
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging) {
            this.dragging = false;
            return true;
        }
        return false;
    }

    private void updateValueFromMouse(double mouseX) {
        int tx = this.getX();
        int tw = this.getWidth();
        int relative = (int) (mouseX - tx);
        int newValue = Math.max(0, Math.min(100, (int) Math.round((relative / (double) tw) * 100)));
        if (newValue != this.value) {
            this.value = newValue;
            try {
                this.onChange.accept(this.value);
            } catch (Exception ignored) {
            }
        }
    }

    public void setValue(int v) {
        this.value = Mth.clamp(v, 0, 100);
    }

    public int getValue() {
        return this.value;
    }
}
