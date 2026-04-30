package earth.terrarium.heracles.client.widgets.modals;

import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import earth.terrarium.heracles.api.quests.GroupDisplay;
import earth.terrarium.heracles.client.handlers.BlueprintsManager;
import earth.terrarium.heracles.client.handlers.ClientQuestClipboard;
import earth.terrarium.heracles.client.handlers.GhostBlueprintManager;
import earth.terrarium.heracles.client.utils.TexturePlacements;
import earth.terrarium.heracles.client.widgets.base.BaseModal;
import earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox;
import earth.terrarium.heracles.client.widgets.buttons.ThemedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BlueprintsLibraryModal extends BaseModal {

    private static final int WIDTH = 420;
    private static final int HEIGHT = 260;
    private static final int ROW_HEIGHT = 18;
    private static final int CONTEXT_WIDTH = 112;
    private static final int CONTEXT_HEIGHT = 88;
    private static final int CONTEXT_ROW_HEIGHT = 16;
    private static final int CONTEXT_INSET_Y = 4;

    private final EnterableEditBox searchBox;
    private final Button copyCodeButton;
    private final Button importCodeButton;
    private List<String> names = new ArrayList<>();
    private int scroll = 0;
    private long lastClickTime = 0;
    private int lastClicked = -1;
    private final Set<String> selected = new LinkedHashSet<>();

    private boolean contextVisible = false;
    private int contextX = 0;
    private int contextY = 0;
    private String contextName = "";

    public BlueprintsLibraryModal(int screenWidth, int screenHeight) {
        super(screenWidth, screenHeight, WIDTH, HEIGHT, 2);
        this.searchBox = addChild(new EnterableEditBox(Minecraft.getInstance().font, this.x + 8, this.y + 8, WIDTH - 40, 14, Component.literal("Search blueprints...")));
        this.searchBox.setResponder(s -> refreshList());
        this.searchBox.setEnter(s -> {
            if (this.selected.size() == 1) useSelected(this.selected.iterator().next());
        });

        this.copyCodeButton = addChild(ThemedButton.builder(Component.literal("Copy Code"), b -> copyCode())
            .bounds(this.x + WIDTH - 244, this.y + HEIGHT - 20, 62, 14).build());
        this.importCodeButton = addChild(ThemedButton.builder(Component.literal("Import Code"), b -> importFromClipboard())
            .bounds(this.x + 120, this.y + HEIGHT - 20, 70, 14).build());

        layoutFooterButtons();

        refreshList();
    }

    private int visibleRows() {
        return Math.max(1, listPanelH() / ROW_HEIGHT);
    }

    private void refreshList() {
        String filter = this.searchBox.getValue();
        String q = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        this.names = new ArrayList<>();
        for (String name : BlueprintsManager.listNames()) {
            if (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q)) {
                this.names.add(name);
            }
        }
        this.selected.removeIf(s -> !this.names.contains(s));
        int maxScroll = Math.max(0, this.names.size() - visibleRows());
        this.scroll = Math.max(0, Math.min(this.scroll, maxScroll));
        updateActionButtons();
    }

    private void updateActionButtons() {
        this.copyCodeButton.active = this.selected.size() == 1;
    }

    private int previewX() {
        return this.x + 8;
    }

    private int previewY() {
        return this.y + 42;
    }

    private int previewW() {
        return 106;
    }

    private int previewH() {
        return HEIGHT - 82;
    }

    private int listPanelH() {
        return previewH();
    }

    private void layoutFooterButtons() {
        int yBottom = this.y + HEIGHT - 20;
        int spacing = 4;
        int copyW = this.copyCodeButton.getWidth();
        int importW = this.importCodeButton.getWidth();
        int totalW = importW + spacing + copyW;
        int startX = this.x + WIDTH - 8 - totalW;

        this.importCodeButton.setX(startX);
        this.importCodeButton.setY(yBottom);
        startX += importW + spacing;

        this.copyCodeButton.setX(startX);
        this.copyCodeButton.setY(yBottom);
    }

    private void useSelected(String name) {
        BlueprintsManager.get(name).ifPresent(list -> {
            List<ClientQuestClipboard.CopiedQuest> cp = new ArrayList<>();
            for (ClientQuestClipboard.CopiedQuest c : list) {
                cp.add(new ClientQuestClipboard.CopiedQuest(c.originalId, ClientQuestClipboard.deepCopy(c.quest), c.sourceGroup, new Vector2i(c.sourcePos.x(), c.sourcePos.y())));
            }
            GhostBlueprintManager.setActiveBlueprint(cp);
            this.setVisible(false);
        });
    }

    private void renameSingle() {
        if (this.selected.size() != 1) return;
        String oldName = this.selected.iterator().next();
        TextInputModal<String> modal = new TextInputModal<>(this.screenWidth, this.screenHeight, Component.translatable("groups.context.rename"), (from, to) -> {
            String newName = to == null ? "" : to.trim();
            if (newName.isEmpty() || newName.equals(from) || BlueprintsManager.exists(newName)) return;
            BlueprintsManager.rename(from, newName);
            this.selected.clear();
            this.selected.add(newName);
            refreshList();
        }, s -> s != null && !s.trim().isEmpty() && (!BlueprintsManager.exists(s.trim()) || s.trim().equals(oldName)), 3);
        modal.setData(oldName);
        if (Minecraft.getInstance().screen instanceof earth.terrarium.heracles.client.screens.AbstractQuestScreen<?> screen) {
            screen.addTemporary(modal);
            modal.setVisible(true);
        }
    }

    private void duplicateSelected() {
        if (this.selected.isEmpty()) return;
        List<String> selectedNames = new ArrayList<>(this.selected);
        for (String base : selectedNames) {
            String candidate = base + "_copy";
            int suffix = 1;
            while (BlueprintsManager.exists(candidate)) {
                candidate = base + "_copy_" + suffix++;
            }
            BlueprintsManager.duplicate(base, candidate);
        }
        refreshList();
    }

    private void deleteSelected() {
        if (this.selected.isEmpty()) return;
        for (String n : new ArrayList<>(this.selected)) {
            BlueprintsManager.remove(n);
        }
        this.selected.clear();
        refreshList();
    }

    private void copyCode() {
        if (this.selected.size() != 1) return;
        String name = this.selected.iterator().next();
        BlueprintsManager.exportToCode(name).ifPresent(code -> Minecraft.getInstance().keyboardHandler.setClipboard(code));
    }

    private void importFromClipboard() {
        String code = Minecraft.getInstance().keyboardHandler.getClipboard();
        BlueprintsManager.importFromCode(code).ifPresent(imported -> {
            this.selected.clear();
            this.selected.add(imported);
            refreshList();
        });
    }

    @Override
    protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        try (CloseablePoseStack pose = new CloseablePoseStack(graphics)) {
            graphics.fill(this.x, this.y, this.x + WIDTH, this.y + HEIGHT, 0xEE121212);
            graphics.fill(this.x + 1, this.y + 1, this.x + WIDTH - 1, this.y + HEIGHT - 1, 0xFF1E1E1E);
            renderChildren(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            this.contextVisible = false;
            refreshList();
            this.setFocused(this.searchBox);
            this.searchBox.setFocused(true);
            this.searchBox.moveCursorToEnd();
        }
    }

    private void renderPreview(GuiGraphics graphics, int left, int top, int width, int height) {
        
        graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, 0xCC1B1B1B);
        graphics.fill(left, top, left + width, top + height, 0xAA0D0D0D);
        if (this.selected.size() != 1) return;
        String selectedName = this.selected.iterator().next();
        BlueprintsManager.get(selectedName).ifPresent(entries -> {
            if (entries.isEmpty()) {
                graphics.drawCenteredString(this.font, Component.translatable("blueprints.library.empty"), left + width / 2, top + height / 2 - 4, 0x8D8D8D);
                return;
            }
            List<PreviewQuest> prepared = new ArrayList<>();
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (ClientQuestClipboard.CopiedQuest cq : entries) {
                TexturePlacements.Info info = copiedTextureInfo(cq);
                float nodeScale = copiedNodeScale(cq);
                int offX = Math.round(info.xOffset() * nodeScale);
                int offY = Math.round(info.yOffset() * nodeScale);
                int questW = Math.max(1, Math.round(info.width() * nodeScale));
                int questH = Math.max(1, Math.round(info.height() * nodeScale));
                int questLeft = cq.sourcePos.x() + offX;
                int questTop = cq.sourcePos.y() + offY;
                minX = Math.min(minX, questLeft);
                minY = Math.min(minY, questTop);
                maxX = Math.max(maxX, questLeft + questW);
                maxY = Math.max(maxY, questTop + questH);
                prepared.add(new PreviewQuest(cq.originalId, cq.quest.dependencies(), questLeft, questTop, questW, questH));
            }

            int spanX = Math.max(1, maxX - minX);
            int spanY = Math.max(1, maxY - minY);
            int padding = 8;
            double scaleX = (width - (padding * 2.0)) / (double) spanX;
            double scaleY = (height - (padding * 2.0)) / (double) spanY;
            double scale = Math.max(0.0001, Math.min(scaleX, scaleY));
            int drawW = Math.max(1, (int) Math.round(spanX * scale));
            int drawH = Math.max(1, (int) Math.round(spanY * scale));
            int drawX = left + (width - drawW) / 2;
            int drawY = top + (height - drawH) / 2;

            Map<String, int[]> centers = new HashMap<>();
            Map<String, int[]> boxes = new HashMap<>();
            for (PreviewQuest quest : prepared) {
                int px = drawX + Mth.clamp((int) Math.round((quest.left() - minX) * scale), 0, drawW - 1);
                int py = drawY + Mth.clamp((int) Math.round((quest.top() - minY) * scale), 0, drawH - 1);
                int pw = Math.max(2, (int) Math.round(quest.width() * scale));
                int ph = Math.max(2, (int) Math.round(quest.height() * scale));
                int maxW = Math.max(1, drawX + drawW - px);
                int maxH = Math.max(1, drawY + drawH - py);
                pw = Math.max(1, Math.min(pw, maxW));
                ph = Math.max(1, Math.min(ph, maxH));
                boxes.put(quest.id(), new int[]{px, py, pw, ph});
                centers.put(quest.id(), new int[]{px + (pw / 2), py + (ph / 2)});
            }

            for (PreviewQuest quest : prepared) {
                int[] from = centers.get(quest.id());
                if (from == null) continue;
                int[] fromBox = boxes.get(quest.id());
                if (fromBox == null) continue;
                for (String dep : quest.dependencies()) {
                    int[] to = centers.get(dep);
                    int[] toBox = boxes.get(dep);
                    if (to == null) continue;
                    if (toBox == null) continue;
                    int[] fromAnchor = edgeAnchorOnRect(fromBox[0], fromBox[1], fromBox[2], fromBox[3], to[0], to[1]);
                    int[] toAnchor = edgeAnchorOnRect(toBox[0], toBox[1], toBox[2], toBox[3], from[0], from[1]);
                    drawLine(graphics, fromAnchor[0], fromAnchor[1], toAnchor[0], toAnchor[1], 0x66D8E7FF);
                }
            }

            for (PreviewQuest quest : prepared) {
                int[] box = boxes.get(quest.id());
                if (box == null) continue;
                int px = box[0];
                int py = box[1];
                int pw = box[2];
                int ph = box[3];
                graphics.fill(px, py, px + pw, py + ph, 0xAA000000);
                if (pw > 2 && ph > 2) {
                    graphics.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xFF8EC6FF);
                } else {
                    graphics.fill(px, py, px + pw, py + ph, 0xFF8EC6FF);
                }
            }
        });
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(this.font, Component.translatable("blueprints.library"), this.x + 8, this.y + 28, 0xEDEDED, false);

        int previewX = previewX();
        int previewY = previewY();
        int previewW = previewW();
        int previewH = previewH();
        renderPreview(graphics, previewX, previewY, previewW, previewH);

        int listX = this.x + 120;
        int listTop = this.y + 42;
        int listW = WIDTH - 128;
        int panelH = listPanelH();
        int rows = visibleRows();
        int renderRows = Math.min(rows, Math.max(0, names.size() - scroll));
        graphics.fill(listX - 1, listTop - 1, listX + listW + 1, listTop + panelH + 1, 0xB01A1A1A);
        graphics.fill(listX, listTop, listX + listW, listTop + panelH, 0x8A121212);

        for (int i = 0; i < renderRows; i++) {
            int idx = i + scroll;
            int ty = listTop + i * ROW_HEIGHT;
            String name = this.names.get(idx);
            boolean hovered = mouseX >= listX && mouseX < listX + listW && mouseY >= ty && mouseY < ty + ROW_HEIGHT;
            boolean isSel = this.selected.contains(name);
            if (hovered) {
                graphics.fill(listX, ty, listX + listW, ty + ROW_HEIGHT - 1, 0x66333333);
            }
            if (isSel) {
                graphics.renderOutline(listX, ty, listW, ROW_HEIGHT - 1, 0xFF8CC9FF);
            }
            graphics.drawString(this.font, name, listX + 6, ty + 4, isSel ? 0xFFFFFF : 0xD4D4D4, false);
            graphics.fill(listX + 1, ty + ROW_HEIGHT - 1, listX + listW - 1, ty + ROW_HEIGHT, 0x55343434);
        }

        if (this.names.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("blueprints.library.empty"), listX + 6, listTop + 6, 0xAAAAAA, false);
        }

        if (this.contextVisible) {
            int cx = this.contextX;
            int cy = this.contextY;
            graphics.fill(cx, cy, cx + CONTEXT_WIDTH, cy + CONTEXT_HEIGHT, 0xEE222222);
            graphics.fill(cx + 1, cy + 1, cx + CONTEXT_WIDTH - 1, cy + CONTEXT_HEIGHT - 1, 0xFF2D2D2D);
            graphics.drawString(this.font, Component.translatable("blueprints.library.use"), cx + 6, cy + CONTEXT_INSET_Y + 2, 0xFFFFFF, false);
            graphics.drawString(this.font, Component.translatable("groups.context.rename"), cx + 6, cy + CONTEXT_INSET_Y + 2 + CONTEXT_ROW_HEIGHT, 0xFFFFFF, false);
            graphics.drawString(this.font, Component.translatable("blueprints.library.duplicate"), cx + 6, cy + CONTEXT_INSET_Y + 2 + CONTEXT_ROW_HEIGHT * 2, 0xFFFFFF, false);
            graphics.drawString(this.font, Component.literal("Copy Code"), cx + 6, cy + CONTEXT_INSET_Y + 2 + CONTEXT_ROW_HEIGHT * 3, 0xFFFFFF, false);
            graphics.drawString(this.font, Component.translatable("groups.context.delete"), cx + 6, cy + CONTEXT_INSET_Y + 2 + CONTEXT_ROW_HEIGHT * 4, 0xFFFFFF, false);
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isVisible()) return false;
        if (this.contextVisible) {
            if (button == 0) {
                int idx = getContextRow(mouseX, mouseY);
                if (idx == 0) {
                    this.selected.clear();
                    this.selected.add(this.contextName);
                    useSelected(this.contextName);
                } else if (idx == 1) {
                    this.selected.clear();
                    this.selected.add(this.contextName);
                    renameSingle();
                } else if (idx == 2) {
                    this.selected.clear();
                    this.selected.add(this.contextName);
                    duplicateSelected();
                } else if (idx == 3) {
                    this.selected.clear();
                    this.selected.add(this.contextName);
                    copyCode();
                } else if (idx == 4) {
                    if (!this.selected.contains(this.contextName)) {
                        this.selected.clear();
                        this.selected.add(this.contextName);
                    }
                    deleteSelected();
                }
            }
            this.contextVisible = false;
            return true;
        }

        int listX = this.x + 120;
        int listTop = this.y + 42;
        int listW = WIDTH - 128;
        if (mouseX >= listX && mouseX < listX + listW && mouseY >= listTop && mouseY < listTop + listPanelH()) {
            int idx = (int) ((mouseY - listTop) / ROW_HEIGHT) + scroll;
            if (idx >= 0 && idx < names.size()) {
                String name = this.names.get(idx);
                long now = System.currentTimeMillis();
                if (button == 0) {
                    if (Screen.hasControlDown()) {
                        if (this.selected.contains(name)) this.selected.remove(name);
                        else this.selected.add(name);
                    } else {
                        if (!this.selected.contains(name) || this.selected.size() > 1) {
                            this.selected.clear();
                            this.selected.add(name);
                        }
                    }
                    if (lastClicked == idx && now - lastClickTime < 300) {
                        useSelected(name);
                        return true;
                    }
                    lastClicked = idx;
                    lastClickTime = now;
                    updateActionButtons();
                    return true;
                } else if (button == 1) {
                    if (!this.selected.contains(name)) {
                        this.selected.clear();
                        this.selected.add(name);
                    }
                    this.contextVisible = true;
                    this.contextX = Math.min((int) mouseX, this.x + WIDTH - CONTEXT_WIDTH - 4);
                    this.contextY = Math.min((int) mouseY, this.y + HEIGHT - CONTEXT_HEIGHT - 4);
                    this.contextName = name;
                    updateActionButtons();
                    return true;
                }
            }
        }
        if (button == 1) {
            this.contextVisible = false;
            return true;
        }

        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (!isVisible()) return false;
        int max = Math.max(0, this.names.size() - visibleRows());
        if (max <= 0) return false;
        this.scroll = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(scrollAmount)));
        return true;
    }

    private static void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int x = x1;
        int y = y1;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == x2 && y == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private static int[] edgeAnchorOnRect(int left, int top, int width, int height, double towardX, double towardY) {
        double cx = left + (width / 2.0);
        double cy = top + (height / 2.0);
        double dx = towardX - cx;
        double dy = towardY - cy;
        if (dx == 0.0 && dy == 0.0) {
            return new int[]{(int) Math.round(cx), (int) Math.round(cy)};
        }

        double halfW = Math.max(0.5, width / 2.0);
        double halfH = Math.max(0.5, height / 2.0);
        double scale = Math.max(Math.abs(dx) / halfW, Math.abs(dy) / halfH);
        if (scale <= 0.0) {
            return new int[]{(int) Math.round(cx), (int) Math.round(cy)};
        }

        int ax = (int) Math.round(cx + (dx / scale));
        int ay = (int) Math.round(cy + (dy / scale));
        return new int[]{ax, ay};
    }

    private int getContextRow(double mouseX, double mouseY) {
        if (mouseX < this.contextX || mouseX >= this.contextX + CONTEXT_WIDTH) return -1;
        int localY = (int) mouseY - (this.contextY + CONTEXT_INSET_Y);
        if (localY < 0) return -1;
        int idx = localY / CONTEXT_ROW_HEIGHT;
        return idx >= 0 && idx < 5 ? idx : -1;
    }

    private TexturePlacements.Info copiedTextureInfo(ClientQuestClipboard.CopiedQuest copiedQuest) {
        return TexturePlacements.getOrDefault(copiedQuest.quest.display().iconBackground(), TexturePlacements.NO_OFFSET_24X);
    }

    private float copiedNodeScale(ClientQuestClipboard.CopiedQuest copiedQuest) {
        GroupDisplay sourceDisplay = copiedQuest.quest.display().groups().get(copiedQuest.sourceGroup);
        if (sourceDisplay == null && !copiedQuest.quest.display().groups().isEmpty()) {
            sourceDisplay = copiedQuest.quest.display().groups().values().iterator().next();
        }
        float value = sourceDisplay == null ? 1.0f : sourceDisplay.nodeScale();
        if (Float.isNaN(value) || Float.isInfinite(value)) return 1.0f;
        return Math.max(0.5f, value);
    }

    private record PreviewQuest(String id, Set<String> dependencies, int left, int top, int width, int height) {}
}
