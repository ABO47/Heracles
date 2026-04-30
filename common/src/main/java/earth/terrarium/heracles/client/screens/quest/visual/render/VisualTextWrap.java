package earth.terrarium.heracles.client.screens.quest.visual.render;

import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

public final class VisualTextWrap {
    private VisualTextWrap() {
    }

    public static final class WrappedEditorLine {
        public final String text;
        public final int startIndex;
        public final int endIndex;

        public WrappedEditorLine(String text, int startIndex, int endIndex) {
            this.text = text;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    public static List<WrappedEditorLine> buildWrappedEditorLines(String value, int maxWidth, Font font) {
        List<WrappedEditorLine> lines = new ArrayList<>();
        int width = Math.max(1, maxWidth);
        String[] paragraphs = value.split("\n", -1);
        int globalIndex = 0;
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];
            if (paragraph.isEmpty()) {
                lines.add(new WrappedEditorLine("", globalIndex, globalIndex));
            } else {
                int local = 0;
                while (local < paragraph.length()) {
                    String remaining = paragraph.substring(local);
                    String piece = font.plainSubstrByWidth(remaining, width);
                    if (piece.isEmpty()) {
                        piece = remaining.substring(0, 1);
                    }
                    int start = globalIndex + local;
                    int end = start + piece.length();
                    lines.add(new WrappedEditorLine(piece, start, end));
                    local += piece.length();
                }
            }
            globalIndex += paragraph.length();
            if (i < paragraphs.length - 1) {
                globalIndex += 1;
            }
        }
        if (lines.isEmpty()) {
            lines.add(new WrappedEditorLine("", 0, 0));
        }
        return lines;
    }
}
