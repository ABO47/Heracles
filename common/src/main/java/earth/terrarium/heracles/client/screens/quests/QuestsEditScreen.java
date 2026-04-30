package earth.terrarium.heracles.client.screens.quests;

import earth.terrarium.heracles.api.quests.GroupDisplay;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.api.quests.QuestDisplay;
import earth.terrarium.heracles.api.quests.QuestSettings;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.client.HeraclesClient;
import earth.terrarium.heracles.client.handlers.ClientQuestNetworking;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.handlers.DisplayConfig;
import earth.terrarium.heracles.client.screens.mousemode.MouseMode;
import earth.terrarium.heracles.client.utils.MouseClick;
import earth.terrarium.heracles.client.utils.QuestIdPolicy;
import earth.terrarium.heracles.client.widgets.SelectableImageButton;
import earth.terrarium.heracles.client.widgets.modals.AddDependencyModal;
import earth.terrarium.heracles.client.widgets.modals.ItemModal;
import earth.terrarium.heracles.client.widgets.modals.TextInputModal;
import earth.terrarium.heracles.client.widgets.modals.BlueprintsLibraryModal;
import earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox;
import net.minecraft.client.gui.components.Button;
import earth.terrarium.heracles.client.utils.TexturePlacements;
import earth.terrarium.heracles.client.widgets.modals.icon.background.IconBackgroundModal;
import earth.terrarium.heracles.client.widgets.modals.upload.UploadModal;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.client.widgets.buttons.ThemedButton;
import earth.terrarium.heracles.common.menus.quests.QuestsContent;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.groups.CreateGroupPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import org.joml.Vector2i;
import earth.terrarium.heracles.client.handlers.UndoRedoManager;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

public class QuestsEditScreen extends QuestsScreen {

    private static final Pattern NON_ASCII = Pattern.compile("[^\\x00-\\x7F]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9_]");
    private static final Pattern RESERVED_WINDOWS_FILENAMES = Pattern.compile(".*\\.|(?:CLOCK\\$|CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])(?:\\..*)?", Pattern.CASE_INSENSITIVE);
    private static final net.minecraft.resources.ResourceLocation BLUEPRINT_BUTTON_TEXTURE = new net.minecraft.resources.ResourceLocation(Heracles.MOD_ID, "textures/gui/blueprints_button.png");
    private static final net.minecraft.resources.ResourceLocation GRID_TOGGLE_CUSTOM_TEXTURE = new net.minecraft.resources.ResourceLocation(Heracles.MOD_ID, "textures/gui/grid_toggle_button.png");
    private static final net.minecraft.resources.ResourceLocation GRID_LOCK_CUSTOM_TEXTURE = new net.minecraft.resources.ResourceLocation(Heracles.MOD_ID, "textures/gui/grid_lock_button.png");
    private static final net.minecraft.resources.ResourceLocation CANVAS_LIMIT_CUSTOM_TEXTURE = new net.minecraft.resources.ResourceLocation(Heracles.MOD_ID, "textures/gui/canvas_size_limit.png");
    private static final int INSPECTOR_TOGGLE_SIZE = 11;

    private MouseMode activeMouseMode = MouseMode.SELECT_MOVE;
    private SelectableImageButton gridToggle;
    private SelectableImageButton gridLock;
    private SelectableImageButton canvasLimitToggle;
    private Button gridPlusButton;
    private Button gridMinusButton;
    private EnterableEditBox gridSizeField;
    private earth.terrarium.heracles.client.widgets.GridOpacitySlider gridOpacitySlider;
    private EnterableEditBox gridOpacityField;
    private earth.terrarium.heracles.client.widgets.GridOpacitySlider backgroundOpacitySlider;
    private EnterableEditBox backgroundOpacityField;
    private EnterableEditBox canvasLimitWidthField;
    private EnterableEditBox canvasLimitHeightField;
    private int canvasLimitSeparatorX = -1;
    private boolean updatingCanvasLimitFields = false;
    private boolean inspectorCollapsed = false;

