// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.search;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.PushbackReader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.PushbackTokenizer.Token;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.DateUtils;

/**
 Implements a google-like search.
 <br>
 Grammar:
<pre>
expression =
  fact | expression
  fact expression
  fact

fact =
 ( expression )
 -fact
 term?
 term=term
 term:term
 term
 </pre>

 @author Imi
 */
public class SearchCompiler {

    private boolean caseSensitive = false;
    private boolean regexSearch = false;
    private static String  rxErrorMsg = marktr("The regex \"{0}\" had a parse error at offset {1}, full error:\n\n{2}");
    private PushbackTokenizer tokenizer;

    public SearchCompiler(boolean caseSensitive, boolean regexSearch, PushbackTokenizer tokenizer) {
        this.caseSensitive = caseSensitive;
        this.regexSearch = regexSearch;
        this.tokenizer = tokenizer;
    }

    abstract public static class Match {
        abstract public boolean match(OsmPrimitive osm);
    }

    public static class Always extends Match {
        public static Always INSTANCE = new Always();
        @Override public boolean match(OsmPrimitive osm) {
            return true;
        }
    }

    public static class Never extends Match {
        @Override
        public boolean match(OsmPrimitive osm) {
            return false;
        }
    }

    private static class Not extends Match {
        private final Match match;
        public Not(Match match) {this.match = match;}
        @Override public boolean match(OsmPrimitive osm) {
            return !match.match(osm);
        }
        @Override public String toString() {return "!"+match;}
    }

    private static class BooleanMatch extends Match {
        private final String key;
        private final boolean defaultValue;

        public BooleanMatch(String key, boolean defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }
        @Override
        public boolean match(OsmPrimitive osm) {
            Boolean ret = OsmUtils.getOsmBoolean(osm.get(key));
            if (ret == null)
                return defaultValue;
            else
                return ret;
        }
    }

    private static class And extends Match {
        private Match lhs;
        private Match rhs;
        public And(Match lhs, Match rhs) {this.lhs = lhs; this.rhs = rhs;}
        @Override public boolean match(OsmPrimitive osm) {
            return lhs.match(osm) && rhs.match(osm);
        }
        @Override public String toString() {return lhs+" && "+rhs;}
    }

    private static class Or extends Match {
        private Match lhs;
        private Match rhs;
        public Or(Match lhs, Match rhs) {this.lhs = lhs; this.rhs = rhs;}
        @Override public boolean match(OsmPrimitive osm) {
            return lhs.match(osm) || rhs.match(osm);
        }
        @Override public String toString() {return lhs+" || "+rhs;}
    }

    private static class Id extends Match {
        private long id;
        public Id(long id) {this.id = id;}
        @Override public boolean match(OsmPrimitive osm) {
            return osm.getId() == id;
        }
        @Override public String toString() {return "id="+id;}
    }

    private static class ChangesetId extends Match {
        private long changesetid;
        public ChangesetId(long changesetid) {this.changesetid = changesetid;}
        @Override public boolean match(OsmPrimitive osm) {
            return osm.getChangesetId() == changesetid;
        }
        @Override public String toString() {return "changeset="+changesetid;}
    }

    private static class Version extends Match {
        private long version;
        public Version(long version) {this.version = version;}
        @Override public boolean match(OsmPrimitive osm) {
            return osm.getVersion() == version;
        }
        @Override public String toString() {return "version="+version;}
    }

    private static class KeyValue extends Match {
        private final String key;
        private final Pattern keyPattern;
        private final String value;
        private final Pattern valuePattern;
        private final boolean caseSensitive;

        public KeyValue(String key, String value, boolean regexSearch, boolean caseSensitive) throws ParseError {
            this.caseSensitive = caseSensitive;
            if (regexSearch) {
                int searchFlags = regexFlags(caseSensitive);

                try {
                    this.keyPattern = Pattern.compile(key, searchFlags);
                    this.valuePattern = Pattern.compile(value, searchFlags);
                } catch (PatternSyntaxException e) {
                    throw new ParseError(tr(rxErrorMsg, e.getPattern(), e.getIndex(), e.getMessage()));
                }
                this.key = key;
                this.value = value;

            } else if (caseSensitive) {
                this.key = key;
                this.value = value;
                this.keyPattern = null;
                this.valuePattern = null;
            } else {
                this.key = key.toLowerCase();
                this.value = value;
                this.keyPattern = null;
                this.valuePattern = null;
            }
        }

