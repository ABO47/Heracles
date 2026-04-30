package earth.terrarium.heracles.client.tags;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import earth.terrarium.heracles.client.handlers.CustomImageManager;
import earth.terrarium.hermes.api.TagElement;
import earth.terrarium.hermes.api.themes.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Map;

public class ImageTagElement implements TagElement {

    private final String source;
    private final ResourceLocation texture;
    private final int width;
    private final int height;
    private final int offsetX;
    private final int offsetY;
    private final int opacity;
    private final int rotation;

    public ImageTagElement(Map<String, String> parameters) {
        this.source = value(parameters, "src");
        this.width = clampInt(value(parameters, "w"), 240, 24, 1024);
        this.height = clampInt(value(parameters, "h"), 135, 24, 1024);
        this.offsetX = clampInt(value(parameters, "x"), 0, -512, 512);
        this.offsetY = clampInt(value(parameters, "y"), 0, -512, 2048);
        this.opacity = clampInt(value(parameters, "opacity"), 100, 0, 100);
        this.rotation = clampInt(value(parameters, "rotation"), 0, -3600, 3600);
        this.texture = resolveTexture(this.source);
    }

    @Override
    public void render(Theme theme, GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, boolean hovered, float partialTicks) {
        int drawX = x + this.offsetX;
        int drawY = y + this.offsetY;
        if (this.texture != null) {
            float alpha = Mth.clamp(this.opacity / 100.0f, 0.0f, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            graphics.pose().pushPose();
            graphics.pose().translate(drawX + this.width / 2.0f, drawY + this.height / 2.0f, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(this.rotation));
            graphics.pose().translate(-this.width / 2.0f, -this.height / 2.0f, 0);
            graphics.blit(this.texture, 0, 0, 0, 0, this.width, this.height, this.width, this.height);
            graphics.pose().popPose();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        } else {
            graphics.fill(drawX, drawY, drawX + this.width, drawY + this.height, 0xAA202020);
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, "Missing image", drawX + 6, drawY + 6, 0xFFCC8888, false);
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
        return value == null ? "" : value.trim();
    }

    private static int clampInt(String value, int fallback, int min, int max) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Mth.clamp(Integer.parseInt(value.trim()), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static ResourceLocation resolveTexture(String source) {
        if (source == null || source.isBlank()) return null;
        ResourceLocation location = ResourceLocation.tryParse(source);
        if (location != null && !source.startsWith("assets/")) {
            return location;
        }
        return CustomImageManager.getTexture(source);
    }
}
