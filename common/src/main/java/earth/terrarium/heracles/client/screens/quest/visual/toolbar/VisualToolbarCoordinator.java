package earth.terrarium.heracles.client.screens.quest.visual.toolbar;

import earth.terrarium.heracles.client.handlers.DisplayConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class VisualToolbarCoordinator {
    private static final int[] ALLOWED_GRID_SIZES = new int[]{4, 8, 16, 32};
    private final Font font;
    private final int contentX;
    private final ResourceLocation headingTexture;
    private final ResourceLocation gridToggleTexture;
    private final ResourceLocation gridLockTexture;
    private final Runnable onModeToggle;
    private final Supplier<VisualToolbarBuilder.ToolbarWidgets> toolbarSupplier;
    private final Consumer<VisualToolbarBuilder.ToolbarWidgets> toolbarSetter;
    private final BooleanSupplier visualGridEnabled;
    private final Consumer<Boolean> setVisualGridEnabled;
    private final BooleanSupplier visualGridSnap;
    private final Consumer<Boolean> setVisualGridSnap;
    private final BooleanSupplier guideSnapEnabled;
    private final Consumer<Boolean> setGuideSnapEnabled;
    private final BooleanSupplier centerSnapEnabled;
    private final Consumer<Boolean> setCenterSnapEnabled;
    private final IntSupplier visualGridSize;
    private final IntConsumer setVisualGridSize;
    private final IntSupplier guideSnapDistance;
    private final IntConsumer setGuideSnapDistance;
    private final IntSupplier visualBackgroundOpacity;
    private final IntConsumer setVisualBackgroundOpacity;
    private final Runnable updateModeButtons;
    private final Runnable applyBackgroundOpacityField;
    private final Runnable applyBackgroundOpacityFieldAndNormalize;
    private final Function<AbstractWidget, AbstractWidget> addRenderableWidget;

    public VisualToolbarCoordinator(
        Font font,
        int contentX,
        ResourceLocation headingTexture,
        ResourceLocation gridToggleTexture,
        ResourceLocation gridLockTexture,
        Runnable onModeToggle,
        Supplier<VisualToolbarBuilder.ToolbarWidgets> toolbarSupplier,
        Consumer<VisualToolbarBuilder.ToolbarWidgets> toolbarSetter,
        BooleanSupplier visualGridEnabled,
        Consumer<Boolean> setVisualGridEnabled,
        BooleanSupplier visualGridSnap,
        Consumer<Boolean> setVisualGridSnap,
        BooleanSupplier guideSnapEnabled,
        Consumer<Boolean> setGuideSnapEnabled,
        BooleanSupplier centerSnapEnabled,
        Consumer<Boolean> setCenterSnapEnabled,
        IntSupplier visualGridSize,
        IntConsumer setVisualGridSize,
        IntSupplier guideSnapDistance,
        IntConsumer setGuideSnapDistance,
        IntSupplier visualBackgroundOpacity,
        IntConsumer setVisualBackgroundOpacity,
        Runnable updateModeButtons,
        Runnable applyBackgroundOpacityField,
        Runnable applyBackgroundOpacityFieldAndNormalize,
        Function<AbstractWidget, AbstractWidget> addRenderableWidget
    ) {
        this.font = font;
        this.contentX = contentX;
        this.headingTexture = headingTexture;
        this.gridToggleTexture = gridToggleTexture;
        this.gridLockTexture = gridLockTexture;
        this.onModeToggle = onModeToggle;
        this.toolbarSupplier = toolbarSupplier;
        this.toolbarSetter = toolbarSetter;
        this.visualGridEnabled = visualGridEnabled;
        this.setVisualGridEnabled = setVisualGridEnabled;
        this.visualGridSnap = visualGridSnap;
        this.setVisualGridSnap = setVisualGridSnap;
        this.guideSnapEnabled = guideSnapEnabled;
        this.setGuideSnapEnabled = setGuideSnapEnabled;
        this.centerSnapEnabled = centerSnapEnabled;
        this.setCenterSnapEnabled = setCenterSnapEnabled;
        this.visualGridSize = visualGridSize;
        this.setVisualGridSize = setVisualGridSize;
        this.guideSnapDistance = guideSnapDistance;
        this.setGuideSnapDistance = setGuideSnapDistance;
        this.visualBackgroundOpacity = visualBackgroundOpacity;
        this.setVisualBackgroundOpacity = setVisualBackgroundOpacity;
        this.updateModeButtons = updateModeButtons;
        this.applyBackgroundOpacityField = applyBackgroundOpacityField;
        this.applyBackgroundOpacityFieldAndNormalize = applyBackgroundOpacityFieldAndNormalize;
        this.addRenderableWidget = addRenderableWidget;
    }

    public VisualToolbarBuilder.ToolbarWidgets build() {
        boolean hasGridToggleTexture = Minecraft.getInstance().getResourceManager().getResource(this.gridToggleTexture).isPresent();
        boolean hasGridLockTexture = Minecraft.getInstance().getResourceManager().getResource(this.gridLockTexture).isPresent();

        VisualToolbarBuilder.ToolbarWidgets widgets = VisualToolbarBuilder.build(
            this.font,
            this.contentX,
            this.headingTexture,
            this.gridToggleTexture,
            this.gridLockTexture,
            hasGridToggleTexture,
            hasGridLockTexture,
            this.onModeToggle,
            this::toggleVisualGridEnabled,
            this::toggleVisualGridSnap,
            this::toggleGuideSnapEnabled,
            this::toggleCenterSnapEnabled,
            this::decrementVisualGridSize,
            this::onGridSizeTyped,
            this::normalizeGridSizeField,
            this::incrementVisualGridSize,
            this::onGuideSnapDistanceTyped,
            this::onBackgroundOpacitySliderChanged,
            s -> this.applyBackgroundOpacityField.run(),
            s -> this.applyBackgroundOpacityFieldAndNormalize.run(),
            this.visualGridSize.getAsInt(),
            this.guideSnapDistance.getAsInt(),
            this.visualBackgroundOpacity.getAsInt(),
            this.addRenderableWidget
        );
        this.toolbarSetter.accept(widgets);
        return widgets;
    }

    private void toggleVisualGridEnabled() {
        boolean next = !this.visualGridEnabled.getAsBoolean();
        this.setVisualGridEnabled.accept(next);
        DisplayConfig.questDescriptionGridEnabled = next;
        DisplayConfig.save();
        this.updateModeButtons.run();
    }

    private void toggleVisualGridSnap() {
        boolean next = !this.visualGridSnap.getAsBoolean();
        this.setVisualGridSnap.accept(next);
        DisplayConfig.questDescriptionGridSnap = next;
        DisplayConfig.save();
        this.updateModeButtons.run();
    }

    private void toggleGuideSnapEnabled() {
        boolean next = !this.guideSnapEnabled.getAsBoolean();
        this.setGuideSnapEnabled.accept(next);
        DisplayConfig.questDescriptionGuideSnapEnabled = next;
        DisplayConfig.save();
        this.updateModeButtons.run();
    }

    private void toggleCenterSnapEnabled() {
        boolean next = !this.centerSnapEnabled.getAsBoolean();
        this.setCenterSnapEnabled.accept(next);
        DisplayConfig.questDescriptionCenterSnapEnabled = next;
        DisplayConfig.save();
        this.updateModeButtons.run();
    }

    private void decrementVisualGridSize() {
        int next = previousGridSize(this.visualGridSize.getAsInt());
        this.setVisualGridSize.accept(next);
        DisplayConfig.questDescriptionGridSize = next;
        DisplayConfig.save();
        VisualToolbarBuilder.ToolbarWidgets toolbar = this.toolbarSupplier.get();
        if (toolbar != null && toolbar.gridSizeField() != null) {
            toolbar.gridSizeField().setValue(String.valueOf(next));
        }
    }

    private void onGridSizeTyped(String value) {
        if (value == null || value.isBlank()) return;
        try {
            int next = nearestGridSize(Integer.parseInt(value.trim()));
            this.setVisualGridSize.accept(next);
            DisplayConfig.questDescriptionGridSize = next;
            DisplayConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void normalizeGridSizeField(String ignored) {
        VisualToolbarBuilder.ToolbarWidgets toolbar = this.toolbarSupplier.get();
        if (toolbar != null && toolbar.gridSizeField() != null) {
            toolbar.gridSizeField().setValue(String.valueOf(this.visualGridSize.getAsInt()));
        }
    }

    private void incrementVisualGridSize() {
        int next = nextGridSize(this.visualGridSize.getAsInt());
        this.setVisualGridSize.accept(next);
        DisplayConfig.questDescriptionGridSize = next;
        DisplayConfig.save();
        VisualToolbarBuilder.ToolbarWidgets toolbar = this.toolbarSupplier.get();
        if (toolbar != null && toolbar.gridSizeField() != null) {
            toolbar.gridSizeField().setValue(String.valueOf(next));
        }
    }

    private void onGuideSnapDistanceTyped(String value) {
        if (value == null || value.isBlank()) return;
        try {
            int next = Mth.clamp(Integer.parseInt(value.trim()), 1, 24);
            this.setGuideSnapDistance.accept(next);
            DisplayConfig.questDescriptionGuideSnapDistance = next;
            DisplayConfig.save();
        } catch (NumberFormatException ignored) {
        }
    }

    private void onBackgroundOpacitySliderChanged(int value) {
        int next = Mth.clamp(value, 0, 100);
        this.setVisualBackgroundOpacity.accept(next);
        VisualToolbarBuilder.ToolbarWidgets toolbar = this.toolbarSupplier.get();
        if (toolbar != null && toolbar.backgroundOpacityField() != null) {
            toolbar.backgroundOpacityField().setValue(String.valueOf(next));
        }
    }

    private static int nearestGridSize(int value) {
        int best = ALLOWED_GRID_SIZES[0];
        int bestDist = Math.abs(value - best);
        for (int size : ALLOWED_GRID_SIZES) {
            int dist = Math.abs(value - size);
            if (dist < bestDist) {
                best = size;
                bestDist = dist;
            }
        }
        return best;
    }

    private static int nextGridSize(int value) {
        for (int size : ALLOWED_GRID_SIZES) {
            if (size > value) return size;
        }
        return ALLOWED_GRID_SIZES[ALLOWED_GRID_SIZES.length - 1];
    }

    private static int previousGridSize(int value) {
        for (int i = ALLOWED_GRID_SIZES.length - 1; i >= 0; i--) {
            if (ALLOWED_GRID_SIZES[i] < value) return ALLOWED_GRID_SIZES[i];
        }
        return ALLOWED_GRID_SIZES[0];
    }
}