        @Override public boolean match(OsmPrimitive osm) {

            if (keyPattern != null) {
                if (!osm.hasKeys())
                    return false;

                /* The string search will just get a key like
                 * 'highway' and look that up as osm.get(key). But
                 * since we're doing a regex match we'll have to loop
                 * over all the keys to see if they match our regex,
                 * and only then try to match against the value
                 */

                for (String k: osm.keySet()) {
                    String v = osm.get(k);

                    Matcher matcherKey = keyPattern.matcher(k);
                    boolean matchedKey = matcherKey.find();

                    if (matchedKey) {
                        Matcher matcherValue = valuePattern.matcher(v);
                        boolean matchedValue = matcherValue.find();

                        if (matchedValue)
                            return true;
                    }
                }
            } else {
                String mv = null;

                if (key.equals("timestamp")) {
                    mv = DateUtils.fromDate(osm.getTimestamp());
                } else {
                    mv = osm.get(key);
                }

                if (mv == null)
                    return false;

                String v1 = caseSensitive ? mv : mv.toLowerCase();
                String v2 = caseSensitive ? value : value.toLowerCase();

                // is not Java 1.5
                //v1 = java.text.Normalizer.normalize(v1, java.text.Normalizer.Form.NFC);
                //v2 = java.text.Normalizer.normalize(v2, java.text.Normalizer.Form.NFC);
                return v1.indexOf(v2) != -1;
            }

            return false;
        }
        @Override public String toString() {return key+"="+value;}
    }

    public static class ExactKeyValue extends Match {

        private enum Mode {
            ANY, ANY_KEY, ANY_VALUE, EXACT, NONE, MISSING_KEY,
            ANY_KEY_REGEXP, ANY_VALUE_REGEXP, EXACT_REGEXP, MISSING_KEY_REGEXP;
        }

        private final String key;
        private final String value;
        private final Pattern keyPattern;
        private final Pattern valuePattern;
        private final Mode mode;

        public ExactKeyValue(boolean regexp, String key, String value) throws ParseError {
            if (key == "")
                throw new ParseError(tr("Key cannot be empty when tag operator is used. Sample use: key=value"));
            this.key = key;
            this.value = value == null?"":value;
            if ("".equals(value) && "*".equals(key)) {
                mode = Mode.NONE;
            } else if ("".equals(value)) {
                if (regexp) {
                    mode = Mode.MISSING_KEY_REGEXP;
                } else {
                    mode = Mode.MISSING_KEY;
                }
            } else if ("*".equals(key) && "*".equals(value)) {
                mode = Mode.ANY;
            } else if ("*".equals(key)) {
                if (regexp) {
                    mode = Mode.ANY_KEY_REGEXP;
                } else {
                    mode = Mode.ANY_KEY;
                }
            } else if ("*".equals(value)) {
                if (regexp) {
                    mode = Mode.ANY_VALUE_REGEXP;
                } else {
                    mode = Mode.ANY_VALUE;
                }
            } else {
                if (regexp) {
                    mode = Mode.EXACT_REGEXP;
                } else {
                    mode = Mode.EXACT;
                }
            }

            if (regexp && key.length() > 0 && !key.equals("*")) {
                keyPattern = Pattern.compile(key);
            } else {
                keyPattern = null;
            }
            if (regexp && value.length() > 0 && !value.equals("*")) {
                try {
                    valuePattern = Pattern.compile(value);
                } catch (PatternSyntaxException e) {
                    throw new ParseError(tr("Pattern Syntax Error: Pattern {0} in {1} is illegal!", e.getPattern(), value));
                }
            } else {
                valuePattern = null;
            }
        }

        @Override
        public boolean match(OsmPrimitive osm) {

            if (!osm.hasKeys())
                return mode == Mode.NONE;

            switch (mode) {
            case NONE:
                return false;
            case MISSING_KEY:
                return osm.get(key) == null;
            case ANY:
                return true;
            case ANY_VALUE:
                return osm.get(key) != null;
            case ANY_KEY:
                for (String v:osm.getKeys().values()) {
                    if (v.equals(value))
                        return true;
                }
                return false;
            case EXACT:
                return value.equals(osm.get(key));
            case ANY_KEY_REGEXP:
                for (String v:osm.getKeys().values()) {
                    if (valuePattern.matcher(v).matches())
                        return true;
                }
                return false;
            case ANY_VALUE_REGEXP:
            case EXACT_REGEXP:
                for (String key: osm.keySet()) {
                    if (keyPattern.matcher(key).matches()) {
                        if (mode == Mode.ANY_VALUE_REGEXP
                                || valuePattern.matcher(osm.get(key)).matches())
                            return true;
                    }
                }
                return false;
            case MISSING_KEY_REGEXP:
                for (String k:osm.keySet()) {
                    if (keyPattern.matcher(k).matches())
                        return false;
                }
                return true;
            }
            throw new AssertionError("Missed state");
        }

