package earth.terrarium.heracles.client.widgets.modals;

import com.mojang.blaze3d.systems.RenderSystem;
import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import earth.terrarium.heracles.client.widgets.base.BaseWidget;
import earth.terrarium.heracles.client.widgets.base.TemporaryWidget;
import earth.terrarium.heracles.client.screens.AbstractQuestScreen;
import earth.terrarium.heracles.client.screens.quests.QuestsWidget;
import earth.terrarium.heracles.client.screens.quests.QuestsEditScreen;
import earth.terrarium.heracles.client.screens.quests.QuestsScreen;
import earth.terrarium.heracles.client.screens.quests.QuestWidget;
import earth.terrarium.heracles.client.handlers.ClientQuestNetworking;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.handlers.ClientQuestClipboard;
import earth.terrarium.heracles.client.handlers.BlueprintsManager;
import earth.terrarium.heracles.client.handlers.UndoRedoManager;
import earth.terrarium.heracles.client.handlers.DisplayConfig;
import earth.terrarium.heracles.client.utils.QuestIdPolicy;
import earth.terrarium.heracles.client.utils.QuestBackgroundTextures;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.api.quests.GroupDisplay;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.api.quests.QuestDisplay;
import earth.terrarium.heracles.api.quests.QuestSettings;
import earth.terrarium.heracles.client.utils.MouseClick;
import earth.terrarium.heracles.common.network.packets.quests.data.NetworkQuestData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

public class ContextMenu extends BaseWidget implements TemporaryWidget {
    private final int x;
    private final int y;
    private int width;
    private int height;
    private boolean visible;

    private final QuestsWidget widget;
    private final AbstractQuestScreen<?> screen;
    private final QuestWidget source;

    private final List<MenuItem> items = new ArrayList<>();
    private final int itemHeight = 18;
    private int scroll = 0;

    public ContextMenu(AbstractQuestScreen<?> screen, QuestsWidget widget, int x, int y) {
        this(screen, widget, x, y, (QuestWidget) null);
    }

