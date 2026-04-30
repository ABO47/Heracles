package earth.terrarium.heracles.client.screens.quest.visual.toolbar;

import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualTextAlign;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntUnaryOperator;

public final class VisualInlineToolbarController {
    private VisualInlineToolbarController() {
    }

    public static @Nullable int[] inlineToolbarBounds(
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        int descriptionX,
        int descriptionY,
        int descriptionWidth,
        int descriptionHeight,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        int toolCount,
        int toolColumns
    ) {
        if (!VisualTextEditingActions.ensureSelectedTextElement(visualElements, visualState)) return null;
        VisualElement element = visualElements.get(visualState.selectedVisualElement);
        int buttonW = 18;
        int rows = (toolCount + toolColumns - 1) / toolColumns;
        int toolbarW = toolColumns * buttonW + 4;
        int toolbarH = rows * 16 + 4;
        int tx = canvasToScreenX.applyAsInt(element.x);
        int ty = canvasToScreenY.applyAsInt(element.y) - toolbarH - 2;
        if (ty < descriptionY + 2) {
            ty = canvasToScreenY.applyAsInt(element.y + element.h) + 2;
        }
        if (tx + toolbarW > descriptionX + descriptionWidth) tx = descriptionX + descriptionWidth - toolbarW;
        if (tx < descriptionX) tx = descriptionX;
        if (ty + toolbarH > descriptionY + descriptionHeight) ty = descriptionY + descriptionHeight - toolbarH;
        if (ty < descriptionY) ty = descriptionY;
        return new int[]{tx, ty, toolbarW, toolbarH, buttonW};
    }

    public static void renderInlineToolbar(
        GuiGraphics graphics,
        Font font,
        ResourceLocation inlineToolbarTexture,
        int mouseX,
        int mouseY,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        int descriptionX,
        int descriptionY,
        int descriptionWidth,
        int descriptionHeight,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        int toolColumns,
        int toolCount
    ) {
        if (!VisualTextEditingActions.ensureSelectedTextElement(visualElements, visualState)) return;
        int[] bounds = inlineToolbarBounds(
            visualElements,
            visualState,
            descriptionX,
            descriptionY,
            descriptionWidth,
            descriptionHeight,
            canvasToScreenX,
            canvasToScreenY,
            toolCount,
            toolColumns
        );
        if (bounds == null) return;
        int selectedTextColor = VisualTextEditingActions.ensureSelectedTextElement(visualElements, visualState)
            ? visualElements.get(visualState.selectedVisualElement).textColor
            : -1;
        InlineToolbarUi.render(
            graphics,
            font,
            inlineToolbarTexture,
            mouseX,
            mouseY,
            bounds[0],
            bounds[1],
            bounds[2],
            bounds[3],
            bounds[4],
            toolColumns,
            toolCount,
            selectedTextColor
        );
    }

    public static boolean handleInlineToolbarClick(
        double mouseX,
        double mouseY,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        @Nullable MultiLineEditBox visualTextEditor,
        int descriptionX,
        int descriptionY,
        int descriptionWidth,
        int descriptionHeight,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        int toolColumns,
        int toolCount,
        Runnable onSyncRequired,
        Runnable onOpenSelectedTextColorPicker,
        Runnable onToggleCase,
        Runnable onOpenTextSize
    ) {
        if (!VisualTextEditingActions.ensureSelectedTextElement(visualElements, visualState)) return false;
        int[] bounds = inlineToolbarBounds(
            visualElements,
            visualState,
            descriptionX,
            descriptionY,
            descriptionWidth,
            descriptionHeight,
            canvasToScreenX,
            canvasToScreenY,
            toolCount,
            toolColumns
        );
        if (bounds == null) return false;
        int index = InlineToolbarUi.hitTestIndex(mouseX, mouseY, bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], toolColumns, toolCount);
        if (index == -1) return false;
        if (index == -2) return true;
        switch (index) {
            case 0 -> {
                if (!VisualTextEditingActions.applySelectionWrapper(visualTextEditor, visualState, visualElements, interactionState, "**", "**")) {
                    VisualTextEditingActions.applyTextToggle(visualElements, visualState, visualTextEditor, VisualTextActions::toggleBold, null, onSyncRequired);
                }
            }
            case 1 -> {
                if (!VisualTextEditingActions.applySelectionWrapper(visualTextEditor, visualState, visualElements, interactionState, "--", "--")) {
                    VisualTextEditingActions.applyTextToggle(visualElements, visualState, visualTextEditor, VisualTextActions::toggleItalic, null, onSyncRequired);
                }
            }
            case 2 -> {
                if (!VisualTextEditingActions.applySelectionWrapper(visualTextEditor, visualState, visualElements, interactionState, "__", "__")) {
                    VisualTextEditingActions.applyTextToggle(visualElements, visualState, visualTextEditor, VisualTextActions::toggleUnderline, null, onSyncRequired);
                }
            }
            case 3 -> {
                if (!VisualTextEditingActions.applySelectionWrapper(visualTextEditor, visualState, visualElements, interactionState, "~~", "~~")
                    && VisualTextEditingActions.ensureSelectedTextElement(visualElements, visualState)) {
                    VisualElement element = visualElements.get(visualState.selectedVisualElement);
                    VisualTextActions.toggleStrike(element);
                    onSyncRequired.run();
                }
            }
            case 4 -> {
                if (!VisualTextEditingActions.applySelectionWrapper(visualTextEditor, visualState, visualElements, interactionState, null, null)) {
                    VisualTextEditingActions.applyTextToggle(
                        visualElements,
                        visualState,
                        visualTextEditor,
                        VisualTextActions::toggleBlockquote,
                        element -> visualTextEditor.setValue(element.text),
                        onSyncRequired
                    );
                }
            }
            case 5 -> {
                if (!VisualTextEditingActions.applySelectionWrapper(visualTextEditor, visualState, visualElements, interactionState, "||", "||")) {
                    VisualTextEditingActions.applyTextToggle(
                        visualElements,
                        visualState,
                        visualTextEditor,
                        VisualTextActions::toggleObfuscated,
                        element -> visualTextEditor.setValue(element.text),
                        onSyncRequired
                    );
                }
            }
            case 6 -> VisualTextEditingActions.setSelectedTextAlign(visualElements, visualState, VisualTextAlign.LEFT, onSyncRequired);
            case 7 -> VisualTextEditingActions.setSelectedTextAlign(visualElements, visualState, VisualTextAlign.CENTER, onSyncRequired);
            case 8 -> VisualTextEditingActions.setSelectedTextAlign(visualElements, visualState, VisualTextAlign.RIGHT, onSyncRequired);
            case 9 -> onOpenSelectedTextColorPicker.run();
            case 10 -> onToggleCase.run();
            case 11 -> onOpenTextSize.run();
            default -> {
            }
        }
        return true;
    }
}
