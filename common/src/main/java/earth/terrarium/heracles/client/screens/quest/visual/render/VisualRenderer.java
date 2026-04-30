package earth.terrarium.heracles.client.screens.quest.visual.render;

import com.mojang.blaze3d.systems.RenderSystem;
import earth.terrarium.heracles.client.screens.quest.MarkdownParser;
import earth.terrarium.heracles.client.screens.quest.visual.geometry.VisualCanvasMetrics;
import earth.terrarium.heracles.client.screens.quest.visual.geometry.VisualGeometry;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionController;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElementType;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualTextAlign;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

public final class VisualRenderer {
    private VisualRenderer() {
    }

    public static void renderVisualCanvas(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        Runnable clampVisualScroll,
        Supplier<String> getVisualBackgroundSrc,
        Function<String, ResourceLocation> resolveImageTexture,
        int visualBackgroundOpacity,
        int descriptionX,
        int descriptionY,
        int descriptionWidth,
        int descriptionHeight,
        boolean visualGridEnabled,
        int visualGridSize,
        int visualScrollX,
        int visualScrollY,
        Font font,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        int selectedVisualElement,
        int editingTextElement,
        MultiLineEditBox visualTextEditor,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        BiFunction<Integer, EntityType<?>, net.minecraft.world.entity.Entity> previewEntityProvider,
        int alignmentGuideX,
        int alignmentGuideY,
        boolean selectingVisualElements,
        int selectBoxStartX,
        int selectBoxStartY,
        int selectBoxEndX,
        int selectBoxEndY,
        BiConsumer<GuiGraphics, int[]> renderInlineToolbar,
        boolean editMode
    ) {
        clampVisualScroll.run();
        String backgroundSrc = getVisualBackgroundSrc.get();
        ResourceLocation background = backgroundSrc == null || backgroundSrc.isBlank() ? null : resolveImageTexture.apply(backgroundSrc);
        graphics.enableScissor(descriptionX + 1, descriptionY + 1, descriptionX + descriptionWidth, descriptionY + descriptionHeight);
        try {
            renderCanvasSurface(
                graphics,
                descriptionX,
                descriptionY,
                descriptionWidth,
                descriptionHeight,
                background,
                visualBackgroundOpacity,
                editMode
            );
            if (visualGridEnabled) {
                renderGrid(
                    graphics,
                    descriptionX,
                    descriptionY,
                    descriptionWidth,
                    descriptionHeight,
                    visualGridSize,
                    visualScrollX,
                    visualScrollY
                );
            }
            renderElements(
                graphics,
                font,
                visualElements,
                multiSelectedVisualElements,
                selectedVisualElement,
                editingTextElement,
                visualTextEditor,
                mouseX,
                mouseY,
                descriptionX,
                descriptionY,
                descriptionWidth,
                descriptionHeight,
                canvasToScreenX,
                canvasToScreenY,
                screenToCanvasX,
                screenToCanvasY,
                resolveImageTexture,
                previewEntityProvider,
                editMode
            );

            renderGuideLines(
                graphics,
                alignmentGuideX,
                alignmentGuideY,
                descriptionX,
                descriptionY,
                descriptionWidth,
                descriptionHeight,
                canvasToScreenX,
                canvasToScreenY
            );
            if (selectingVisualElements) {
                renderSelectionBox(
                    graphics,
                    selectBoxStartX,
                    selectBoxStartY,
                    selectBoxEndX,
                    selectBoxEndY
                );
            }
        } finally {
            graphics.disableScissor();
        }
        renderVerticalScrollbar(
            graphics,
            descriptionX,
            descriptionY,
            descriptionWidth,
            descriptionHeight,
            visualGridSize,
            visualScrollY,
            visualElements,
            editMode
        );
        if (editMode) {
            renderInlineToolbar.accept(graphics, new int[]{mouseX, mouseY});
        }
    }

