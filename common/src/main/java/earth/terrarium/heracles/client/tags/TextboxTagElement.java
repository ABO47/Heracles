package earth.terrarium.heracles.client.tags;

import earth.terrarium.heracles.client.screens.quest.MarkdownParser;
import earth.terrarium.hermes.api.TagElement;
import earth.terrarium.hermes.api.themes.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Map;

public class TextboxTagElement implements TagElement {

    private final String text;
    private final int x;
    private final int y;
    private final int w;
    private final int h;
    private final boolean bold;
    private final boolean italic;
    private final boolean underline;
    private final boolean strike;
    private final int color;
    private final boolean hasExplicitColor;
    private final String align;
    private final int opacity;
    private final int rotation;
    private final int textSize;

    public TextboxTagElement(Map<String, String> parameters) {
        this.text = decode(value(parameters, "text"));
        this.x = clampInt(value(parameters, "x"), 0, -1024, 4096);
        this.y = clampInt(value(parameters, "y"), 0, -1024, 4096);
        this.w = clampInt(value(parameters, "w"), 220, 24, 4096);
        this.h = clampInt(value(parameters, "h"), 60, 14, 4096);
        this.bold = Boolean.parseBoolean(value(parameters, "bold"));
        this.italic = Boolean.parseBoolean(value(parameters, "italic"));
        this.underline = Boolean.parseBoolean(value(parameters, "underline"));
        this.strike = Boolean.parseBoolean(value(parameters, "strike"));
        this.hasExplicitColor = parameters.containsKey("color");
        this.color = clampInt(value(parameters, "color"), 0x1B1B1B, 0x000000, 0xFFFFFF);
        this.align = value(parameters, "align").toLowerCase().trim();
        this.opacity = clampInt(value(parameters, "opacity"), 100, 0, 100);
        this.rotation = clampInt(value(parameters, "rotation"), 0, -3600, 3600);
        this.textSize = clampInt(value(parameters, "size"), 100, 50, 300);
    }

    @Override
    public void render(Theme theme, GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, boolean hovered, float partialTicks) {
        int drawX = x + this.x;
        int drawY = y + this.y;
        int baseX = drawX;
        int baseY = drawY;
        if (((this.rotation % 360) + 360) % 360 != 0) {
            graphics.pose().pushPose();
            graphics.pose().translate(drawX + this.w / 2.0f, drawY + this.h / 2.0f, 0);
            graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(this.rotation));
            baseX = -this.w / 2;
            baseY = -this.h / 2;
        }

        Component styled = MarkdownParser.parseTextToComponent(this.text).withStyle(style -> style
            .withBold(this.bold)
            .withItalic(this.italic)
            .withUnderlined(this.underline)
            .withStrikethrough(this.strike));

        float textScale = Mth.clamp(this.textSize / 100.0f, 0.5f, 3.0f);
        int lineHeight = Math.max(1, Math.round(Minecraft.getInstance().font.lineHeight * textScale));
        int innerWidth = Math.max(16, Math.round((this.w - 6) / textScale));
        int maxLines = Math.max(1, (this.h - 6) / lineHeight);
        int lineY = baseY + 3;
        int lineCount = 0;
        for (var line : Minecraft.getInstance().font.split(styled, innerWidth)) {
            if (lineCount >= maxLines) break;
            int lineX = baseX + 3;
            int lineWidth = Math.round(Minecraft.getInstance().font.width(line) * textScale);
            if ("center".equals(this.align)) {
                lineX = baseX + (this.w - lineWidth) / 2;
            } else if ("right".equals(this.align)) {
                lineX = baseX + this.w - lineWidth - 3;
            }
            int drawColor = this.hasExplicitColor ? this.color : 0xE6E6E6;
            int alpha = Mth.clamp((int) Math.round(this.opacity * 255.0 / 100.0), 0, 255);
            drawScaledLine(graphics, line, lineX, lineY, textScale, (alpha << 24) | drawColor);
            lineY += lineHeight;
            lineCount++;
        }
        if (((this.rotation % 360) + 360) % 360 != 0) {
            graphics.pose().popPose();
        }
    }

    @Override
    public int getHeight(int width) {
        return 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int width) {
        return false;
    }

    private static String value(Map<String, String> parameters, String key) {
        String value = parameters.getOrDefault(key, "");
        return value == null ? "" : value;
    }

    private static int clampInt(String value, int fallback, int min, int max) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Mth.clamp(Integer.parseInt(value.trim()), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String decode(String value) {
        return value
            .replace("&#10;", "\n")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&");
    }

    private static void drawScaledLine(GuiGraphics graphics, net.minecraft.util.FormattedCharSequence line, int x, int y, float scale, int color) {
        if (Math.abs(scale - 1.0f) < 0.001f) {
            graphics.drawString(Minecraft.getInstance().font, line, x, y, color, true);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(Minecraft.getInstance().font, line, 0, 0, color, true);
        graphics.pose().popPose();
    }
}
