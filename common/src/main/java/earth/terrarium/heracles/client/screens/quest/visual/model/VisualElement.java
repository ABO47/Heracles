package earth.terrarium.heracles.client.screens.quest.visual.model;

public final class VisualElement {
    public final VisualElementType type;
    public int x;
    public int y;
    public int w;
    public int h;
    public String text = "";
    public String imageSrc = "";
    public String itemId = "";
    public String itemTag = "";
    public String entityId = "";
    public String entityVariant = "";
    public int rotation = 0;
    public int spinSpeed = 0;
    public boolean bold = false;
    public boolean italic = false;
    public boolean underline = false;
    public boolean strike = false;
    public int textColor = 0x1B1B1B;
    public int textSize = 100;
    public VisualTextAlign align = VisualTextAlign.LEFT;
    public int opacity = 100;

    private VisualElement(VisualElementType type, int x, int y, int w, int h) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public static VisualElement text(int x, int y, int w, int h, String text) {
        VisualElement element = new VisualElement(VisualElementType.TEXT, x, y, w, h);
        element.text = text == null ? "" : text;
        return element;
    }

    public static VisualElement image(int x, int y, int w, int h, String source) {
        VisualElement element = new VisualElement(VisualElementType.IMAGE, x, y, w, h);
        element.imageSrc = source == null ? "" : source;
        return element;
    }

    public static VisualElement item(int x, int y, int w, int h, String itemId, String itemTag) {
        VisualElement element = new VisualElement(VisualElementType.ITEM, x, y, w, h);
        element.itemId = itemId == null ? "" : itemId;
        element.itemTag = itemTag == null ? "" : itemTag;
        return element;
    }

    public static VisualElement entity(int x, int y, int w, int h, String entityId) {
        VisualElement element = new VisualElement(VisualElementType.ENTITY, x, y, w, h);
        element.entityId = entityId == null ? "" : entityId;
        return element;
    }

    public VisualElement copy() {
        VisualElement copy = new VisualElement(this.type, this.x, this.y, this.w, this.h);
        copy.text = this.text;
        copy.imageSrc = this.imageSrc;
        copy.itemId = this.itemId;
        copy.itemTag = this.itemTag;
        copy.entityId = this.entityId;
        copy.entityVariant = this.entityVariant;
        copy.rotation = this.rotation;
        copy.spinSpeed = this.spinSpeed;
        copy.bold = this.bold;
        copy.italic = this.italic;
        copy.underline = this.underline;
        copy.strike = this.strike;
        copy.textColor = this.textColor;
        copy.textSize = this.textSize;
        copy.align = this.align;
        copy.opacity = this.opacity;
        return copy;
    }
}