        @Override
        public String toString() {
            return key + '=' + value;
        }

    }

    private static class Any extends Match {
        private final String search;
        private final Pattern searchRegex;
        private final boolean caseSensitive;

        public Any(String s, boolean regexSearch, boolean caseSensitive) throws ParseError {
            this.caseSensitive = caseSensitive;
            if (regexSearch) {
                try {
                    this.searchRegex = Pattern.compile(s, regexFlags(caseSensitive));
                } catch (PatternSyntaxException e) {
                    throw new ParseError(tr(rxErrorMsg, e.getPattern(), e.getIndex(), e.getMessage()));
                }
                this.search = s;
            } else if (caseSensitive) {
                this.search = s;
                this.searchRegex = null;
            } else {
                this.search = s.toLowerCase();
                this.searchRegex = null;
            }
        }

        @Override public boolean match(OsmPrimitive osm) {
            if (!osm.hasKeys())
                return search.equals("");

            // is not Java 1.5
            //search = java.text.Normalizer.normalize(search, java.text.Normalizer.Form.NFC);
            for (String key: osm.keySet()) {
                String value = osm.get(key);
                if (searchRegex != null) {

                    // is not Java 1.5
                    //value = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFC);

                    Matcher keyMatcher = searchRegex.matcher(key);
                    Matcher valMatcher = searchRegex.matcher(value);

                    boolean keyMatchFound = keyMatcher.find();
                    boolean valMatchFound = valMatcher.find();

                    if (keyMatchFound || valMatchFound)
                        return true;
                } else {
                    if (caseSensitive) {
                        key = key.toLowerCase();
                        value = value.toLowerCase();
                    }

                    // is not Java 1.5
                    //value = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFC);

                    if (key.indexOf(search) != -1 || value.indexOf(search) != -1)
                        return true;
                }
            }
            if (osm.getUser() != null) {
                String name = osm.getUser().getName();
                // is not Java 1.5
                //String name = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFC);
                if (!caseSensitive) {
                    name = name.toLowerCase();
                }
                if (name.indexOf(search) != -1)
                    return true;
            }
            return false;
        }
        @Override public String toString() {
            return search;
        }
    }

    private static class ExactType extends Match {
        private final Class<?> type;
        public ExactType(String type) throws ParseError {
            if ("node".equals(type)) {
                this.type = Node.class;
            } else if ("way".equals(type)) {
                this.type = Way.class;
            } else if ("relation".equals(type)) {
                this.type = Relation.class;
            } else
                throw new ParseError(tr("Unknown primitive type: {0}. Allowed values are node, way or relation",
                        type));
        }
        @Override public boolean match(OsmPrimitive osm) {
            return osm.getClass() == type;
        }
        @Override public String toString() {return "type="+type;}
    }

    private static class UserMatch extends Match {
        private String user;
        public UserMatch(String user) {
            if (user.equals("anonymous")) {
                this.user = null;
            } else {
                this.user = user;
            }
        }

        @Override public boolean match(OsmPrimitive osm) {
            if (osm.getUser() == null)
                return user == null;
            else
                return osm.getUser().hasName(user);
        }

        @Override public String toString() {
            return "user=" + user == null ? "" : user;
        }
    }

    private static class NodeCount extends Match {
        private int count;
        public NodeCount(int count) {this.count = count;}
        @Override public boolean match(OsmPrimitive osm) {
            return osm instanceof Way && ((Way) osm).getNodesCount() == count;
        }
        @Override public String toString() {return "nodes="+count;}
    }

    private static class NodeCountRange extends Match {
        private int minCount;
        private int maxCount;
        public NodeCountRange(int minCount, int maxCount) {
            if(maxCount < minCount) {
                this.minCount = maxCount;
                this.maxCount = minCount;
            } else {
                this.minCount = minCount;
                this.maxCount = maxCount;
            }
        }
        @Override public boolean match(OsmPrimitive osm) {
            if(!(osm instanceof Way)) return false;
            int size = ((Way)osm).getNodesCount();
            return (size >= minCount) && (size <= maxCount);
        }
        @Override public String toString() {return "nodes="+minCount+"-"+maxCount;}
    }