    public static void renderCanvasSurface(
        GuiGraphics graphics,
        int descriptionX,
        int descriptionY,
        int descriptionWidth,
        int descriptionHeight,
        ResourceLocation background,
        int backgroundOpacity,
        boolean editMode
    ) {
        if (editMode || background != null) {
            graphics.fill(descriptionX, descriptionY, descriptionX + descriptionWidth, descriptionY + descriptionHeight, 0xFFFFFFFF);
        }
        if (background != null) {
            float alpha = Mth.clamp(backgroundOpacity / 100.0f, 0.0f, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            graphics.blit(background, descriptionX, descriptionY, 0, 0, descriptionWidth, descriptionHeight, descriptionWidth, descriptionHeight);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }

    public static void renderGrid(
        GuiGraphics graphics,
        int descriptionX,
        int descriptionY,
        int descriptionWidth,
        int descriptionHeight,
        int visualGridSize,
        int visualScrollX,
        int visualScrollY
    ) {
        if (visualGridSize <= 1) return;
        int color = 0x22000000;
        int left = descriptionX;
        int top = descriptionY;
        int right = descriptionX + descriptionWidth;
        int bottom = descriptionY + descriptionHeight;

        int offsetX = Math.floorMod(visualScrollX, visualGridSize);
        int offsetY = Math.floorMod(visualScrollY, visualGridSize);
        int firstX = left - offsetX;
        int firstY = top - offsetY;

        // Draw only interior lines; no outer frame lines.
        for (int x = firstX; x < right; x += visualGridSize) {
            if (x <= left) continue;
            graphics.fill(x, top, x + 1, bottom, color);
        }
        for (int y = firstY; y < bottom; y += visualGridSize) {
            if (y <= top) continue;
            graphics.fill(left, y, right, y + 1, color);
        }
    }

    private static void renderVerticalScrollbar(
        GuiGraphics graphics,
        int descriptionX,
        int descriptionY,
        int descriptionWidth,
        int descriptionHeight,
        int visualGridSize,
        int visualScrollY,
        List<VisualElement> visualElements,
        boolean editMode
    ) {
        int canvasHeight = VisualCanvasMetrics.getCanvasHeight(descriptionHeight, visualElements, Math.max(1, visualGridSize));
        if (canvasHeight <= descriptionHeight) return;
        int trackX = descriptionX + descriptionWidth + 2;
        int trackY = descriptionY;
        int trackH = descriptionHeight;
        if (editMode) {
            graphics.fill(trackX, trackY, trackX + 4, trackY + trackH, 0x33111111);
        }

        int knobH = Math.max(18, (int) Math.round((descriptionHeight / (double) canvasHeight) * trackH));
        int maxScroll = Math.max(1, canvasHeight - descriptionHeight);
        int maxTravel = Math.max(1, trackH - knobH);
        int knobY = trackY + (int) Math.round((visualScrollY / (double) maxScroll) * maxTravel);
        graphics.fill(trackX, knobY, trackX + 4, knobY + knobH, 0x884C95FF);
    }

    public static void renderGuideLines(
        GuiGraphics graphics,
        int guideX,
        int guideY,
        int descriptionX,
        int descriptionY,
        int descriptionWidth,
        int descriptionHeight,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY
    ) {
        if (guideX >= 0) {
            int gx = canvasToScreenX.applyAsInt(guideX) - 1;
            graphics.fill(gx, descriptionY, gx + 1, descriptionY + descriptionHeight, 0xFF27C4FF);
        }
        if (guideY >= 0) {
            int gy = canvasToScreenY.applyAsInt(guideY) - 1;
            graphics.fill(descriptionX, gy, descriptionX + descriptionWidth, gy + 1, 0xFF27C4FF);
        }
    }

    public static void renderSelectionBox(GuiGraphics graphics, int startX, int startY, int endX, int endY) {
        int left = Math.min(startX, endX);
        int top = Math.min(startY, endY);
        int right = Math.max(startX, endX);
        int bottom = Math.max(startY, endY);
        graphics.fill(left, top, right, bottom, 0x3327C4FF);
        graphics.renderOutline(left, top, right - left, bottom - top, 0xFF27C4FF);
    }

    public static void renderElements(
        GuiGraphics graphics,
        Font font,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        int selectedVisualElement,
        int editingTextElement,
        MultiLineEditBox visualTextEditor,
        int mouseX,
        int mouseY,
        int descriptionX,
        int descriptionY,
        int descriptionWidth,
        int descriptionHeight,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        Function<String, ResourceLocation> resolveImageTexture,
        BiFunction<Integer, EntityType<?>, net.minecraft.world.entity.Entity> previewEntityProvider,
        boolean editMode
    ) {
        int hovered = editMode ? VisualInteractionController.findTopElement(
            visualElements,
            mouseX,
            mouseY,
            screenToCanvasX.applyAsInt(mouseX),
            screenToCanvasY.applyAsInt(mouseY),
            canvasToScreenX,
            canvasToScreenY
        ) : -1;
        for (int i = 0; i < visualElements.size(); i++) {
            VisualElement element = visualElements.get(i);
            int x = canvasToScreenX.applyAsInt(element.x);
            int y = canvasToScreenY.applyAsInt(element.y);
            int w = element.w;
            int h = element.h;
            int bx = x;
            int by = y;
            int rw = Math.max(1, w - 1);
            int rh = Math.max(1, h - 1);
            if (x + w < descriptionX || y + h < descriptionY || x > descriptionX + descriptionWidth || y > descriptionY + descriptionHeight) {
                continue;
            }

            if (element.type == VisualElementType.TEXT) {
                renderTextElement(graphics, font, element, editingTextElement == i, visualTextEditor, rw, rh, bx, by, editMode);
            } else if (element.type == VisualElementType.ITEM) {
                ItemStack stack = VisualElementResolvers.parseItemStack(element);
                if (!stack.isEmpty()) {
                    int drawSize = Math.max(8, Math.min(rw, rh));
                    int drawX = bx + Math.max(0, (rw - drawSize) / 2);
                    int drawY = by + Math.max(0, (rh - drawSize) / 2);
                    float scale = drawSize / 16.0f;
                    graphics.pose().pushPose();
                    graphics.pose().translate(drawX + drawSize / 2.0f, drawY + drawSize / 2.0f, 0);
                    graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(element.rotation));
                    graphics.pose().translate(-drawSize / 2.0f, -drawSize / 2.0f, 0);
                    graphics.pose().scale(scale, scale, 1.0f);
                    graphics.renderFakeItem(stack, 0, 0);
                    graphics.pose().popPose();
                }
            } else if (element.type == VisualElementType.IMAGE) {
                ResourceLocation texture = resolveImageTexture.apply(element.imageSrc);
                if (texture != null) {
                    float alpha = Mth.clamp(element.opacity / 100.0f, 0.0f, 1.0f);
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
                    graphics.pose().pushPose();
                    graphics.pose().translate(bx + rw / 2.0f, by + rh / 2.0f, 0);
                    graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(element.rotation));
                    graphics.pose().translate(-rw / 2.0f, -rh / 2.0f, 0);
                    graphics.blit(texture, 0, 0, 0, 0, rw, rh, rw, rh);
                    graphics.pose().popPose();
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                    RenderSystem.disableBlend();
                } else {
                    graphics.fill(bx, by, bx + rw, by + rh, 0xFF3A3A3A);
                    graphics.drawString(font, "Missing image", bx + 4, by + 4, 0xFFFFAAAA, false);
                }
            } else if (element.type == VisualElementType.ENTITY) {
                EntityType<?> entityType = VisualElementResolvers.parseEntityType(element);
                if (entityType != null && Minecraft.getInstance().level != null) {
                    var entity = previewEntityProvider.apply(i, entityType);
                    if (entity != null) {
                        VisualElementResolvers.applyEntityVariant(element, entity);
                        float angle = computeEntityRenderAngle(element);
                        earth.terrarium.heracles.api.client.WidgetUtils.drawEntityInBox(graphics, bx, by, rw, rh, entity, angle);
                    }
                }
            }

            boolean selected = editMode && (i == selectedVisualElement || multiSelectedVisualElements.contains(i));
            boolean hoveredElement = i == hovered;
            if (!editMode && !selected) {
                continue;
            }
            int border = selected ? 0xFF4C95FF : (hoveredElement ? 0xFFD7A64A : 0xFF7C7C7C);
            int borderX = x;
            int borderY = y;
            int borderW = Math.max(1, w - 1);
            int borderH = Math.max(1, h - 1);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            boolean drawRotatedSelection = isRotatable(element) && (VisualGeometry.normalizeRotation(element.rotation) != 0);
            if (drawRotatedSelection) {
                int cx = borderX + borderW / 2;
                int cy = borderY + borderH / 2;
                graphics.pose().pushPose();
                graphics.pose().translate(cx, cy, 0);
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(element.rotation));
                int localX = -borderW / 2;
                int localY = -borderH / 2;
                graphics.renderOutline(localX, localY, borderW, borderH, border);
                if (selected) {
                    int handleX = localX + borderW - 7;
                    int handleY = localY + borderH - 7;
                    graphics.fill(handleX, handleY, handleX + 7, handleY + 7, 0xFFFFFFFF);
                    graphics.renderOutline(handleX, handleY, 7, 7, 0xFF000000);
                    int rotateX = localX + borderW - 7;
                    int rotateY = localY;
                    graphics.fill(rotateX, rotateY, rotateX + 7, rotateY + 7, 0xFF9FE8FF);
                    graphics.renderOutline(rotateX, rotateY, 7, 7, 0xFF000000);
                }
                graphics.pose().popPose();
            } else {
                graphics.renderOutline(borderX, borderY, borderW, borderH, border);
                if (selected) {
                    int handleX = borderX + borderW - 7;
                    int handleY = borderY + borderH - 7;
                    graphics.fill(handleX, handleY, handleX + 7, handleY + 7, 0xFFFFFFFF);
                    graphics.renderOutline(handleX, handleY, 7, 7, 0xFF000000);
                    if (isRotatable(element)) {
                        int rotateX = borderX + borderW - 7;
                        int rotateY = borderY;
                        graphics.fill(rotateX, rotateY, rotateX + 7, rotateY + 7, 0xFF9FE8FF);
                        graphics.renderOutline(rotateX, rotateY, 7, 7, 0xFF000000);
                    }
                }
            }
            graphics.pose().popPose();
        }