    public ContextMenu(AbstractQuestScreen<?> screen, QuestsWidget widget, int x, int y, QuestWidget source) {
        this.screen = screen;
        this.widget = widget;
        this.x = x;
        this.y = y;
        this.source = source;
        this.visible = true;

        boolean hasQuestClipboard = ClientQuestClipboard.hasCopied();
        boolean hasMultiSelection = widget.getMultiSelectedIds().size() > 1;
        boolean multiSelectionContext = source != null
            && hasMultiSelection
            && widget.getMultiSelectedIds().contains(source.id())
            ;
        boolean canvasSelectionContext = source == null && hasMultiSelection;

        if (source != null) {
            if (!multiSelectionContext) {
                this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.link_to"), (click) -> {
                    
                    widget.startLink(source);
                    this.setVisible(false);
                }));
            }

            this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.copy"), (click) -> {
                if (!widget.getMultiSelectedIds().isEmpty() && widget.getMultiSelectedIds().contains(source.id())) {
                    ClientQuestClipboard.copyMultiple(widget.getMultiSelectedEntries(), widget.group());
                } else {
                    ClientQuestClipboard.copySingle(source.id(), source.entry(), widget.group());
                }
                this.setVisible(false);
            }));

            if (screen instanceof QuestsEditScreen) {
                this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.change_quest_background"), (click) -> {
                    List<ClientQuests.QuestEntry> targets;
                    if (!widget.getMultiSelectedIds().isEmpty() && widget.getMultiSelectedIds().contains(source.id())) {
                        targets = widget.getMultiSelectedEntries();
                    } else {
                        targets = List.of(source.entry());
                    }

                    int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    AssetsLibraryModal modal = new AssetsLibraryModal(screenW, screenH, selected -> {
                        for (ClientQuests.QuestEntry target : targets) {
                            ResourceLocation background = selected == null || selected.isBlank()
                                ? QuestBackgroundTextures.defaultBackground()
                                : QuestBackgroundTextures.fromManagedPath(selected);
                            ClientQuests.updateQuest(target, quest -> NetworkQuestData.builder().background(background));
                        }
                    });
                    screen.addTemporary(modal);
                    modal.setVisible(true);
                    this.setVisible(false);
                }));
            }

            this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.save_blueprint"), (click) -> {
                List<ClientQuests.QuestEntry> toSave;
                if (!widget.getMultiSelectedIds().isEmpty() && widget.getMultiSelectedIds().contains(source.id())) {
                    toSave = widget.getMultiSelectedEntries();
                } else {
                    toSave = List.of(source.entry());
                }
                List<ClientQuestClipboard.CopiedQuest> copied = new ArrayList<>();
                for (ClientQuests.QuestEntry e : toSave) {
                    copied.add(new ClientQuestClipboard.CopiedQuest(e.key(), ClientQuestClipboard.deepCopy(e.value()), widget.group(), e.value().display().position(widget.group())));
                }
                this.setVisible(false);
                int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                TextInputModal<java.util.List<ClientQuestClipboard.CopiedQuest>> modal = new TextInputModal<>(screenW, screenH, Component.translatable("blueprints.save.title"), (data, name) -> {
                    BlueprintsManager.addBlueprint(name.trim(), data);
                }, name -> name != null && !name.trim().isEmpty() && !BlueprintsManager.exists(name.trim()));
                modal.setData(copied);
                screen.addTemporary(modal);
                modal.setVisible(true);
            }));
            if (hasQuestClipboard && screen instanceof QuestsEditScreen) {
                this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.paste"), (click) -> {
                    java.util.concurrent.atomic.AtomicReference<java.util.List<String>> pastedRef = new java.util.concurrent.atomic.AtomicReference<>(java.util.List.of());
                    Runnable apply = () -> pastedRef.set(ClientQuestClipboard.pasteAt(widget, new MouseClick(this.x, this.y, 1)));
                    Runnable revert = () -> {
                        for (String id : pastedRef.get()) {
                            ClientQuestNetworking.remove(id);
                        }
                        widget.setMultiSelectedIds(Collections.emptySet());
                    };
                    UndoRedoManager.getInstance().execute(apply, revert);
                    this.setVisible(false);
                }));
            }

            this.items.add(new MenuItem(ConstantComponents.DELETE, (click) -> {
                this.setVisible(false);
                List<ClientQuests.QuestEntry> toDelete;
                if (!widget.getMultiSelectedIds().isEmpty() && widget.getMultiSelectedIds().contains(source.id())) {
                    toDelete = widget.getMultiSelectedEntries();
                } else {
                    toDelete = List.of(source.entry());
                }

                java.util.Map<String, Quest> snapshots = new java.util.HashMap<>();
                for (ClientQuests.QuestEntry e : toDelete) {
                    snapshots.put(e.key(), ClientQuestClipboard.deepCopy(e.value()));
                }

                Runnable apply = () -> {
                        for (ClientQuests.QuestEntry e : toDelete) {
                            if (e.value().display().groups().size() == 1) {
                                ClientQuestNetworking.remove(e.key());
                            } else {
                                ClientQuests.updateQuest(e, (Function<Quest, NetworkQuestData.Builder>) (quest -> {
                                    quest.display().groups().remove(widget.group());
                                    return NetworkQuestData.builder().groups(quest.display().groups());
                                }));
                            }
                            widget.removeQuest(e);
                        }
                    widget.setMultiSelectedIds(Collections.emptySet());
                };

                Runnable revert = () -> {
                    for (java.util.Map.Entry<String, Quest> s : snapshots.entrySet()) {
                        widget.addQuest(ClientQuestNetworking.add(s.getKey(), ClientQuestClipboard.deepCopy(s.getValue())));
                    }
                };

                if (screen instanceof QuestsScreen qs) {
                    qs.confirmModal().setVisible(true);
                    qs.confirmModal().setCallback(() -> UndoRedoManager.getInstance().execute(apply, revert));
                } else {
                    UndoRedoManager.getInstance().execute(apply, revert);
                }
            }));
        } else {
            if (canvasSelectionContext) {
                this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.copy"), click -> {
                    ClientQuestClipboard.copyMultiple(widget.getMultiSelectedEntries(), widget.group());
                    this.setVisible(false);
                }));

                this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.save_blueprint"), click -> {
                    List<ClientQuests.QuestEntry> toSave = widget.getMultiSelectedEntries();
                    if (!toSave.isEmpty()) {
                        saveBlueprintSelection(toSave);
                    }
                    this.setVisible(false);
                }));

            }

            if (hasQuestClipboard && screen instanceof QuestsEditScreen) {
                this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.paste"), (click) -> {
                    java.util.concurrent.atomic.AtomicReference<java.util.List<String>> pastedRef = new java.util.concurrent.atomic.AtomicReference<>(java.util.List.of());
                    Runnable apply = () -> pastedRef.set(ClientQuestClipboard.pasteAt(widget, new MouseClick(this.x, this.y, 1)));
                    Runnable revert = () -> {
                        for (String id : pastedRef.get()) {
                            ClientQuestNetworking.remove(id);
                        }
                        widget.setMultiSelectedIds(Collections.emptySet());
                    };
                    UndoRedoManager.getInstance().execute(apply, revert);
                    this.setVisible(false);
                }));
            }

            if (widget.hasCopiedCanvasSprite() && screen instanceof QuestsEditScreen) {
                this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.canvas_image_paste"), click -> {
                    widget.pasteCanvasSprite(new MouseClick(this.x, this.y, 1));
                    this.setVisible(false);
                }));
            }

            if (canvasSelectionContext) {
                this.items.add(new MenuItem(ConstantComponents.DELETE, click -> {
                    deleteEntries(widget.getMultiSelectedEntries());
                    this.setVisible(false);
                }));
            } else {
                this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.quick_add"), (click) -> {
                    String id = QuestIdPolicy.nextAvailable(widget.group(), "quick");
                    MouseClick local = widget.getLocal(new MouseClick(this.x, this.y, 1));
                    int half = 12;
                    int px = (int) local.x() - half;
                    int py = (int) local.y() - half;
                    GroupDisplay template = groupTemplate(widget.group());
                    QuestDisplay display = QuestDisplay.createDefault(new GroupDisplay(widget.group(), new Vector2i(px, py), template.nodeScale(), "", ""));
                    display.setTitle(Component.literal("Quick Quest"));
                    Quest quest = new Quest(display, QuestSettings.createDefault(), new HashSet<>(), new java.util.HashMap<>(), new java.util.HashMap<>());
                    widget.addQuest(ClientQuestNetworking.add(id, quest));
                    this.setVisible(false);
                }));

                this.items.add(new MenuItem(ConstantComponents.Quests.CREATE, (click) -> {
                    this.setVisible(false);
                    int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    TextInputModal<MouseClick> modal = new TextInputModal<>(screenW, screenH, ConstantComponents.Quests.CREATE, (position, text) -> {
                        MouseClick local = widget.getLocal(position);
                        int half = 12;
                        int px = (int) local.x() - half;
                        int py = (int) local.y() - half;
                        GroupDisplay template = groupTemplate(widget.group());
                        QuestDisplay display = QuestDisplay.createDefault(new GroupDisplay(widget.group(), new Vector2i(px, py), template.nodeScale(), "", ""));
                        display.setTitle(Component.literal(text));
                        Quest quest = new Quest(display, QuestSettings.createDefault(), new HashSet<>(), new java.util.HashMap<>(), new java.util.HashMap<>());
                        String id = QuestIdPolicy.nextAvailable(widget.group(), text);
                        widget.addQuest(ClientQuestNetworking.add(id, quest));
                    }, text -> {
                        return !QuestIdPolicy.sanitizeSegment(text).isBlank();
                    });
                    modal.setData(new MouseClick(this.x, this.y, 1));
                    screen.addTemporary(modal);
                    modal.setVisible(true);
                }));
            }

            if (screen instanceof QuestsEditScreen) {
                this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.add_canvas_image"), click -> {
                    int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    AssetsLibraryModal modal = new AssetsLibraryModal(screenW, screenH, relative -> {
                        widget.addCanvasSprite(relative, new MouseClick(this.x, this.y, 1));
                    });
                    screen.addTemporary(modal);
                    modal.setVisible(true);
                    this.setVisible(false);
                }));

                this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.change_canvas_background"), click -> {
                    int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    AssetsLibraryModal modal = new AssetsLibraryModal(screenW, screenH, relative -> {
                        DisplayConfig.setCanvasBackground(widget.group(), relative);
                        widget.refreshFromClient();
                    });
                    screen.addTemporary(modal);
                    modal.setVisible(true);
                    this.setVisible(false);
                }));

                if (!DisplayConfig.getCanvasBackground(widget.group()).isBlank()) {
                    this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.remove_canvas_background"), click -> {
                        DisplayConfig.setCanvasBackground(widget.group(), "");
                        widget.refreshFromClient();
                        this.setVisible(false);
                    }));
                }
            }
        }

        int maxLabel = 0;
        for (MenuItem item : this.items) {
            maxLabel = Math.max(maxLabel, this.font.width(item.label.getString()));
        }
        this.width = Math.max(110, maxLabel + 16);
        this.height = 6 + this.items.size() * this.itemHeight;

        
        widget.setContextMenuOpen(true);
    }

    public ContextMenu(AbstractQuestScreen<?> screen, QuestsWidget widget, int x, int y, String sourceId, String targetId) {
        this(screen, widget, x, y, (QuestWidget) null);
        this.items.clear();
        boolean hiddenConnection = DisplayConfig.isConnectionHidden(widget.group(), sourceId, targetId);
        this.items.add(new MenuItem(Component.translatable(hiddenConnection
            ? "contextmenu.heracles.show_connection"
            : "contextmenu.heracles.hide_connection"), (click) -> {
            DisplayConfig.setConnectionHidden(widget.group(), sourceId, targetId, !hiddenConnection);
            widget.refreshFromClient();
            this.setVisible(false);
        }));
        this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.delete_connection"), (click) -> {
            this.setVisible(false);
            if (screen instanceof QuestsScreen qs) {
                qs.confirmModal().setVisible(true);
                qs.confirmModal().setCallback(() -> ClientQuests.get(targetId).ifPresent((dependentEntry) -> {
                    dependentEntry.value().dependencies().remove(sourceId);
                    dependentEntry.dependencies().removeIf((dep) -> dep.key().equals(sourceId));
                    ClientQuests.get(sourceId).ifPresent((srcEntry) -> srcEntry.dependents().removeIf((child) -> child.key().equals(targetId)));
                    DisplayConfig.setConnectionHidden(widget.group(), sourceId, targetId, false);
                    ClientQuests.updateQuest(dependentEntry, (Function<Quest, NetworkQuestData.Builder>)(q) -> NetworkQuestData.builder().dependencies(dependentEntry.value().dependencies()));
                }));
            } else {
                ClientQuests.get(targetId).ifPresent((dependentEntry) -> {
                    dependentEntry.value().dependencies().remove(sourceId);
                    dependentEntry.dependencies().removeIf((dep) -> dep.key().equals(sourceId));
                    ClientQuests.get(sourceId).ifPresent((srcEntry) -> srcEntry.dependents().removeIf((child) -> child.key().equals(targetId)));
                    DisplayConfig.setConnectionHidden(widget.group(), sourceId, targetId, false);
                    ClientQuests.updateQuest(dependentEntry, (Function<Quest, NetworkQuestData.Builder>)(q) -> NetworkQuestData.builder().dependencies(dependentEntry.value().dependencies()));
                });
            }
        }));
        int maxLabel = 0;
        for (MenuItem item : this.items) {
            int w = this.font.width(item.label.getString());
            if (w > maxLabel) maxLabel = w;
        }
        this.width = Math.max(110, maxLabel + 12);
        this.height = 6 + this.items.size() * this.itemHeight;
    }

    public ContextMenu(AbstractQuestScreen<?> screen, QuestsWidget widget, int x, int y, String canvasSpriteId) {
        this(screen, widget, x, y, (QuestWidget) null);
        this.items.clear();
        this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.canvas_image_copy"), click -> {
            widget.copyCanvasSprite(canvasSpriteId);
            this.setVisible(false);
        }));
        this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.canvas_image_duplicate"), click -> {
            widget.duplicateCanvasSprite(canvasSpriteId);
            this.setVisible(false);
        }));
        if (widget.hasCopiedCanvasSprite()) {
            this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.canvas_image_paste"), click -> {
                widget.pasteCanvasSprite(new MouseClick(this.x, this.y, 1));
                this.setVisible(false);
            }));
        }
        this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.canvas_image_options"), click -> {
            DisplayConfig.CanvasSprite sprite = widget.getCanvasSprite(canvasSpriteId);
            if (sprite == null) return;
            int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            CanvasSpriteOptionsModal modal = new CanvasSpriteOptionsModal(
                screenW,
                screenH,
                sprite,
                opacity -> widget.setCanvasSpriteOpacity(canvasSpriteId, opacity)
            );
            screen.addTemporary(modal);
            modal.setVisible(true);
            this.setVisible(false);
        }));
        this.items.add(new MenuItem(Component.translatable("contextmenu.heracles.canvas_image_delete"), click -> {
            widget.removeCanvasSprite(canvasSpriteId);
            this.setVisible(false);
        }));
        int maxLabel = 0;
        for (MenuItem item : this.items) {
            int w = this.font.width(item.label.getString());
            if (w > maxLabel) maxLabel = w;
        }
        this.width = Math.max(140, maxLabel + 12);
        this.height = 6 + this.items.size() * this.itemHeight;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.isVisible()) return;
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int displayHeight = Math.min(this.height, Math.max(32, screenH - 10));
        int effectiveX = this.x;
        int effectiveY = this.y;
        if (effectiveX + this.width > screenW) effectiveX = Math.max(5, screenW - this.width - 5);
        if (effectiveY + displayHeight > screenH) effectiveY = Math.max(5, this.y - displayHeight);

        try (CloseablePoseStack pose = new CloseablePoseStack(graphics)) {
            pose.translate(0, 0, 300);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            graphics.fill(effectiveX, effectiveY, effectiveX + this.width, effectiveY + displayHeight, 0xEE222222);
            graphics.fill(effectiveX + 1, effectiveY + 1, effectiveX + this.width - 1, effectiveY + displayHeight - 1, 0xFF2B2B2B);

            int totalHeight = this.items.size() * this.itemHeight;
            int maxScroll = Math.max(0, totalHeight - (displayHeight - 8));
            if (this.scroll > maxScroll) this.scroll = maxScroll;

            int startIndex = Math.max(0, this.scroll / this.itemHeight);
            int offsetPixels = this.scroll % this.itemHeight;
            int visibleCount = (displayHeight - 8) / this.itemHeight + 1;
            int iy = effectiveY + 4;

            for (int i = startIndex; i < Math.min(this.items.size(), startIndex + visibleCount); ++i) {
                MenuItem item = this.items.get(i);
                int itemY = iy + (i - startIndex) * this.itemHeight - offsetPixels;
                if (mouseX >= effectiveX && mouseX < effectiveX + this.width && mouseY >= itemY && mouseY < itemY + this.itemHeight) {
                    graphics.fill(effectiveX + 2, itemY, effectiveX + this.width - 2, itemY + this.itemHeight, 0xAAFFFFFF);
                }
                graphics.drawString(font, item.label, effectiveX + 6, itemY + (this.itemHeight - 9) / 2, 0xFFFFFFFF, false);
            }

            if (totalHeight > displayHeight - 8) {
                int barHeight = Math.max(10, (displayHeight - 8) * (displayHeight - 8) / totalHeight);
                int track = displayHeight - 8 - barHeight;
                int barTop = iy + (int) ((long) this.scroll * (long) track / (long) Math.max(1, totalHeight - (displayHeight - 8)));
                int barX = effectiveX + this.width - 8;
                graphics.fill(barX, iy, barX + 6, iy + displayHeight - 8, 0xFF333333);
                graphics.fill(barX + 1, barTop, barX + 5, barTop + barHeight, 0xFF777777);
            }

            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        }
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) widget.setContextMenuOpen(false);
    }

    @Override
    public int depth() {
        return 100000;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!this.isVisible()) return false;
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
        if (!this.isVisible()) return false;
        if (button == 0) {
            
            if (isMouseOver(mouseX, mouseY)) {
                int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                int displayHeight = Math.min(this.height, Math.max(32, screenH - 10));
                int effectiveX = this.x;
                int effectiveY = this.y;
                if (effectiveX + this.width > Minecraft.getInstance().getWindow().getGuiScaledWidth()) effectiveX = Math.max(5, Minecraft.getInstance().getWindow().getGuiScaledWidth() - this.width - 5);
                if (effectiveY + displayHeight > screenH) effectiveY = Math.max(5, this.y - displayHeight);
                int relativeY = (int) mouseY - (effectiveY + 4);
                int index = (this.scroll + relativeY) / this.itemHeight;
                if (index >= 0 && index < this.items.size()) {
                    try {
                        this.items.get(index).action.accept(new MouseClick(mouseX, mouseY, button));
                    } catch (Exception ignored) {}
                }
            }
            this.setVisible(false);
            return true;
        } else if (button == 1) {
            this.setVisible(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (!this.isVisible()) return false;
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int displayHeight = Math.min(this.height, Math.max(32, screenH - 10));
        int totalHeight = this.items.size() * this.itemHeight;
        int maxScroll = Math.max(0, totalHeight - (displayHeight - 8));
        if (maxScroll <= 0) return false;
        int delta = (int) (-scrollAmount * (double) 18.0F);
        this.scroll = Math.max(0, Math.min(this.scroll + delta, maxScroll));
        return true;
    }

    private void saveBlueprintSelection(List<ClientQuests.QuestEntry> entries) {
        if (entries == null || entries.isEmpty()) return;
        List<ClientQuestClipboard.CopiedQuest> copied = new ArrayList<>();
        for (ClientQuests.QuestEntry e : entries) {
            copied.add(new ClientQuestClipboard.CopiedQuest(e.key(), ClientQuestClipboard.deepCopy(e.value()), widget.group(), e.value().display().position(widget.group())));
        }

        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        TextInputModal<java.util.List<ClientQuestClipboard.CopiedQuest>> modal = new TextInputModal<>(screenW, screenH, Component.translatable("blueprints.save.title"), (data, name) -> {
            BlueprintsManager.addBlueprint(name.trim(), data);
        }, name -> name != null && !name.trim().isEmpty() && !BlueprintsManager.exists(name.trim()));
        modal.setData(copied);
        screen.addTemporary(modal);
        modal.setVisible(true);
    }

    private void deleteEntries(List<ClientQuests.QuestEntry> toDelete) {
        if (toDelete == null || toDelete.isEmpty()) return;

        java.util.Map<String, Quest> snapshots = new java.util.HashMap<>();
        for (ClientQuests.QuestEntry e : toDelete) {
            snapshots.put(e.key(), ClientQuestClipboard.deepCopy(e.value()));
        }

        Runnable apply = () -> {
            for (ClientQuests.QuestEntry e : toDelete) {
                if (e.value().display().groups().size() == 1) {
                    ClientQuestNetworking.remove(e.key());
                } else {
                    ClientQuests.updateQuest(e, (Function<Quest, NetworkQuestData.Builder>) (quest -> {
                        quest.display().groups().remove(widget.group());
                        return NetworkQuestData.builder().groups(quest.display().groups());
                    }));
                }
                widget.removeQuest(e);
            }
            widget.setMultiSelectedIds(Collections.emptySet());
        };

        Runnable revert = () -> {
            for (java.util.Map.Entry<String, Quest> s : snapshots.entrySet()) {
                widget.addQuest(ClientQuestNetworking.add(s.getKey(), ClientQuestClipboard.deepCopy(s.getValue())));
            }
        };

        if (screen instanceof QuestsScreen qs) {
            qs.confirmModal().setVisible(true);
            qs.confirmModal().setCallback(() -> UndoRedoManager.getInstance().execute(apply, revert));
        } else {
            UndoRedoManager.getInstance().execute(apply, revert);
        }
    }

    private GroupDisplay groupTemplate(String group) {
        for (ClientQuests.QuestEntry entry : ClientQuests.byGroup(group)) {
            GroupDisplay display = entry.value().display().groups().get(group);
            if (display != null) return display.withNodeScale(1.0f);
        }
        return GroupDisplay.create(group)
            .withNodeScale(1.0f);
    }

    private static record MenuItem(Component label, Consumer<MouseClick> action) {}
}
