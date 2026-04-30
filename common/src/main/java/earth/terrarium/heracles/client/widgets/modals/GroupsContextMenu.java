package earth.terrarium.heracles.client.widgets.modals;

import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.handlers.ClientQuestNetworking;
import earth.terrarium.heracles.client.handlers.DisplayConfig;
import earth.terrarium.heracles.client.screens.AbstractQuestScreen;
import earth.terrarium.heracles.client.screens.quests.GroupsList;
import earth.terrarium.heracles.client.screens.quests.QuestsScreen;
import earth.terrarium.heracles.client.utils.MouseClick;
import earth.terrarium.heracles.client.widgets.base.BaseWidget;
import earth.terrarium.heracles.client.widgets.base.TemporaryWidget;
import earth.terrarium.heracles.api.quests.GroupDisplay;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.groups.CreateGroupPacket;
import earth.terrarium.heracles.common.network.packets.groups.DeleteGroupPacket;
import earth.terrarium.heracles.common.network.packets.groups.MoveGroupPacket;
import earth.terrarium.heracles.common.network.packets.groups.RenameGroupPacket;
import earth.terrarium.heracles.common.network.packets.quests.data.NetworkQuestData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GroupsContextMenu extends BaseWidget implements TemporaryWidget {

    private final int x;
    private final int y;
    private int width;
    private int height;
    private boolean visible = true;
    private int scroll = 0;

    private final AbstractQuestScreen<?> screen;
    private final GroupsList list;
    private final String initialGroup;
    private final List<MenuItem> items = new ArrayList<>();
    private static final int ITEM_H = 18;

    public GroupsContextMenu(AbstractQuestScreen<?> screen, int x, int y, String selectedGroup, GroupsList list) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.list = list;
        this.initialGroup = selectedGroup == null ? "" : selectedGroup;

        this.items.add(new MenuItem(Component.translatable("groups.context.add"), this::handleAdd));

        String group = resolveGroup();
        if (!group.isBlank()) {
            this.items.add(new MenuItem(Component.translatable("groups.context.rename"), this::handleRename));
            this.items.add(new MenuItem(Component.translatable("groups.context.change_card_texture"), this::handleChangeCardTexture));
            if (!DisplayConfig.getGroupCardTexture(group).isBlank()) {
                this.items.add(new MenuItem(Component.translatable("groups.context.remove_card_texture"), this::handleRemoveCardTexture));
            }
            if (ClientQuests.groups().size() > 1) {
                this.items.add(new MenuItem(Component.translatable("groups.context.delete"), this::handleDelete));
            }
            int idx = ClientQuests.groups().indexOf(group);
            if (idx > 0) {
                this.items.add(new MenuItem(Component.translatable("groups.context.move_up"), click -> handleMove(-1)));
            }
            if (idx >= 0 && idx < ClientQuests.groups().size() - 1) {
                this.items.add(new MenuItem(Component.translatable("groups.context.move_down"), click -> handleMove(1)));
            }
        }

        int maxW = 0;
        for (MenuItem item : this.items) {
            maxW = Math.max(maxW, this.font.width(item.label.getString()));
        }
        this.width = Math.max(145, maxW + 14);
        this.height = 6 + this.items.size() * ITEM_H;
    }

    private String resolveGroup() {
        if (!this.initialGroup.isBlank()) return this.initialGroup;
        GroupsList.Entry selected = this.list.getSelected();
        return selected != null ? selected.name() : "";
    }

    private void handleAdd(MouseClick click) {
        TextInputModal<Void> modal = new TextInputModal<>(Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight(), ConstantComponents.Groups.CREATE, (d, name) -> {
            String trimmed = name == null ? "" : name.trim();
            if (trimmed.isEmpty() || ClientQuests.groups().contains(trimmed)) return;
            ClientQuests.groups().add(trimmed);
            this.list.update(ClientQuests.groups(), trimmed);
            NetworkHandler.CHANNEL.sendToServer(new CreateGroupPacket(trimmed));
        }, name -> name != null && !name.trim().isEmpty() && !ClientQuests.groups().contains(name.trim()));
        this.screen.addTemporary(modal);
        modal.setVisible(true);
    }

    private void handleRename(MouseClick click) {
        String group = resolveGroup();
        if (group.isBlank()) return;
        TextInputModal<String> modal = new TextInputModal<>(Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight(), Component.translatable("groups.context.rename"), (oldName, newName) -> {
            String trimmed = newName == null ? "" : newName.trim();
            if (trimmed.isEmpty() || trimmed.equals(oldName) || ClientQuests.groups().contains(trimmed)) return;
            int idx = ClientQuests.groups().indexOf(oldName);
            if (idx >= 0) {
                ClientQuests.groups().set(idx, trimmed);
            }
            for (ClientQuests.QuestEntry entry : ClientQuests.entries()) {
                GroupDisplay display = entry.value().display().groups().remove(oldName);
                if (display == null) continue;
                entry.value().display().groups().put(trimmed, display.withId(trimmed));
            }
            DisplayConfig.renameGroupVisuals(oldName, trimmed);
            ClientQuests.rebuildGroupIndex();
            this.list.update(ClientQuests.groups(), trimmed);
            NetworkHandler.CHANNEL.sendToServer(new RenameGroupPacket(oldName, trimmed));
            if (this.screen instanceof QuestsScreen qs && oldName.equals(qs.getGroup())) {
                NetworkHandler.CHANNEL.sendToServer(new earth.terrarium.heracles.common.network.packets.groups.OpenGroupPacket(trimmed, true));
            }
        }, name -> name != null && !name.trim().isEmpty() && (!ClientQuests.groups().contains(name.trim()) || name.trim().equals(group)));
        modal.setData(group);
        this.screen.addTemporary(modal);
        modal.setVisible(true);
    }

    private void handleDelete(MouseClick click) {
        String group = resolveGroup();
        if (group.isBlank()) return;
        if (ClientQuests.groups().size() <= 1) return;
        if (this.screen instanceof QuestsScreen qs) {
            qs.confirmModal().setVisible(true);
            qs.confirmModal().setCallback(() -> {
                List<ClientQuests.QuestEntry> affected = new ArrayList<>(ClientQuests.byGroup(group));
                for (ClientQuests.QuestEntry entry : affected) {
                    Quest quest = entry.value();
                    if (quest.display().groups().size() <= 1) {
                        ClientQuestNetworking.remove(entry.key());
                    } else {
                        ClientQuests.updateQuest(entry, q -> {
                            q.display().groups().remove(group);
                            return NetworkQuestData.builder().groups(q.display().groups());
                        });
                    }
                }
                ClientQuests.rebuildGroupIndex();
                ClientQuests.groups().remove(group);
                DisplayConfig.removeGroupVisuals(group);
                String selected = ClientQuests.groups().isEmpty() ? "" : ClientQuests.groups().get(0);
                this.list.update(ClientQuests.groups(), selected);
                NetworkHandler.CHANNEL.sendToServer(new DeleteGroupPacket(group));
                if (!selected.isBlank()) {
                    NetworkHandler.CHANNEL.sendToServer(new earth.terrarium.heracles.common.network.packets.groups.OpenGroupPacket(selected, true));
                }
            });
        }
    }

    private void handleChangeCardTexture(MouseClick click) {
        if (!(this.screen instanceof QuestsScreen)) return;
        String group = resolveGroup();
        if (group.isBlank()) return;
        AssetsLibraryModal modal = new AssetsLibraryModal(Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight(), relative -> {
            DisplayConfig.setGroupCardTexture(group, relative);
            ClientQuests.rebuildGroupIndex();
            this.list.update(ClientQuests.groups(), group);
        });
        this.screen.addTemporary(modal);
        modal.setVisible(true);
    }

    private void handleRemoveCardTexture(MouseClick click) {
        String group = resolveGroup();
        if (group.isBlank()) return;
        DisplayConfig.setGroupCardTexture(group, "");
        ClientQuests.rebuildGroupIndex();
        this.list.update(ClientQuests.groups(), group);
    }

    private void handleMove(int delta) {
        String group = resolveGroup();
        if (group.isBlank()) return;
        int idx = ClientQuests.groups().indexOf(group);
        int target = idx + Integer.signum(delta);
        if (idx < 0 || target < 0 || target >= ClientQuests.groups().size()) return;
        String value = ClientQuests.groups().remove(idx);
        ClientQuests.groups().add(target, value);
        this.list.update(ClientQuests.groups(), value);
        NetworkHandler.CHANNEL.sendToServer(new MoveGroupPacket(group, delta));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int displayHeight = Math.min(this.height, Math.max(32, screenH - 10));
        int effectiveX = this.x;
        int effectiveY = this.y;
        if (effectiveX + this.width > screenW) effectiveX = Math.max(5, screenW - this.width - 5);
        if (effectiveY + displayHeight > screenH) effectiveY = Math.max(5, this.y - displayHeight);

        try (CloseablePoseStack pose = new CloseablePoseStack(graphics)) {
            pose.translate(0, 0, 300);
            graphics.fill(effectiveX, effectiveY, effectiveX + this.width, effectiveY + displayHeight, 0xEE222222);
            graphics.fill(effectiveX + 1, effectiveY + 1, effectiveX + this.width - 1, effectiveY + displayHeight - 1, 0xFF2B2B2B);

            int totalHeight = this.items.size() * ITEM_H;
            int maxScroll = Math.max(0, totalHeight - (displayHeight - 8));
            this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));
            int startIndex = this.scroll / ITEM_H;
            int offsetPixels = this.scroll % ITEM_H;
            int visibleCount = (displayHeight - 8) / ITEM_H + 1;
            int iy = effectiveY + 4;

            for (int i = startIndex; i < Math.min(this.items.size(), startIndex + visibleCount); ++i) {
                MenuItem item = this.items.get(i);
                int itemY = iy + (i - startIndex) * ITEM_H - offsetPixels;
                if (mouseX >= effectiveX && mouseX < effectiveX + this.width && mouseY >= itemY && mouseY < itemY + ITEM_H) {
                    graphics.fill(effectiveX + 2, itemY, effectiveX + this.width - 2, itemY + ITEM_H, 0xAAFFFFFF);
                }
                graphics.drawString(this.font, item.label, effectiveX + 6, itemY + (ITEM_H - 9) / 2, 0xFFFFFFFF, false);
            }
        }
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public int depth() {
        return 100000;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.visible) return false;
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int displayHeight = Math.min(this.height, Math.max(32, screenH - 10));
        int effectiveX = this.x;
        int effectiveY = this.y;
        if (effectiveX + this.width > screenW) effectiveX = Math.max(5, screenW - this.width - 5);
        if (effectiveY + displayHeight > screenH) effectiveY = Math.max(5, this.y - displayHeight);
        return mouseX >= effectiveX && mouseX < effectiveX + this.width && mouseY >= effectiveY && mouseY < effectiveY + displayHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) return false;
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            int displayHeight = Math.min(this.height, Math.max(32, screenH - 10));
            int effectiveX = this.x;
            int effectiveY = this.y;
            if (effectiveX + this.width > Minecraft.getInstance().getWindow().getGuiScaledWidth()) effectiveX = Math.max(5, Minecraft.getInstance().getWindow().getGuiScaledWidth() - this.width - 5);
            if (effectiveY + displayHeight > screenH) effectiveY = Math.max(5, this.y - displayHeight);
            int relativeY = (int) mouseY - (effectiveY + 4);
            int index = (this.scroll + relativeY) / ITEM_H;
            if (index >= 0 && index < this.items.size()) {
                this.items.get(index).action.accept(new MouseClick(mouseX, mouseY, button));
            }
            this.setVisible(false);
            return true;
        }
        if (button == 0 || button == 1) {
            this.setVisible(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (!this.visible) return false;
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int displayHeight = Math.min(this.height, Math.max(32, screenH - 10));
        int totalHeight = this.items.size() * ITEM_H;
        int maxScroll = Math.max(0, totalHeight - (displayHeight - 8));
        if (maxScroll <= 0) return false;
        int delta = (int) (-scrollAmount * 18.0F);
        this.scroll = Math.max(0, Math.min(this.scroll + delta, maxScroll));
        return true;
    }

    private record MenuItem(Component label, Consumer<MouseClick> action) {}
}
