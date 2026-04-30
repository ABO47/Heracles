package earth.terrarium.heracles.client.screens.quests;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.components.selection.ListEntry;
import com.teamresourceful.resourcefullib.client.components.selection.SelectionList;
import com.teamresourceful.resourcefullib.client.scissor.ScissorBoxStack;
import com.teamresourceful.resourcefullib.client.screens.CursorScreen;
import com.teamresourceful.resourcefullib.client.utils.CursorUtils;
import earth.terrarium.heracles.api.client.theme.QuestsScreenTheme;
import earth.terrarium.heracles.client.handlers.CustomImageManager;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.handlers.DisplayConfig;
import earth.terrarium.heracles.client.screens.AbstractQuestScreen;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.groups.MoveGroupPacket;
import earth.terrarium.heracles.common.network.packets.groups.OpenGroupPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GroupsList extends SelectionList<GroupsList.Entry> {

    private final int width;
    private final int y;
    private final int rowHeight;
    private String draggingGroup = null;
    private double dragStartY = 0;
    private boolean dragMoved = false;

    public GroupsList(int x, int y, int width, int height, Consumer<@Nullable Entry> onSelection) {
        super(x, y, width, height, 20, onSelection, true);
        this.width = width;
        this.y = y;
        this.rowHeight = 20;
    }

    @Override
    public void setSelected(@Nullable Entry entry) {
        if (!(Minecraft.getInstance().screen instanceof QuestsEditScreen screen) || !screen.isTemporaryWidgetVisible()) {
            super.setSelected(entry);
        }
    }

    private void internalSetSelected(@Nullable Entry entry) {
        super.setSelected(entry);
    }

    public void update(List<String> groups, String selected) {
        List<Entry> entries = new ArrayList<>(groups.size());
        Entry selectedEntry = null;
        for (String group : groups) {
            Entry entry = new Entry(this, group);
            if (group.equals(selected)) {
                selectedEntry = entry;
            }
            entries.add(entry);
        }
        updateEntries(entries);
        if (selectedEntry != null) {
            setSelected(selectedEntry);
        }
    }

    public void addGroup(String group) {
        addEntry(new Entry(this, group));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && this.isMouseOver(mouseX, mouseY) && Minecraft.getInstance().screen instanceof QuestsEditScreen screen) {
            String selected = this.getSelected() == null ? "" : this.getSelected().name();
            var menu = new earth.terrarium.heracles.client.widgets.modals.GroupsContextMenu(screen, (int) mouseX, (int) mouseY, selected, this);
            screen.addTemporary(menu);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingGroup != null && this.isMouseOver(mouseX, mouseY)) {
            if (Math.abs(mouseY - this.dragStartY) > 3.0D) {
                this.dragMoved = true;
            }
            int targetIndex = Mth.clamp((int) ((mouseY - this.y) / this.rowHeight), 0, Math.max(0, ClientQuests.groups().size() - 1));
            moveGroupToIndex(this.draggingGroup, targetIndex);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.draggingGroup != null && !this.dragMoved && Minecraft.getInstance().screen instanceof QuestsScreen screen) {
                if (!screen.getGroup().equals(this.draggingGroup)) {
                    NetworkHandler.CHANNEL.sendToServer(new OpenGroupPacket(this.draggingGroup, screen instanceof QuestsEditScreen));
                }
            }
            this.draggingGroup = null;
            this.dragMoved = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void moveGroupToIndex(String group, int targetIndex) {
        List<String> groups = new ArrayList<>(ClientQuests.groups());
        int currentIndex = groups.indexOf(group);
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= groups.size() || currentIndex == targetIndex) {
            return;
        }
        groups.remove(currentIndex);
        groups.add(targetIndex, group);
        ClientQuests.groups().clear();
        ClientQuests.groups().addAll(groups);
        this.update(groups, group);

        int steps = targetIndex - currentIndex;
        int direction = Integer.signum(steps);
        for (int i = 0; i < Math.abs(steps); i++) {
            NetworkHandler.CHANNEL.sendToServer(new MoveGroupPacket(group, direction));
        }
    }

    public static class Entry extends ListEntry {

        private final GroupsList list;
        private final String name;

        public Entry(GroupsList list, String name) {
            this.list = list;
            this.name = name;
        }

        @Override
        protected void render(@NotNull GuiGraphics graphics, @NotNull ScissorBoxStack scissorStack, int id, int left, int top, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick, boolean selected) {
            var customTexture = CustomImageManager.getTexture(DisplayConfig.getGroupCardTexture(this.name));
            if (customTexture != null) {
                RenderSystem.enableBlend();
                graphics.blit(customTexture, left, top, 0, 0, width, height, width, height);
                if (selected) {
                    graphics.fill(left, top, left + width, top + height, 0x6600B7FF);
                } else if (hovered) {
                    graphics.fill(left, top, left + width, top + height, 0x55FFFFFF);
                }
                RenderSystem.disableBlend();
            } else {
                RenderSystem.enableBlend();
                graphics.blitNineSliced(AbstractQuestScreen.HEADING, left, top, width, height, 5, 64, 20, 192, selected ? 35 : 15);
                if (hovered) {
                    graphics.blitNineSliced(AbstractQuestScreen.HEADING, left, top, width, height, 5, 64, 20, 192, 55);
                }
                RenderSystem.disableBlend();
            }
            graphics.drawCenteredString(Minecraft.getInstance().font, name, left + width / 2, top + height / 2 - 4, QuestsScreenTheme.getGroupName());
            CursorUtils.setCursor(hovered, CursorScreen.Cursor.POINTER);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.list.getSelected() != this) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (Minecraft.getInstance().screen instanceof QuestsEditScreen screen && button == 0) {
                this.list.draggingGroup = this.name;
                this.list.dragStartY = mouseY;
                this.list.dragMoved = false;
                this.list.internalSetSelected(this);
                return true;
            }
            if (button == 1 && Minecraft.getInstance().screen instanceof QuestsEditScreen scr) {
                var menu = new earth.terrarium.heracles.client.widgets.modals.GroupsContextMenu(scr, (int) mouseX, (int) mouseY, this.name, this.list);
                scr.addTemporary(menu);
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void setFocused(boolean bl) {

        }

        @Override
        public boolean isFocused() {
            return false;
        }

        public String name() {
            return name;
        }
    }
}
