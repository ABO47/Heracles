package earth.terrarium.heracles.client.screens.quest.visual.toolbar;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class InlineToolbarUi {
    private InlineToolbarUi() {
    }

    public static void render(
        GuiGraphics graphics,
        Font font,
        ResourceLocation toolbarTexture,
        int mouseX,
        int mouseY,
        int tx,
        int ty,
        int toolbarW,
        int toolbarH,
        int buttonW,
        int toolColumns,
        int toolCount,
        int selectedTextColor
    ) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 600);
        graphics.fill(tx, ty, tx + toolbarW, ty + toolbarH, 0xEE1C1C1C);
        graphics.renderOutline(tx, ty, toolbarW, toolbarH, 0xFF4C95FF);
        for (int i = 0; i < toolCount; i++) {
            int col = i % toolColumns;
            int row = i / toolColumns;
            int bx = tx + 2 + col * buttonW;
            int by = ty + 2 + row * 16;
            boolean hovered = mouseX >= bx && mouseX < bx + buttonW - 1 && mouseY >= by && mouseY < by + 14;
            if (hovered) {
                graphics.fill(bx, by, bx + buttonW - 1, by + 14, 0x44FFFFFF);
            }
            if (i >= 0 && i <= 3) {
                int iconU = i * 11;
                graphics.blit(toolbarTexture, bx + 3, by + 1, iconU, 0, 11, 11, 256, 256);
            } else if (i == 4) {
                graphics.blit(toolbarTexture, bx + 3, by + 1, 44, hovered ? 11 : 0, 11, 11, 256, 256);
            } else if (i == 5) {
                graphics.blit(toolbarTexture, bx + 3, by + 1, 55, hovered ? 11 : 0, 11, 11, 256, 256);
            } else if (i == 6) {
                graphics.drawString(font, "L", bx + 5, by + 3, 0xFFFFFFFF, false);
            } else if (i == 7) {
                graphics.drawString(font, "C", bx + 5, by + 3, 0xFFFFFFFF, false);
            } else if (i == 8) {
                graphics.drawString(font, "R", bx + 5, by + 3, 0xFFFFFFFF, false);
            } else if (i == 9) {
                if (selectedTextColor >= 0) {
                    graphics.fill(bx + 4, by + 3, bx + buttonW - 5, by + 11, 0xFF000000 | selectedTextColor);
                    graphics.renderOutline(bx + 4, by + 3, buttonW - 9, 8, 0xFFFFFFFF);
                }
            } else if (i == 10) {
                graphics.drawString(font, "Aa", bx + 3, by + 3, 0xFFFFFFFF, false);
            } else if (i == 11) {
                graphics.drawString(font, "Sz", bx + 3, by + 3, 0xFFFFFFFF, false);
            }
        }
        graphics.pose().popPose();
    }

    public static int hitTestIndex(double mouseX, double mouseY, int tx, int ty, int toolbarW, int toolbarH, int buttonW, int toolColumns, int toolCount) {
        if (!(mouseX >= tx && mouseX < tx + toolbarW && mouseY >= ty && mouseY < ty + toolbarH)) return -1;
        int localX = (int) mouseX - tx - 2;
        int localY = (int) mouseY - ty - 2;
        int col = localX / buttonW;
        int row = localY / 16;
        int index = row * toolColumns + col;
        if (index < 0 || index >= toolCount) return -2;
        return index;
    }
}
