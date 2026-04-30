package earth.terrarium.heracles.client.screens.quests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import com.teamresourceful.resourcefullib.client.scissor.ScissorBoxStack;
import com.teamresourceful.resourcefullib.client.screens.CursorScreen;
import com.teamresourceful.resourcefullib.client.utils.CursorUtils;
import com.teamresourceful.resourcefullib.client.utils.ScreenUtils;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.utils.ClientUtils;
import earth.terrarium.heracles.client.utils.QuestBackgroundTextures;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.client.utils.TexturePlacements;
import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class QuestWidget {

    private final ClientQuests.QuestEntry entry;
    private final Quest quest;
    private final ModUtils.QuestStatus status;
    private final String id;

    private TexturePlacements.Info info = TexturePlacements.NO_OFFSET_24X;

    public QuestWidget(ClientQuests.QuestEntry entry, ModUtils.QuestStatus status) {
        this.entry = entry;
        this.quest = entry.value();
        this.status = status;
        this.id = entry.key();
    }

    public void render(GuiGraphics graphics, ScissorBoxStack scissor, int x, int y, int mouseX, int mouseY, boolean hovered, float ignoredPartialTicks) {
        hovered = hovered && isMouseOver(mouseX - x, mouseY - y);

        info = TexturePlacements.getOrDefault(quest.display().iconBackground(), TexturePlacements.NO_OFFSET_24X);
        float scale = nodeScale();

        
        
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();

        try (var pose = new CloseablePoseStack(graphics)) {
            pose.translate(x + x(), y + y(), 0);
            pose.scale(scale, scale, 1.0f);

            ResourceLocation background = quest.display().iconBackground();
            if (QuestBackgroundTextures.isCustomBackground(background)) {
                ResourceLocation texture = QuestBackgroundTextures.resolve(background);
                float[] tint = tintForState(status, hovered);
                RenderSystem.setShaderColor(tint[0], tint[1], tint[2], 1.0f);
                graphics.blit(texture,
                    info.xOffset(), info.yOffset(),
                    0, 0,
                    info.width(), info.height(),
                    info.width(), info.height()
                );
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                graphics.blit(background,
                    info.xOffset(), info.yOffset(),
                    status.ordinal() * info.width(), 0,
                    info.width(), info.height(),
                    info.width() * 5, info.height()
                );

                if (hovered) {
                    graphics.blit(background,
                        info.xOffset(), info.yOffset(),
                        4 * info.width(), 0,
                        info.width(), info.height(),
                        info.width() * 5, info.height()
                    );
                }
            }
            quest.display().icon().render(graphics, scissor, 4, 4, 24, 24);
        }
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        CursorUtils.setCursor(hovered, CursorScreen.Cursor.POINTER);
        if (hovered && (!(ClientUtils.screen() instanceof QuestsScreen screen) || !screen.isTemporaryWidgetVisible())) {
            String subtitleText = quest.display().subtitle().getString().trim();
            if (subtitleText.isBlank()) {
                ScreenUtils.setTooltip(quest.display().title().copy().withStyle(style -> style.withBold(true)), false);
            } else {
                List<Component> lines = new ArrayList<>(List.of(
                    quest.display().title().copy().withStyle(style -> style.withBold(true)),
                    quest.display().subtitle()
                ));
                if (status == ModUtils.QuestStatus.COMPLETED) lines.add(ConstantComponents.Quests.CLAIMABLE);
                ScreenUtils.setTooltip(lines, false);
            }
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        int left = x() + scaledOffsetX();
        int top = y() + scaledOffsetY();
        return mouseX >= left && mouseX <= left + width() && mouseY >= top && mouseY <= top + height();
    }

    public int x() {
        return position().x();
    }

    public int y() {
        return position().y();
    }

    public Vector2i position() {
        if (ClientUtils.screen() instanceof QuestsScreen screen) {
            return this.quest.display().position(screen.getGroup());
        }
        return new Vector2i();
    }

    public String group() {
        if (ClientUtils.screen() instanceof QuestsScreen screen) {
            return screen.getGroup();
        }
        return "";
    }

    public Quest quest() {
        return this.quest;
    }

    public ClientQuests.QuestEntry entry() {
        return this.entry;
    }

    public String id() {
        return this.id;
    }

    public TexturePlacements.Info getTextureInfo() {
        return this.info;
    }

    public float nodeScale() {
        float value = this.quest.display().groups().getOrDefault(group(), new earth.terrarium.heracles.api.quests.GroupDisplay(group(), new Vector2i())).nodeScale();
        if (Float.isNaN(value) || Float.isInfinite(value)) return 1.0f;
        return Math.max(0.5f, value);
    }

    public int width() {
        return Math.max(1, Math.round(info.width() * nodeScale()));
    }

    public int height() {
        return Math.max(1, Math.round(info.height() * nodeScale()));
    }

    public int scaledOffsetX() {
        return Math.round(info.xOffset() * nodeScale());
    }

    public int scaledOffsetY() {
        return Math.round(info.yOffset() * nodeScale());
    }

    public int centerX() {
        return x() + scaledOffsetX() + (width() / 2);
    }

    public int centerY() {
        return y() + scaledOffsetY() + (height() / 2);
    }

    private static float[] tintForState(ModUtils.QuestStatus status, boolean hovered) {
        if (hovered) {
            return rgb(0x717178); // hover
        }
        return switch (status) {
            case COMPLETED, COMPLETED_CLAIMED -> rgb(0x6CCB4D); // completed
            case LOCKED, IN_PROGRESS -> rgb(0x6A6B6D); // uncompleted
        };
    }

    private static float[] rgb(int hex) {
        float r = ((hex >> 16) & 0xFF) / 255.0f;
        float g = ((hex >> 8) & 0xFF) / 255.0f;
        float b = (hex & 0xFF) / 255.0f;
        return new float[]{r, g, b};
    }
}
