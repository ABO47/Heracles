package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElementType;
import earth.terrarium.heracles.client.widgets.modals.ItemModal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.SpawnEggItem;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class VisualInsertionController {
    public enum InserterType {
        ITEM,
        ENTITY,
        IMAGE
    }

    private VisualInsertionController() {
    }

    private static boolean hasSelectedElementOfType(List<VisualElement> visualElements, VisualEditorState visualState, VisualElementType type) {
        return visualState.selectedVisualElement >= 0
            && visualState.selectedVisualElement < visualElements.size()
            && visualElements.get(visualState.selectedVisualElement).type == type;
    }

    private static void applyInlineInsertion(
        boolean visualMode,
        boolean replaceSelected,
        VisualElementType replaceType,
        Runnable replaceSelectedAction,
        Supplier<VisualElement> newElementFactory,
        String rawSnippet,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        Consumer<String> insertRawSnippetAndSync,
        Consumer<VisualElement> addVisualElementAndSync,
        Runnable syncRawFromVisual
    ) {
        if (visualMode) {
            if (replaceSelected && hasSelectedElementOfType(visualElements, visualState, replaceType)) {
                replaceSelectedAction.run();
                syncRawFromVisual.run();
                return;
            }
            addVisualElementAndSync.accept(newElementFactory.get());
            return;
        }
        insertRawSnippetAndSync.accept(rawSnippet);
    }

    public static void openInlineItemInserter(
        boolean replaceSelected,
        boolean visualMode,
        ItemModal itemModal,
        MultiLineEditBox descriptionBox,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        Runnable closeDescriptionContextMenu,
        Consumer<String> insertRawSnippetAndSync,
        Consumer<VisualElement> addVisualElementAndSync,
        Runnable syncRawFromVisual
    ) {
        if (itemModal == null || descriptionBox == null) return;
        closeDescriptionContextMenu.run();
        itemModal.setCallback(value -> {
            String snippet = value.map(
                stack -> "<item id=\"" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + "\"/>",
                tag -> "<item tag=\"" + tag.location() + "\"/>"
            );
            applyInlineInsertion(
                visualMode,
                replaceSelected,
                VisualElementType.ITEM,
                () -> {
                    VisualElement selected = visualElements.get(visualState.selectedVisualElement);
                    value.ifLeft(stack -> {
                        selected.itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                        selected.itemTag = "";
                    }).ifRight(tag -> {
                        selected.itemId = "";
                        selected.itemTag = tag.location().toString();
                    });
                },
                () -> value.map(
                    stack -> VisualElement.item(interactionState.contextVisualX, interactionState.contextVisualY, 64, 64, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), ""),
                    tag -> VisualElement.item(interactionState.contextVisualX, interactionState.contextVisualY, 64, 64, "", tag.location().toString())
                ),
                snippet,
                visualElements,
                visualState,
                insertRawSnippetAndSync,
                addVisualElementAndSync,
                syncRawFromVisual
            );
            itemModal.setVisible(false);
        });
        itemModal.setCurrent(null);
        itemModal.setTagsAllowed(true);
        itemModal.setVisible(true);
    }

    public static void openInlineEntityInserter(
        boolean replaceSelected,
        boolean visualMode,
        ItemModal itemModal,
        MultiLineEditBox descriptionBox,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        Map<Integer, net.minecraft.world.entity.Entity> entityPreviewCache,
        Map<Integer, String> entityPreviewIds,
        Runnable closeDescriptionContextMenu,
        Consumer<String> insertRawSnippetAndSync,
        Consumer<VisualElement> addVisualElementAndSync,
        Runnable syncRawFromVisual
    ) {
        if (itemModal == null || descriptionBox == null) return;
        closeDescriptionContextMenu.run();
        itemModal.setCallback(value -> {
            String entityId = value.left()
                .filter(stack -> stack.getItem() instanceof SpawnEggItem)
                .map(stack -> BuiltInRegistries.ENTITY_TYPE.getKey(((SpawnEggItem) stack.getItem()).getType(stack.getTag())).toString())
                .orElse("");
            if (entityId.isBlank()) {
                itemModal.setVisible(false);
                return;
            }
            String snippet = "<entity id=\"" + entityId + "\"/>";
            applyInlineInsertion(
                visualMode,
                replaceSelected,
                VisualElementType.ENTITY,
                () -> {
                    VisualElement selected = visualElements.get(visualState.selectedVisualElement);
                    selected.entityId = entityId;
                    selected.entityVariant = "";
                    entityPreviewCache.remove(visualState.selectedVisualElement);
                    entityPreviewIds.remove(visualState.selectedVisualElement);
                },
                () -> VisualElement.entity(interactionState.contextVisualX, interactionState.contextVisualY, 64, 64, entityId),
                snippet,
                visualElements,
                visualState,
                insertRawSnippetAndSync,
                addVisualElementAndSync,
                syncRawFromVisual
            );
            itemModal.setVisible(false);
        });
        itemModal.setCurrent(null);
        itemModal.setTagsAllowed(false);
        itemModal.setVisible(true);
    }

    public static void openInlineImageInserter(
        boolean replaceSelected,
        boolean visualMode,
        MultiLineEditBox descriptionBox,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        Runnable closeDescriptionContextMenu,
        Consumer<Consumer<String>> openAssetsLibraryModal,
        Function<String, int[]> resolveInsertedImageSize,
        Consumer<String> insertRawSnippetAndSync,
        Consumer<VisualElement> addVisualElementAndSync,
        Runnable syncRawFromVisual
    ) {
        if (descriptionBox == null) return;
        closeDescriptionContextMenu.run();
        openAssetsLibraryModal.accept(selected -> {
            String escaped = selected.replace("\"", "");
            int defaultW = 240;
            int defaultH = 135;
            int[] originalSize = resolveInsertedImageSize.apply(escaped);
            if (originalSize != null) {
                defaultW = originalSize[0];
                defaultH = originalSize[1];
            }
            String snippet = "<image src=\"" + escaped + "\" w=\"" + defaultW + "\" h=\"" + defaultH + "\" x=\"0\" y=\"0\"/>";
            int finalDefaultW = defaultW;
            int finalDefaultH = defaultH;
            applyInlineInsertion(
                visualMode,
                replaceSelected,
                VisualElementType.IMAGE,
                () -> visualElements.get(visualState.selectedVisualElement).imageSrc = escaped,
                () -> VisualElement.image(interactionState.contextVisualX, interactionState.contextVisualY, finalDefaultW, finalDefaultH, escaped),
                snippet,
                visualElements,
                visualState,
                insertRawSnippetAndSync,
                addVisualElementAndSync,
                syncRawFromVisual
            );
        });
    }

    public static void openInlineInserter(
        InserterType type,
        boolean replaceSelected,
        boolean visualMode,
        ItemModal itemModal,
        MultiLineEditBox descriptionBox,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        Map<Integer, net.minecraft.world.entity.Entity> entityPreviewCache,
        Map<Integer, String> entityPreviewIds,
        Runnable closeDescriptionContextMenu,
        Consumer<Consumer<String>> openAssetsLibraryModal,
        Function<String, int[]> resolveInsertedImageSize,
        Consumer<String> insertRawSnippetAndSync,
        Consumer<VisualElement> addVisualElementAndSync,
        Runnable syncRawFromVisual
    ) {
        switch (type) {
            case ITEM -> openInlineItemInserter(
                replaceSelected,
                visualMode,
                itemModal,
                descriptionBox,
                visualElements,
                visualState,
                interactionState,
                closeDescriptionContextMenu,
                insertRawSnippetAndSync,
                addVisualElementAndSync,
                syncRawFromVisual
            );
            case ENTITY -> openInlineEntityInserter(
                replaceSelected,
                visualMode,
                itemModal,
                descriptionBox,
                visualElements,
                visualState,
                interactionState,
                entityPreviewCache,
                entityPreviewIds,
                closeDescriptionContextMenu,
                insertRawSnippetAndSync,
                addVisualElementAndSync,
                syncRawFromVisual
            );
            case IMAGE -> openInlineImageInserter(
                replaceSelected,
                visualMode,
                descriptionBox,
                visualElements,
                visualState,
                interactionState,
                closeDescriptionContextMenu,
                openAssetsLibraryModal,
                resolveInsertedImageSize,
                insertRawSnippetAndSync,
                addVisualElementAndSync,
                syncRawFromVisual
            );
        }
    }
}
