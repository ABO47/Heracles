package earth.terrarium.heracles.client.screens.quest.visual.toolbar;

import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.widgets.GridOpacitySlider;
import earth.terrarium.heracles.client.widgets.SelectableImageButton;
import earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;

public final class VisualToolbarStateApplier {
    private VisualToolbarStateApplier() {
    }

    public static void apply(
        boolean visualMode,
        SelectableImageButton modeToggleButton,
        SelectableImageButton gridButton,
        SelectableImageButton snapButton,
        SelectableImageButton guideSnapButton,
        SelectableImageButton centerSnapButton,
        Button gridSizeMinusButton,
        Button gridSizePlusButton,
        EnterableEditBox gridSizeField,
        EnterableEditBox guideSnapDistanceField,
        GridOpacitySlider backgroundOpacitySlider,
        EnterableEditBox backgroundOpacityField,
        boolean visualGridEnabled,
        boolean visualGridSnap,
        boolean guideSnapEnabled,
        boolean centerSnapEnabled,
        int visualGridSize,
        int guideSnapDistance,
        int visualBackgroundOpacity,
        MultiLineEditBox visualTextEditor,
        VisualEditorState visualState
    ) {
        boolean visual = visualMode;
        if (modeToggleButton != null) {
            modeToggleButton.setSelected(visual);
            modeToggleButton.setTooltip(Tooltip.create(net.minecraft.network.chat.Component.literal(
                visual ? "Switch to Raw Description" : "Switch to Visual Description"
            )));
        }
        if (gridButton != null) {
            gridButton.visible = visual;
            gridButton.setSelected(visualGridEnabled);
        }
        if (snapButton != null) {
            snapButton.visible = visual;
            snapButton.setSelected(visualGridSnap);
        }
        if (guideSnapButton != null) {
            guideSnapButton.visible = visual;
            guideSnapButton.setSelected(guideSnapEnabled);
            guideSnapButton.active = true;
        }
        if (centerSnapButton != null) {
            centerSnapButton.visible = visual;
            centerSnapButton.setSelected(centerSnapEnabled);
            centerSnapButton.active = guideSnapEnabled;
        }
        if (gridSizeMinusButton != null) gridSizeMinusButton.visible = visual;
        if (gridSizePlusButton != null) gridSizePlusButton.visible = visual;
        if (gridSizeField != null) {
            gridSizeField.setVisible(visual);
            gridSizeField.setValue(String.valueOf(visualGridSize));
        }
        if (guideSnapDistanceField != null) {
            guideSnapDistanceField.setVisible(visual && guideSnapEnabled);
            guideSnapDistanceField.setValue(String.valueOf(guideSnapDistance));
        }
        if (backgroundOpacitySlider != null) backgroundOpacitySlider.visible = visual;
        if (backgroundOpacityField != null) {
            backgroundOpacityField.setVisible(visual);
            backgroundOpacityField.setValue(String.valueOf(visualBackgroundOpacity));
        }
        if (visualTextEditor != null && !visual) {
            visualTextEditor.visible = false;
            visualTextEditor.setFocused(false);
            visualState.editingTextElement = -1;
        }
    }
}
