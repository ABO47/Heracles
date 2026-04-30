package earth.terrarium.heracles.client.screens.quests;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import com.teamresourceful.resourcefullib.client.CloseablePoseStack;
import com.teamresourceful.resourcefullib.client.utils.RenderUtils;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.api.quests.QuestDisplayStatus;
import earth.terrarium.heracles.client.handlers.ClientQuestClipboard;
import earth.terrarium.heracles.client.handlers.ClientQuestNetworking;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.handlers.CustomImageManager;
import earth.terrarium.heracles.client.handlers.GhostBlueprintManager;
import earth.terrarium.heracles.client.handlers.UndoRedoManager;
import earth.terrarium.heracles.client.screens.AbstractQuestScreen;
import earth.terrarium.heracles.client.screens.mousemode.MouseMode;
import earth.terrarium.heracles.client.utils.ClientUtils;
import earth.terrarium.heracles.client.utils.MouseClick;
import earth.terrarium.heracles.client.handlers.DisplayConfig;
import earth.terrarium.heracles.client.utils.TexturePlacements;
import earth.terrarium.heracles.api.quests.QuestDisplay;
import earth.terrarium.heracles.api.quests.GroupDisplay;
import earth.terrarium.heracles.client.widgets.base.BaseWidget;
import earth.terrarium.heracles.common.menus.quests.QuestsContent;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.quests.data.NetworkQuestData;
import earth.terrarium.heracles.common.network.packets.groups.OpenGroupPacket;
import earth.terrarium.heracles.common.network.packets.quests.OpenQuestPacket;
import earth.terrarium.heracles.common.utils.ModUtils;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.joml.Vector2i;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class QuestsWidget extends BaseWidget {

    public static final Vector2i offset = new Vector2i();

    private static final Vector2i MAX = new Vector2i(5000, 5000);
    private static final Vector2i MIN = new Vector2i(-5000, -5000);

    private static final ResourceLocation ARROW = new ResourceLocation(Heracles.MOD_ID, "textures/gui/arrow.png");


    private final Set<String> visibleQuests = new HashSet<>();
    private final List<QuestWidget> widgets = new ArrayList<>();
    private final List<ClientQuests.QuestEntry> entries = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();
    private boolean multiSelecting = false;
    private MouseClick multiSelectStart = null;
    private MouseClick multiSelectEnd = null;
    private final Set<String> multiSelected = new HashSet<>();
    private boolean groupDragging = false;
    private MouseClick groupDragStartLocal = null;
    private final Map<String, Vector2i> groupDragOriginalPositions = new HashMap<>();
    private MouseClick potentialMultiSelectStart = null;
    private boolean linkingActive = false;
    private String linkingSourceId = null;
    private boolean resizingSelection = false;
    private boolean rotatingSelection = false;
    private MouseClick resizeStartLocal = null;
    private SelectionBounds resizeStartBounds = null;
    private final Map<String, Float> resizeOriginalScales = new HashMap<>();
    private final Map<String, Vector2i> resizeOriginalPositions = new HashMap<>();
    private final Map<String, Vector2i> rotateOriginalPositions = new HashMap<>();
    private final Map<String, Vector2i> rotateOriginalCenters = new HashMap<>();
    private double rotatePivotLocalX = 0.0;
    private double rotatePivotLocalY = 0.0;
    private double rotateStartAngle = 0.0;
    private final List<DisplayConfig.CanvasSprite> canvasSprites = new ArrayList<>();
    private String selectedCanvasSpriteId = null;
    private String draggingCanvasSpriteId = null;
    private String resizingCanvasSpriteId = null;
    private Vector2i canvasSpriteDragStartMouseLocal = null;
    private Vector2i canvasSpriteResizeStartMouseLocal = null;
    private Vector2i canvasSpriteStartPosition = null;
    private Vector2i canvasSpriteStartSize = null;

    private final int x;
    private final int y;
    private final int fullWidth;
    private final int selectedWidth;
    private int width;
    private final int height;

    private final Vector2i start = new Vector2i();
    private final Vector2i startOffset = new Vector2i();
    private final Vector2i centreOffset = new Vector2i();

    private Vector2i lastClick = null;
    private boolean contextMenuOpen = false;

    private final SelectQuestHandler selectHandler;

    private final Supplier<MouseMode> mouseMode;
    private final BooleanSupplier inspectorOpened;

    private final String group;

    private String searchFilter = "";
    private final Set<String> searchMatches = new HashSet<>();
    private final Set<String> searchContext = new HashSet<>();

    private boolean minimapCollapsed = false;
    private static final int MINIMAP_W = 170;
    private static final int MINIMAP_H = 118;
    private static final int MINIMAP_MARGIN = 6;
    private static final int RESIZE_HANDLE_SIZE = 8;
    private static final int ROTATE_HANDLE_SIZE = 8;
    private static final int CANVAS_SPRITE_HANDLE_SIZE = 8;
    private static final int CANVAS_SPRITE_MIN_SIZE = 8;
    private static DisplayConfig.CanvasSprite copiedCanvasSprite = null;

    private QuestsContent content;

    private int maxX = 0;
    private int maxY = 0;
    private int minX = 0;
    private int minY = 0;

    public QuestsWidget(int x, int y, int width, int selectedWidth, int height, BooleanSupplier inspectorOpened, Supplier<MouseMode> mouseMode, Consumer<ClientQuests.QuestEntry> onSelection) {
        this.x = x;
        this.y = y;
        this.fullWidth = width;
        this.selectedWidth = selectedWidth;
        this.width = width;
        this.height = height;
        this.inspectorOpened = inspectorOpened;
        this.mouseMode = mouseMode;
        this.group = ClientUtils.screen() instanceof QuestsScreen screen ? screen.getGroup() : "";
        this.minimapCollapsed = DisplayConfig.isMinimapCollapsed(this.group);
        this.selectHandler = new SelectQuestHandler(this.group, onSelection);
    }

    public void update(QuestsContent content, List<Pair<ClientQuests.QuestEntry, ModUtils.QuestStatus>> quests) {
        this.content = content;
        this.widgets.clear();
        this.entries.clear();
        this.visibleQuests.clear();
        this.searchMatches.clear();
        this.searchContext.clear();

        Object2BooleanMap<String> statuses = new Object2BooleanOpenHashMap<>();
        statuses.defaultReturnValue(true);
        quests.forEach(quest -> statuses.put(quest.getFirst().key(), quest.getSecond().isComplete()));

        List<Pair<ClientQuests.QuestEntry, ModUtils.QuestStatus>> visible = new ArrayList<>();
        List<Pair<ClientQuests.QuestEntry, ModUtils.QuestStatus>> unfilteredVisible = new ArrayList<>();

        boolean isEditing = isEditing();
        boolean useCanvasLimit = DisplayConfig.hasCanvasLimit(this.group);

        if (useCanvasLimit || isEditing) {
            applyEditorBounds();
        }
        if (isEditing) {
            centreOffset.set(0, 0);
        }
        reloadCanvasSprites();
        if (this.selectedCanvasSpriteId != null && findCanvasSprite(this.selectedCanvasSpriteId) == null) {
            this.selectedCanvasSpriteId = null;
        }

        for (Pair<ClientQuests.QuestEntry, ModUtils.QuestStatus> pair : quests) {
            ClientQuests.QuestEntry entry = pair.getFirst();
            if (shouldHide(this.group, statuses, entry)) continue;
            unfilteredVisible.add(pair);
        }

        if (this.searchFilter == null || this.searchFilter.isBlank()) {
            visible.addAll(unfilteredVisible);
        } else {
            for (Pair<ClientQuests.QuestEntry, ModUtils.QuestStatus> pair : unfilteredVisible) {
                if (matchesSearch(pair.getFirst())) {
                    this.searchMatches.add(pair.getFirst().key());
                }
                visible.add(pair);
            }
            for (ClientQuests.QuestEntry entry : unfilteredVisible.stream().map(Pair::getFirst).toList()) {
                if (!this.searchMatches.contains(entry.key())) continue;
                this.searchContext.add(entry.key());
                for (ClientQuests.QuestEntry dep : entry.dependencies()) {
                    if (dep.value().display().groups().containsKey(this.group)) this.searchContext.add(dep.key());
                }
                for (ClientQuests.QuestEntry child : entry.dependents()) {
                    if (child.value().display().groups().containsKey(this.group)) this.searchContext.add(child.key());
                }
            }
        }

        if (!isEditing && !useCanvasLimit) {
            this.minX = Integer.MAX_VALUE;
            this.minY = Integer.MAX_VALUE;
            this.maxX = Integer.MIN_VALUE;
            this.maxY = Integer.MIN_VALUE;
        }

        for (Pair<ClientQuests.QuestEntry, ModUtils.QuestStatus> pair : visible) {
            ClientQuests.QuestEntry entry = pair.getFirst();
            this.entries.add(entry);
            this.widgets.add(new QuestWidget(entry, getRenderStatus(entry, pair.getSecond(), isEditing)));
            this.visibleQuests.add(entry.key());
            Vector2i pos = entry.value().display().position(this.group);
            if (!isEditing && !useCanvasLimit) {
                this.minX = Math.min(this.minX, pos.x() + questOffsetX(entry));
                this.minY = Math.min(this.minY, pos.y() + questOffsetY(entry));
                this.maxX = Math.max(this.maxX, pos.x() + questOffsetX(entry) + questWidth(entry));
                this.maxY = Math.max(this.maxY, pos.y() + questOffsetY(entry) + questHeight(entry));
            }
        }

        if (!isEditing && !useCanvasLimit && this.minX == Integer.MAX_VALUE) {
            this.minX = 0;
            this.minY = 0;
            this.maxX = 0;
            this.maxY = 0;
        }

        clampOffsetToBounds();
    }

    private boolean matchesSearch(ClientQuests.QuestEntry entry) {
        String q = this.searchFilter == null ? "" : this.searchFilter.trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) return true;
        String[] terms = q.split("\\s+");
        StringBuilder haystack = new StringBuilder();
        haystack.append(entry.key()).append(' ');
        haystack.append(entry.value().display().title().getString()).append(' ');
        haystack.append(entry.value().display().subtitle().getString()).append(' ');
        if (entry.value().display().title().getContents() instanceof TranslatableContents t) {
            haystack.append(t.getKey()).append(' ');
        }
        StringBuilder desc = new StringBuilder();
        for (String line : entry.value().display().description()) desc.append(line).append(' ');
        haystack.append(desc);
        String normalized = haystack.toString().toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (term.isBlank()) continue;
            if (!normalized.contains(term)) return false;
        }
        return true;
    }

    public void setContextMenuOpen(boolean open) {
        this.contextMenuOpen = open;
    }

    private static boolean shouldHide(String group, Object2BooleanMap<String> statuses, ClientQuests.QuestEntry quest) {
        var value = quest.value();
        boolean inGroup = value.display().groups().containsKey(group);
        if (!inGroup) return true;
        return shouldHide(statuses, quest, value.settings().hiddenUntil());
    }

    private static boolean shouldHide(Object2BooleanMap<String> statuses, ClientQuests.QuestEntry quest, QuestDisplayStatus status) {
        if (status == QuestDisplayStatus.COMPLETED) {
            return !statuses.getBoolean(quest.key());
        } else if (status == QuestDisplayStatus.IN_PROGRESS) {
            for (var dependency : quest.dependencies()) {
                if (!statuses.getBoolean(dependency.key())) {
                    return true;
                }
            }
        } else if (status == QuestDisplayStatus.DEPENDENCIES_VISIBLE) {
            boolean visible = statuses.getBoolean(quest.key());
            if (visible) {
                return false;
            }
            for (var dependency : quest.dependencies()) {
                if (shouldHide(statuses, dependency, QuestDisplayStatus.DEPENDENCIES_VISIBLE)) {
                    return true;
                }
            }
            return quest.value().settings().hiddenUntil() != QuestDisplayStatus.DEPENDENCIES_VISIBLE;
        }
        return false;
    }

    public void addQuest(ClientQuests.QuestEntry quest) {
        for (QuestWidget widget : this.widgets) {
            if (widget.id().equals(quest.key())) {
                return;
            }
        }
        ModUtils.QuestStatus status = getRenderStatus(quest, ClientQuests.getStatus(quest.key()).orElse(ModUtils.QuestStatus.LOCKED), this.isEditing());
        this.widgets.add(new QuestWidget(quest, status));
        this.entries.add(quest);
        if (this.content != null) {
            this.content.quests().put(quest.key(), status);
        }
        if (quest.value().display().groups().containsKey(this.group)) {
            this.visibleQuests.add(quest.key());
        }
        Vector2i pos = quest.value().display().position(this.group);
        if (!this.isEditing() && !DisplayConfig.hasCanvasLimit(this.group)) {
            this.minX = Math.min(this.minX, pos.x() + questOffsetX(quest));
            this.minY = Math.min(this.minY, pos.y() + questOffsetY(quest));
            this.maxX = Math.max(this.maxX, pos.x() + questOffsetX(quest) + questWidth(quest));
            this.maxY = Math.max(this.maxY, pos.y() + questOffsetY(quest) + questHeight(quest));
        }
    }

    public void removeQuest(ClientQuests.QuestEntry quest) {
        this.widgets.removeIf(widget -> widget.id().equals(quest.key()));
        this.entries.removeIf(entry -> entry.key().equals(quest.key()));
        QuestWidget questWidget = this.selectHandler.selectedQuest();
        if (questWidget != null && Objects.equals(this.selectHandler.selectedQuest().id(), quest.key())) {
            this.selectHandler.release();
        }
        if (this.content != null) {
            this.content.quests().remove(quest.key());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.x;
        int y = this.y;
        this.width = inspectorOpened.getAsBoolean() ? this.selectedWidth : this.fullWidth;
        clampOffsetToBounds();

        try (var scissor = RenderUtils.createScissor(Minecraft.getInstance(), graphics, x, y, width, height)) {
            x += this.fullWidth / 2;
            y += this.height / 2;

            renderCanvasBackground(graphics, x, y);
            renderCanvasSprites(graphics, x, y);
            if (isEditing() && DisplayConfig.hasCanvasLimit(this.group)) {
                int borderLeft = x + offset.x() + this.minX;
                int borderTop = y + offset.y() + this.minY;
                int borderWidth = Math.max(1, this.maxX - this.minX);
                int borderHeight = Math.max(1, this.maxY - this.minY);
                graphics.renderOutline(borderLeft, borderTop, borderWidth, borderHeight, 0x88EFC874);
            }

            if (isEditing() && DisplayConfig.isGridEnabled(this.group)) {
                int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
                int centreX = x;
                int centreY = y;
                int scissorLeft = centreX - this.fullWidth / 2;
                int scissorTop = centreY - this.height / 2;

                int opacityPercent = DisplayConfig.getGridOpacity(this.group);
                int alpha = Mth.clamp((int) Math.round(opacityPercent * 2.55), 0, 255);
                int gridColor = (alpha << 24) | 0xFFFFFF;

                int minWorldX = (int) Math.floor((-this.fullWidth / 2.0 - offset.x()) / (double) gs) * gs;
                int maxWorldX = (int) Math.ceil((this.width - this.fullWidth / 2.0 - offset.x()) / (double) gs) * gs;
                for (int wx = minWorldX; wx <= maxWorldX; wx += gs) {
                    int sx = (int) (centreX + offset.x() + wx);
                    graphics.fill(sx, scissorTop, sx + 1, scissorTop + this.height, gridColor);
                }

                int minWorldY = (int) Math.floor((-this.height / 2.0 - offset.y()) / (double) gs) * gs;
                int maxWorldY = (int) Math.ceil((this.height / 2.0 - offset.y()) / (double) gs) * gs;
                for (int wy = minWorldY; wy <= maxWorldY; wy += gs) {
                    int sy = (int) (centreY + offset.y() + wy);
                    graphics.fill(scissorLeft, sy, scissorLeft + this.width, sy + 1, gridColor);
                }

                var font = Minecraft.getInstance().font;
                int desiredSpacing = 64;
                int stepCount = Math.max(1, desiredSpacing / gs);
                int labelInterval = gs * stepCount;

                for (int wx = minWorldX; wx <= maxWorldX; wx += labelInterval) {
                    int sx = (int) (centreX + offset.x() + wx);
                    if (sx >= scissorLeft && sx <= scissorLeft + this.width) {
                        String label = String.valueOf(wx);
                        int lw = font.width(label);
                        int lx = sx - lw / 2;
                        int ly = scissorTop + 2;
                        graphics.fill(lx - 2, ly - 1, lx + lw + 2, ly + font.lineHeight + 1, 0x66000000);
                        graphics.drawString(font, label, lx, ly, 0xFFFFFFFF, false);
                    }
                }

                for (int wy = minWorldY; wy <= maxWorldY; wy += labelInterval) {
                    int sy = (int) (centreY + offset.y() + wy);
                    if (sy >= scissorTop && sy <= scissorTop + this.height) {
                        String label = String.valueOf(wy);
                        int lw = font.width(label);
                        int lx = scissorLeft + 2;
                        int ly = sy - (font.lineHeight / 2);
                        graphics.fill(lx - 2, ly - 1, lx + lw + 6, ly + font.lineHeight + 1, 0x66000000);
                        graphics.drawString(font, label, lx, ly, 0xFFFFFFFF, false);
                    }
                }
            }
            RenderSystem.setShaderTexture(0, ARROW);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.enableBlend();

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();

            final Set<Pair<Vector2i, Vector2i>> lines = new HashSet<>();
            final Map<String, QuestWidget> widgetById = new HashMap<>();
            for (QuestWidget questWidget : this.widgets) {
                widgetById.put(questWidget.id(), questWidget);
            }

            this.connections.clear();
            for (ClientQuests.QuestEntry entry : this.entries) {
                QuestWidget sourceWidget = widgetById.get(entry.key());
                if (sourceWidget == null) continue;

                boolean isHovered = isMouseOver(mouseX, mouseY) &&
                                    mouseX >= x + offset.x() + sourceWidget.x() + sourceWidget.scaledOffsetX() &&
                                    mouseX <= x + offset.x() + sourceWidget.x() + sourceWidget.scaledOffsetX() + sourceWidget.width() &&
                                    mouseY >= y + offset.y() + sourceWidget.y() + sourceWidget.scaledOffsetY() &&
                                    mouseY <= y + offset.y() + sourceWidget.y() + sourceWidget.scaledOffsetY() + sourceWidget.height();

                RenderSystem.setShaderColor(0.9F, 0.9F, 0.9F, isHovered ? 0.8f : 0.4F);

                for (ClientQuests.QuestEntry child : entry.dependents()) {
                    if (!child.value().display().groups().containsKey(this.group)) continue;
                    if (!this.visibleQuests.contains(child.key())) continue;
                    if (!child.value().settings().showDependencyArrow()) continue;
                    QuestWidget childWidget = widgetById.get(child.key());
                    if (childWidget == null) continue;
                    boolean hiddenConnection = DisplayConfig.isConnectionHidden(this.group, entry.key(), child.key());

                    Vector2i position = sourceWidget.position();
                    Vector2i childPosition = childWidget.position();

                    if (lines.contains(new Pair<>(position, childPosition))) continue;
                    lines.add(new Pair<>(position, childPosition));

                    this.connections.add(new Connection(entry.key(), child.key(), sourceWidget.centerX(), sourceWidget.centerY(), childWidget.centerX(), childWidget.centerY(), hiddenConnection));

                    if (hiddenConnection) continue;

                    float px = sourceWidget.centerX();
                    float py = sourceWidget.centerY();
                    float cx = childWidget.centerX();
                    float cy = childWidget.centerY();

                    float length = Mth.sqrt(Mth.square(cx - px) + Mth.square(cy - py));

                    try (var pose = new CloseablePoseStack(graphics)) {
                        pose.translate(px + 2, py + 2, 0);
                        pose.translate(x + offset.x(), y + offset.y(), 0);
                        pose.mulPose(Axis.ZP.rotation((float) Mth.atan2(cy - py, cx - px)));

                        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                        buffer.vertex(pose.last().pose(), 0, -2, 0).uv(0, 0).endVertex();
                        buffer.vertex(pose.last().pose(), 0, 2, 0).uv(0, 1).endVertex();
                        buffer.vertex(pose.last().pose(), length, 2, 0).uv(length / 3f, 1).endVertex();
                        buffer.vertex(pose.last().pose(), length, -2, 0).uv(length / 3f, 0).endVertex();
                        tesselator.end();
                    }
                }
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();

            for (QuestWidget widget : this.widgets) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                widget.render(graphics, scissor.stack(), x + offset.x(), y + offset.y(), mouseX, mouseY, isMouseOver(mouseX, mouseY), partialTick);
                if (mouseMode.get().canSelect() && widget == this.selectHandler.selectedQuest()) {
                    graphics.renderOutline(
                        x + offset.x() + widget.x() + widget.scaledOffsetX() - 2, y + offset.y() + widget.y() + widget.scaledOffsetY() - 2,
                        widget.width() + 4, widget.height() + 4,
                        0xFFA8EFF0
                    );
                }
                if (mouseMode.get().canSelect() && this.multiSelected.contains(widget.id())) {
                    graphics.renderOutline(
                        x + offset.x() + widget.x() + widget.scaledOffsetX() - 2, y + offset.y() + widget.y() + widget.scaledOffsetY() - 2,
                        widget.width() + 4, widget.height() + 4,
                        0xFFA8EFF0
                    );
                }
                if (this.searchFilter != null && !this.searchFilter.isBlank() && this.searchMatches.contains(widget.id())) {
                    int ox = x + offset.x() + widget.x() + widget.scaledOffsetX() - 3;
                    int oy = y + offset.y() + widget.y() + widget.scaledOffsetY() - 3;
                    int ow = widget.width() + 6;
                    int oh = widget.height() + 6;
                    graphics.fill(ox, oy, ox + ow, oy + oh, 0x22F2CE71);
                    graphics.renderOutline(ox, oy, ow, oh, 0xFFEFC874);
                }
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (this.isEditing() && this.mouseMode.get().canDragSelection()) {
                SelectionBounds selectedBounds = getSelectionBoundsOnScreen(x + offset.x(), y + offset.y());
                if (selectedBounds != null) {
                    int handleLeft = selectedBounds.right() - RESIZE_HANDLE_SIZE;
                    int handleTop = selectedBounds.bottom() - RESIZE_HANDLE_SIZE;
                    graphics.fill(handleLeft, handleTop, selectedBounds.right(), selectedBounds.bottom(), 0xCCFFFFFF);
                    graphics.renderOutline(handleLeft, handleTop, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE, 0xFF3A3A3A);
                    int rotateHandleLeft = selectedBounds.right() - ROTATE_HANDLE_SIZE;
                    int rotateHandleTop = selectedBounds.top();
                    graphics.fill(rotateHandleLeft, rotateHandleTop, selectedBounds.right(), selectedBounds.top() + ROTATE_HANDLE_SIZE, 0xFF9FE8FF);
                    graphics.renderOutline(rotateHandleLeft, rotateHandleTop, ROTATE_HANDLE_SIZE, ROTATE_HANDLE_SIZE, 0xFF000000);
                }
            }

            if (GhostBlueprintManager.hasActive()) {
                var active = GhostBlueprintManager.getActive();
                if (!active.isEmpty()) {
                    double mouseLocalX = mouseX - (x + offset.x());
                    double mouseLocalY = mouseY - (y + offset.y());
                    int minX = Integer.MAX_VALUE;
                    int minY = Integer.MAX_VALUE;
                    int maxX = Integer.MIN_VALUE;
                    int maxY = Integer.MIN_VALUE;
                    List<GhostPreviewItem> prepared = new ArrayList<>();
                    for (var cq : active) {
                        var display = cq.quest.display();
                        var info = TexturePlacements.getOrDefault(display.iconBackground(), TexturePlacements.NO_OFFSET_24X);
                        GroupDisplay sourceDisplay = display.groups().get(cq.sourceGroup);
                        if (sourceDisplay == null) sourceDisplay = display.groups().get(this.group);
                        if (sourceDisplay == null && !display.groups().isEmpty()) {
                            sourceDisplay = display.groups().values().iterator().next();
                        }
                        float nodeScale = sourceDisplay == null ? 1.0f : sourceDisplay.nodeScale();
                        if (Float.isNaN(nodeScale) || Float.isInfinite(nodeScale)) nodeScale = 1.0f;
                        nodeScale = Math.max(0.5f, nodeScale);

                        int offX = Math.round(info.xOffset() * nodeScale);
                        int offY = Math.round(info.yOffset() * nodeScale);
                        int width = Math.max(1, Math.round(info.width() * nodeScale));
                        int height = Math.max(1, Math.round(info.height() * nodeScale));
                        int left = cq.sourcePos.x() + offX;
                        int top = cq.sourcePos.y() + offY;
                        minX = Math.min(minX, left);
                        minY = Math.min(minY, top);
                        maxX = Math.max(maxX, left + width);
                        maxY = Math.max(maxY, top + height);
                        prepared.add(new GhostPreviewItem(cq.originalId, display, info, nodeScale, cq.quest.dependencies(), left, top, width, height));
                    }
                    double anchorX = (minX == Integer.MAX_VALUE) ? 0.0 : (minX + maxX) / 2.0;
                    double anchorY = (minY == Integer.MAX_VALUE) ? 0.0 : (minY + maxY) / 2.0;

                    Map<String, int[]> ghostCenters = new HashMap<>();
                    for (GhostPreviewItem item : prepared) {
                        int drawLeft = (int) Math.round(mouseLocalX + (item.left() - anchorX));
                        int drawTop = (int) Math.round(mouseLocalY + (item.top() - anchorY));
                        int drawX = x + offset.x() + drawLeft - Math.round(item.info().xOffset() * item.scale());
                        int drawY = y + offset.y() + drawTop - Math.round(item.info().yOffset() * item.scale());

                        try (var pose = new CloseablePoseStack(graphics)) {
                            pose.translate(drawX, drawY, 0);
                            pose.scale(item.scale(), item.scale(), 1.0f);
                            graphics.blit(item.display().iconBackground(), item.info().xOffset(), item.info().yOffset(), 0, 0, item.info().width(), item.info().height(), item.info().width() * 5, item.info().height());
                            item.display().icon().render(graphics, scissor.stack(), 4, 4, 24, 24);
                        }

                        int centerX = x + offset.x() + drawLeft + (item.width() / 2);
                        int centerY = y + offset.y() + drawTop + (item.height() / 2);
                        ghostCenters.put(item.id(), new int[]{centerX, centerY});
                    }

                    for (GhostPreviewItem item : prepared) {
                        int[] from = ghostCenters.get(item.id());
                        if (from == null) continue;
                        for (String dep : item.dependencies()) {
                            int[] to = ghostCenters.get(dep);
                            if (to == null) continue;
                            drawMiniLine(graphics, from[0], from[1], to[0], to[1], 0x88D8E7FF);
                        }
                    }
                }
            }
        }

        try (var pose = new CloseablePoseStack(graphics)) {
            pose.translate(0, 0, 300);
            int xFromCentre = centreOffset.x - offset.x;
            if (xFromCentre > this.width / 4 || xFromCentre < -this.width / 4) {
                int canvasWidth = this.maxX - this.minX;
                int denom = canvasWidth + this.width - 10;
                if (denom > 0) {
                    int width = (this.width - 10) * (this.width - 10) / denom;
                    int barX;
                    if (canvasWidth != 0) {
                        barX = this.x + 5 + (this.width - 10 - width) / 2 + (this.width - 10 - width) * xFromCentre / canvasWidth;
                    } else {
                        barX = this.x + 5 + (this.width - 10 - width) / 2;
                    }
                    graphics.blitNineSliced(AbstractQuestScreen.HEADING, barX, this.y + this.height - 4, width, 2, 2, 32, 2, 224, 126);
                }
            }

            int yFromCentre = centreOffset.y - offset.y;
            if (yFromCentre > this.height / 4 || yFromCentre < -this.height / 4) {
                int canvasHeight = this.maxY - this.minY;
                int denomY = canvasHeight + this.height - 10;
                if (denomY > 0) {
                    int height = (this.height - 10) * (this.height - 10) / denomY;
                    int barY;
                    if (canvasHeight != 0) {
                        barY = this.y + 5 + (this.height - 10 - height) / 2 + (this.height - 10 - height) * yFromCentre / canvasHeight;
                    } else {
                        barY = this.y + 5 + (this.height - 10 - height) / 2;
                    }
                    graphics.blitNineSliced(AbstractQuestScreen.HEADING, this.x + this.width - 4, barY, 2, height, 2, 2, 32, 222, 96);
                }
            }
        }

        if (this.multiSelecting && this.multiSelectStart != null && this.multiSelectEnd != null) {
            int sx = (int) Math.min(this.multiSelectStart.x(), this.multiSelectEnd.x());
            int sy = (int) Math.min(this.multiSelectStart.y(), this.multiSelectEnd.y());
            int ex = (int) Math.max(this.multiSelectStart.x(), this.multiSelectEnd.x());
            int ey = (int) Math.max(this.multiSelectStart.y(), this.multiSelectEnd.y());
            int screenSX = x + offset.x() + sx;
            int screenSY = y + offset.y() + sy;
            int screenEX = x + offset.x() + ex;
            int screenEY = y + offset.y() + ey;
            graphics.fill(screenSX, screenSY, screenEX, screenEY, 0x33868686);
            graphics.fill(screenSX, screenSY, screenEX, screenSY + 1, 0xFFAAAAAA);
            graphics.fill(screenSX, screenSY, screenSX + 1, screenEY, 0xFFAAAAAA);
            graphics.fill(screenEX - 1, screenSY, screenEX, screenEY, 0xFFAAAAAA);
            graphics.fill(screenSX, screenEY - 1, screenEX, screenEY, 0xFFAAAAAA);
        }

        SelectionBounds selectedBounds = getSelectionBoundsOnScreen(x + offset.x(), y + offset.y());
        if (!this.multiSelecting && selectedBounds != null && this.multiSelected.size() > 1) {
            graphics.fill(selectedBounds.left, selectedBounds.top, selectedBounds.right, selectedBounds.bottom, 0x144FA5FF);
            graphics.fill(selectedBounds.left, selectedBounds.top, selectedBounds.right, selectedBounds.top + 1, 0xCC8CC9FF);
            graphics.fill(selectedBounds.left, selectedBounds.top, selectedBounds.left + 1, selectedBounds.bottom, 0xCC8CC9FF);
            graphics.fill(selectedBounds.right - 1, selectedBounds.top, selectedBounds.right, selectedBounds.bottom, 0xCC8CC9FF);
            graphics.fill(selectedBounds.left, selectedBounds.bottom - 1, selectedBounds.right, selectedBounds.bottom, 0xCC8CC9FF);
        }

        try (var pose = new CloseablePoseStack(graphics)) {
            pose.translate(0, 0, 300);
            renderMinimap(graphics, mouseX, mouseY);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (Screen.hasShiftDown()) {
            offset.add((int) scrollAmount * 10, 0);
        } else {
            offset.add(0, (int) scrollAmount * 10);
        }
        clampOffsetToBounds();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            MiniMapBox minimap = getMinimapBounds();
            if (minimap.toggleContains(mouseX, mouseY)) {
                this.minimapCollapsed = !this.minimapCollapsed;
                DisplayConfig.setMinimapCollapsed(this.group, this.minimapCollapsed);
                return true;
            }
            if (!this.minimapCollapsed && minimap.mapContains(mouseX, mouseY)) {
                jumpToMinimap(mouseX, mouseY, minimap);
                return true;
            }
        }
        MouseMode mode = this.mouseMode.get();
        SelectionBounds selectionBounds = getSelectionBoundsOnScreen(this.x + this.fullWidth / 2 + offset.x(), this.y + this.height / 2 + offset.y());
        boolean insideSelectionBox = selectionBounds != null && selectionBounds.contains(mouseX, mouseY);
        boolean onResizeHandle = selectionBounds != null && isOnResizeHandle(selectionBounds, mouseX, mouseY);
        boolean onRotateHandle = selectionBounds != null && isOnRotateHandle(selectionBounds, mouseX, mouseY);
        lastClick = (button == 0 || button == 2) ? new Vector2i((int) mouseX, (int) mouseY) : null;
        if (isMouseOver(mouseX, mouseY)) {
            if (GhostBlueprintManager.hasActive() && button == 0 && this.isEditing()) {
                MouseClick placeClick = new MouseClick((int) mouseX, (int) mouseY, button);
                List<ClientQuestClipboard.CopiedQuest> blueprint = GhostBlueprintManager.getActive();
                if (blueprint.isEmpty()) return true;

                List<ClientQuestClipboard.CopiedQuest> snapshot = new ArrayList<>();
                for (ClientQuestClipboard.CopiedQuest cq : blueprint) {
                    snapshot.add(new ClientQuestClipboard.CopiedQuest(
                        cq.originalId,
                        ClientQuestClipboard.deepCopy(cq.quest),
                        cq.sourceGroup,
                        new Vector2i(cq.sourcePos.x(), cq.sourcePos.y())
                    ));
                }

                java.util.concurrent.atomic.AtomicReference<List<String>> placedRef = new java.util.concurrent.atomic.AtomicReference<>(List.of());
                Runnable apply = () -> {
                    placedRef.set(ClientQuestClipboard.pasteBlueprintAt(this, placeClick, snapshot));
                    GhostBlueprintManager.clear();
                    this.refreshFromClient();
                };
                Runnable revert = () -> {
                    for (String id : placedRef.get()) {
                        ClientQuestNetworking.remove(id);
                    }
                    this.setMultiSelectedIds(Collections.emptySet());
                    this.refreshFromClient();
                };
                UndoRedoManager.getInstance().execute(apply, revert);
                return true;
            }
            MouseClick localClick = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, button));
            QuestWidget hoveredQuest = findQuestUnderMouse(mouseX, mouseY);
            DisplayConfig.CanvasSprite hoveredSprite = this.isEditing() && hoveredQuest == null ? findTopCanvasSpriteAt(localClick) : null;

            if (this.isEditing() && this.canEdit() && hoveredSprite != null) {
                this.selectedCanvasSpriteId = hoveredSprite.id();
                if (button == 1 && Minecraft.getInstance().screen instanceof AbstractQuestScreen<?> screen) {
                    this.lastClick = null;
                    var menu = new earth.terrarium.heracles.client.widgets.modals.ContextMenu(screen, this, (int) mouseX, (int) mouseY, hoveredSprite.id());
                    screen.addTemporary(menu);
                    return true;
                }
                if (button == 0 && mode.canDragSelection()) {
                    if (isOnCanvasSpriteResizeHandle(hoveredSprite, localClick)) {
                        this.resizingCanvasSpriteId = hoveredSprite.id();
                        this.canvasSpriteResizeStartMouseLocal = new Vector2i((int) localClick.x(), (int) localClick.y());
                        this.canvasSpriteStartPosition = new Vector2i(hoveredSprite.x(), hoveredSprite.y());
                        this.canvasSpriteStartSize = new Vector2i(hoveredSprite.width(), hoveredSprite.height());
                    } else {
                        this.draggingCanvasSpriteId = hoveredSprite.id();
                        this.canvasSpriteDragStartMouseLocal = new Vector2i((int) localClick.x(), (int) localClick.y());
                        this.canvasSpriteStartPosition = new Vector2i(hoveredSprite.x(), hoveredSprite.y());
                    }
                    return true;
                }
            }

            if (button == 0 && this.isEditing() && this.canEdit() && Screen.hasShiftDown()) {
                Connection clicked = findConnectionAt(mouseX, mouseY, true);
                if (clicked != null) {
                    this.lastClick = null;
                    boolean hidden = DisplayConfig.isConnectionHidden(this.group, clicked.source(), clicked.target());
                    DisplayConfig.setConnectionHidden(this.group, clicked.source(), clicked.target(), !hidden);
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    this.refreshFromClient();
                    return true;
                }
            }

            if (mode.canSelect() || mode.canOpen()) {
                if (button == 0 && this.isEditing() && mode.canDragSelection() && onRotateHandle) {
                    this.rotatingSelection = true;
                    this.rotateOriginalPositions.clear();
                    this.rotateOriginalCenters.clear();
                    this.rotatePivotLocalX = ((selectionBounds.left() + selectionBounds.right()) / 2.0) - (this.x + (this.fullWidth / 2.0) + offset.x());
                    this.rotatePivotLocalY = ((selectionBounds.top() + selectionBounds.bottom()) / 2.0) - (this.y + (this.height / 2.0) + offset.y());
                    MouseClick startLocal = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, button));
                    this.rotateStartAngle = Math.atan2(startLocal.y() - this.rotatePivotLocalY, startLocal.x() - this.rotatePivotLocalX);
                    for (String id : getActiveSelectionIds()) {
                        ClientQuests.get(id).ifPresent(entry -> {
                            Vector2i pos = entry.value().display().position(this.group);
                            this.rotateOriginalPositions.put(id, new Vector2i(pos.x(), pos.y()));
                            int centerX = pos.x() + questOffsetX(entry) + (questWidth(entry) / 2);
                            int centerY = pos.y() + questOffsetY(entry) + (questHeight(entry) / 2);
                            this.rotateOriginalCenters.put(id, new Vector2i(centerX, centerY));
                        });
                    }
                    return true;
                }
                if (button == 0 && this.isEditing() && mode.canDragSelection() && onResizeHandle) {
                    this.resizingSelection = true;
                    this.resizeStartLocal = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, button));
                    this.resizeStartBounds = selectionBounds;
                    this.resizeOriginalScales.clear();
                    this.resizeOriginalPositions.clear();
                    for (String id : getActiveSelectionIds()) {
                        ClientQuests.get(id).ifPresent(entry -> {
                            GroupDisplay display = entry.value().display().groups().getOrDefault(this.group, GroupDisplay.create(this.group));
                            this.resizeOriginalScales.put(id, display.nodeScale());
                            this.resizeOriginalPositions.put(id, new Vector2i(display.position().x(), display.position().y()));
                        });
                    }
                    return true;
                }
                if (button == 0 && mode.canDragSelection() && insideSelectionBox && !this.multiSelected.isEmpty()) {
                    this.groupDragging = true;
                    this.groupDragStartLocal = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, button));
                    this.groupDragOriginalPositions.clear();
                    for (String id : this.multiSelected) {
                        ClientQuests.get(id).ifPresent(e -> {
                            Vector2i pos = e.value().display().position(this.group);
                            this.groupDragOriginalPositions.put(id, new Vector2i(pos.x(), pos.y()));
                        });
                    }
                    return true;
                }
                for (QuestWidget widget : this.widgets) {
                    if (!widget.isMouseOver(mouseX - (this.x + (this.fullWidth / 2f) + offset.x()), mouseY - (this.y + (this.height / 2f) + offset.y()))) continue;
                    this.selectedCanvasSpriteId = null;
                    if (button == 0 && mode.canDragSelection() && this.multiSelected.contains(widget.id())) {
                        this.groupDragging = true;
                        this.groupDragStartLocal = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, button));
                        this.groupDragOriginalPositions.clear();
                        for (String id : this.multiSelected) {
                            ClientQuests.get(id).ifPresent(e -> {
                                Vector2i pos = e.value().display().position(this.group);
                                this.groupDragOriginalPositions.put(id, new Vector2i(pos.x(), pos.y()));
                            });
                        }
                        return true;
                    }
                    if (button == 1 && this.canEdit() && this.isEditing() && Minecraft.getInstance().screen instanceof AbstractQuestScreen<?> screen) {
                        this.lastClick = null;
                        var menu = new earth.terrarium.heracles.client.widgets.modals.ContextMenu((AbstractQuestScreen<?>) screen, this, (int) mouseX, (int) mouseY, widget);
                        screen.addTemporary(menu);
                        return true;
                    }
                    if (this.linkingActive) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        boolean remove = Screen.hasShiftDown();
                        this.completeLink(widget, remove);
                    } else if (mode.canSelect()) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        this.selectHandler.clickQuest(mode, (int) mouseX, (int) mouseY, widget);
                    } else if (mode.canOpen()) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        NetworkHandler.CHANNEL.sendToServer(new OpenQuestPacket(
                            this.group, widget.id(), Minecraft.getInstance().screen instanceof QuestsEditScreen
                        ));
                    }
                    return true;
                }
            }
            if (button == 1) {
                var player = Minecraft.getInstance().player;
                if (player != null && this.canEdit()) {
                    if (Minecraft.getInstance().screen instanceof AbstractQuestScreen<?> screen) {
                        if (this.isEditing()) {
                            if (insideSelectionBox && !this.multiSelected.isEmpty()) {
                                QuestWidget sourceWidget = this.widgets.stream().filter(w -> this.multiSelected.contains(w.id())).findFirst().orElse(null);
                                if (sourceWidget != null) {
                                    this.lastClick = null;
                                    var menu = new earth.terrarium.heracles.client.widgets.modals.ContextMenu(screen, this, (int) mouseX, (int) mouseY, sourceWidget);
                                    screen.addTemporary(menu);
                                    return true;
                                }
                            }
                            Connection clicked = findConnectionAt(mouseX, mouseY, true);
                            if (clicked != null) {
                                this.lastClick = null;
                                var menu = new earth.terrarium.heracles.client.widgets.modals.ContextMenu((AbstractQuestScreen<?>) screen, this, (int) mouseX, (int) mouseY, clicked.source(), clicked.target());
                                screen.addTemporary(menu);
                                return true;
                            }

                            this.multiSelected.clear();
                            this.selectHandler.release();
                            this.lastClick = null;
                            var menu = new earth.terrarium.heracles.client.widgets.modals.ContextMenu((AbstractQuestScreen<?>) screen, this, (int) mouseX, (int) mouseY);
                            screen.addTemporary(menu);
                            return true;
                        } else {
                            NetworkHandler.CHANNEL.sendToServer(new OpenGroupPacket(this.group, true));
                            return true;
                        }
                    }
                }
            }
            if (button == 0 && mode.canDragSelection()) {
                this.potentialMultiSelectStart = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, button));
                this.multiSelected.clear();
                this.selectHandler.release();
                if (hoveredSprite == null && hoveredQuest == null) {
                    this.selectedCanvasSpriteId = null;
                }
                return true;
            }
            if (this.linkingActive) {
                this.cancelLink();
            } else {
                this.selectHandler.release();
            }
            start.set((int) mouseX, (int) mouseY);
            startOffset.set(offset.x(), offset.y());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 1) return false;
        if (!this.resizingSelection && !this.rotatingSelection && this.draggingCanvasSpriteId == null && this.resizingCanvasSpriteId == null && !isMouseOver(mouseX, mouseY)) return false;
        if (lastClick == null || !isMouseOver(lastClick.x(), lastClick.y())) return false;

        if (this.draggingCanvasSpriteId != null && this.canvasSpriteDragStartMouseLocal != null && this.canvasSpriteStartPosition != null) {
            MouseClick nowLocal = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, 0));
            int dx = (int) Math.round(nowLocal.x() - this.canvasSpriteDragStartMouseLocal.x());
            int dy = (int) Math.round(nowLocal.y() - this.canvasSpriteDragStartMouseLocal.y());
            int newX = this.canvasSpriteStartPosition.x() + dx;
            int newY = this.canvasSpriteStartPosition.y() + dy;
            if (DisplayConfig.isGridLocked(this.group)) {
                int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
                newX = Math.round((float) newX / (float) gs) * gs;
                newY = Math.round((float) newY / (float) gs) * gs;
            }
            DisplayConfig.CanvasSprite sprite = findCanvasSprite(this.draggingCanvasSpriteId);
            if (sprite != null) {
                Vector2i clamped = clampCanvasSpritePosition(newX, newY, sprite.width(), sprite.height());
                updateCanvasSprite(this.draggingCanvasSpriteId, current -> current.withPosition(clamped.x(), clamped.y()));
            }
            return true;
        }

        if (this.resizingCanvasSpriteId != null && this.canvasSpriteResizeStartMouseLocal != null && this.canvasSpriteStartSize != null) {
            MouseClick nowLocal = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, 0));
            int dx = (int) Math.round(nowLocal.x() - this.canvasSpriteResizeStartMouseLocal.x());
            int dy = (int) Math.round(nowLocal.y() - this.canvasSpriteResizeStartMouseLocal.y());
            int baseWidth = Math.max(CANVAS_SPRITE_MIN_SIZE, this.canvasSpriteStartSize.x());
            int baseHeight = Math.max(CANVAS_SPRITE_MIN_SIZE, this.canvasSpriteStartSize.y());
            boolean keepRatio = Screen.hasShiftDown() && baseWidth > 0 && baseHeight > 0;
            float aspect = keepRatio ? (float) baseWidth / (float) baseHeight : 1.0f;
            boolean widthDriven = Math.abs(dx) >= Math.abs(dy);

            int newWidth;
            int newHeight;
            if (keepRatio) {
                if (widthDriven) {
                    newWidth = Math.max(CANVAS_SPRITE_MIN_SIZE, baseWidth + dx);
                    newHeight = Math.max(CANVAS_SPRITE_MIN_SIZE, Math.round(newWidth / aspect));
                } else {
                    newHeight = Math.max(CANVAS_SPRITE_MIN_SIZE, baseHeight + dy);
                    newWidth = Math.max(CANVAS_SPRITE_MIN_SIZE, Math.round(newHeight * aspect));
                }
            } else {
                newWidth = Math.max(CANVAS_SPRITE_MIN_SIZE, baseWidth + dx);
                newHeight = Math.max(CANVAS_SPRITE_MIN_SIZE, baseHeight + dy);
            }

            if (DisplayConfig.isGridLocked(this.group)) {
                int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
                if (keepRatio) {
                    if (widthDriven) {
                        newWidth = Math.max(CANVAS_SPRITE_MIN_SIZE, Math.round((float) newWidth / (float) gs) * gs);
                        newHeight = Math.max(CANVAS_SPRITE_MIN_SIZE, Math.round(newWidth / aspect));
                    } else {
                        newHeight = Math.max(CANVAS_SPRITE_MIN_SIZE, Math.round((float) newHeight / (float) gs) * gs);
                        newWidth = Math.max(CANVAS_SPRITE_MIN_SIZE, Math.round(newHeight * aspect));
                    }
                } else {
                    newWidth = Math.max(CANVAS_SPRITE_MIN_SIZE, Math.round((float) newWidth / (float) gs) * gs);
                    newHeight = Math.max(CANVAS_SPRITE_MIN_SIZE, Math.round((float) newHeight / (float) gs) * gs);
                }
            }
            if (this.canvasSpriteStartPosition != null) {
                Vector2i clamped = clampCanvasSpritePosition(this.canvasSpriteStartPosition.x(), this.canvasSpriteStartPosition.y(), newWidth, newHeight);
                final int fx = clamped.x();
                final int fy = clamped.y();
                final int fw = newWidth;
                final int fh = newHeight;
                updateCanvasSprite(this.resizingCanvasSpriteId, current -> current.withPosition(fx, fy).withSize(fw, fh));
            }
            return true;
        }

        MouseMode mode = this.mouseMode.get();
        if (mode.canDrag() || button == InputConstants.MOUSE_BUTTON_MIDDLE) {
            int newX = (int) (mouseX - start.x() + startOffset.x());
            int newY = (int) (mouseY - start.y() + startOffset.y());
            offset.set(newX, newY);
            clampOffsetToBounds();
        } else if (mode.canDragSelection()) {
            if (!this.multiSelecting && this.potentialMultiSelectStart != null) {
                MouseClick nowLocal = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, 0));
                double dx = Math.abs(nowLocal.x() - this.potentialMultiSelectStart.x());
                double dy = Math.abs(nowLocal.y() - this.potentialMultiSelectStart.y());
                if (dx > 4.0 || dy > 4.0) {
                    this.multiSelecting = true;
                    this.multiSelectStart = this.potentialMultiSelectStart;
                    this.multiSelectEnd = nowLocal;
                    this.multiSelected.clear();
                    this.potentialMultiSelectStart = null;
                } else {
                    return true;
                }
            }
            if (this.multiSelecting) {
                this.multiSelectEnd = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, 0));
                int sx = (int) Math.min(this.multiSelectStart.x(), this.multiSelectEnd.x());
                int sy = (int) Math.min(this.multiSelectStart.y(), this.multiSelectEnd.y());
                int ex = (int) Math.max(this.multiSelectStart.x(), this.multiSelectEnd.x());
                int ey = (int) Math.max(this.multiSelectStart.y(), this.multiSelectEnd.y());
                this.multiSelected.clear();
                for (QuestWidget widget : this.widgets) {
                    int wx = widget.x() + widget.scaledOffsetX();
                    int wy = widget.y() + widget.scaledOffsetY();
                    if (wx + widget.width() < sx || wy + widget.height() < sy || wx > ex || wy > ey) continue;
                    this.multiSelected.add(widget.id());
                }
                return true;
            }
            if (this.groupDragging) {
                MouseClick nowLocal = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, 0));
                int dx = (int) Math.round(nowLocal.x() - this.groupDragStartLocal.x());
                int dy = (int) Math.round(nowLocal.y() - this.groupDragStartLocal.y());
                for (Map.Entry<String, Vector2i> entry : this.groupDragOriginalPositions.entrySet()) {
                    String id = entry.getKey();
                    Vector2i orig = entry.getValue();
                    int newX = orig.x() + dx;
                    int newY = orig.y() + dy;
                    if (DisplayConfig.isGridLocked(this.group)) {
                        int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
                        ClientQuests.QuestEntry entryObj = ClientQuests.get(id).orElse(null);
                        if (entryObj != null) {
                            TexturePlacements.Info info = textureInfo(entryObj);
                            float scale = nodeScale(entryObj);
                            int offX = Math.round(info.xOffset() * scale);
                            int offY = Math.round(info.yOffset() * scale);
                            int insetX = visualInsetX(info, scale);
                            int insetY = visualInsetY(info, scale);
                            int snappedTopLeftX = Math.round((float) (newX + offX + insetX) / (float) gs) * gs;
                            int snappedTopLeftY = Math.round((float) (newY + offY + insetY) / (float) gs) * gs;
                            newX = snappedTopLeftX - offX - insetX;
                            newY = snappedTopLeftY - offY - insetY;
                        } else {
                            newX = Math.round((float) newX / (float) gs) * gs;
                            newY = Math.round((float) newY / (float) gs) * gs;
                        }
                    }
                    ClientQuests.QuestEntry clampEntry = ClientQuests.get(id).orElse(null);
                    if (clampEntry != null) {
                        TexturePlacements.Info info = textureInfo(clampEntry);
                        float scale = nodeScale(clampEntry);
                        Vector2i clamped = clampQuestPositionToCanvas(info, scale, newX, newY);
                        newX = clamped.x();
                        newY = clamped.y();
                    }
                    int fx = newX;
                    int fy = newY;
                    ClientQuests.get(id).ifPresent(entryObj -> ClientQuests.updateQuest(entryObj, quest -> NetworkQuestData.builder().group(quest, this.group, pos -> {
                        pos.x = fx;
                        pos.y = fy;
                        return pos;
                    }), false));
                }
                return true;
            }
            if (this.resizingSelection && this.resizeStartLocal != null && this.resizeStartBounds != null) {
                MouseClick nowLocal = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, 0));
                int startRight = this.resizeStartBounds.right - (this.x + this.fullWidth / 2 + offset.x());
                int startBottom = this.resizeStartBounds.bottom - (this.y + this.height / 2 + offset.y());
                int startLeft = this.resizeStartBounds.left - (this.x + this.fullWidth / 2 + offset.x());
                int startTop = this.resizeStartBounds.top - (this.y + this.height / 2 + offset.y());
                float baseW = Math.max(1f, startRight - startLeft);
                float baseH = Math.max(1f, startBottom - startTop);
                float newW = (float) Math.max(4.0, nowLocal.x() - startLeft);
                float newH = (float) Math.max(4.0, nowLocal.y() - startTop);
                float factor = Math.max(newW / baseW, newH / baseH);
                final float resizeFactor = Math.max(0.5f, factor);
                boolean snapScaleToGrid = DisplayConfig.isGridLocked(this.group);
                for (Map.Entry<String, Float> entry : this.resizeOriginalScales.entrySet()) {
                    String id = entry.getKey();
                    float original = entry.getValue() == null ? 1.0f : entry.getValue();
                    float finalScale = Math.max(0.5f, original * resizeFactor);
                    ClientQuests.get(id).ifPresent(entryObj -> {
                        float scaleValue = finalScale;
                        TexturePlacements.Info info = textureInfo(entryObj);
                        if (snapScaleToGrid) {
                            int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
                            scaleValue = snapScaleForGrid(info, original, resizeFactor, gs, finalScale);
                        }
                        final float appliedScale = scaleValue;
                        GroupDisplay currentDisplay = entryObj.value().display().groups().getOrDefault(this.group, GroupDisplay.create(this.group));
                        Vector2i originalPos = this.resizeOriginalPositions.getOrDefault(id, currentDisplay.position());
                        int originalOffsetX = Math.round(info.xOffset() * original);
                        int originalOffsetY = Math.round(info.yOffset() * original);
                        int newOffsetX = Math.round(info.xOffset() * appliedScale);
                        int newOffsetY = Math.round(info.yOffset() * appliedScale);
                        int newPosX = originalPos.x() + (originalOffsetX - newOffsetX);
                        int newPosY = originalPos.y() + (originalOffsetY - newOffsetY);
                        if (snapScaleToGrid) {
                            int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
                            int insetX = visualInsetX(info, appliedScale);
                            int insetY = visualInsetY(info, appliedScale);
                            int snappedTopLeftX = Math.round((float) (newPosX + newOffsetX + insetX) / (float) gs) * gs;
                            int snappedTopLeftY = Math.round((float) (newPosY + newOffsetY + insetY) / (float) gs) * gs;
                            newPosX = snappedTopLeftX - newOffsetX - insetX;
                            newPosY = snappedTopLeftY - newOffsetY - insetY;
                        }
                        Vector2i clamped = clampQuestPositionToCanvas(info, appliedScale, newPosX, newPosY);
                        newPosX = clamped.x();
                        newPosY = clamped.y();
                        final int fPosX = newPosX;
                        final int fPosY = newPosY;
                        ClientQuests.updateQuest(entryObj, quest ->
                            NetworkQuestData.builder().groupDisplay(quest, this.group, display ->
                                display.withNodeScale(appliedScale).withPosition(new Vector2i(fPosX, fPosY))
                            ), false);
                    });
                }
                return true;
            }
            if (this.rotatingSelection && !this.rotateOriginalPositions.isEmpty() && !this.rotateOriginalCenters.isEmpty()) {
                MouseClick nowLocal = this.getLocal(new MouseClick((int) mouseX, (int) mouseY, 0));
                double currentAngle = Math.atan2(nowLocal.y() - this.rotatePivotLocalY, nowLocal.x() - this.rotatePivotLocalX);
                double delta = currentAngle - this.rotateStartAngle;
                if (Screen.hasShiftDown()) {
                    delta = Math.round(delta / (Math.PI / 4.0)) * (Math.PI / 4.0);
                }
                double cos = Math.cos(delta);
                double sin = Math.sin(delta);

                for (Map.Entry<String, Vector2i> entry : this.rotateOriginalPositions.entrySet()) {
                    String id = entry.getKey();
                    Vector2i originalCenter = this.rotateOriginalCenters.get(id);
                    if (originalCenter == null) continue;

                    double relX = originalCenter.x() - this.rotatePivotLocalX;
                    double relY = originalCenter.y() - this.rotatePivotLocalY;
                    double rotatedCenterX = this.rotatePivotLocalX + relX * cos - relY * sin;
                    double rotatedCenterY = this.rotatePivotLocalY + relX * sin + relY * cos;

                    ClientQuests.get(id).ifPresent(entryObj -> {
                        int offsetToCenterX = questOffsetX(entryObj) + (questWidth(entryObj) / 2);
                        int offsetToCenterY = questOffsetY(entryObj) + (questHeight(entryObj) / 2);
                        int newX = (int) Math.round(rotatedCenterX - offsetToCenterX);
                        int newY = (int) Math.round(rotatedCenterY - offsetToCenterY);
                        TexturePlacements.Info info = textureInfo(entryObj);
                        float scale = nodeScale(entryObj);
                        Vector2i clamped = clampQuestPositionToCanvas(info, scale, newX, newY);
                        int fx = clamped.x();
                        int fy = clamped.y();
                        ClientQuests.updateQuest(entryObj, quest -> NetworkQuestData.builder().group(quest, this.group, pos -> {
                            pos.x = fx;
                            pos.y = fy;
                            return pos;
                        }), false);
                    });
                }
                return true;
            }
            this.selectHandler.onDrag((int) mouseX, (int) mouseY);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.draggingCanvasSpriteId != null || this.resizingCanvasSpriteId != null) {
            this.draggingCanvasSpriteId = null;
            this.resizingCanvasSpriteId = null;
            this.canvasSpriteDragStartMouseLocal = null;
            this.canvasSpriteResizeStartMouseLocal = null;
            this.canvasSpriteStartPosition = null;
            this.canvasSpriteStartSize = null;
            persistCanvasSprites();
            return true;
        }
        if (this.resizingSelection) {
            this.resizingSelection = false;
            this.resizeStartLocal = null;
            this.resizeStartBounds = null;
            this.resizeOriginalScales.clear();
            this.resizeOriginalPositions.clear();
            return true;
        }
        if (this.rotatingSelection) {
            this.rotatingSelection = false;
            this.rotateOriginalPositions.clear();
            this.rotateOriginalCenters.clear();
            return true;
        }
        if (this.groupDragging) {
            this.groupDragging = false;
            this.groupDragOriginalPositions.clear();
            this.groupDragStartLocal = null;
            return true;
        }
        if (this.multiSelecting) {
            this.multiSelecting = false;
            this.multiSelectStart = null;
            this.multiSelectEnd = null;
            if (this.multiSelected.size() == 1) {
                String id = this.multiSelected.iterator().next();
                ClientQuests.get(id).ifPresent(entry -> {});
            }
            return true;
        }
        if (this.potentialMultiSelectStart != null) {
            this.potentialMultiSelectStart = null;
            this.multiSelected.clear();
            this.selectHandler.release();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        MouseMode mode = this.mouseMode.get();
        if (mode.canSelect() && this.selectHandler.onKeyPress(keyCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    public SelectQuestHandler selectHandler() {
        return this.selectHandler;
    }

    public void cancelActiveMouseInteraction() {
        this.lastClick = null;
        this.potentialMultiSelectStart = null;
        this.multiSelecting = false;
        this.multiSelectStart = null;
        this.multiSelectEnd = null;
        this.groupDragging = false;
        this.groupDragStartLocal = null;
        this.groupDragOriginalPositions.clear();
        this.resizingSelection = false;
        this.resizeStartLocal = null;
        this.resizeStartBounds = null;
        this.resizeOriginalScales.clear();
        this.resizeOriginalPositions.clear();
        this.rotatingSelection = false;
        this.rotateOriginalPositions.clear();
        this.rotateOriginalCenters.clear();
    }

    public MouseClick getLocal(MouseClick click) {
        int localX = (int) (click.x() - (this.x + (this.fullWidth / 2f) + offset.x()));
        int localY = (int) (click.y() - (this.y + (this.height / 2f) + offset.y()));
        return new MouseClick(localX, localY, click.button());
    }

    private Connection findConnectionAt(double mouseX, double mouseY, boolean includeHidden) {
        for (Connection conn : this.connections) {
            if (!includeHidden && conn.hidden()) continue;
            float sx = this.x + this.fullWidth / 2 + offset.x() + conn.sx();
            float sy = this.y + this.height / 2 + offset.y() + conn.sy();
            float tx = this.x + this.fullWidth / 2 + offset.x() + conn.tx();
            float ty = this.y + this.height / 2 + offset.y() + conn.ty();
            if (isPointNearSegment((float) mouseX, (float) mouseY, sx, sy, tx, ty, 4f)) {
                return conn;
            }
        }
        return null;
    }

    private static boolean isPointNearSegment(float px, float py, float x1, float y1, float x2, float y2, float threshold) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            float distSq = (px - x1) * (px - x1) + (py - y1) * (py - y1);
            return distSq <= threshold * threshold;
        }
        float t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        float projX = x1 + t * dx;
        float projY = y1 + t * dy;
        float distSq = (px - projX) * (px - projX) + (py - projY) * (py - projY);
        return distSq <= threshold * threshold;
    }

    public String group() {
        return this.group;
    }

    public boolean isEditing() {
        return ClientUtils.screen() instanceof QuestsEditScreen;
    }

    private ModUtils.QuestStatus getRenderStatus(ClientQuests.QuestEntry entry, ModUtils.QuestStatus status, boolean editing) {
        if (!editing) return status;
        return ClientQuests.getStatus(entry.key()).orElse(ModUtils.QuestStatus.LOCKED);
    }

    private void renderMinimap(GuiGraphics graphics, int mouseX, int mouseY) {
        MiniMapBox minimap = getMinimapBounds();
        graphics.fill(minimap.toggleX, minimap.toggleY, minimap.toggleX + 11, minimap.toggleY + 11, 0xCC202020);
        graphics.drawString(this.font, this.minimapCollapsed ? "+" : "-", minimap.toggleX + 3, minimap.toggleY + 2, 0xFFFFFFFF, false);
        if (this.minimapCollapsed) return;

        graphics.fill(minimap.mapX - 1, minimap.mapY - 1, minimap.mapX + minimap.mapW + 1, minimap.mapY + minimap.mapH + 1, 0xCC1B1B1B);
        graphics.fill(minimap.mapX, minimap.mapY, minimap.mapX + minimap.mapW, minimap.mapY + minimap.mapH, 0xAA0D0D0D);

        if (this.entries.isEmpty()) return;

        int minQuestX = Integer.MAX_VALUE;
        int minQuestY = Integer.MAX_VALUE;
        int maxQuestX = Integer.MIN_VALUE;
        int maxQuestY = Integer.MIN_VALUE;
        for (ClientQuests.QuestEntry entry : this.entries) {
            Vector2i pos = entry.value().display().position(this.group);
            minQuestX = Math.min(minQuestX, pos.x() + questOffsetX(entry));
            minQuestY = Math.min(minQuestY, pos.y() + questOffsetY(entry));
            maxQuestX = Math.max(maxQuestX, pos.x() + questOffsetX(entry) + questWidth(entry));
            maxQuestY = Math.max(maxQuestY, pos.y() + questOffsetY(entry) + questHeight(entry));
        }
        double leftWorld = -this.fullWidth / 2.0 - offset.x();
        double topWorld = -this.height / 2.0 - offset.y();
        double rightWorld = this.width - this.fullWidth / 2.0 - offset.x();
        double bottomWorld = this.height / 2.0 - offset.y();
        minQuestX = Math.min(minQuestX, (int) Math.floor(leftWorld));
        minQuestY = Math.min(minQuestY, (int) Math.floor(topWorld));
        maxQuestX = Math.max(maxQuestX, (int) Math.ceil(rightWorld));
        maxQuestY = Math.max(maxQuestY, (int) Math.ceil(bottomWorld));
        int mapSpanX = Math.max(1, maxQuestX - minQuestX);
        int mapSpanY = Math.max(1, maxQuestY - minQuestY);
        int padding = 8;
        double scaleX = (minimap.mapW - (padding * 2.0)) / (double) mapSpanX;
        double scaleY = (minimap.mapH - (padding * 2.0)) / (double) mapSpanY;
        double scale = Math.max(0.0001, Math.min(scaleX, scaleY));
        int drawW = Math.max(1, (int) Math.round(mapSpanX * scale));
        int drawH = Math.max(1, (int) Math.round(mapSpanY * scale));
        int drawX = minimap.mapX + (minimap.mapW - drawW) / 2;
        int drawY = minimap.mapY + (minimap.mapH - drawH) / 2;

        Map<String, int[]> minimapBoxes = new HashMap<>();
        for (ClientQuests.QuestEntry entry : this.entries) {
            Vector2i pos = entry.value().display().position(this.group);
            int qx = pos.x() + questOffsetX(entry);
            int qy = pos.y() + questOffsetY(entry);
            int qw = Math.max(1, questWidth(entry));
            int qh = Math.max(1, questHeight(entry));
            int px = drawX + Mth.clamp((int) Math.round((qx - minQuestX) * scale), 0, drawW - 1);
            int py = drawY + Mth.clamp((int) Math.round((qy - minQuestY) * scale), 0, drawH - 1);
            int pw = Math.max(2, (int) Math.round(qw * scale));
            int ph = Math.max(2, (int) Math.round(qh * scale));
            int maxW = Math.max(1, drawX + drawW - px);
            int maxH = Math.max(1, drawY + drawH - py);
            pw = Math.max(1, Math.min(pw, maxW));
            ph = Math.max(1, Math.min(ph, maxH));
            minimapBoxes.put(entry.key(), new int[]{px, py, pw, ph});
            int color = this.searchMatches.contains(entry.key()) ? 0xFFEFC874 : 0xFF8EC6FF;
            graphics.fill(px, py, px + pw, py + ph, 0xAA000000);
            if (pw > 2 && ph > 2) {
                graphics.fill(px + 1, py + 1, px + pw - 1, py + ph - 1, color);
            } else {
                graphics.fill(px, py, px + pw, py + ph, color);
            }
        }

        for (Connection conn : this.connections) {
            if (conn.hidden()) continue;
            int sx;
            int sy;
            int tx;
            int ty;
            int[] sourceBox = minimapBoxes.get(conn.source());
            int[] targetBox = minimapBoxes.get(conn.target());
            if (sourceBox != null && targetBox != null) {
                int sourceCenterX = sourceBox[0] + sourceBox[2] / 2;
                int sourceCenterY = sourceBox[1] + sourceBox[3] / 2;
                int targetCenterX = targetBox[0] + targetBox[2] / 2;
                int targetCenterY = targetBox[1] + targetBox[3] / 2;
                int[] sourceAnchor = edgeAnchorOnRect(sourceBox[0], sourceBox[1], sourceBox[2], sourceBox[3], targetCenterX, targetCenterY);
                int[] targetAnchor = edgeAnchorOnRect(targetBox[0], targetBox[1], targetBox[2], targetBox[3], sourceCenterX, sourceCenterY);
                sx = sourceAnchor[0];
                sy = sourceAnchor[1];
                tx = targetAnchor[0];
                ty = targetAnchor[1];
            } else {
                sx = drawX + Mth.clamp((int) Math.round((conn.sx() - minQuestX) * scale), 0, drawW - 1);
                sy = drawY + Mth.clamp((int) Math.round((conn.sy() - minQuestY) * scale), 0, drawH - 1);
                tx = drawX + Mth.clamp((int) Math.round((conn.tx() - minQuestX) * scale), 0, drawW - 1);
                ty = drawY + Mth.clamp((int) Math.round((conn.ty() - minQuestY) * scale), 0, drawH - 1);
            }
            drawMiniLine(graphics, sx, sy, tx, ty, 0x66D8E7FF);
        }

        int vx1 = drawX + Mth.clamp((int) Math.round((leftWorld - minQuestX) * scale), 0, drawW - 1);
        int vy1 = drawY + Mth.clamp((int) Math.round((topWorld - minQuestY) * scale), 0, drawH - 1);
        int vx2 = drawX + Mth.clamp((int) Math.round((rightWorld - minQuestX) * scale), 1, drawW);
        int vy2 = drawY + Mth.clamp((int) Math.round((bottomWorld - minQuestY) * scale), 1, drawH);

        graphics.renderOutline(Math.min(vx1, vx2), Math.min(vy1, vy2), Math.max(1, Math.abs(vx2 - vx1)), Math.max(1, Math.abs(vy2 - vy1)), 0xFFFFFFFF);
    }

    private static void drawMiniLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
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

    private void jumpToMinimap(double mouseX, double mouseY, MiniMapBox minimap) {
        if (this.entries.isEmpty()) return;
        int minQuestX = Integer.MAX_VALUE;
        int minQuestY = Integer.MAX_VALUE;
        int maxQuestX = Integer.MIN_VALUE;
        int maxQuestY = Integer.MIN_VALUE;
        for (ClientQuests.QuestEntry entry : this.entries) {
            Vector2i pos = entry.value().display().position(this.group);
            minQuestX = Math.min(minQuestX, pos.x() + questOffsetX(entry));
            minQuestY = Math.min(minQuestY, pos.y() + questOffsetY(entry));
            maxQuestX = Math.max(maxQuestX, pos.x() + questOffsetX(entry) + questWidth(entry));
            maxQuestY = Math.max(maxQuestY, pos.y() + questOffsetY(entry) + questHeight(entry));
        }
        int mapSpanX = Math.max(1, maxQuestX - minQuestX);
        int mapSpanY = Math.max(1, maxQuestY - minQuestY);
        int padding = 8;
        double scaleX = (minimap.mapW - (padding * 2.0)) / (double) mapSpanX;
        double scaleY = (minimap.mapH - (padding * 2.0)) / (double) mapSpanY;
        double scale = Math.max(0.0001, Math.min(scaleX, scaleY));
        int drawW = Math.max(1, (int) Math.round(mapSpanX * scale));
        int drawH = Math.max(1, (int) Math.round(mapSpanY * scale));
        int drawX = minimap.mapX + (minimap.mapW - drawW) / 2;
        int drawY = minimap.mapY + (minimap.mapH - drawH) / 2;

        double mapU = Mth.clamp((mouseX - drawX) / (double) Math.max(1, drawW), 0.0, 1.0);
        double mapV = Mth.clamp((mouseY - drawY) / (double) Math.max(1, drawH), 0.0, 1.0);
        double worldX = minQuestX + mapU * mapSpanX;
        double worldY = minQuestY + mapV * mapSpanY;

        offset.set((int) Math.round(-worldX), (int) Math.round(-worldY));
        clampOffsetToBounds();
    }

    private MiniMapBox getMinimapBounds() {
        int mapW = MINIMAP_W;
        int mapH = MINIMAP_H;
        int mapX = this.x + this.width - mapW - MINIMAP_MARGIN;
        int mapY = this.y + this.height - mapH - MINIMAP_MARGIN;
        int toggleX = this.x + this.width - 11 - MINIMAP_MARGIN;
        int toggleY = mapY - 13;
        return new MiniMapBox(mapX, mapY, mapW, mapH, toggleX, toggleY);
    }

    private SelectionBounds getSelectionBoundsOnScreen(int centreX, int centreY) {
        Set<String> active = getActiveSelectionIds();
        if (active.isEmpty()) return null;
        int minSelX = Integer.MAX_VALUE;
        int minSelY = Integer.MAX_VALUE;
        int maxSelX = Integer.MIN_VALUE;
        int maxSelY = Integer.MIN_VALUE;
        int count = 0;
        for (QuestWidget widget : this.widgets) {
            if (!active.contains(widget.id())) continue;
            minSelX = Math.min(minSelX, widget.x() + widget.scaledOffsetX());
            minSelY = Math.min(minSelY, widget.y() + widget.scaledOffsetY());
            maxSelX = Math.max(maxSelX, widget.x() + widget.scaledOffsetX() + widget.width());
            maxSelY = Math.max(maxSelY, widget.y() + widget.scaledOffsetY() + widget.height());
            count++;
        }
        if (count == 0) return null;
        return new SelectionBounds(centreX + minSelX - 4, centreY + minSelY - 4, centreX + maxSelX + 4, centreY + maxSelY + 4);
    }

    private Set<String> getActiveSelectionIds() {
        Set<String> ids = new HashSet<>();
        if (!this.multiSelected.isEmpty()) {
            ids.addAll(this.multiSelected);
            return ids;
        }
        QuestWidget selected = this.selectHandler.selectedQuest();
        if (selected != null) {
            ids.add(selected.id());
        }
        return ids;
    }

    private boolean isOnResizeHandle(SelectionBounds bounds, double mouseX, double mouseY) {
        int handleLeft = bounds.right() - RESIZE_HANDLE_SIZE;
        int handleTop = bounds.bottom() - RESIZE_HANDLE_SIZE;
        return mouseX >= handleLeft && mouseX <= bounds.right() && mouseY >= handleTop && mouseY <= bounds.bottom();
    }

    private boolean isOnRotateHandle(SelectionBounds bounds, double mouseX, double mouseY) {
        int handleLeft = bounds.right() - ROTATE_HANDLE_SIZE;
        int handleTop = bounds.top();
        return mouseX >= handleLeft && mouseX <= bounds.right() && mouseY >= handleTop && mouseY <= bounds.top() + ROTATE_HANDLE_SIZE;
    }

    private void clampOffsetToBounds() {
        if (DisplayConfig.hasCanvasLimit(this.group)) {
            int canvasWidth = Math.max(1, DisplayConfig.getCanvasLimitWidth(this.group));
            int canvasHeight = Math.max(1, DisplayConfig.getCanvasLimitHeight(this.group));
            int canvasMinX = -(canvasWidth / 2);
            int canvasMinY = -(canvasHeight / 2);
            int canvasMaxX = canvasMinX + canvasWidth;
            int canvasMaxY = canvasMinY + canvasHeight;

            int baseMinX = -(DisplayConfig.MIN_CANVAS_LIMIT_WIDTH / 2);
            int baseMinY = -(DisplayConfig.MIN_CANVAS_LIMIT_HEIGHT / 2);
            int baseMaxX = baseMinX + DisplayConfig.MIN_CANVAS_LIMIT_WIDTH;
            int baseMaxY = baseMinY + DisplayConfig.MIN_CANVAS_LIMIT_HEIGHT;

            int minOffsetX = baseMaxX - canvasMaxX;
            int maxOffsetX = baseMinX - canvasMinX;
            int minOffsetY = baseMaxY - canvasMaxY;
            int maxOffsetY = baseMinY - canvasMinY;

            int clampedX;
            int clampedY;
            if (canvasWidth <= DisplayConfig.MIN_CANVAS_LIMIT_WIDTH) {
                clampedX = 0;
            } else if (minOffsetX > maxOffsetX) {
                clampedX = (minOffsetX + maxOffsetX) / 2;
            } else {
                clampedX = Mth.clamp(offset.x(), minOffsetX, maxOffsetX);
            }
            if (canvasHeight <= DisplayConfig.MIN_CANVAS_LIMIT_HEIGHT) {
                clampedY = 0;
            } else if (minOffsetY > maxOffsetY) {
                clampedY = (minOffsetY + maxOffsetY) / 2;
            } else {
                clampedY = Mth.clamp(offset.y(), minOffsetY, maxOffsetY);
            }
            offset.set(clampedX, clampedY);
            return;
        }

        offset.set(-Mth.clamp(-offset.x(), minX, maxX), -Mth.clamp(-offset.y(), minY, maxY));
    }

    private void reloadCanvasSprites() {
        this.canvasSprites.clear();
        this.canvasSprites.addAll(DisplayConfig.getCanvasSprites(this.group));
    }

    private void persistCanvasSprites() {
        DisplayConfig.setCanvasSprites(this.group, this.canvasSprites);
    }

    private DisplayConfig.CanvasSprite findCanvasSprite(String id) {
        if (id == null || id.isBlank()) return null;
        for (DisplayConfig.CanvasSprite sprite : this.canvasSprites) {
            if (id.equals(sprite.id())) return sprite;
        }
        return null;
    }

    private DisplayConfig.CanvasSprite findTopCanvasSpriteAt(MouseClick local) {
        if (local == null) return null;
        for (int i = this.canvasSprites.size() - 1; i >= 0; i--) {
            DisplayConfig.CanvasSprite sprite = this.canvasSprites.get(i);
            if (local.x() >= sprite.x() && local.x() < sprite.x() + sprite.width()
                && local.y() >= sprite.y() && local.y() < sprite.y() + sprite.height()) {
                return sprite;
            }
        }
        return null;
    }

    private boolean isOnCanvasSpriteResizeHandle(DisplayConfig.CanvasSprite sprite, MouseClick local) {
        if (sprite == null || local == null) return false;
        int handleLeft = sprite.x() + sprite.width() - CANVAS_SPRITE_HANDLE_SIZE;
        int handleTop = sprite.y() + sprite.height() - CANVAS_SPRITE_HANDLE_SIZE;
        return local.x() >= handleLeft && local.x() <= sprite.x() + sprite.width()
            && local.y() >= handleTop && local.y() <= sprite.y() + sprite.height();
    }

    private boolean updateCanvasSprite(String id, java.util.function.UnaryOperator<DisplayConfig.CanvasSprite> update) {
        if (id == null || id.isBlank() || update == null) return false;
        for (int i = 0; i < this.canvasSprites.size(); i++) {
            DisplayConfig.CanvasSprite current = this.canvasSprites.get(i);
            if (!id.equals(current.id())) continue;
            DisplayConfig.CanvasSprite next = update.apply(current);
            if (next == null || !next.isValid()) return false;
            this.canvasSprites.set(i, next);
            return true;
        }
        return false;
    }

    private QuestWidget findQuestUnderMouse(double mouseX, double mouseY) {
        for (QuestWidget widget : this.widgets) {
            if (widget.isMouseOver(
                mouseX - (this.x + (this.fullWidth / 2f) + offset.x()),
                mouseY - (this.y + (this.height / 2f) + offset.y())
            )) {
                return widget;
            }
        }
        return null;
    }

    private Vector2i clampCanvasSpritePosition(int spriteX, int spriteY, int spriteWidth, int spriteHeight) {
        if (!DisplayConfig.hasCanvasLimit(this.group)) {
            return new Vector2i(spriteX, spriteY);
        }
        int canvasWidth = Math.max(1, DisplayConfig.getCanvasLimitWidth(this.group));
        int canvasHeight = Math.max(1, DisplayConfig.getCanvasLimitHeight(this.group));
        int minSpriteX = -(canvasWidth / 2);
        int minSpriteY = -(canvasHeight / 2);
        int maxSpriteX = minSpriteX + canvasWidth - Math.max(1, spriteWidth);
        int maxSpriteY = minSpriteY + canvasHeight - Math.max(1, spriteHeight);

        int x = maxSpriteX < minSpriteX ? (minSpriteX + maxSpriteX) / 2 : Mth.clamp(spriteX, minSpriteX, maxSpriteX);
        int y = maxSpriteY < minSpriteY ? (minSpriteY + maxSpriteY) / 2 : Mth.clamp(spriteY, minSpriteY, maxSpriteY);
        return new Vector2i(x, y);
    }

    public DisplayConfig.CanvasSprite getCanvasSprite(String id) {
        return findCanvasSprite(id);
    }

    public boolean hasCopiedCanvasSprite() {
        return copiedCanvasSprite != null && copiedCanvasSprite.isValid();
    }

    public void copyCanvasSprite(String id) {
        DisplayConfig.CanvasSprite sprite = findCanvasSprite(id);
        if (sprite == null) return;
        copiedCanvasSprite = new DisplayConfig.CanvasSprite(
            sprite.id(),
            sprite.path(),
            sprite.x(),
            sprite.y(),
            sprite.width(),
            sprite.height(),
            sprite.opacity()
        );
    }

    public void duplicateCanvasSprite(String id) {
        DisplayConfig.CanvasSprite sprite = findCanvasSprite(id);
        if (sprite == null) return;
        DisplayConfig.CanvasSprite previous = copiedCanvasSprite;
        copyCanvasSprite(id);
        int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
        int offsetBy = DisplayConfig.isGridLocked(this.group) ? gs : 16;
        pasteCanvasSprite(new MouseClick(
            (int) (this.x + (this.fullWidth / 2f) + offset.x() + sprite.x() + offsetBy),
            (int) (this.y + (this.height / 2f) + offset.y() + sprite.y() + offsetBy),
            1
        ));
        copiedCanvasSprite = previous;
    }

    public void pasteCanvasSprite(MouseClick atScreen) {
        if (copiedCanvasSprite == null || !copiedCanvasSprite.isValid()) return;
        MouseClick local = atScreen == null ? null : getLocal(atScreen);
        int width = Math.max(CANVAS_SPRITE_MIN_SIZE, copiedCanvasSprite.width());
        int height = Math.max(CANVAS_SPRITE_MIN_SIZE, copiedCanvasSprite.height());
        int spriteX;
        int spriteY;
        if (local != null) {
            spriteX = (int) local.x() - width / 2;
            spriteY = (int) local.y() - height / 2;
        } else {
            int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
            int delta = DisplayConfig.isGridLocked(this.group) ? gs : 16;
            spriteX = copiedCanvasSprite.x() + delta;
            spriteY = copiedCanvasSprite.y() + delta;
        }

        if (DisplayConfig.isGridLocked(this.group)) {
            int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
            spriteX = Math.round((float) spriteX / (float) gs) * gs;
            spriteY = Math.round((float) spriteY / (float) gs) * gs;
        }
        Vector2i clamped = clampCanvasSpritePosition(spriteX, spriteY, width, height);
        String id = "sprite_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        DisplayConfig.CanvasSprite placed = new DisplayConfig.CanvasSprite(
            id,
            copiedCanvasSprite.path(),
            clamped.x(),
            clamped.y(),
            width,
            height,
            copiedCanvasSprite.opacity()
        );
        this.canvasSprites.add(placed);
        this.selectedCanvasSpriteId = id;
        persistCanvasSprites();
        this.refreshFromClient();
    }

    public void setCanvasSpriteOpacity(String id, int opacity) {
        if (updateCanvasSprite(id, sprite -> sprite.withOpacity(opacity))) {
            persistCanvasSprites();
        }
    }

    public void removeCanvasSprite(String id) {
        if (id == null || id.isBlank()) return;
        boolean removed = this.canvasSprites.removeIf(sprite -> id.equals(sprite.id()));
        if (removed) {
            if (Objects.equals(this.selectedCanvasSpriteId, id)) {
                this.selectedCanvasSpriteId = null;
            }
            persistCanvasSprites();
            this.refreshFromClient();
        }
    }

    public void addCanvasSprite(String managedPath, MouseClick atScreen) {
        if (managedPath == null || managedPath.isBlank() || atScreen == null) return;
        MouseClick local = getLocal(atScreen);
        Vector2i imageSize = CustomImageManager.getImageSize(managedPath);
        int width = imageSize == null ? 128 : Mth.clamp(imageSize.x(), 16, 1024);
        int height = imageSize == null ? 128 : Mth.clamp(imageSize.y(), 16, 1024);
        int spriteX = (int) local.x() - width / 2;
        int spriteY = (int) local.y() - height / 2;
        if (DisplayConfig.isGridLocked(this.group)) {
            int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
            spriteX = Math.round((float) spriteX / (float) gs) * gs;
            spriteY = Math.round((float) spriteY / (float) gs) * gs;
        }
        Vector2i clamped = clampCanvasSpritePosition(spriteX, spriteY, width, height);
        String id = "sprite_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        DisplayConfig.CanvasSprite sprite = new DisplayConfig.CanvasSprite(id, managedPath, clamped.x(), clamped.y(), width, height, 100);
        this.canvasSprites.add(sprite);
        this.selectedCanvasSpriteId = id;
        persistCanvasSprites();
        this.refreshFromClient();
    }

    private void applyEditorBounds() {
        if (DisplayConfig.hasCanvasLimit(this.group)) {
            int canvasWidth = Math.max(1, DisplayConfig.getCanvasLimitWidth(this.group));
            int canvasHeight = Math.max(1, DisplayConfig.getCanvasLimitHeight(this.group));
            this.minX = -(canvasWidth / 2);
            this.minY = -(canvasHeight / 2);
            this.maxX = this.minX + canvasWidth;
            this.maxY = this.minY + canvasHeight;
        } else {
            this.minX = MIN.x();
            this.minY = MIN.y();
            this.maxX = MAX.x();
            this.maxY = MAX.y();
        }
    }

    private Vector2i clampQuestPositionToCanvas(TexturePlacements.Info info, float scale, int posX, int posY) {
        if (!DisplayConfig.hasCanvasLimit(this.group)) {
            return new Vector2i(posX, posY);
        }
        int canvasWidth = Math.max(1, DisplayConfig.getCanvasLimitWidth(this.group));
        int canvasHeight = Math.max(1, DisplayConfig.getCanvasLimitHeight(this.group));
        int canvasMinX = -(canvasWidth / 2);
        int canvasMinY = -(canvasHeight / 2);
        int canvasMaxX = canvasMinX + canvasWidth;
        int canvasMaxY = canvasMinY + canvasHeight;

        int offX = Math.round(info.xOffset() * scale);
        int offY = Math.round(info.yOffset() * scale);
        int width = Math.max(1, Math.round(info.width() * scale));
        int height = Math.max(1, Math.round(info.height() * scale));

        int minPosX = canvasMinX - offX;
        int minPosY = canvasMinY - offY;
        int maxPosX = canvasMaxX - offX - width;
        int maxPosY = canvasMaxY - offY - height;

        int clampedX = maxPosX < minPosX ? (minPosX + maxPosX) / 2 : Mth.clamp(posX, minPosX, maxPosX);
        int clampedY = maxPosY < minPosY ? (minPosY + maxPosY) / 2 : Mth.clamp(posY, minPosY, maxPosY);
        return new Vector2i(clampedX, clampedY);
    }

    private float nodeScale(ClientQuests.QuestEntry entry) {
        GroupDisplay display = entry.value().display().groups().getOrDefault(this.group, GroupDisplay.create(this.group));
        float value = display.nodeScale();
        if (Float.isNaN(value) || Float.isInfinite(value)) return 1.0f;
        return Math.max(0.5f, value);
    }

    private TexturePlacements.Info textureInfo(ClientQuests.QuestEntry entry) {
        return TexturePlacements.getOrDefault(entry.value().display().iconBackground(), TexturePlacements.NO_OFFSET_24X);
    }

    private int questWidth(ClientQuests.QuestEntry entry) {
        TexturePlacements.Info info = textureInfo(entry);
        return Math.max(1, Math.round(info.width() * nodeScale(entry)));
    }

    private int questHeight(ClientQuests.QuestEntry entry) {
        TexturePlacements.Info info = textureInfo(entry);
        return Math.max(1, Math.round(info.height() * nodeScale(entry)));
    }

    private int questOffsetX(ClientQuests.QuestEntry entry) {
        float scale = nodeScale(entry);
        return Math.round(textureInfo(entry).xOffset() * scale);
    }

    private int questOffsetY(ClientQuests.QuestEntry entry) {
        float scale = nodeScale(entry);
        return Math.round(textureInfo(entry).yOffset() * scale);
    }

    private int visualInsetX(TexturePlacements.Info info, float scale) {
        if (info.xOffset() == 0 && info.yOffset() == 0 && info.width() == 24 && info.height() == 24) {
            return Math.max(0, Math.round(scale) - 1);
        }
        return 0;
    }

    private int visualInsetY(TexturePlacements.Info info, float scale) {
        if (info.xOffset() == 0 && info.yOffset() == 0 && info.width() == 24 && info.height() == 24) {
            return Math.max(0, Math.round(scale) - 1);
        }
        return 0;
    }

    private float snapScaleForGrid(TexturePlacements.Info info, float originalScale, float factor, int gridSize, float fallbackScale) {
        if (isDefaultQuestFrame(info)) {
            float baseVisible = Math.max(1.0f, info.width() - 2.0f);
            float targetVisible = Math.max(1.0f, baseVisible * originalScale * factor);
            int cells = Math.max(1, Math.round((targetVisible + 1.0f) / Math.max(1.0f, gridSize)));
            float snapped = ((cells * gridSize) - 1.0f) / baseVisible;
            return Math.max(0.5f, snapped);
        }

        float step = Math.max(0.01f, (Math.max(2, gridSize) - 1) / 22.0f);
        float snapped = Math.max(step, Math.round(fallbackScale / step) * step);
        return Math.max(0.5f, snapped);
    }

    private boolean isDefaultQuestFrame(TexturePlacements.Info info) {
        return info.xOffset() == 0 && info.yOffset() == 0 && info.width() == 24 && info.height() == 24;
    }

    private void renderCanvasSprites(GuiGraphics graphics, int centreX, int centreY) {
        if (this.canvasSprites.isEmpty()) return;
        for (DisplayConfig.CanvasSprite sprite : this.canvasSprites) {
            ResourceLocation texture = CustomImageManager.getTexture(sprite.path());
            if (texture == null) continue;
            int width = Math.max(1, sprite.width());
            int height = Math.max(1, sprite.height());
            float alpha = Mth.clamp(sprite.opacity() / 100.0f, 0.0f, 1.0f);
            if (alpha <= 0.0f) continue;
            int left = centreX + offset.x() + sprite.x();
            int top = centreY + offset.y() + sprite.y();
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            graphics.blit(texture, left, top, 0, 0, width, height, width, height);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();

            if (this.isEditing() && Objects.equals(this.selectedCanvasSpriteId, sprite.id())) {
                graphics.renderOutline(left - 1, top - 1, width + 2, height + 2, 0xFFA8EFF0);
                int handleLeft = left + width - CANVAS_SPRITE_HANDLE_SIZE;
                int handleTop = top + height - CANVAS_SPRITE_HANDLE_SIZE;
                graphics.fill(handleLeft, handleTop, left + width, top + height, 0xCCFFFFFF);
                graphics.renderOutline(handleLeft, handleTop, CANVAS_SPRITE_HANDLE_SIZE, CANVAS_SPRITE_HANDLE_SIZE, 0xFF3A3A3A);
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

    private void renderCanvasBackground(GuiGraphics graphics, int centreX, int centreY) {
        String configuredPath = DisplayConfig.getCanvasBackground(this.group);
        String backgroundPath = configuredPath;
        if (backgroundPath == null || backgroundPath.isBlank()) return;
        ResourceLocation texture = CustomImageManager.getTexture(backgroundPath);
        if (texture == null) return;
        int opacityPercent = DisplayConfig.getBackgroundOpacity(this.group);
        if (opacityPercent <= 0) return;
        float alpha = Mth.clamp(opacityPercent / 100.0f, 0.0f, 1.0f);
        int left = centreX - this.fullWidth / 2;
        int top = centreY - this.height / 2;
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        graphics.blit(texture, left, top, 0, 0, this.width, this.height, this.width, this.height);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private boolean canEdit() {
        return this.content != null && this.content.canEdit();
    }

    public void startLink(QuestWidget source) {
        if (source == null) return;
        this.linkingActive = true;
        this.linkingSourceId = source.id();
        this.selectHandler.startPendingLink(source);
    }

    public void cancelLink() {
        this.linkingActive = false;
        this.linkingSourceId = null;
        this.selectHandler.release();
    }

    public boolean completeLink(QuestWidget target, boolean remove) {
        if (!this.linkingActive || this.linkingSourceId == null || target == null) {
            this.cancelLink();
            return false;
        }
        String srcId = this.linkingSourceId;
        String tgtId = target.id();
        if (Objects.equals(srcId, tgtId)) {
            this.cancelLink();
            return false;
        }
        ClientQuests.get(tgtId).ifPresent(dependentEntry -> {
            Set<String> newDeps = new HashSet<>(dependentEntry.value().dependencies());
            boolean changed = remove ? newDeps.remove(srcId) : newDeps.add(srcId);
            if (changed) {
                if (remove) {
                    dependentEntry.dependencies().removeIf(dep -> dep.key().equals(srcId));
                    ClientQuests.get(srcId).ifPresent(srcEntry -> srcEntry.dependents().removeIf(child -> child.key().equals(tgtId)));
                } else {
                    ClientQuests.get(srcId).ifPresent(srcEntry -> {
                        if (dependentEntry.dependencies().stream().noneMatch(d -> d.key().equals(srcId))) {
                            dependentEntry.dependencies().add(srcEntry);
                        }
                        if (srcEntry.dependents().stream().noneMatch(d -> d.key().equals(tgtId))) {
                            srcEntry.dependents().add(dependentEntry);
                        }
                    });
                }
                ClientQuests.updateQuest(dependentEntry, q -> NetworkQuestData.builder().dependencies(newDeps));
            }
        });
        this.cancelLink();
        return true;
    }

    public Set<String> getMultiSelectedIds() {
        return Collections.unmodifiableSet(this.multiSelected);
    }

    public List<ClientQuests.QuestEntry> getMultiSelectedEntries() {
        List<ClientQuests.QuestEntry> list = new ArrayList<>();
        for (String id : this.multiSelected) {
            ClientQuests.get(id).ifPresent(list::add);
        }
        return list;
    }

    public void setMultiSelectedIds(Collection<String> ids) {
        this.multiSelected.clear();
        if (ids != null) this.multiSelected.addAll(ids);
        if (this.multiSelected.isEmpty()) {
            this.refreshFromClient();
        }
    }

    public void setSearchFilter(String filter) {
        this.searchFilter = filter == null ? "" : filter.trim();
        this.refreshFromClient();
    }

    public void focusQuest(String questId) {
        if (questId == null || questId.isBlank()) return;
        ClientQuests.get(questId).ifPresent(entry -> {
            if (!entry.value().display().groups().containsKey(this.group)) return;
            Vector2i pos = entry.value().display().position(this.group);
            int cx = pos.x() + questOffsetX(entry) + (questWidth(entry) / 2);
            int cy = pos.y() + questOffsetY(entry) + (questHeight(entry) / 2);
            offset.set(-cx, -cy);
            clampOffsetToBounds();
        });
    }

    public void refreshFromClient() {
        List<Pair<ClientQuests.QuestEntry, ModUtils.QuestStatus>> quests = new ArrayList<>();
        if (this.content != null) {
            this.content.quests().forEach((id, status) -> ClientQuests.get(id).filter(q -> q.value().display().groups().containsKey(this.group)).ifPresent(q -> quests.add(Pair.of(q, status))));
        } else {
            for (ClientQuests.QuestEntry entry : ClientQuests.byGroup(this.group)) {
                quests.add(Pair.of(entry, ClientQuests.getStatus(entry.key()).orElse(ModUtils.QuestStatus.IN_PROGRESS)));
            }
        }
        this.update(this.content, quests);
    }

    public static void refreshOpenScreens() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        Screen screen = mc.screen;
        if (screen instanceof AbstractQuestScreen<?> qs) {
            for (var child : qs.actualChildren()) {
                if (child instanceof QuestsWidget qw) qw.refreshFromClient();
            }
        }
    }

    private record MiniMapBox(int mapX, int mapY, int mapW, int mapH, int toggleX, int toggleY) {
        boolean mapContains(double mx, double my) {
            return mx >= this.mapX && mx < this.mapX + this.mapW && my >= this.mapY && my < this.mapY + this.mapH;
        }

        boolean toggleContains(double mx, double my) {
            return mx >= this.toggleX && mx < this.toggleX + 11 && my >= this.toggleY && my < this.toggleY + 11;
        }
    }

    private record SelectionBounds(int left, int top, int right, int bottom) {
        boolean contains(double mx, double my) {
            return mx >= this.left && mx < this.right && my >= this.top && my < this.bottom;
        }
    }

    private record GhostPreviewItem(
        String id,
        QuestDisplay display,
        TexturePlacements.Info info,
        float scale,
        Set<String> dependencies,
        int left,
        int top,
        int width,
        int height
    ) {}

    private record Connection(String source, String target, int sx, int sy, int tx, int ty, boolean hidden) {}
}
