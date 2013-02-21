package org.openstreetmap.josm.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.io.XmlWriter;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

/**
 * Class that helps to parse tags from arbitrary text
 */
public class TextTagParser {
    
    // properties need JOSM restart to apply, modified rarely enough
    protected static final int MAX_KEY_LENGTH = Main.pref.getInteger("tags.paste.max-key-length", 50);
    protected static final int MAX_KEY_COUNT = Main.pref.getInteger("tags.paste.max-key-count", 30);
    protected static final String KEY_PATTERN = Main.pref.get("tags.paste.tag-pattern", "[0-9a-zA-Z:_]*");
    protected static final int MAX_VALUE_LENGTH = 255;
    
    public static class TextAnalyzer {
        int start = 0;
        boolean keyFound = false;
        boolean quotesStarted = false;
        boolean esc = false;
        StringBuilder s = new StringBuilder(200);
        int pos;
        String data;
        int n;
        boolean notFound;

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
                k = parseString(true);
                if (pos == n) { tags.clear();  break; }
                skipSign();
                if (pos == n) { tags.clear();  break; }
                v = parseString(false);
                tags.put(k, v);
            }
            return tags;
        }
        
        private String parseString(boolean stopOnEquals) {
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
                } else if (!quotesStarted && (c=='\n'|| c=='\t'|| c==' ' || c=='\r'
                      || (c=='=' && stopOnEquals))) {  // stop-symbols
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
            boolean signFound = false;;
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

    private static String unescape(String k) {
        if(! (k.startsWith("\"") && k.endsWith("\"")) ) {
            if (k.contains("=")) {
                // '=' not in quotes will be treated as an error!
                return null;
            } else {
                return k;
            }
        }
        String text = k.substring(1,k.length()-1);
        return (new TextAnalyzer(text)).parseString(false);
    }

    /**
     * Try to find tag-value pairs in given text  
     * @param text - text in which tags are looked for
     * @param splitRegex - text is splitted into parts with this delimiter
     * @param tagRegex - each part is matched against this regex
     * @param unescapeTextInQuotes - if true, matched tag and value will be analyzed more thoroughly
     */
    public static Map<String, String> readTagsByRegexp(String text, String splitRegex, String tagRegex, boolean unescapeTextInQuotes) {
         String lines[] = text.split(splitRegex);
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
        tags = readTagsByRegexp(buf, "[\r\n]+]", "(.*?)\t(.*?)", false);
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
     * @param tags - map key->value to check
     * @return true if user decision was "OK"
     */
    public static boolean validateTags(Map<String, String> tags) {
        String value;
        int r;
        int s = tags.size();
        if (s > MAX_KEY_COUNT) {
            // Use trn() even if for english it makes no sense, as s > 30
            r=warning(trn("There was {0} tag found in the buffer, it is suspicious!",
            "There were {0} tags found in the buffer, it is suspicious!", s,
            s), "", "toomanytags");
            if (r==2) return false; if (r==3) return true;
        }
        for (String key: tags.keySet()) {
            value = tags.get(key);
            if (key.length() > MAX_KEY_LENGTH) {
                r = warning(tr("Key is too long (max {0} characters):", MAX_KEY_LENGTH), key+"="+value, "keytoolong");
                if (r==2) return false; if (r==3) return true;
            }
            if (!key.matches(KEY_PATTERN)) {
                r = warning(tr("Suspicious characters in key:"), key, "keydoesnotmatch");
                if (r==2) return false; if (r==3) return true;
            }
            if (value.length() > MAX_VALUE_LENGTH) {
                r = warning(tr("Value is too long (max {0} characters):", MAX_VALUE_LENGTH), value, "valuetoolong");
                if (r==2) return false; if (r==3) return true;
            }
        }
        return true;
    }
    
    private static int warning(String text, String data, String code) {
        ExtendedDialog ed = new ExtendedDialog(
                    Main.parent,
                    tr("Do you want to paste these tags?"),
                    new String[]{tr("Ok"), tr("Cancel"), tr("Ignore warnings")});
        ed.setButtonIcons(new String[]{"ok.png", "cancel.png", "pastetags.png"});
        ed.setContent("<html><b>"+text + "</b><br/><br/><div width=\"300px\">"+XmlWriter.encode(data,true)+"</html>");
        ed.setDefaultButton(2);
        ed.setCancelButton(2);
        ed.setIcon(JOptionPane.WARNING_MESSAGE);
        ed.toggleEnable(code);
        ed.showDialog();
        Object o = ed.getValue();
        if (o instanceof Integer) 
            return ((Integer)o).intValue(); 
        else 
            return 2;
    }
}
