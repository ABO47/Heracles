package earth.terrarium.heracles.client.tags;

import com.mojang.blaze3d.systems.RenderSystem;
import earth.terrarium.heracles.client.handlers.CustomImageManager;
import earth.terrarium.hermes.api.TagElement;
import earth.terrarium.hermes.api.themes.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Map;

public class BackgroundTagElement implements TagElement {

    private final ResourceLocation texture;
    private final int x;
    private final int y;
    private final int w;
    private final int h;
    private final int opacity;

    public BackgroundTagElement(Map<String, String> parameters) {
        this.x = parseInt(parameters.get("x"), 0, -4096, 4096);
        this.y = parseInt(parameters.get("y"), 0, -4096, 4096);
        this.w = parseInt(parameters.get("w"), 0, 0, 8192);
        this.h = parseInt(parameters.get("h"), 0, 0, 8192);
        this.opacity = parseInt(parameters.get("opacity"), 100, 0, 100);
        this.texture = resolveTexture(parameters.get("src"));
    }

    @Override
    public void render(Theme theme, GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, boolean hovered, float partialTicks) {
        if (this.texture == null) return;
        int drawX = x + this.x;
        int drawY = y + this.y;
        int drawW = this.w > 0 ? this.w : width;
        int drawH = this.h > 0 ? this.h : Math.max(320, (int) (drawW * 0.6f));
        if (width > 0 && drawW > width * 2) {
            drawW = width;
        }
        if (width > 0 && drawH > width * 2) {
            drawH = Math.max(320, (int) (drawW * 0.6f));
        }
        if (drawW <= 0 || drawH <= 0) return;

        float alpha = Mth.clamp(this.opacity / 100.0f, 0.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        graphics.blit(this.texture, drawX, drawY, 0, 0, drawW, drawH, drawW, drawH);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    @Override
    public int getHeight(int width) {
        return 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int width) {
        return false;
    }

    private static ResourceLocation resolveTexture(String src) {
        if (src == null || src.isBlank()) return null;
        if (src.startsWith("assets/")) return CustomImageManager.getTexture(src);
        ResourceLocation parsed = ResourceLocation.tryParse(src);
        if (parsed != null) return parsed;
        return CustomImageManager.getTexture(src);
    }

    private static int parseInt(String raw, int fallback, int min, int max) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Mth.clamp(Integer.parseInt(raw.trim()), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
