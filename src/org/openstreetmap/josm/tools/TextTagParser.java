// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.LanguageInfo.LocaleType;

/**
 * Class that helps to parse tags from arbitrary text
 */
public final class TextTagParser {

    // properties need JOSM restart to apply, modified rarely enough
    protected static final int MAX_KEY_LENGTH = Main.pref.getInteger("tags.paste.max-key-length", 50);
    protected static final int MAX_KEY_COUNT = Main.pref.getInteger("tags.paste.max-key-count", 30);
    protected static final String KEY_PATTERN = Main.pref.get("tags.paste.tag-pattern", "[0-9a-zA-Z:_]*");
    protected static final int MAX_VALUE_LENGTH = 255;

    private TextTagParser() {
        // Hide default constructor for utils classes
    }

    public static class TextAnalyzer {
        boolean quotesStarted = false;
        boolean esc = false;
        StringBuilder s = new StringBuilder(200);
        int pos;
        String data;
        int n;

        public TextAnalyzer(String text) {
            pos = 0;
            data = text;
            n = data.length();
        }

        /**
         * Read tags from "Free format"
         */
        Map<String, String>  getFreeParsedTags() {
            String k, v;
            Map<String, String> tags = new HashMap<String,String>();

            while (true) {
                skipEmpty();
                if (pos == n) { break; }
                k = parseString("\n\r\t= ");
                if (pos == n) { tags.clear();  break; }
                skipSign();
                if (pos == n) { tags.clear();  break; }
                v = parseString("\n\r\t ");
                tags.put(k, v);
            }
            return tags;
        }

        private String parseString(String stopChars) {
            char[] stop = stopChars.toCharArray();
            Arrays.sort(stop);
            char c;
            while (pos < n) {
                c = data.charAt(pos);
                if (esc) {
                    esc = false;
                    s.append(c); //  \" \\
                } else if (c == '\\') {
                    esc = true;
                } else if (c == '\"' && !quotesStarted) { // opening "
                    if (s.toString().trim().length()>0) { // we had   ||some text"||
                        s.append(c); // just add ", not open
                    } else {
                        s.delete(0, s.length()); // forget that empty characthers and start reading "....
                        quotesStarted = true;
                    }
                } else if (c == '\"' && quotesStarted) {  // closing "
                    quotesStarted = false;
                    pos++;
                    break;
                } else if (!quotesStarted && (Arrays.binarySearch(stop, c)>=0)) {
                    // stop-symbol found
                    pos++;
                    break;
                } else {
                    // skip non-printable characters
                    if(c>=32) s.append(c);
                }
                pos++;
            }

            String res = s.toString();
            s.delete(0, s.length());
            return res.trim();
        }

        private void skipSign() {
            char c;
            boolean signFound = false;
            while (pos < n) {
                c = data.charAt(pos);
                if (c == '\t' || c == '\n'  || c == ' ') {
                    pos++;
                } else if (c== '=') {
                    if (signFound) break; // a  =  =qwerty means "a"="=qwerty"
                    signFound = true;
                    pos++;
                } else {
                    break;
                }
            }
        }

        private void skipEmpty() {
            char c;
            while (pos < n) {
                c = data.charAt(pos);
                if (c == '\t' || c == '\n' || c == '\r' || c == ' ' ) {
                    pos++;
                } else {
                    break;
                }
            }
        }
    }

    protected static String unescape(String k) {
        if(! (k.startsWith("\"") && k.endsWith("\"")) ) {
            if (k.contains("=")) {
                // '=' not in quotes will be treated as an error!
                return null;
            } else {
                return k;
            }
        }
        String text = k.substring(1,k.length()-1);
        return (new TextAnalyzer(text)).parseString("\r\t\n");
    }

    /**
     * Try to find tag-value pairs in given text
     * @param text - text in which tags are looked for
     * @param splitRegex - text is splitted into parts with this delimiter
     * @param tagRegex - each part is matched against this regex
     * @param unescapeTextInQuotes - if true, matched tag and value will be analyzed more thoroughly
     */
    public static Map<String, String> readTagsByRegexp(String text, String splitRegex, String tagRegex, boolean unescapeTextInQuotes) {
         String[] lines = text.split(splitRegex);
         Pattern p = Pattern.compile(tagRegex);
         Map<String, String> tags = new HashMap<String,String>();
         String k=null, v=null;
         for (String  line: lines) {
            if (line.trim().isEmpty()) continue; // skip empty lines
            Matcher m = p.matcher(line);
            if (m.matches()) {
                 k=m.group(1).trim(); v=m.group(2).trim();
                 if (unescapeTextInQuotes) {
                     k = unescape(k);
                     v = unescape(v);
                     if (k==null || v==null) return null;
                 }
                 tags.put(k,v);
            } else {
                return null;
            }
         }
         if (!tags.isEmpty()) {
            return tags;
         }  else {
            return null;
         }
    }

    public static Map<String,String> getValidatedTagsFromText(String buf) {
        Map<String,String> tags = readTagsFromText(buf);
        return validateTags(tags) ? tags : null;
    }

    /**
     * Apply different methods to extract tag-value pairs from arbitrary text
     * @param buf
     * @return null if no format is suitable
     */