        if (editMode && multiSelectedVisualElements.size() > 1) {
            int[] bounds = VisualInteractionController.getMultiSelectionBounds(multiSelectedVisualElements, visualElements);
            if (bounds != null) {
                int bx = canvasToScreenX.applyAsInt(bounds[0]);
                int by = canvasToScreenY.applyAsInt(bounds[1]);
                int bw = bounds[2] - bounds[0];
                int bh = bounds[3] - bounds[1];
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 300);
                graphics.renderOutline(bx, by, Math.max(1, bw - 1), Math.max(1, bh - 1), 0xFF1FA2FF);
                int hx = bx + bw - 8;
                int hy = by + bh - 8;
                graphics.fill(hx, hy, hx + 7, hy + 7, 0xFFFFFFFF);
                graphics.renderOutline(hx, hy, 7, 7, 0xFF000000);
                int rx = bx + bw - 8;
                int ry = by;
                graphics.fill(rx, ry, rx + 7, ry + 7, 0xFF9FE8FF);
                graphics.renderOutline(rx, ry, 7, 7, 0xFF000000);
                graphics.pose().popPose();
            }
        }
    }

    private static void renderTextElement(
        GuiGraphics graphics,
        Font font,
        VisualElement element,
        boolean editingThis,
        MultiLineEditBox visualTextEditor,
        int rw,
        int rh,
        int bx,
        int by,
        boolean editMode
    ) {
        int alpha = Mth.clamp((int) Math.round(element.opacity * 255.0 / 100.0), 0, 255);
        boolean rotatedText = VisualGeometry.normalizeRotation(element.rotation) != 0;
        boolean editorOverlay = editingThis && visualTextEditor != null;
        String drawText = editorOverlay ? visualTextEditor.getValue() : element.text;
        int baseX = bx;
        int baseY = by;
        if (rotatedText) {
            graphics.pose().pushPose();
            graphics.pose().translate(bx + rw / 2.0f, by + rh / 2.0f, 0);
            graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(element.rotation));
            baseX = -rw / 2;
            baseY = -rh / 2;
        }
        if (editMode) {
            graphics.fill(baseX, baseY, baseX + rw, baseY + rh, (alpha << 24) | 0x00F4F4F4);
        }
        float textScale = Mth.clamp(element.textSize / 100.0f, 0.5f, 3.0f);
        int lineHeight = Math.max(1, Math.round(font.lineHeight * textScale));
        int innerWidth = Math.max(16, Math.round((element.w - 6) / textScale));
        int maxLines = Math.max(1, (element.h - 6) / lineHeight);
        int lineY = baseY + 3;
        int count = 0;
        if (editorOverlay) {
            int cursorIndex = Mth.clamp(visualTextEditor.getCursorIndex(), 0, drawText.length());
            boolean showCursor = visualTextEditor.isFocused() && (System.currentTimeMillis() / 500 % 2 == 0);
            boolean drewCursor = false;
            for (VisualTextWrap.WrappedEditorLine line : VisualTextWrap.buildWrappedEditorLines(drawText, innerWidth, font)) {
                if (count >= maxLines) break;
                int lineX = alignedLineX(font, line.text, baseX, rw, element.align);
                Component rendered = Component.literal(line.text).withStyle(style -> style
                    .withBold(element.bold)
                    .withItalic(element.italic)
                    .withUnderlined(element.underline)
                    .withStrikethrough(element.strike));
                drawScaledComponent(graphics, font, rendered, lineX, lineY, textScale, (alpha << 24) | element.textColor);

                if (showCursor && !drewCursor && cursorIndex >= line.startIndex && cursorIndex <= line.endIndex) {
                    int relative = cursorIndex - line.startIndex;
                    int cursorX = lineX + Math.round(font.width(line.text.substring(0, Math.min(relative, line.text.length()))) * textScale);
                    if (cursorIndex >= drawText.length() || cursorIndex == line.endIndex) {
                        cursorX++;
                    }
                    graphics.fill(cursorX - 1, lineY - 1, cursorX, lineY - 1 + lineHeight, 0xFF000000);
                    drewCursor = true;
                }
                lineY += lineHeight;
                count++;
            }
        } else {
            Component styled = MarkdownParser.parseTextToComponent(drawText).withStyle(style -> style
                .withBold(element.bold)
                .withItalic(element.italic)
                .withUnderlined(element.underline)
                .withStrikethrough(element.strike));
            for (var line : font.split(styled, innerWidth)) {
                if (count >= maxLines) break;
                int scaledLineWidth = Math.round(font.width(line) * textScale);
                int lineX = alignedLineX(baseX, rw, scaledLineWidth, element.align);
                drawScaledSequence(graphics, font, line, lineX, lineY, textScale, (alpha << 24) | element.textColor);
                lineY += lineHeight;
                count++;
            }
        }
        if (editMode && drawText.isBlank() && !editingThis) {
            graphics.drawString(font, "Text", baseX + 3, baseY + 3, 0x99707070, false);
        }
        if (rotatedText) {
            graphics.pose().popPose();
        }
    }

    private static int alignedLineX(Font font, String line, int baseX, int rw, VisualTextAlign align) {
        return alignedLineX(baseX, rw, font.width(line), align);
    }

    private static int alignedLineX(int baseX, int rw, int lineWidth, VisualTextAlign align) {
        if (align == VisualTextAlign.CENTER) {
            return baseX + (rw - lineWidth) / 2;
        }
        if (align == VisualTextAlign.RIGHT) {
            return baseX + rw - lineWidth - 3;
        }
        return baseX + 3;
    }

    private static void drawScaledComponent(GuiGraphics graphics, Font font, Component text, int x, int y, float scale, int color) {
        if (Math.abs(scale - 1.0f) < 0.001f) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void drawScaledSequence(GuiGraphics graphics, Font font, net.minecraft.util.FormattedCharSequence text, int x, int y, float scale, int color) {
        if (Math.abs(scale - 1.0f) < 0.001f) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static boolean isRotatable(VisualElement element) {
        return element != null && (element.type == VisualElementType.TEXT
            || element.type == VisualElementType.ITEM
            || element.type == VisualElementType.IMAGE
            || element.type == VisualElementType.ENTITY);
    }

    private static float computeEntityRenderAngle(VisualElement element) {
        float angle = element.rotation;
        if (element.spinSpeed != 0) {
            angle += ((System.currentTimeMillis() % 1000000L) / 1000.0f) * element.spinSpeed;
        }
        return angle;
    }
}