    private static class TagCount extends Match {
        private int count;
        public TagCount(int count) {this.count = count;}
        @Override public boolean match(OsmPrimitive osm) {
            int size = osm.getKeys().size();
            return size == count;
        }
        @Override public String toString() {return "tags="+count;}
    }

    private static class TagCountRange extends Match {
        private int minCount;
        private int maxCount;
        public TagCountRange(int minCount, int maxCount) {
            if(maxCount < minCount) {
                this.minCount = maxCount;
                this.maxCount = minCount;
            } else {
                this.minCount = minCount;
                this.maxCount = maxCount;
            }
        }
        @Override public boolean match(OsmPrimitive osm) {
            int size = osm.getKeys().size();
            return (size >= minCount) && (size <= maxCount);
        }
        @Override public String toString() {return "tags="+minCount+"-"+maxCount;}
    }

    private static class Modified extends Match {
        @Override public boolean match(OsmPrimitive osm) {
            return osm.isModified() || osm.isNew();
        }
        @Override public String toString() {return "modified";}
    }

    private static class Selected extends Match {
        @Override public boolean match(OsmPrimitive osm) {
            return Main.main.getCurrentDataSet().isSelected(osm);
        }
        @Override public String toString() {return "selected";}
    }

    private static class Incomplete extends Match {
        @Override public boolean match(OsmPrimitive osm) {
            return osm.isIncomplete();
        }
        @Override public String toString() {return "incomplete";}
    }

    private static class Untagged extends Match {
        @Override public boolean match(OsmPrimitive osm) {
            return !osm.isTagged();
        }
        @Override public String toString() {return "untagged";}
    }

    private static class Parent extends Match {
        private Match child;
        public Parent(Match m) { child = m; }
        @Override public boolean match(OsmPrimitive osm) {
            boolean isParent = false;

            // "parent" (null) should mean the same as "parent()"
            // (Always). I.e. match everything
            if (child == null) {
                child = new Always();
            }

            if (osm instanceof Way) {
                for (Node n : ((Way)osm).getNodes()) {
                    isParent |= child.match(n);
                }
            } else if (osm instanceof Relation) {
                for (RelationMember member : ((Relation)osm).getMembers()) {
                    isParent |= child.match(member.getMember());
                }
            }
            return isParent;
        }
        @Override public String toString() {return "parent(" + child + ")";}
    }

    private static class Child extends Match {
        private final Match parent;

        public Child(Match m) {
            // "child" (null) should mean the same as "child()"
            // (Always). I.e. match everything
            if (m == null) {
                parent = new Always();
            } else {
                parent = m;
            }
        }

        @Override public boolean match(OsmPrimitive osm) {
            boolean isChild = false;
            for (OsmPrimitive p : osm.getReferrers()) {
                isChild |= parent.match(p);
            }
            return isChild;
        }
        @Override public String toString() {return "child(" + parent + ")";}
    }

    public static class ParseError extends Exception {
        public ParseError(String msg) {
            super(msg);
        }
    }

    public static Match compile(String searchStr, boolean caseSensitive, boolean regexSearch)
    throws ParseError {
        return new SearchCompiler(caseSensitive, regexSearch,
                new PushbackTokenizer(
                        new PushbackReader(new StringReader(searchStr))))
        .parse();
    }

    public Match parse() throws ParseError {
        Match m = parseExpression();
        if (!tokenizer.readIfEqual(Token.EOF))
            throw new ParseError(tr("Unexpected token: {0}", tokenizer.nextToken()));
        if (m == null)
            return new Always();
        return m;
    }

    private Match parseExpression() throws ParseError {
        Match factor = parseFactor();
        if (factor == null)
            return null;
        if (tokenizer.readIfEqual(Token.OR))
            return new Or(factor, parseExpression(tr("Missing parameter for OR")));
        else {
            Match expression = parseExpression();
            if (expression == null)
                return factor;
            else
                return new And(factor, expression);
        }
    }

    private Match parseExpression(String errorMessage) throws ParseError {
        Match expression = parseExpression();
        if (expression == null)
            throw new ParseError(errorMessage);
        else
            return expression;
    }

