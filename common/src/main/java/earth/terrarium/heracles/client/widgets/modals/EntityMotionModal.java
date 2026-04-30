package earth.terrarium.heracles.client.widgets.modals;

import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import earth.terrarium.heracles.client.widgets.GridOpacitySlider;
import earth.terrarium.heracles.client.widgets.base.BaseModal;
import earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox;
import earth.terrarium.heracles.client.widgets.buttons.ThemedButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

public class EntityMotionModal extends BaseModal {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 130;

    private final GridOpacitySlider rotationSlider;
    private final GridOpacitySlider speedSlider;
    private final EnterableEditBox rotationField;
    private final EnterableEditBox speedField;
    private final Consumer<MotionState> onApply;

    public record MotionState(int rotation, int spinSpeed) {
    }

    public EntityMotionModal(int screenWidth, int screenHeight, int rotation, int spinSpeed, Consumer<MotionState> onApply) {
        super(screenWidth, screenHeight, WIDTH, HEIGHT, 2);
        this.onApply = onApply;

        this.rotationField = addChild(new EnterableEditBox(this.font, this.x + 232, this.y + 28, 56, 14, Component.nullToEmpty("")));
        this.rotationField.setMaxLength(4);
        this.rotationField.setValue(String.valueOf(Mth.clamp(rotation, 0, 360)));
        this.rotationField.setResponder(s -> applyRotationField(false));
        this.rotationField.setEnter(s -> applyRotationField(true));
        this.rotationSlider = addChild(new GridOpacitySlider(this.x + 10, this.y + 30, 214, 12, Math.round((Mth.clamp(rotation, 0, 360) / 360.0f) * 100.0f), v -> {
            int rot = Math.round((Mth.clamp(v, 0, 100) / 100.0f) * 360.0f);
            this.rotationField.setValue(String.valueOf(rot));
            publish();
        }));

        this.speedField = addChild(new EnterableEditBox(this.font, this.x + 232, this.y + 58, 56, 14, Component.nullToEmpty("")));
        this.speedField.setMaxLength(4);
        this.speedField.setValue(String.valueOf(Mth.clamp(spinSpeed, 0, 360)));
        this.speedField.setResponder(s -> applySpeedField(false));
        this.speedField.setEnter(s -> applySpeedField(true));
        this.speedSlider = addChild(new GridOpacitySlider(this.x + 10, this.y + 60, 214, 12, Math.round((Mth.clamp(spinSpeed, 0, 360) / 360.0f) * 100.0f), v -> {
            int speed = Math.round((Mth.clamp(v, 0, 100) / 100.0f) * 360.0f);
            this.speedField.setValue(String.valueOf(speed));
            publish();
        }));

        addChild(ThemedButton.builder(Component.literal("Done"), b -> setVisible(false))
            .bounds(this.x + WIDTH - 62, this.y + HEIGHT - 20, 52, 14).build());
    }

    @Override
    protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        try (CloseablePoseStack ignored = new CloseablePoseStack(graphics)) {
            graphics.fill(this.x, this.y, this.x + WIDTH, this.y + HEIGHT, 0xEE121212);
            graphics.fill(this.x + 1, this.y + 1, this.x + WIDTH - 1, this.y + HEIGHT - 1, 0xFF1E1E1E);
            renderChildren(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(this.font, "Entity Motion", this.x + 10, this.y + 10, 0xFFFFFF, false);
        graphics.drawString(this.font, "Rotation", this.x + 10, this.y + 22, 0xD6D6D6, false);
        graphics.drawString(this.font, "Spin speed (deg/s)", this.x + 10, this.y + 52, 0xD6D6D6, false);
    }

    private void applyRotationField(boolean normalize) {
        int value = parse(this.rotationField.getValue(), 0, 360, -1);
        if (value < 0) return;
        this.rotationSlider.setValue(Math.round((value / 360.0f) * 100.0f));
        if (normalize) this.rotationField.setValue(String.valueOf(value));
        publish();
    }

    private void applySpeedField(boolean normalize) {
        int value = parse(this.speedField.getValue(), 0, 360, -1);
        if (value < 0) return;
        this.speedSlider.setValue(Math.round((value / 360.0f) * 100.0f));
        if (normalize) this.speedField.setValue(String.valueOf(value));
        publish();
    }

    private void publish() {
        if (this.onApply == null) return;
        int rotation = parse(this.rotationField.getValue(), 0, 360, 0);
        int speed = parse(this.speedField.getValue(), 0, 360, 0);
        this.onApply.accept(new MotionState(rotation, speed));
    }

    private static int parse(String raw, int min, int max, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Mth.clamp(Integer.parseInt(raw.trim()), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
