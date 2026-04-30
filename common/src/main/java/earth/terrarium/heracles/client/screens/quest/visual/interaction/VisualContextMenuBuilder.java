package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElementType;
import earth.terrarium.heracles.client.widgets.modals.EditorTextContextMenu;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class VisualContextMenuBuilder {
    private VisualContextMenuBuilder() {
    }

    public static List<EditorTextContextMenu.MenuItem> buildDescriptionMenu(
        Runnable copy,
        Runnable cut,
        Runnable paste,
        Runnable selectAll,
        Runnable insertItem,
        Runnable insertImage,
        Runnable insertEntity
    ) {
        List<EditorTextContextMenu.MenuItem> items = new ArrayList<>();
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Copy"), copy));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Cut"), cut));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Paste"), paste));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Select All"), selectAll));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Insert Item"), insertItem));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Insert Image"), insertImage));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Insert Entity"), insertEntity));
        return items;
    }

    public static List<EditorTextContextMenu.MenuItem> buildVisualBaseMenu(
        Runnable changeBackground,
        Runnable removeBackground,
        Runnable addText,
        Runnable addItem,
        Runnable addImage,
        Runnable addEntity
    ) {
        List<EditorTextContextMenu.MenuItem> items = new ArrayList<>();
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Change Background"), changeBackground));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Remove Background"), removeBackground));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Add Text"), addText));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Add Item"), addItem));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Add Image"), addImage));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Add Entity"), addEntity));
        return items;
    }

    public static void appendSelectedActions(
        List<EditorTextContextMenu.MenuItem> items,
        VisualElement selected,
        boolean villagerLikeEntity,
        Runnable copy,
        Runnable duplicate,
        Runnable editText,
        Runnable changeItem,
        Runnable changeImage,
        Runnable changeEntity,
        Runnable changeEntityVariant,
        Runnable entityMotion,
        Runnable openOpacity,
        Runnable bringToFront,
        Runnable sendToBack,
        Runnable delete
    ) {
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Copy"), copy));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Duplicate"), duplicate));
        if (selected.type == VisualElementType.TEXT) {
            items.add(new EditorTextContextMenu.MenuItem(Component.literal("Edit Text"), editText));
        } else if (selected.type == VisualElementType.ITEM) {
            items.add(new EditorTextContextMenu.MenuItem(Component.literal("Change Item"), changeItem));
        } else if (selected.type == VisualElementType.IMAGE) {
            items.add(new EditorTextContextMenu.MenuItem(Component.literal("Change Image"), changeImage));
        } else if (selected.type == VisualElementType.ENTITY) {
            items.add(new EditorTextContextMenu.MenuItem(Component.literal("Change Entity"), changeEntity));
            if (villagerLikeEntity) {
                items.add(new EditorTextContextMenu.MenuItem(Component.literal("Change To Variant"), changeEntityVariant));
            }
            items.add(new EditorTextContextMenu.MenuItem(Component.literal("Entity Motion..."), entityMotion));
        }
        if (selected.type == VisualElementType.TEXT || selected.type == VisualElementType.IMAGE) {
            items.add(new EditorTextContextMenu.MenuItem(Component.literal("Opacity..."), openOpacity));
        }
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Bring To Front"), bringToFront));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Send To Back"), sendToBack));
        items.add(new EditorTextContextMenu.MenuItem(Component.literal("Delete"), delete));
    }

    public static void appendPasteAction(List<EditorTextContextMenu.MenuItem> items, boolean hasClipboard, Runnable paste) {
        if (hasClipboard) {
            items.add(new EditorTextContextMenu.MenuItem(Component.literal("Paste"), paste));
        }
    }

    public static List<EditorTextContextMenu.MenuItem> buildVisualMenu(
        Runnable changeBackground,
        Runnable removeBackground,
        Runnable addText,
        Runnable addItem,
        Runnable addImage,
        Runnable addEntity,
        @Nullable VisualElement selected,
        boolean villagerLikeEntity,
        boolean hasClipboard,
        Runnable copy,
        Runnable duplicate,
        Runnable editText,
        Runnable changeItem,
        Runnable changeImage,
        Runnable changeEntity,
        Runnable changeEntityVariant,
        Runnable entityMotion,
        Runnable openOpacity,
        Runnable bringToFront,
        Runnable sendToBack,
        Runnable delete,
        Runnable paste
    ) {
        List<EditorTextContextMenu.MenuItem> items = buildVisualBaseMenu(
            changeBackground,
            removeBackground,
            addText,
            addItem,
            addImage,
            addEntity
        );
        if (selected != null) {
            appendSelectedActions(
                items,
                selected,
                villagerLikeEntity,
                copy,
                duplicate,
                editText,
                changeItem,
                changeImage,
                changeEntity,
                changeEntityVariant,
                entityMotion,
                openOpacity,
                bringToFront,
                sendToBack,
                delete
            );
        }
        appendPasteAction(items, hasClipboard, paste);
        return items;
    }
}
