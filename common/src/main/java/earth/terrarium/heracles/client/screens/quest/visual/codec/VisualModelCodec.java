package earth.terrarium.heracles.client.screens.quest.visual.codec;

import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElementType;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualTextAlign;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VisualModelCodec {
    private static final Pattern VISUAL_TAG_PATTERN = Pattern.compile("<(textbox|image|item|entity|background)\\b([^>]*)/>");
    private static final Pattern ATTR_PATTERN = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    private VisualModelCodec() {
    }

    public record ParseResult(List<VisualElement> elements, String backgroundSrc, int backgroundOpacity) {
    }

    public static ParseResult parse(String source, int descriptionWidth) {
        List<VisualElement> elements = new ArrayList<>();
        String backgroundSrc = "";
        int backgroundOpacity = 100;
        boolean sawVisualTag = false;
        Matcher matcher = VISUAL_TAG_PATTERN.matcher(source);
        while (matcher.find()) {
            sawVisualTag = true;
            String type = matcher.group(1);
            Map<String, String> attrs = parseAttributes(matcher.group(0));
            if ("background".equals(type)) {
                backgroundSrc = attrs.getOrDefault("src", "");
                backgroundOpacity = parseInt(attrs.get("opacity"), 100, 0, 100);
                continue;
            }

            int x = parseInt(attrs.get("x"), 8, -8192, 8192);
            int y = parseInt(attrs.get("y"), 8, -8192, 8192);
            int minTypeW = type.equals("item") ? 16 : 24;
            int minTypeH = type.equals("item") ? 16 : 14;
            int w = parseInt(attrs.get("w"), type.equals("item") ? 64 : 220, minTypeW, 8192);
            int h = parseInt(attrs.get("h"), type.equals("item") ? 64 : 60, minTypeH, 8192);

            if ("textbox".equals(type)) {
                VisualElement text = VisualElement.text(x, y, w, h, decodeAttribute(attrs.getOrDefault("text", "")));
                text.bold = Boolean.parseBoolean(attrs.getOrDefault("bold", "false"));
                text.italic = Boolean.parseBoolean(attrs.getOrDefault("italic", "false"));
                text.underline = Boolean.parseBoolean(attrs.getOrDefault("underline", "false"));
                text.strike = Boolean.parseBoolean(attrs.getOrDefault("strike", "false"));
                text.align = VisualTextAlign.byName(attrs.getOrDefault("align", "left"));
                text.textColor = parseInt(attrs.get("color"), 0x1B1B1B, 0x000000, 0xFFFFFF);
                text.textSize = parseInt(attrs.get("size"), 100, 50, 300);
                text.opacity = parseInt(attrs.get("opacity"), 100, 0, 100);
                text.rotation = parseInt(attrs.get("rotation"), 0, -3600, 3600);
                elements.add(text);
            } else if ("item".equals(type)) {
                VisualElement item = VisualElement.item(x, y, w, h, attrs.getOrDefault("id", ""), attrs.getOrDefault("tag", ""));
                item.opacity = 100;
                item.rotation = parseInt(attrs.get("rotation"), 0, -3600, 3600);
                elements.add(item);
            } else if ("image".equals(type)) {
                VisualElement image = VisualElement.image(x, y, w, h, attrs.getOrDefault("src", ""));
                image.opacity = parseInt(attrs.get("opacity"), 100, 0, 100);
                image.rotation = parseInt(attrs.get("rotation"), 0, -3600, 3600);
                elements.add(image);
            } else if ("entity".equals(type)) {
                VisualElement entity = VisualElement.entity(x, y, w, h, attrs.getOrDefault("id", ""));
                entity.opacity = 100;
                entity.entityVariant = attrs.getOrDefault("variant", "");
                entity.rotation = parseInt(attrs.get("rotation"), 0, -3600, 3600);
                entity.spinSpeed = parseInt(attrs.get("spin"), 0, 0, 360);
                elements.add(entity);
            }
        }

        if (elements.isEmpty() && !source.isBlank() && !sawVisualTag) {
            elements.add(VisualElement.text(8, 8, Math.max(120, descriptionWidth - 16), 70, source));
        }
        return new ParseResult(elements, backgroundSrc, backgroundOpacity);
    }

    public static String serialize(String visualBackgroundSrc, int visualBackgroundOpacity, List<VisualElement> elements) {
        StringBuilder builder = new StringBuilder();
        if (!visualBackgroundSrc.isBlank()) {
            builder.append("<background src=\"").append(encodeAttribute(visualBackgroundSrc))
                .append("\" opacity=\"").append(visualBackgroundOpacity)
                .append("\"/>");
            if (!elements.isEmpty()) builder.append('\n');
        }
        for (int i = 0; i < elements.size(); i++) {
            VisualElement element = elements.get(i);
            if (i > 0) builder.append('\n');
            if (element.type == VisualElementType.TEXT) {
                builder.append("<textbox x=\"").append(element.x)
                    .append("\" y=\"").append(element.y)
                    .append("\" w=\"").append(element.w)
                    .append("\" h=\"").append(element.h)
                    .append("\" align=\"").append(element.align.name().toLowerCase())
                    .append("\" color=\"").append(element.textColor)
                    .append("\" bold=\"").append(element.bold)
                    .append("\" italic=\"").append(element.italic)
                    .append("\" underline=\"").append(element.underline)
                    .append("\" strike=\"").append(element.strike)
                    .append("\" opacity=\"").append(element.opacity)
                    .append("\" size=\"").append(element.textSize)
                    .append("\" rotation=\"").append(element.rotation)
                    .append("\" text=\"").append(encodeAttribute(element.text))
                    .append("\"/>");
            } else if (element.type == VisualElementType.ITEM) {
                builder.append("<item ");
                if (!element.itemId.isBlank()) {
                    builder.append("id=\"").append(encodeAttribute(element.itemId)).append("\" ");
                } else {
                    builder.append("tag=\"").append(encodeAttribute(element.itemTag)).append("\" ");
                }
                builder.append("x=\"").append(element.x)
                    .append("\" y=\"").append(element.y)
                    .append("\" w=\"").append(element.w)
                    .append("\" h=\"").append(element.h)
                    .append("\" rotation=\"").append(element.rotation)
                    .append("\"/>");
            } else if (element.type == VisualElementType.IMAGE) {
                builder.append("<image src=\"").append(encodeAttribute(element.imageSrc))
                    .append("\" x=\"").append(element.x)
                    .append("\" y=\"").append(element.y)
                    .append("\" w=\"").append(element.w)
                    .append("\" h=\"").append(element.h)
                    .append("\" rotation=\"").append(element.rotation)
                    .append("\" opacity=\"").append(element.opacity)
                    .append("\"/>");
            } else if (element.type == VisualElementType.ENTITY) {
                builder.append("<entity id=\"").append(encodeAttribute(element.entityId))
                    .append("\" x=\"").append(element.x)
                    .append("\" y=\"").append(element.y)
                    .append("\" w=\"").append(element.w)
                    .append("\" h=\"").append(element.h)
                    .append("\" rotation=\"").append(element.rotation)
                    .append("\" spin=\"").append(element.spinSpeed);
                if (!element.entityVariant.isBlank()) {
                    builder.append("\" variant=\"").append(encodeAttribute(element.entityVariant));
                }
                builder.append("\"/>");
            }
        }
        return builder.toString();
    }

    public static int parseInt(String raw, int fallback, int min, int max) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Mth.clamp(Integer.parseInt(raw.trim()), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Map<String, String> parseAttributes(String tag) {
        Map<String, String> attrs = new HashMap<>();
        Matcher attrMatcher = ATTR_PATTERN.matcher(tag);
        while (attrMatcher.find()) {
            attrs.put(attrMatcher.group(1), attrMatcher.group(2));
        }
        return attrs;
    }

    private static String encodeAttribute(String value) {
        return value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "&#10;");
    }

    private static String decodeAttribute(String value) {
        return value
            .replace("&#10;", "\n")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&");
    }
}
