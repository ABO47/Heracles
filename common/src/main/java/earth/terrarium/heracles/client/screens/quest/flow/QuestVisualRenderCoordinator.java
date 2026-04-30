package earth.terrarium.heracles.client.screens.quest.flow;

import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualCanvasController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElementType;
import earth.terrarium.heracles.client.screens.quest.visual.render.VisualRenderer;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class QuestVisualRenderCoordinator {
    @FunctionalInterface
    public interface PreviewEntityResolver {
        @Nullable net.minecraft.world.entity.Entity get(int index, EntityType<?> entityType);
    }

    private final Supplier<VisualCanvasController> canvasController;
    private final Supplier<String> visualBackgroundSrc;
    private final Function<String, ResourceLocation> resolveImageTexture;
    private final IntSupplier visualBackgroundOpacity;
    private final IntSupplier descriptionX;
    private final IntSupplier descriptionY;
    private final IntSupplier descriptionWidth;
    private final IntSupplier descriptionHeight;
    private final Supplier<Boolean> visualGridEnabled;
    private final IntSupplier visualGridSize;
    private final Font font;
    private final List<VisualElement> visualElements;
    private final Set<Integer> multiSelectedVisualElements;
    private final VisualEditorState visualState;
    private final VisualInteractionState interactionState;
    private final Supplier<MultiLineEditBox> visualTextEditor;
    private final PreviewEntityResolver previewEntityResolver;
    private final BiConsumer<GuiGraphics, int[]> renderInlineToolbar;
    private final Consumer<VisualElement> positionVisualTextEditor;

    public QuestVisualRenderCoordinator(
        Supplier<VisualCanvasController> canvasController,
        Supplier<String> visualBackgroundSrc,
        Function<String, ResourceLocation> resolveImageTexture,
        IntSupplier visualBackgroundOpacity,
        IntSupplier descriptionX,
        IntSupplier descriptionY,
        IntSupplier descriptionWidth,
        IntSupplier descriptionHeight,
        Supplier<Boolean> visualGridEnabled,
        IntSupplier visualGridSize,
        Font font,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        Supplier<MultiLineEditBox> visualTextEditor,
        PreviewEntityResolver previewEntityResolver,
        BiConsumer<GuiGraphics, int[]> renderInlineToolbar,
        Consumer<VisualElement> positionVisualTextEditor
    ) {
        this.canvasController = canvasController;
        this.visualBackgroundSrc = visualBackgroundSrc;
        this.resolveImageTexture = resolveImageTexture;
        this.visualBackgroundOpacity = visualBackgroundOpacity;
        this.descriptionX = descriptionX;
        this.descriptionY = descriptionY;
        this.descriptionWidth = descriptionWidth;
        this.descriptionHeight = descriptionHeight;
        this.visualGridEnabled = visualGridEnabled;
        this.visualGridSize = visualGridSize;
        this.font = font;
        this.visualElements = visualElements;
        this.multiSelectedVisualElements = multiSelectedVisualElements;
        this.visualState = visualState;
        this.interactionState = interactionState;
        this.visualTextEditor = visualTextEditor;
        this.previewEntityResolver = previewEntityResolver;
        this.renderInlineToolbar = renderInlineToolbar;
        this.positionVisualTextEditor = positionVisualTextEditor;
    }

    public void renderVisualTab(GuiGraphics graphics, int mouseX, int mouseY) {
        VisualCanvasController canvas = this.canvasController.get();
        if (canvas == null) return;

        VisualRenderer.renderVisualCanvas(
            graphics,
            mouseX,
            mouseY,
            canvas::clampVisualScroll,
            this.visualBackgroundSrc,
            this.resolveImageTexture,
            this.visualBackgroundOpacity.getAsInt(),
            this.descriptionX.getAsInt(),
            this.descriptionY.getAsInt(),
            this.descriptionWidth.getAsInt(),
            this.descriptionHeight.getAsInt(),
            this.visualGridEnabled.get(),
            this.visualGridSize.getAsInt(),
            this.visualState.visualScrollX,
            this.visualState.visualScrollY,
            this.font,
            this.visualElements,
            this.multiSelectedVisualElements,
            this.visualState.selectedVisualElement,
            this.visualState.editingTextElement,
            this.visualTextEditor.get(),
            canvas::canvasToScreenX,
            canvas::canvasToScreenY,
            canvas::screenToCanvasX,
            canvas::screenToCanvasY,
            this.previewEntityResolver::get,
            this.visualState.alignmentGuideX,
            this.visualState.alignmentGuideY,
            this.interactionState.selectingVisualElements,
            this.interactionState.selectBoxStartX,
            this.interactionState.selectBoxStartY,
            this.interactionState.selectBoxEndX,
            this.interactionState.selectBoxEndY,
            this.renderInlineToolbar,
            true
        );
        syncLiveTextEditorToElement();
    }

    private void syncLiveTextEditorToElement() {
        if (this.visualState.editingTextElement < 0 || this.visualState.editingTextElement >= this.visualElements.size()) return;
        VisualElement editing = this.visualElements.get(this.visualState.editingTextElement);
        MultiLineEditBox editor = this.visualTextEditor.get();
        if (editing.type == VisualElementType.TEXT && editor != null) {
            String live = editor.getValue();
            if (!live.equals(editing.text)) {
                editing.text = live;
                this.interactionState.pendingVisualSync = true;
            }
        }
        this.positionVisualTextEditor.accept(editing);
    }
}