    public static Map<String,String> readTagsFromText(String buf) {
        Map<String,String> tags;

        // Format
        // tag1\tval1\ntag2\tval2\n
        tags = readTagsByRegexp(buf, "[\\r\\n]+", "(.*?)\\t(.*?)", false);
                // try "tag\tvalue\n" format
        if (tags!=null) return tags;

        // Format
        // a=b \n c=d \n "a b"=hello
        // SORRY: "a=b" = c is not supported fror now, only first = will be considered
        // a = "b=c" is OK
        // a = b=c  - this method of parsing fails intentionally
        tags = readTagsByRegexp(buf, "[\\n\\t\\r]+", "(.*?)=(.*?)", true);
                // try format  t1=v1\n t2=v2\n ...
        if (tags!=null) return tags;

        // JSON-format
        String bufJson = buf.trim();
        // trim { }, if there are any
        if (bufJson.startsWith("{") && bufJson.endsWith("}") ) bufJson = bufJson.substring(1,bufJson.length()-1);
        tags = readTagsByRegexp(bufJson, "[\\s]*,[\\s]*",
                "[\\s]*(\\\".*?[^\\\\]\\\")"+"[\\s]*:[\\s]*"+"(\\\".*?[^\\\\]\\\")[\\s]*", true);
        if (tags!=null) return tags;

        // Free format
        // a 1 "b" 2 c=3 d 4 e "5"
        TextAnalyzer parser = new TextAnalyzer(buf);
        tags = parser.getFreeParsedTags();
        return tags;
    }

    /**
     * Check tags for correctness and display warnings if needed
     * @param tags - map key-&gt;value to check
     * @return true if the tags should be pasted
     */
    public static boolean validateTags(Map<String, String> tags) {
        int r;
        int s = tags.size();
        if (s > MAX_KEY_COUNT) {
            // Use trn() even if for english it makes no sense, as s > 30
            r=warning(trn("There was {0} tag found in the buffer, it is suspicious!",
            "There were {0} tags found in the buffer, it is suspicious!", s,
            s), "", "tags.paste.toomanytags");
            if (r==2 || r==3) return false; if (r==4) return true;
        }
        for (Entry<String, String> entry : tags.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.length() > MAX_KEY_LENGTH) {
                r = warning(tr("Key is too long (max {0} characters):", MAX_KEY_LENGTH), key+"="+value, "tags.paste.keytoolong");
                if (r==2 || r==3) return false; if (r==4) return true;
            }
            if (!key.matches(KEY_PATTERN)) {
                r = warning(tr("Suspicious characters in key:"), key, "tags.paste.keydoesnotmatch");
                if (r==2 || r==3) return false; if (r==4) return true;
            }
            if (value.length() > MAX_VALUE_LENGTH) {
                r = warning(tr("Value is too long (max {0} characters):", MAX_VALUE_LENGTH), value, "tags.paste.valuetoolong");
                if (r==2 || r==3) return false; if (r==4) return true;
            }
        }
        return true;
    }

    private static int warning(String text, String data, String code) {
        ExtendedDialog ed = new ExtendedDialog(
                    Main.parent,
                    tr("Do you want to paste these tags?"),
                    new String[]{tr("Ok"), tr("Cancel"), tr("Clear buffer"), tr("Ignore warnings")});
        ed.setButtonIcons(new String[]{"ok.png", "cancel.png", "dialogs/delete.png", "pastetags.png"});
        ed.setContent("<html><b>"+text + "</b><br/><br/><div width=\"300px\">"+XmlWriter.encode(data,true)+"</html>");
        ed.setDefaultButton(2);
        ed.setCancelButton(2);
        ed.setIcon(JOptionPane.WARNING_MESSAGE);
        ed.toggleEnable(code);
        ed.showDialog();
        int r = ed.getValue();
        if (r==0) r = 2;
        // clean clipboard if user asked
        if (r==3) Utils.copyToClipboard("");
        return r;
    }

    /**
     * Shows message that the buffer can not be pasted, allowing user to clean the buffer
     * @param helpTopic the help topic of the parent action
     * TODO: Replace by proper HelpAwareOptionPane instead of self-made help link
     */
    public static void showBadBufferMessage(String helpTopic) {
        String msg = tr("<html><p> Sorry, it is impossible to paste tags from buffer. It does not contain any JOSM object"
            + " or suitable text. </p></html>");
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(msg),GBC.eop());
        String helpUrl = HelpUtil.getHelpTopicUrl(HelpUtil.buildAbsoluteHelpTopic(helpTopic, LocaleType.DEFAULT));
        if (helpUrl != null) {
            p.add(new UrlLabel(helpUrl), GBC.eop());
        }

        ExtendedDialog ed = new ExtendedDialog(
                    Main.parent,
                    tr("Warning"),
                    new String[]{tr("Ok"), tr("Clear buffer")});

        ed.setButtonIcons(new String[]{"ok.png", "dialogs/delete.png"});

        ed.setContent(p);
        ed.setDefaultButton(1);
        ed.setCancelButton(1);
        ed.setIcon(JOptionPane.WARNING_MESSAGE);
        ed.toggleEnable("tags.paste.cleanbadbuffer");
        ed.showDialog();

        int r = ed.getValue();
        // clean clipboard if user asked
        if (r==2) Utils.copyToClipboard("");
    }
}