    private Match parseFactor() throws ParseError {
        if (tokenizer.readIfEqual(Token.LEFT_PARENT)) {
            Match expression = parseExpression();
            if (!tokenizer.readIfEqual(Token.RIGHT_PARENT))
                throw new ParseError(tr("Unexpected token. Expected {0}, found {1}", Token.RIGHT_PARENT, tokenizer.nextToken()));
            return expression;
        } else if (tokenizer.readIfEqual(Token.NOT))
            return new Not(parseFactor(tr("Missing operator for NOT")));
        else if (tokenizer.readIfEqual(Token.KEY)) {
            String key = tokenizer.getText();
            if (tokenizer.readIfEqual(Token.EQUALS))
                return new ExactKeyValue(regexSearch, key, tokenizer.readText());
            else if (tokenizer.readIfEqual(Token.COLON))
                return parseKV(key, tokenizer.readText());
            else if (tokenizer.readIfEqual(Token.QUESTION_MARK))
                return new BooleanMatch(key, false);
            else if ("modified".equals(key))
                return new Modified();
            else if ("incomplete".equals(key))
                return new Incomplete();
            else if ("untagged".equals(key))
                return new Untagged();
            else if ("selected".equals(key))
                return new Selected();
            else if ("child".equals(key))
                return new Child(parseFactor());
            else if ("parent".equals(key))
                return new Parent(parseFactor());
            else
                return new Any(key, regexSearch, caseSensitive);
        } else
            return null;
    }

    private Match parseFactor(String errorMessage) throws ParseError {
        Match fact = parseFactor();
        if (fact == null)
            throw new ParseError(errorMessage);
        else
            return fact;
    }

    private Match parseKV(String key, String value) throws ParseError {
        if (value == null) {
            value = "";
        }
        if (key.equals("type"))
            return new ExactType(value);
        else if (key.equals("user"))
            return new UserMatch(value);
        else if (key.equals("tags")) {
            try {
                String[] range = value.split("-");
                if (range.length == 1)
                    return new TagCount(Integer.parseInt(value));
                else if (range.length == 2)
                    return new TagCountRange(Integer.parseInt(range[0]), Integer.parseInt(range[1]));
                else
                    throw new ParseError(tr("Wrong number of parameters for tags operator."));
            } catch (NumberFormatException e) {
                throw new ParseError(tr("Incorrect value of tags operator: {0}. Tags operator expects number of tags or range, for example tags:1 or tags:2-5", value));
            }
        } else if (key.equals("nodes")) {
            try {
                String[] range = value.split("-");
                if (range.length == 1)
                    return new NodeCount(Integer.parseInt(value));
                else if (range.length == 2)
                    return new NodeCountRange(Integer.parseInt(range[0]), Integer.parseInt(range[1]));
                else
                    throw new ParseError(tr("Wrong number of parameters for nodes operator."));
            } catch (NumberFormatException e) {
                throw new ParseError(tr("Incorrect value of nodes operator: {0}. Nodes operator expects number of nodes or range, for example nodes:10-20", value));
            }

        } else if (key.equals("id")) {
            try {
                return new Id(Long.parseLong(value));
            } catch (NumberFormatException x) {
                throw new ParseError(tr("Incorrect value of id operator: {0}. Number is expected.", value));
            }
        } else if (key.equals("changeset")) {
            try {
                return new ChangesetId(Integer.parseInt(value));
            } catch (NumberFormatException x) {
                throw new ParseError(tr("Incorrect value of changeset operator: {0}. Number is expected.", value));
            }

        }   else if (key.equals("version")) {
            try {
                return new Version(Long.parseLong(value));
            } catch (NumberFormatException x) {
                throw new ParseError(tr("Incorrect value of version operator: {0}. Number is expected.", value));
            }
        }
        else
            return new KeyValue(key, value, regexSearch, caseSensitive);
    }

    private static int regexFlags(boolean caseSensitive) {
        int searchFlags = 0;

        // Enables canonical Unicode equivalence so that e.g. the two
        // forms of "\u00e9gal" and "e\u0301gal" will match.
        //
        // It makes sense to match no matter how the character
        // happened to be constructed.
        searchFlags |= Pattern.CANON_EQ;

        // Make "." match any character including newline (/s in Perl)
        searchFlags |= Pattern.DOTALL;

        // CASE_INSENSITIVE by itself only matches US-ASCII case
        // insensitively, but the OSM data is in Unicode. With
        // UNICODE_CASE casefolding is made Unicode-aware.
        if (!caseSensitive) {
            searchFlags |= (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }

        return searchFlags;
    }
}
