// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Class that helps to parse tags from arbitrary text
 */
public final class TextTagParser {

    // properties need JOSM restart to apply, modified rarely enough
    private static final int MAX_KEY_LENGTH = Config.getPref().getInt("tags.paste.max-key-length", 50);
    private static final int MAX_KEY_COUNT = Config.getPref().getInt("tags.paste.max-key-count", 30);
    private static final String KEY_PATTERN = Config.getPref().get("tags.paste.tag-pattern", "[0-9a-zA-Z:_]*");
    private static final int MAX_VALUE_LENGTH = 255;

    private TextTagParser() {
        // Hide default constructor for utils classes
    }

    static String unescape(String k) {
        if (!(k.startsWith("\"") && k.endsWith("\""))) {
            if (k.contains("=")) {
                // '=' not in quotes will be treated as an error!
                return null;
            } else {
                return k;
            }
        }
        String text = k.substring(1, k.length()-1);
        return (new TextAnalyzer(text)).parseString("\r\t\n");
    }

    /**
     * Try to find tag-value pairs in given text
     * @param text - text in which tags are looked for
     * @param splitRegex - text is splitted into parts with this delimiter
     * @param tagRegex - each part is matched against this regex
     * @param unescapeTextInQuotes - if true, matched tag and value will be analyzed more thoroughly
     * @return map of tags
     */
    public static Map<String, String> readTagsByRegexp(String text, String splitRegex, String tagRegex, boolean unescapeTextInQuotes) {
         String[] lines = text.split(splitRegex);
         Pattern p = Pattern.compile(tagRegex);
         Map<String, String> tags = new HashMap<>();
         String k;
         String v;
         for (String line: lines) {
            if (line.trim().isEmpty()) continue; // skip empty lines
            Matcher m = p.matcher(line);
            if (m.matches()) {
                 k = m.group(1).trim();
                 v = m.group(2).trim();
                 if (unescapeTextInQuotes) {
                     k = unescape(k);
                     v = unescape(v);
                     if (k == null || v == null) return null;
                 }
                 tags.put(k, v);
            } else {
                return null;
            }
         }
         if (!tags.isEmpty()) {
            return tags;
         } else {
            return null;
         }
    }

    /**
     * Gets a list of tags that are in the given text
     * @param buf The text to parse
     * @param callback warning callback
     * @return The tags or <code>null</code> if the tags are not valid
     * @since 12683
     */
    public static Map<String, String> getValidatedTagsFromText(String buf, TagWarningCallback callback) {
        Map<String, String> tags = readTagsFromText(buf);
        return validateTags(tags, callback) ? tags : null;
    }

    /**
     * Apply different methods to extract tag-value pairs from arbitrary text
     * @param buf buffer
     * @return null if no format is suitable
     */
    public static Map<String, String> readTagsFromText(String buf) {
        Map<String, String> tags;

        // Format
        // tag1\tval1\ntag2\tval2\n
        tags = readTagsByRegexp(buf, "[\\r\\n]+", ".*?([a-zA-Z0-9:_]+).*\\t(.*?)", false);
        // try "tag\tvalue\n" format
        if (tags != null) return tags;

        // Format
        // a=b \n c=d \n "a b"=hello
        // SORRY: "a=b" = c is not supported fror now, only first = will be considered
        // a = "b=c" is OK
        // a = b=c  - this method of parsing fails intentionally
        tags = readTagsByRegexp(buf, "[\\n\\t\\r]+", "(.*?)=(.*?)", true);
        // try format  t1=v1\n t2=v2\n ...
        if (tags != null) return tags;

        // JSON-format
        String bufJson = buf.trim();
        // trim { }, if there are any
        if (bufJson.startsWith("{") && bufJson.endsWith("}"))
            bufJson = bufJson.substring(1, bufJson.length()-1);
        tags = readTagsByRegexp(bufJson, "[\\s]*,[\\s]*",
                "[\\s]*(\\\".*?[^\\\\]\\\")"+"[\\s]*:[\\s]*"+"(\\\".*?[^\\\\]\\\")[\\s]*", true);
        if (tags != null) return tags;

        // Free format
        // a 1 "b" 2 c=3 d 4 e "5"
        return new TextAnalyzer(buf).getFreeParsedTags();
    }

    /**
     * Check tags for correctness and display warnings if needed
     * @param tags - map key-&gt;value to check
     * @param callback warning callback
     * @return true if the tags should be pasted
     * @since 12683
     */
    public static boolean validateTags(Map<String, String> tags, TagWarningCallback callback) {
        int r;
        int s = tags.size();
        if (s > MAX_KEY_COUNT) {
            // Use trn() even if for english it makes no sense, as s > 30
            r = callback.warning(trn("There was {0} tag found in the buffer, it is suspicious!",
            "There were {0} tags found in the buffer, it is suspicious!", s,
            s), "", "tags.paste.toomanytags");
            if (r == 2 || r == 3) return false; if (r == 4) return true;
        }
        for (Entry<String, String> entry : tags.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.length() > MAX_KEY_LENGTH) {
                r = callback.warning(tr("Key is too long (max {0} characters):", MAX_KEY_LENGTH), key+'='+value, "tags.paste.keytoolong");
                if (r == 2 || r == 3) return false; if (r == 4) return true;
            }
            if (!key.matches(KEY_PATTERN)) {
                r = callback.warning(tr("Suspicious characters in key:"), key, "tags.paste.keydoesnotmatch");
                if (r == 2 || r == 3) return false; if (r == 4) return true;
            }
            if (value.length() > MAX_VALUE_LENGTH) {
                r = callback.warning(tr("Value is too long (max {0} characters):", MAX_VALUE_LENGTH), value, "tags.paste.valuetoolong");
                if (r == 2 || r == 3) return false; if (r == 4) return true;
            }
        }
        return true;
    }

    /**
     * Called when a problematic tag is encountered.
     * @since 12683
     */
    @FunctionalInterface
    public interface TagWarningCallback {
        /**
         * Displays a warning about a problematic tag and ask user what to do about it.
         * @param text Message to display
         * @param data Tag key and/or value
         * @param code to use with {@code ExtendedDialog#toggleEnable(String)}
         * @return 1 to validate and display next warnings if any, 2 to cancel operation, 3 to clear buffer, 4 to paste tags
         */
        int warning(String text, String data, String code);
    }
}