    private UploadModal uploadModal;
    private earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox searchBox;
    private earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox groupFilter;
    private TextInputModal<Unit> groupModal;
    private BlueprintsLibraryModal blueprintsLibraryModal;
    private IconBackgroundModal iconBackgroundModal;
    private ItemModal itemModal;
    private AddDependencyModal dependencyModal;
    private TextInputModal<MouseClick> questModal;

    public QuestsEditScreen(QuestsContent content) {
        super(content);
    }

    public void setMouseMode(MouseMode mode) {
        this.activeMouseMode = mode == null ? MouseMode.SELECT_MOVE : mode;
    }

    @Override
    protected void init() {
        super.init();
        this.inspectorCollapsed = DisplayConfig.isInspectorCollapsed(this.content.group());
        addRenderableWidget(new ImageButton(sideBarWidth - 12, 1, 11, 11, 22, 15, 11, HEADING, 256, 256, (button) -> {
            if (this.groupModal != null) {
                this.groupModal.setVisible(true);
            }
        })).setTooltip(Tooltip.create(ConstantComponents.Groups.CREATE));

        this.groupFilter = addRenderableWidget(new EnterableEditBox(Minecraft.getInstance().font, 2, 1, Math.max(40, sideBarWidth - 20), 11, Component.nullToEmpty("")));
        this.groupFilter.setMaxLength(64);
        this.groupFilter.setResponder(s -> {
            String filter = normalize(s);
            java.util.List<String> groups = new java.util.ArrayList<>();
            for (String g : ClientQuests.groups()) {
                if (matchesQuery(g, filter)) groups.add(g);
            }
            if (this.getGroupsList() != null) this.getGroupsList().update(groups, this.content.group());
        });
        this.groupFilter.setEnter(this::updateGroupFilter);

        selectQuestWidget = new SelectQuestWidget(
            (int) (this.width * 0.75f) + 2,
            15,
            sideBarWidth,
            this.height - 15,
            this.questsWidget
        );

        int pos = sideBarWidth + 3;
        int spacing = 3;
        int small = 11;
        boolean hasGridToggleTexture = Minecraft.getInstance().getResourceManager().getResource(GRID_TOGGLE_CUSTOM_TEXTURE).isPresent();
        boolean hasGridLockTexture = Minecraft.getInstance().getResourceManager().getResource(GRID_LOCK_CUSTOM_TEXTURE).isPresent();
        boolean hasCanvasLimitTexture = Minecraft.getInstance().getResourceManager().getResource(CANVAS_LIMIT_CUSTOM_TEXTURE).isPresent();

        pos += 4;

        this.gridToggle = addRenderableWidget(new SelectableImageButton(
            pos, 1, 11, 11,
            0, hasGridToggleTexture ? 0 : 15, 11,
            hasGridToggleTexture ? GRID_TOGGLE_CUSTOM_TEXTURE : HEADING,
            hasGridToggleTexture ? 11 : 256,
            hasGridToggleTexture ? 22 : 256,
            (button) -> {
            String group = this.content.group();
            DisplayConfig.setGridEnabled(group, !DisplayConfig.isGridEnabled(group));
            this.gridToggle.setSelected(DisplayConfig.isGridEnabled(group));
            refreshGridControlVisibility();
        }));
        this.gridToggle.setTooltip(Tooltip.create(ConstantComponents.Tools.GRID));
        this.gridToggle.setSelected(DisplayConfig.isGridEnabled(this.content.group()));
        pos += small + spacing;

        this.gridLock = addRenderableWidget(new SelectableImageButton(
            pos, 1, 11, 11,
            hasGridLockTexture ? 0 : 11, hasGridLockTexture ? 0 : 15, 11,
            hasGridLockTexture ? GRID_LOCK_CUSTOM_TEXTURE : HEADING,
            hasGridLockTexture ? 11 : 256,
            hasGridLockTexture ? 22 : 256,
            (button) -> {
            String group = this.content.group();
            DisplayConfig.setGridLocked(group, !DisplayConfig.isGridLocked(group));
            this.gridLock.setSelected(DisplayConfig.isGridLocked(group));
            refreshGridControlVisibility();
        }));
        this.gridLock.setTooltip(Tooltip.create(ConstantComponents.Tools.GRID_LOCK));
        this.gridLock.setSelected(DisplayConfig.isGridLocked(this.content.group()));
        pos += small + spacing;

        this.gridMinusButton = addRenderableWidget(ThemedButton.builder(ConstantComponents.MINUS, b -> {
            String group = this.content.group();
            int newSize = Math.max(1, DisplayConfig.getGridSize(group) - 1);
            DisplayConfig.setGridSize(group, newSize);
            if (this.gridSizeField != null) this.gridSizeField.setValue(String.valueOf(newSize));
        }).bounds(pos, 1, 12, 11).build());
        pos += 12 + spacing;

        this.gridSizeField = addRenderableWidget(new EnterableEditBox(Minecraft.getInstance().font, pos, 1, 24, 11, Component.nullToEmpty("")));
        pos += 24 + spacing;
        this.gridSizeField.setMaxLength(4);
        this.gridSizeField.setValue(String.valueOf(DisplayConfig.getGridSize(this.content.group())));
        this.gridSizeField.setResponder(s -> {
            try {
                String group = this.content.group();
                if (s == null || s.isBlank()) return;
                int v = Integer.parseInt(s.trim());
                DisplayConfig.setGridSize(group, Math.max(1, v));
            } catch (NumberFormatException ignored) {
            }
        });

        this.gridPlusButton = addRenderableWidget(ThemedButton.builder(ConstantComponents.PLUS, b -> {
            String group = this.content.group();
            int newSize = Math.min(256, DisplayConfig.getGridSize(group) + 1);
            DisplayConfig.setGridSize(group, newSize);
            if (this.gridSizeField != null) this.gridSizeField.setValue(String.valueOf(newSize));
        }).bounds(pos, 1, 12, 11).build());
        pos += 12 + spacing;

        this.gridOpacitySlider = addRenderableWidget(new earth.terrarium.heracles.client.widgets.GridOpacitySlider(pos, 1, 30, 11, DisplayConfig.getGridOpacity(this.content.group()), v -> {
            DisplayConfig.setGridOpacity(this.content.group(), v);
            if (this.gridOpacityField != null) this.gridOpacityField.setValue(String.valueOf(Math.max(1, v)));
        }));
        this.gridOpacitySlider.setTooltip(Tooltip.create(Component.literal("Grid Opacity")));
        pos += 30 + 2;

        this.gridOpacityField = addRenderableWidget(new EnterableEditBox(Minecraft.getInstance().font, pos, 1, 26, 11, Component.nullToEmpty("")));
        this.gridOpacityField.setMaxLength(3);
        this.gridOpacityField.setTooltip(Tooltip.create(Component.literal("Grid Opacity Percent (1-100)")));
        this.gridOpacityField.setValue(String.valueOf(Math.max(1, DisplayConfig.getGridOpacity(this.content.group()))));
        this.gridOpacityField.setResponder(s -> applyGridOpacityField(false));
        this.gridOpacityField.setEnter(s -> applyGridOpacityField(true));
        pos += 26 + spacing;

        this.backgroundOpacitySlider = addRenderableWidget(new earth.terrarium.heracles.client.widgets.GridOpacitySlider(pos, 1, 30, 11, DisplayConfig.getBackgroundOpacity(this.content.group()), v -> {
            DisplayConfig.setBackgroundOpacity(this.content.group(), v);
            if (this.backgroundOpacityField != null) this.backgroundOpacityField.setValue(String.valueOf(Math.max(1, v)));
        }));
        this.backgroundOpacitySlider.setTooltip(Tooltip.create(Component.literal("Background Opacity")));
        pos += 30 + 2;

        this.backgroundOpacityField = addRenderableWidget(new EnterableEditBox(Minecraft.getInstance().font, pos, 1, 26, 11, Component.nullToEmpty("")));
        this.backgroundOpacityField.setMaxLength(3);
        this.backgroundOpacityField.setTooltip(Tooltip.create(Component.literal("Background Opacity Percent (1-100)")));
        this.backgroundOpacityField.setValue(String.valueOf(Math.max(1, DisplayConfig.getBackgroundOpacity(this.content.group()))));
        this.backgroundOpacityField.setResponder(s -> applyBackgroundOpacityField(false));
        this.backgroundOpacityField.setEnter(s -> applyBackgroundOpacityField(true));
        pos += 26 + spacing;

        this.canvasLimitToggle = addRenderableWidget(new SelectableImageButton(
            pos, 1, 11, 11,
            0, hasCanvasLimitTexture ? 0 : 15, 11,
            hasCanvasLimitTexture ? CANVAS_LIMIT_CUSTOM_TEXTURE : HEADING,
            hasCanvasLimitTexture ? 11 : 256,
            hasCanvasLimitTexture ? 22 : 256,
            (button) -> toggleCanvasLimitEnabled()
        ));
        this.canvasLimitToggle.setTooltip(Tooltip.create(Component.translatable("gui.heracles.tools.canvas_limit")));
        this.canvasLimitToggle.setSelected(DisplayConfig.hasCanvasLimit(this.content.group()));
        pos += small + spacing;

        int canvasLimitWidth = DisplayConfig.getCanvasLimitWidth(this.content.group());
        int canvasLimitHeight = DisplayConfig.getCanvasLimitHeight(this.content.group());

        this.canvasLimitWidthField = addRenderableWidget(new EnterableEditBox(Minecraft.getInstance().font, pos, 1, 36, 11, Component.nullToEmpty("")));
        this.canvasLimitWidthField.setMaxLength(5);
        this.canvasLimitWidthField.setTooltip(Tooltip.create(Component.literal("Canvas Width Limit")));
        this.canvasLimitWidthField.setValue(canvasLimitWidth > 0 ? String.valueOf(canvasLimitWidth) : "");
        this.canvasLimitWidthField.setResponder(s -> applyCanvasLimitFromFields(false));
        this.canvasLimitWidthField.setEnter(s -> applyCanvasLimitFromFields(true));
        pos += 36 + 2;

        this.canvasLimitSeparatorX = pos;
        pos += 6;

        this.canvasLimitHeightField = addRenderableWidget(new EnterableEditBox(Minecraft.getInstance().font, pos, 1, 36, 11, Component.nullToEmpty("")));
        this.canvasLimitHeightField.setMaxLength(5);
        this.canvasLimitHeightField.setTooltip(Tooltip.create(Component.literal("Canvas Height Limit")));
        this.canvasLimitHeightField.setValue(canvasLimitHeight > 0 ? String.valueOf(canvasLimitHeight) : "");
        this.canvasLimitHeightField.setResponder(s -> applyCanvasLimitFromFields(false));
        this.canvasLimitHeightField.setEnter(s -> applyCanvasLimitFromFields(true));
        pos += 36 + spacing;

        refreshGridControlVisibility();

        int importX = this.width - 36;
        int blueprintBtnX = importX - 14;
        int searchEnd = blueprintBtnX - spacing;
        int searchW = Math.max(40, searchEnd - pos);

        this.searchBox = addRenderableWidget(new earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox(
            Minecraft.getInstance().font, pos, 1, searchW, 11, Component.nullToEmpty("")
        ));
        this.searchBox.setMaxLength(64);
        this.searchBox.setResponder(this::applyQuestSearch);
        this.searchBox.setEnter(this::applyQuestSearchAndJump);
        if (!earth.terrarium.heracles.client.HeraclesClient.lastQuestSearch.isBlank()) {
            this.searchBox.setValue(earth.terrarium.heracles.client.HeraclesClient.lastQuestSearch);
            this.applyQuestSearch(earth.terrarium.heracles.client.HeraclesClient.lastQuestSearch);
        }
        if (earth.terrarium.heracles.client.HeraclesClient.focusQuestSearchOnOpen) {
            this.setFocused(this.searchBox);
            this.searchBox.setFocused(true);
            this.searchBox.moveCursorToEnd();
            earth.terrarium.heracles.client.HeraclesClient.focusQuestSearchOnOpen = false;
        }

        addRenderableWidget(new ImageButton(blueprintBtnX, 1, 11, 11, 0, 0, 11, BLUEPRINT_BUTTON_TEXTURE, 11, 22, (button) -> {
            if (this.blueprintsLibraryModal == null) {
                this.blueprintsLibraryModal = addTemporary(new BlueprintsLibraryModal(this.width, this.height));
            }
            this.blueprintsLibraryModal.setVisible(true);
        })).setTooltip(Tooltip.create(Component.translatable("blueprints.library")));

        addRenderableWidget(new ImageButton(importX, 1, 11, 11, 33, 37, 11, HEADING, 256, 256, (button) -> {
            if (this.uploadModal != null) {
                this.uploadModal.setVisible(true);
            }
        })).setTooltip(Tooltip.create(ConstantComponents.Quests.IMPORT));

        this.uploadModal = addTemporary(new UploadModal(this.width, this.height));
        this.blueprintsLibraryModal = addTemporary(new BlueprintsLibraryModal(this.width, this.height));
        this.groupModal = addTemporary(new TextInputModal<>(this.width, this.height, ConstantComponents.Groups.CREATE, (ignored, text) -> {
            NetworkHandler.CHANNEL.sendToServer(new CreateGroupPacket(text));
            ClientQuests.groups().add(text);
            if (Minecraft.getInstance().screen instanceof QuestsScreen screen) {
                screen.getGroupsList().addGroup(text);
            }
        }, text -> !ClientQuests.groups().contains(text.trim())));
        this.iconBackgroundModal = addTemporary(new IconBackgroundModal(this.width, this.height));
        this.itemModal = addTemporary(new ItemModal(this.width, this.height));
        this.dependencyModal = addTemporary(new AddDependencyModal(this.width, this.height));
        this.questModal = addTemporary(new TextInputModal<>(this.width, this.height, ConstantComponents.Quests.CREATE, (position, text) -> {
            MouseClick local = this.questsWidget.getLocal(position);
            int half = TexturePlacements.getOrDefault(QuestDisplay.createDefault(GroupDisplay.createDefault()).iconBackground(), TexturePlacements.NO_OFFSET_24X).width() / 2;
            GroupDisplay template = groupTemplate(this.content.group());
            QuestDisplay display = QuestDisplay.createDefault(new GroupDisplay(
                this.content.group(),
                new Vector2i((int) local.x() - half, (int) local.y() - half),
                template.nodeScale(),
                "",
                ""
            ));
            display.setTitle(Component.literal(text));
            Quest quest = new Quest(
                display,
                QuestSettings.createDefault(),
                new HashSet<>(),
                new HashMap<>(),
                new HashMap<>()
            );
            String id = QuestIdPolicy.nextAvailable(this.content.group(), text);
            this.questsWidget.addQuest(ClientQuestNetworking.add(id, quest));
        }, text -> {
            return !QuestIdPolicy.sanitizeSegment(text).isBlank();
        }));
    }

