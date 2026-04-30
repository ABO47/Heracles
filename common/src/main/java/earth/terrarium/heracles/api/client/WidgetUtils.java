package earth.terrarium.heracles.api.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import com.teamresourceful.resourcefullib.client.utils.ScreenUtils;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.api.client.theme.QuestScreenTheme;
import earth.terrarium.heracles.api.tasks.QuestTask;
import earth.terrarium.heracles.api.tasks.QuestTaskDisplayFormatter;
import earth.terrarium.heracles.common.handlers.progress.TaskProgress;
import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

public final class WidgetUtils {
    public static final ResourceLocation TEXTURE = new ResourceLocation(Heracles.MOD_ID, "textures/gui/widgets.png");

    public static void drawBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        RenderSystem.enableBlend();
        graphics.blitNineSliced(TEXTURE, x, y, 42, height, 3, 42, 42, 0, 0);
        graphics.blitNineSliced(TEXTURE, x + 42, y, width - 42, height, 3, 86, 42, 42, 0);
        RenderSystem.disableBlend();
    }

    public static void drawSummaryBackground(GuiGraphics graphics, int x, int y, int width, int height) {
        drawStatusSummaryBackground(graphics, x, y, width, height, ModUtils.QuestStatus.IN_PROGRESS);
    }

    public static void drawStatusSummaryBackground(GuiGraphics graphics, int x, int y, int width, int height, ModUtils.QuestStatus status) {
        RenderSystem.enableBlend();
        graphics.blitNineSliced(TEXTURE, x, y, width, height, 3, 128, 42, 128, 42 * status.ordinal());
        RenderSystem.disableBlend();
    }

    public static <T extends Tag> void drawProgressBar(GuiGraphics graphics, int minX, int minY, int maxX, int maxY, QuestTask<?, T, ?> task, TaskProgress<T> progress) {
        RenderSystem.enableBlend();
        graphics.blitNineSliced(TEXTURE, minX, minY, maxX - minX, maxY - minY, 3, 128, 8, 0, 168 + (progress.isComplete() ? 8 : 0));
        float fill = Math.min(1f, task.getProgress(progress.progress()));
        if (fill != 0.0 && !progress.isComplete()) {
            int progressWidth = (int) ((maxX - minX) * fill);
            graphics.blitNineSliced(TEXTURE, minX, minY, progressWidth, maxY - minY, 3, 128, 8, 0, 168 + 8 + 8);
        }
        RenderSystem.disableBlend();
    }

    public static <T extends Tag> void drawProgressText(GuiGraphics graphics, int x, int y, int width, QuestTask<?, T, ?> task, TaskProgress<T> progress) {
        Font font = Minecraft.getInstance().font;
        String text = QuestTaskDisplayFormatter.create(task, progress);
        graphics.drawString(
            font,
            text, x + width - 5 - font.width(text), y + 6, QuestScreenTheme.getTaskProgress(),
            false
        );
    }

    public static void drawEntity(GuiGraphics graphics, int x, int y, int size, Entity entity) {
        drawEntityInBox(graphics, x, y, size, size, entity, -35.0f);
    }

    public static void drawEntityInBox(GuiGraphics graphics, int x, int y, int width, int height, Entity entity, float rotationDegrees) {
        Minecraft mc = Minecraft.getInstance();
        int boxW = Math.max(1, width);
        int boxH = Math.max(1, height);
        float bw = Math.max(0.1f, entity.getBbWidth());
        float bh = Math.max(0.1f, entity.getBbHeight());
        float fitW = boxW / (bw * 1.05f);
        float fitH = boxH / (bh * 1.0f);
        float scaledSize = Math.max(0.1f, Math.min(fitW, fitH));

        float rot = rotationDegrees;
        if (entity instanceof EnderDragon) {
            rot += 180.0f;
        }

        int centerX = x + boxW / 2;
        int centerY = y + boxH - 4;
        try (var pose = new CloseablePoseStack(graphics)) {
            pose.translate(centerX, centerY, 50.0);
            pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
            pose.scale(scaledSize, scaledSize, scaledSize);
            pose.mulPose(Axis.YP.rotationDegrees(rot));
            EntityRenderDispatcher entityRenderer = mc.getEntityRenderDispatcher();
            MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
            entityRenderer.render(entity, 0, 0, 0.0D, mc.getFrameTime(), 1, pose, buffer, LightTexture.FULL_BRIGHT);
            buffer.endBatch();
        }
    }

    public static boolean drawItemIcon(GuiGraphics graphics, ItemStack stack, int x, int y, int size) {
        if (stack != null && !stack.is(Items.AIR)) {
            int scale = size / 16;
            try (var pose = new CloseablePoseStack(graphics)) {
                pose.translate(x, y, 0);
                pose.scale(scale, scale, 1);
                graphics.renderFakeItem(stack, 0, 0);
            }
            return true;
        }
        return false;
    }

    public static void drawItemIconWithTooltip(GuiGraphics graphics, ItemStack icon, int x, int y, int size, Supplier<List<Component>> tooltipCallback, int mouseX, int mouseY) {
        WidgetUtils.drawItemIcon(graphics, icon, x, y, size);
        boolean inBounds = (mouseX >= x && mouseX < x + size) && (mouseY >= y && mouseY < y + size);
        if (inBounds) {
            List<Component> tooltipLines = tooltipCallback.get();
            if (tooltipLines != null) {
                ScreenUtils.setTooltip(tooltipLines);
            }
        }
    }

    public static void drawItemIconWithTooltip(GuiGraphics graphics, ItemStack icon, int x, int y, int iconSize, int mouseX, int mouseY) {
        drawItemIconWithTooltip(graphics, icon, x, y, iconSize, () -> Screen.getTooltipFromItem(Minecraft.getInstance(), icon), mouseX, mouseY);
    }
}
