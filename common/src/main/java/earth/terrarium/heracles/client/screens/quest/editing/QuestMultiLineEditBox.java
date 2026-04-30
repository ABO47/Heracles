package earth.terrarium.heracles.client.screens.quest.editing;

import com.mojang.blaze3d.platform.InputConstants;
import com.teamresourceful.resourcefullib.client.components.CursorWidget;
import com.teamresourceful.resourcefullib.client.screens.CursorScreen;
import com.teamresourceful.resourcefullib.client.utils.RenderUtils;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.client.screens.AbstractQuestScreen;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import earth.terrarium.heracles.client.widgets.editor.MultilineTextField;
import earth.terrarium.heracles.client.widgets.modals.ColorPickerModal;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class QuestMultiLineEditBox extends MultiLineEditBox implements CursorWidget {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Heracles.MOD_ID, "textures/gui/editor.png");
    private static final int TOOLBAR_FIRST_X = 20;
    private static final int TOOLBAR_BUTTON_SIZE = 11;
    private static final int COLOR_BUTTON_X = 97;

    private CursorScreen.Cursor cursor = null;
    private int selectedColor = 0xF2D13F;
    
    public QuestMultiLineEditBox(int x, int y, int width, int height) {
        super(Minecraft.getInstance().font, x, y + 13, width, height - 13, text -> {
            if (text.startsWith("> ")) {
                return Component.literal("> ").withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.literal(text.substring(2)).withStyle(ChatFormatting.DARK_GRAY));
            } else if (text.startsWith("- ")) {
                return Component.literal("- ").withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.literal(text.substring(2)).withStyle(ChatFormatting.DARK_GRAY));
            } else if (text.startsWith("# ")) {
                return Component.literal("# ").withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.literal(text.substring(2)).withStyle(ChatFormatting.DARK_GRAY));
            } else if (text.startsWith("## ")) {
                return Component.literal("## ").withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.literal(text.substring(3)).withStyle(ChatFormatting.DARK_GRAY));
            } else if (text.equals("---")) {
                return Component.literal("---").withStyle(ChatFormatting.DARK_AQUA);
            }
            return Component.literal(text);
        });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.cursor = null;
        graphics.fill(this.getX() - 1, this.getY() - 13, this.getX() + this.width + 1, this.getY() + this.height + 1, 0xFF000000);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFFF0F0F0);
        super.render(graphics, mouseX, mouseY, partialTicks);

        try (var ignored = RenderUtils.createScissor(Minecraft.getInstance(), graphics, this.getX(), this.getY() - 12, this.width, 11)) {

            graphics.fill(this.getX(), this.getY() - 12, this.getX() + width, this.getY() - 1, 0xFFC6C6C6);
            graphics.fill(this.getX(), this.getY() - 1, this.getX() + width, this.getY(), 0xFF000000);

            graphics.blit(TEXTURE, this.getX() + 20, this.getY() - 12, 0, isHovered(mouseX, mouseY, 0) ? 11 : 0, 11, 11);
            graphics.blit(TEXTURE, this.getX() + 31, this.getY() - 12, 11, isHovered(mouseX, mouseY, 1) ? 11 : 0, 11, 11);
            graphics.blit(TEXTURE, this.getX() + 42, this.getY() - 12, 22, isHovered(mouseX, mouseY, 2) ? 11 : 0, 11, 11);
            graphics.blit(TEXTURE, this.getX() + 53, this.getY() - 12, 33, isHovered(mouseX, mouseY, 3) ? 11 : 0, 11, 11);
            graphics.blit(TEXTURE, this.getX() + 64, this.getY() - 12, 44, isHovered(mouseX, mouseY, 4) ? 11 : 0, 11, 11);
            graphics.blit(TEXTURE, this.getX() + 75, this.getY() - 12, 55, isHovered(mouseX, mouseY, 5) ? 11 : 0, 11, 11);
            renderColorButton(graphics, mouseX, mouseY);
        }

        if (this.cursor == null && !canClickText(mouseX, mouseY) && isMouseOver(mouseX, mouseY)) {
            cursor = CursorScreen.Cursor.DEFAULT;
        }
    }

    public boolean isHovered(int mouseX, int mouseY, int index) {
        if (mouseX >= this.getX() + TOOLBAR_FIRST_X + index * TOOLBAR_BUTTON_SIZE && mouseX < this.getX() + TOOLBAR_FIRST_X + index * TOOLBAR_BUTTON_SIZE + TOOLBAR_BUTTON_SIZE && mouseY >= this.getY() - 12 && mouseY <= this.getY() - 1) {
            cursor = CursorScreen.Cursor.POINTER;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && !canClickText(mouseX, mouseY) && button == 0) {
            int x = TOOLBAR_FIRST_X;
            for (Buttons value : Buttons.values()) {
                if (mouseX >= this.getX() + x && mouseX < this.getX() + x + TOOLBAR_BUTTON_SIZE && value.change(this.field)) {
                    return true;
                }
                x += TOOLBAR_BUTTON_SIZE;
            }
            if (isColorButtonHovered((int) mouseX, (int) mouseY)) {
                openColorPicker();
                return true;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= this.getY() - 13 && mouseY <= this.getY() + this.height;
    }

    public boolean canClickText(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.width && mouseY >= this.getY() && mouseY <= this.getY() + this.height;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) && applySlashCommandAtCursor()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean applySlashCommandAtCursor() {
        String value = this.field.value();
        int cursor = this.field.cursor();
        int lineStart = Math.max(0, value.lastIndexOf('\n', Math.max(0, cursor - 1)) + 1);
        int lineEnd = value.indexOf('\n', cursor);
        if (lineEnd == -1) lineEnd = value.length();

        String trimmed = value.substring(lineStart, lineEnd).trim();
        String replacement = expandSlashCommand(trimmed);
        if (replacement == null) return false;

        this.field.replaceRange(lineStart, lineEnd, replacement, true);
        this.field.insertText("\n");
        return true;
    }

    private static String expandSlashCommand(String commandLine) {
        if (commandLine == null || commandLine.isBlank() || !commandLine.startsWith("/")) return null;
        String[] parts = commandLine.substring(1).split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String payload = parts.length > 1 ? parts[1].trim() : "";

        return switch (command) {
            case "tip" -> "> **Tip:** " + (payload.isBlank() ? "Add helpful guidance here." : payload);
            case "warning" -> "> **Warning:** " + (payload.isBlank() ? "Add an important warning here." : payload);
            case "item" -> {
                String item = payload.isBlank() ? "minecraft:stone" : payload.replace("`", "");
                if (item.startsWith("#")) {
                    yield "<item tag=\"" + item.substring(1) + "\"/>";
                }
                yield "<item id=\"" + item + "\"/>";
            }
            case "image" -> {
                String src = payload.isBlank() ? "assets/pictures/example.png" : payload.replace("\"", "");
                yield "<image src=\"" + src + "\" w=\"240\" h=\"135\" x=\"0\" y=\"0\"/>";
            }
            default -> null;
        };
    }

    private void renderColorButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.getX() + COLOR_BUTTON_X;
        int y = this.getY() - 12;
        boolean hovered = isColorButtonHovered(mouseX, mouseY);
        int frame = hovered ? 0xFFEDEDED : 0xFF8C8C8C;
        graphics.fill(x, y, x + TOOLBAR_BUTTON_SIZE, y + TOOLBAR_BUTTON_SIZE, frame);
        graphics.fill(x + 1, y + 1, x + TOOLBAR_BUTTON_SIZE - 1, y + TOOLBAR_BUTTON_SIZE - 1, 0xFF1F1F1F);
        graphics.fill(x + 2, y + 2, x + TOOLBAR_BUTTON_SIZE - 2, y + TOOLBAR_BUTTON_SIZE - 2, 0xFF000000 | this.selectedColor);
        if (hovered) {
            graphics.drawString(Minecraft.getInstance().font, "C", x + 3, y + 2, 0xFFFFFFFF, false);
            this.cursor = CursorScreen.Cursor.POINTER;
        }
    }

    private boolean isColorButtonHovered(int mouseX, int mouseY) {
        int x = this.getX() + COLOR_BUTTON_X;
        return mouseX >= x && mouseX < x + TOOLBAR_BUTTON_SIZE && mouseY >= this.getY() - 12 && mouseY <= this.getY() - 1;
    }

    private void openColorPicker() {
        if (!(Minecraft.getInstance().screen instanceof AbstractQuestScreen<?> screen)) {
            return;
        }
        ColorPickerModal modal = new ColorPickerModal(
            Minecraft.getInstance().getWindow().getGuiScaledWidth(),
            Minecraft.getInstance().getWindow().getGuiScaledHeight(),
            this.selectedColor,
            this::applyHexColor
        );
        screen.addTemporary(modal);
        modal.setVisible(true);
    }

    private void applyHexColor(int color) {
        this.selectedColor = color & 0xFFFFFF;
        String hex = String.format(Locale.ROOT, "%06X", this.selectedColor);
        if (this.field.hasSelection()) {
            String selected = this.field.getSelectedText();
            this.field.insertText("/#{" + hex + "}/" + selected + "/#/");
            return;
        }
        this.field.insertText("/#{" + hex + "}//#/");
        this.field.seekCursor(Whence.RELATIVE, -3);
    }

    private enum Buttons {
        BOLD("**"),
        ITALIC("--"),
        UNDERLINE("__"),
        STRIKETHROUGH("~~"),
        BLOCKQUOTE(content -> {
            MultilineTextField.StringView selection = content.getSelected();
            blockquote(content, selection.beginIndex(), selection.endIndex());
            return true;
        }),
        OBFUSCATED("||"),
        ;
        private final Function<MultilineTextField, Boolean> change;

        Buttons(String text) {
            this((MultilineTextField content) -> {
                if (!content.hasSelection()) return false;
                content.insertText(text + content.getSelectedText() + text);
                return true;
            });
        }
        Buttons(Function<MultilineTextField, Boolean> change) {
            this.change = change;
        }


        public boolean change(MultilineTextField field) {
            return this.change.apply(field);
        }
    }

    private static void blockquote(MultilineTextField content, int min, int max) {
        String[] values = content.value().split("\n");

        boolean add = false;
        int count = 0;
        for (String s : values) {
            int newCount = count + s.length();
            if ((count >= min && count <= max) || (newCount >= min && newCount <= max)) {
                if (!s.startsWith("> ")) {
                    add = true;
                    break;
                }
            }
            count = newCount + 1;
        }

        List<String> newValue = new ArrayList<>();
        count = 0;

        for (String s : values) {
            int newCount = count + s.length();
            if ((count >= min && count <= max) || (newCount >= min && newCount <= max)) {
                if (s.startsWith("> ")) {
                    if (add) {
                        newValue.add(s);
                        continue;
                    }
                    newValue.add(s.substring(2));
                } else if (add) {
                    newValue.add("> " + s);
                }
            } else {
                newValue.add(s);
            }
            count = newCount + 1;
        }

        int cursor = content.cursor();
        int selection = content.selectCursor();

        boolean changed = !String.join("\n", newValue).equals(content.value());
        if (!changed) return;

        content.setValue(String.join("\n", newValue), true);

        if (cursor == selection) {
            int offset = add ? 2 : -2;
            content.setCursor(Mth.clamp(cursor + offset, 0, content.value().length()));
        }
    }

    @Override
    public CursorScreen.Cursor getCursor() {
        if (cursor != null) {
            return cursor;
        }
        return CursorScreen.Cursor.TEXT;
    }
}