    private static String toSafeFilename(String text) {
        text = text.trim();
        text = text.replace(" ", "_");
        text = NON_ASCII.matcher(text).replaceAll(result -> Base64.getEncoder().encodeToString(result.group().getBytes(StandardCharsets.UTF_8)));
        text = NON_ALPHANUMERIC.matcher(text).replaceAll("");
        text = text.toLowerCase(Locale.ROOT);
        if (RESERVED_WINDOWS_FILENAMES.matcher(text).matches()) {
            return "";
        }
        return text;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.activeMouseMode == MouseMode.DRAG_MOVE) {
            setCursor(Cursor.RESIZE_ALL);
        }
        if (this.activeMouseMode == MouseMode.ADD) {
            setCursor(Cursor.CROSSHAIR);
        }
        super.renderLabels(graphics, mouseX, mouseY);
        if (this.canvasLimitSeparatorX > 0) {
            graphics.drawString(this.font, "x", this.canvasLimitSeparatorX, 3, 0xFFFFFFFF, false);
        }
        if (hasInspectorToggle()) {
            int toggleX = getInspectorToggleX();
            int toggleY = getInspectorToggleY();
            graphics.fill(toggleX, toggleY, toggleX + INSPECTOR_TOGGLE_SIZE, toggleY + INSPECTOR_TOGGLE_SIZE, 0xCC202020);
            graphics.drawString(this.font, this.inspectorCollapsed ? "+" : "-", toggleX + 3, toggleY + 2, 0xFFFFFFFF, false);
        }
    }

    @Override
    protected MouseMode getMouseMode() {
        return this.activeMouseMode;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hasInspectorToggle()) {
            int toggleX = getInspectorToggleX();
            int toggleY = getInspectorToggleY();
            if (mouseX >= toggleX && mouseX <= toggleX + INSPECTOR_TOGGLE_SIZE && mouseY >= toggleY && mouseY <= toggleY + INSPECTOR_TOGGLE_SIZE) {
                if (this.questsWidget != null) {
                    this.questsWidget.cancelActiveMouseInteraction();
                }
                this.inspectorCollapsed = !this.inspectorCollapsed;
                DisplayConfig.setInspectorCollapsed(this.content.group(), this.inspectorCollapsed);
                if (this.inspectorCollapsed) {
                    clearWidget();
                } else if (this.questsWidget != null && this.questsWidget.selectHandler().selectedQuest() != null) {
                    showSelectQuestWidget(this.questsWidget.selectHandler().selectedQuest().entry());
                }
                return true;
            }
        }
        if (!isTemporaryWidgetVisible() && getMouseMode() == MouseMode.ADD && this.questsWidget.isMouseOver(mouseX, mouseY) && button == 0) {
            this.questModal.setVisible(true);
            this.questModal.setData(new MouseClick(mouseX, mouseY, button));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (selectQuestWidget.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (isTemporaryWidgetVisible()) return false;

        if (net.minecraft.client.gui.screens.Screen.hasControlDown()) {
            if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_Z) {
                if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                    UndoRedoManager.getInstance().redo();
                } else {
                    UndoRedoManager.getInstance().undo();
                }
                return true;
            }
            if (keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_Y) {
                UndoRedoManager.getInstance().redo();
                return true;
            }
        }
        if (HeraclesClient.EDITOR_SELECT_MOVE.matches(keyCode, scanCode)) {
            setMouseMode(MouseMode.SELECT_MOVE);
            return true;
        }
        if (HeraclesClient.EDITOR_DRAG.matches(keyCode, scanCode)) {
            setMouseMode(MouseMode.DRAG_MOVE);
            return true;
        }
        if (HeraclesClient.EDITOR_ADD.matches(keyCode, scanCode)) {
            setMouseMode(MouseMode.ADD);
            return true;
        }
        if (HeraclesClient.EDITOR_LINK.matches(keyCode, scanCode)) {
            setMouseMode(MouseMode.SELECT_LINK);
            return true;
        }
        if (HeraclesClient.EDITOR_GRID_TOGGLE.matches(keyCode, scanCode)) {
            this.gridToggle.onPress();
            return true;
        }
        if (HeraclesClient.EDITOR_GRID_LOCK.matches(keyCode, scanCode)) {
            if (this.gridLock != null) {
                this.gridLock.onPress();
            }
            return true;
        }
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        uploadModal.setVisible(false);
    }

    private void clearWidget() {
        if (selectQuestWidget != null) {
            selectQuestWidget.setEntry(null);
            removeWidget(selectQuestWidget);
        }
    }

    @Override
    protected void showSelectQuestWidget(ClientQuests.QuestEntry quest) {
        if (quest == null) {
            clearWidget();
            return;
        }
        if (this.selectQuestWidget == null) return;
        this.selectQuestWidget.setEntry(quest);
        if (this.inspectorCollapsed) {
            removeWidget(this.selectQuestWidget);
            return;
        }
        if (!actualChildren().contains(this.selectQuestWidget)) {
            addRenderableWidget(this.selectQuestWidget);
        }
    }

    private boolean hasInspectorToggle() {
        if (this.questsWidget == null) return false;
        if (this.selectQuestWidget != null && this.selectQuestWidget.entry() != null) return true;
        return this.questsWidget.selectHandler().selectedQuest() != null;
    }

    private int getInspectorToggleX() {
        if (this.inspectorCollapsed) {
            return this.width - INSPECTOR_TOGGLE_SIZE - 2;
        }
        return (int) (this.width * 0.75f) - INSPECTOR_TOGGLE_SIZE;
    }

    private int getInspectorToggleY() {
        return 16;
    }

    public IconBackgroundModal iconBackgroundModal() {
        return this.iconBackgroundModal;
    }

    public ItemModal itemModal() {
        return this.itemModal;
    }

    public AddDependencyModal dependencyModal() {
        return this.dependencyModal;
    }

    public TextInputModal<MouseClick> questModal() {
        return this.questModal;
    }

    private GroupDisplay groupTemplate(String group) {
        for (ClientQuests.QuestEntry entry : ClientQuests.byGroup(group)) {
            GroupDisplay display = entry.value().display().groups().get(group);
            if (display != null) return display.withNodeScale(1.0f);
        }
        return GroupDisplay.create(group);
    }

    private void applyCanvasLimitFromFields(boolean allowClear) {
        if (this.updatingCanvasLimitFields) return;
        int width = parsePositiveInt(this.canvasLimitWidthField == null ? "" : this.canvasLimitWidthField.getValue());
        int height = parsePositiveInt(this.canvasLimitHeightField == null ? "" : this.canvasLimitHeightField.getValue());
        String group = this.content.group();
        if (width > 0 && height > 0) {
            int normalizedWidth = Math.max(DisplayConfig.MIN_CANVAS_LIMIT_WIDTH, width);
            int normalizedHeight = Math.max(DisplayConfig.MIN_CANVAS_LIMIT_HEIGHT, height);
            this.updatingCanvasLimitFields = true;
            try {
                if (this.canvasLimitWidthField != null) {
                    String value = String.valueOf(normalizedWidth);
                    if (!value.equals(this.canvasLimitWidthField.getValue())) {
                        this.canvasLimitWidthField.setValue(value);
                    }
                }
                if (this.canvasLimitHeightField != null) {
                    String value = String.valueOf(normalizedHeight);
                    if (!value.equals(this.canvasLimitHeightField.getValue())) {
                        this.canvasLimitHeightField.setValue(value);
                    }
                }
            } finally {
                this.updatingCanvasLimitFields = false;
            }
            if (DisplayConfig.getCanvasLimitWidth(group) != normalizedWidth || DisplayConfig.getCanvasLimitHeight(group) != normalizedHeight) {
                DisplayConfig.setCanvasLimit(group, normalizedWidth, normalizedHeight);
                if (this.questsWidget != null) this.questsWidget.refreshFromClient();
            }
            syncCanvasLimitToggle();
        } else if (allowClear && DisplayConfig.hasCanvasLimit(group)) {
            DisplayConfig.setCanvasLimit(group, 0, 0);
            this.updatingCanvasLimitFields = true;
            try {
                if (this.canvasLimitWidthField != null && !this.canvasLimitWidthField.getValue().isEmpty()) this.canvasLimitWidthField.setValue("");
                if (this.canvasLimitHeightField != null && !this.canvasLimitHeightField.getValue().isEmpty()) this.canvasLimitHeightField.setValue("");
            } finally {
                this.updatingCanvasLimitFields = false;
            }
            if (this.questsWidget != null) this.questsWidget.refreshFromClient();
            syncCanvasLimitToggle();
        }
    }

    private void toggleCanvasLimitEnabled() {
        String group = this.content.group();
        boolean enable = this.canvasLimitToggle == null || !this.canvasLimitToggle.isSelected();
        if (enable) {
            int width = parsePositiveInt(this.canvasLimitWidthField == null ? "" : this.canvasLimitWidthField.getValue());
            int height = parsePositiveInt(this.canvasLimitHeightField == null ? "" : this.canvasLimitHeightField.getValue());
            if (width <= 0) width = DisplayConfig.getCanvasLimitWidth(group);
            if (height <= 0) height = DisplayConfig.getCanvasLimitHeight(group);
            width = Math.max(DisplayConfig.MIN_CANVAS_LIMIT_WIDTH, width > 0 ? width : DisplayConfig.MIN_CANVAS_LIMIT_WIDTH);
            height = Math.max(DisplayConfig.MIN_CANVAS_LIMIT_HEIGHT, height > 0 ? height : DisplayConfig.MIN_CANVAS_LIMIT_HEIGHT);

            this.updatingCanvasLimitFields = true;
            try {
                if (this.canvasLimitWidthField != null) {
                    String value = String.valueOf(width);
                    if (!value.equals(this.canvasLimitWidthField.getValue())) this.canvasLimitWidthField.setValue(value);
                }
                if (this.canvasLimitHeightField != null) {
                    String value = String.valueOf(height);
                    if (!value.equals(this.canvasLimitHeightField.getValue())) this.canvasLimitHeightField.setValue(value);
                }
            } finally {
                this.updatingCanvasLimitFields = false;
            }

            DisplayConfig.setCanvasLimit(group, width, height);
        } else {
            DisplayConfig.setCanvasLimit(group, 0, 0);
        }
        if (this.questsWidget != null) this.questsWidget.refreshFromClient();
    }

    private void syncCanvasLimitToggle() {
        if (this.canvasLimitToggle != null) {
            this.canvasLimitToggle.setSelected(DisplayConfig.hasCanvasLimit(this.content.group()));
        }
    }

    private void applyGridOpacityField(boolean normalize) {
        int parsed = parsePercent1To100(this.gridOpacityField == null ? "" : this.gridOpacityField.getValue());
        if (parsed < 0) return;
        DisplayConfig.setGridOpacity(this.content.group(), parsed);
        if (this.gridOpacitySlider != null) this.gridOpacitySlider.setValue(parsed);
        if (normalize && this.gridOpacityField != null) this.gridOpacityField.setValue(String.valueOf(parsed));
    }

    private void applyBackgroundOpacityField(boolean normalize) {
        int parsed = parsePercent1To100(this.backgroundOpacityField == null ? "" : this.backgroundOpacityField.getValue());
        if (parsed < 0) return;
        DisplayConfig.setBackgroundOpacity(this.content.group(), parsed);
        if (this.backgroundOpacitySlider != null) this.backgroundOpacitySlider.setValue(parsed);
        if (normalize && this.backgroundOpacityField != null) this.backgroundOpacityField.setValue(String.valueOf(parsed));
    }

    private void refreshGridControlVisibility() {
        String group = this.content.group();
        boolean gridVisible = DisplayConfig.isGridEnabled(group);
        boolean lockEnabled = DisplayConfig.isGridLocked(group);
        boolean snapControlsVisible = gridVisible || lockEnabled;

        if (this.gridMinusButton != null) this.gridMinusButton.visible = snapControlsVisible;
        if (this.gridPlusButton != null) this.gridPlusButton.visible = snapControlsVisible;
        if (this.gridSizeField != null) this.gridSizeField.setVisible(snapControlsVisible);
        if (this.gridLock != null) this.gridLock.visible = true;
        if (this.gridOpacitySlider != null) this.gridOpacitySlider.visible = gridVisible;
        if (this.gridOpacityField != null) this.gridOpacityField.setVisible(gridVisible);
    }

    private static int parsePositiveInt(String value) {
        if (value == null) return -1;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return -1;
        try {
            int parsed = Integer.parseInt(trimmed);
            return parsed > 0 ? parsed : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static int parsePercent1To100(String value) {
        if (value == null) return -1;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return -1;
        try {
            int parsed = Integer.parseInt(trimmed);
            return Math.max(1, Math.min(100, parsed));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
