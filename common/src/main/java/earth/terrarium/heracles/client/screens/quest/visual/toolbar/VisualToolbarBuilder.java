package earth.terrarium.heracles.client.screens.quest.visual.toolbar;

import earth.terrarium.heracles.client.widgets.GridOpacitySlider;
import earth.terrarium.heracles.client.widgets.SelectableImageButton;
import earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox;
import earth.terrarium.heracles.client.widgets.buttons.ThemedButton;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

public final class VisualToolbarBuilder {
    public record ToolbarWidgets(
        SelectableImageButton modeToggleButton,
        SelectableImageButton gridButton,
        SelectableImageButton snapButton,
        SelectableImageButton guideSnapButton,
        SelectableImageButton centerSnapButton,
        Button gridSizeMinusButton,
        EnterableEditBox gridSizeField,
        Button gridSizePlusButton,
        EnterableEditBox guideSnapDistanceField,
        GridOpacitySlider backgroundOpacitySlider,
        EnterableEditBox backgroundOpacityField
    ) {
    }

    private VisualToolbarBuilder() {
    }

    public static ToolbarWidgets build(
        Font font,
        int contentX,
        ResourceLocation headingTexture,
        ResourceLocation gridToggleTexture,
        ResourceLocation gridLockTexture,
        boolean hasGridToggleTexture,
        boolean hasGridLockTexture,
        Runnable onModeToggle,
        Runnable onGridToggle,
        Runnable onSnapToggle,
        Runnable onGuideSnapToggle,
        Runnable onCenterSnapToggle,
        Runnable onGridSizeMinus,
        Consumer<String> onGridSizeChanged,
        Consumer<String> onGridSizeEnter,
        Runnable onGridSizePlus,
        Consumer<String> onGuideSnapDistanceChanged,
        IntConsumer onBackgroundOpacitySlider,
        Consumer<String> onBackgroundOpacityFieldChanged,
        Consumer<String> onBackgroundOpacityFieldEnter,
        int initialGridSize,
        int initialGuideSnapDistance,
        int initialBackgroundOpacity,
        Function<AbstractWidget, AbstractWidget> addRenderableWidget
    ) {
        SelectableImageButton modeToggleButton = (SelectableImageButton) addRenderableWidget.apply(new SelectableImageButton(
            contentX + 4, 1, 11, 11,
            0, hasGridToggleTexture ? 0 : 15, 11,
            hasGridToggleTexture ? gridToggleTexture : headingTexture,
            hasGridToggleTexture ? 11 : 256,
            hasGridToggleTexture ? 22 : 256,
            button -> onModeToggle.run()
        ));
        modeToggleButton.setTooltip(Tooltip.create(Component.literal("Description Mode (Visual/Raw)")));

        SelectableImageButton gridButton = (SelectableImageButton) addRenderableWidget.apply(new SelectableImageButton(
            contentX + 18, 1, 11, 11,
            0, hasGridToggleTexture ? 0 : 15, 11,
            hasGridToggleTexture ? gridToggleTexture : headingTexture,
            hasGridToggleTexture ? 11 : 256,
            hasGridToggleTexture ? 22 : 256,
            button -> onGridToggle.run()
        ));
        gridButton.setTooltip(Tooltip.create(Component.literal("Grid")));

        SelectableImageButton snapButton = (SelectableImageButton) addRenderableWidget.apply(new SelectableImageButton(
            contentX + 32, 1, 11, 11,
            hasGridLockTexture ? 0 : 11, hasGridLockTexture ? 0 : 15, 11,
            hasGridLockTexture ? gridLockTexture : headingTexture,
            hasGridLockTexture ? 11 : 256,
            hasGridLockTexture ? 22 : 256,
            button -> onSnapToggle.run()
        ));
        snapButton.setTooltip(Tooltip.create(Component.literal("Snap")));

        SelectableImageButton guideSnapButton = (SelectableImageButton) addRenderableWidget.apply(new SelectableImageButton(
            contentX + 103, 1, 11, 11,
            hasGridLockTexture ? 0 : 11, hasGridLockTexture ? 0 : 15, 11,
            hasGridLockTexture ? gridLockTexture : headingTexture,
            hasGridLockTexture ? 11 : 256,
            hasGridLockTexture ? 22 : 256,
            button -> onGuideSnapToggle.run()
        ));
        guideSnapButton.setTooltip(Tooltip.create(Component.literal("Snap To Guides")));

        SelectableImageButton centerSnapButton = (SelectableImageButton) addRenderableWidget.apply(new SelectableImageButton(
            contentX + 117, 1, 11, 11,
            hasGridLockTexture ? 0 : 11, hasGridLockTexture ? 0 : 15, 11,
            hasGridLockTexture ? gridLockTexture : headingTexture,
            hasGridLockTexture ? 11 : 256,
            hasGridLockTexture ? 22 : 256,
            button -> onCenterSnapToggle.run()
        ));
        centerSnapButton.setTooltip(Tooltip.create(Component.literal("Snap To Canvas Center")));

        Button gridSizeMinusButton = (Button) addRenderableWidget.apply(ThemedButton.builder(ConstantComponents.MINUS, b -> onGridSizeMinus.run())
            .bounds(contentX + 46, 1, 12, 11)
            .build());

        EnterableEditBox gridSizeField = (EnterableEditBox) addRenderableWidget.apply(new EnterableEditBox(font, contentX + 61, 1, 24, 11, Component.nullToEmpty("")));
        gridSizeField.setMaxLength(3);
        gridSizeField.setValue(String.valueOf(initialGridSize));
        gridSizeField.setResponder(onGridSizeChanged);
        gridSizeField.setEnter(onGridSizeEnter);

        Button gridSizePlusButton = (Button) addRenderableWidget.apply(ThemedButton.builder(ConstantComponents.PLUS, b -> onGridSizePlus.run())
            .bounds(contentX + 88, 1, 12, 11)
            .build());

        EnterableEditBox guideSnapDistanceField = (EnterableEditBox) addRenderableWidget.apply(new EnterableEditBox(font, contentX + 131, 1, 24, 11, Component.nullToEmpty("")));
        guideSnapDistanceField.setMaxLength(2);
        guideSnapDistanceField.setTooltip(Tooltip.create(Component.literal("Guide Snap Distance")));
        guideSnapDistanceField.setValue(String.valueOf(initialGuideSnapDistance));
        guideSnapDistanceField.setResponder(onGuideSnapDistanceChanged);

        int bgSliderX = contentX + 159;
        GridOpacitySlider backgroundOpacitySlider = (GridOpacitySlider) addRenderableWidget.apply(new GridOpacitySlider(
            bgSliderX,
            1,
            30,
            11,
            initialBackgroundOpacity,
            onBackgroundOpacitySlider::accept
        ));
        backgroundOpacitySlider.setTooltip(Tooltip.create(Component.literal("Description Background Opacity")));

        EnterableEditBox backgroundOpacityField = (EnterableEditBox) addRenderableWidget.apply(new EnterableEditBox(font, bgSliderX + 32, 1, 26, 11, Component.nullToEmpty("")));
        backgroundOpacityField.setMaxLength(3);
        backgroundOpacityField.setTooltip(Tooltip.create(Component.literal("Description Background Opacity Percent (0-100)")));
        backgroundOpacityField.setValue(String.valueOf(initialBackgroundOpacity));
        backgroundOpacityField.setResponder(onBackgroundOpacityFieldChanged);
        backgroundOpacityField.setEnter(onBackgroundOpacityFieldEnter);

        return new ToolbarWidgets(
            modeToggleButton,
            gridButton,
            snapButton,
            guideSnapButton,
            centerSnapButton,
            gridSizeMinusButton,
            gridSizeField,
            gridSizePlusButton,
            guideSnapDistanceField,
            backgroundOpacitySlider,
            backgroundOpacityField
        );
    }
}
