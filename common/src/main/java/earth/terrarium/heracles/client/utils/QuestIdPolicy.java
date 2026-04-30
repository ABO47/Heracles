package earth.terrarium.heracles.client.utils;

import earth.terrarium.heracles.client.handlers.ClientQuests;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class QuestIdPolicy {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9_]");
    private static final Pattern MULTI_UNDERSCORE = Pattern.compile("_+");
    private static final Pattern TRAILING_COUNTER = Pattern.compile("_(\\d+)$");
    private static final Pattern TRAILING_COPY = Pattern.compile("(?i)(?:_copy(?:_\\d+)*)+$");
    private static final Pattern TRAILING_SHORT_HEX = Pattern.compile("_[a-f0-9]{6,8}$");
    private static final Pattern QUICK_TOKEN = Pattern.compile("(?i)(^|_)quick(?=_|$)");

    private QuestIdPolicy() {
    }

    public static String nextAvailable(String group, String hint) {
        return nextAvailable(group, hint, Set.of());
    }

    public static String nextAvailable(String group, String hint, Set<String> reservedIds) {
        String groupPrefix = sanitizeSegment(group);
        if (groupPrefix.isBlank()) groupPrefix = "quest";

        String hintSegment = sanitizeSegment(hint);
        if (hintSegment.isBlank()) hintSegment = "";
        hintSegment = normalizeHintStem(groupPrefix, hintSegment);

        String stem = hintSegment.isBlank()
            ? groupPrefix
            : (hintSegment.startsWith(groupPrefix + "_") ? hintSegment : groupPrefix + "_" + hintSegment);
        stem = stripGeneratedSuffixes(stem, false);
        if (stem.isBlank()) stem = groupPrefix.isBlank() ? "quest" : groupPrefix;

        int counter = Math.max(1, findHighestCounter(stem, reservedIds) + 1);
        String candidate = format(stem, counter);
        while (!ClientQuests.get(candidate).isEmpty() || reservedIds.contains(candidate)) {
            counter++;
            candidate = format(stem, counter);
        }
        return candidate;
    }

    public static String sanitizeSegment(String input) {
        if (input == null) return "";
        String text = input.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        text = NON_ALPHANUMERIC.matcher(text).replaceAll("_");
        text = MULTI_UNDERSCORE.matcher(text).replaceAll("_");
        while (text.startsWith("_")) text = text.substring(1);
        while (text.endsWith("_")) text = text.substring(0, text.length() - 1);
        return text;
    }

    private static int findHighestCounter(String stem, Set<String> reservedIds) {
        int max = 0;
        max = Math.max(max, counterOf(stem, stem));
        for (ClientQuests.QuestEntry entry : ClientQuests.entries()) {
            max = Math.max(max, counterOf(stem, entry.key()));
        }
        for (String id : reservedIds) {
            max = Math.max(max, counterOf(stem, id));
        }
        return max;
    }

    private static int counterOf(String stem, String id) {
        if (id.equals(stem)) return 1;
        if (!id.startsWith(stem + "_")) return 0;
        var matcher = TRAILING_COUNTER.matcher(id);
        if (!matcher.find()) return 0;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String stripTrailingCounter(String stem) {
        return TRAILING_COUNTER.matcher(stem).replaceFirst("");
    }

    private static String normalizeHintStem(String groupPrefix, String hintSegment) {
        String normalized = stripAnyGroupPrefixChain(hintSegment, groupPrefix);
        normalized = removeQuickToken(normalized);
        normalized = stripGeneratedSuffixes(normalized, true);
        if (normalized.equals("quest")) return "";
        return normalized;
    }

    private static String stripAnyGroupPrefixChain(String hintSegment, String targetGroupPrefix) {
        String normalized = hintSegment == null ? "" : hintSegment;
        List<String> prefixes = new ArrayList<>();
        if (targetGroupPrefix != null && !targetGroupPrefix.isBlank()) {
            prefixes.add(targetGroupPrefix);
        }
        for (String group : ClientQuests.groups()) {
            String prefix = sanitizeSegment(group);
            if (!prefix.isBlank() && !prefixes.contains(prefix)) {
                prefixes.add(prefix);
            }
        }
        prefixes.sort(Comparator.comparingInt(String::length).reversed());

        boolean changed;
        do {
            changed = false;
            for (String prefix : prefixes) {
                String prefixWithSep = prefix + "_";
                if (normalized.startsWith(prefixWithSep)) {
                    normalized = normalized.substring(prefixWithSep.length());
                    changed = true;
                    break;
                }
            }
        } while (changed && !normalized.isBlank());

        return normalized;
    }

    private static String stripGeneratedSuffixes(String stem, boolean stripTrailingNumber) {
        String value = stem == null ? "" : stem;
        String previous;
        do {
            previous = value;
            value = removeQuickToken(value);
            value = TRAILING_COPY.matcher(value).replaceFirst("");
            if (stripTrailingNumber) {
                value = stripTrailingCounter(value);
            }
            if (value.split("_").length >= 3) {
                value = TRAILING_SHORT_HEX.matcher(value).replaceFirst("");
            }
            value = MULTI_UNDERSCORE.matcher(value).replaceAll("_");
            while (value.startsWith("_")) value = value.substring(1);
            while (value.endsWith("_")) value = value.substring(0, value.length() - 1);
        } while (!value.equals(previous));
        return value;
    }

    private static String removeQuickToken(String value) {
        String next = QUICK_TOKEN.matcher(value == null ? "" : value).replaceAll("$1");
        next = MULTI_UNDERSCORE.matcher(next).replaceAll("_");
        while (next.startsWith("_")) next = next.substring(1);
        while (next.endsWith("_")) next = next.substring(0, next.length() - 1);
        return next;
    }

    private static String format(String stem, int counter) {
        return stem + "_" + String.format(Locale.ROOT, "%04d", counter);
    }
}
