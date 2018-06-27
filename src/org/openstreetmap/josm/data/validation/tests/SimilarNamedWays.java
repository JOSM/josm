// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.UNICODE_CASE;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.ValUtil;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

/**
 * Checks for similar named ways, symptom of a possible typo. It uses the
 * Levenshtein distance to check for similarity
 *
 * @author frsantos
 */
public class SimilarNamedWays extends Test {

    protected static final int SIMILAR_NAMED = 701;

    /** All ways, grouped by cells */
    private Map<Point2D, List<Way>> cellWays;
    /** The already detected errors */
    private MultiMap<Way, Way> errorWays;

    private final List<NormalizeRule> rules = new ArrayList<>();

    /**
     * Constructor
     */
    public SimilarNamedWays() {
        super(tr("Similarly named ways"),
                tr("This test checks for ways with similar names that may have been misspelled."));

        // FIXME: hardcode these rules for now. Replace them with preferences later
        // See https://josm.openstreetmap.de/ticket/3733#comment:19
        addRegExprRule("\\pN+", "0"); // Unicode numbers: matches "Highway 66" but also persian numbers
        addRegExprRule("M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$", "0"); // Roman numbers: matches "Building II"
        addRegExprRule("\\d+(st|nd|rd|th)", "0st"); // 3rd Ave
        addRegExprRule("^[A-Z] ", "X"); // E Street
        addSynonyms("east", "west", "north", "south");
        addSynonyms("first", "second", "third");
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        cellWays = new HashMap<>(1000);
        errorWays = new MultiMap<>();
    }

    @Override
    public void endTest() {
        cellWays = null;
        errorWays = null;
        super.endTest();
    }

    @Override
    public void visit(Way w) {
        if (!w.isUsable())
            return;

        String name = w.get("name");
        if (name == null || name.length() < 6)
            return;

        List<List<Way>> theCellWays = ValUtil.getWaysInCell(w, cellWays);
        for (List<Way> ways : theCellWays) {
            for (Way w2 : ways) {
                if (errorWays.contains(w, w2) || errorWays.contains(w2, w)) {
                    continue;
                }

                String name2 = w2.get("name");
                if (name2 == null || name2.length() < 6) {
                    continue;
                }

                if (similaryName(name, name2)) {
                    List<OsmPrimitive> primitives = new ArrayList<>(2);
                    primitives.add(w);
                    primitives.add(w2);
                    errors.add(TestError.builder(this, Severity.WARNING, SIMILAR_NAMED)
                            .message(tr("Similarly named ways"))
                            .primitives(primitives)
                            .build());
                    errorWays.put(w, w2);
                }
            }
            ways.add(w);
        }
    }

    /**
     * Compute Levenshtein distance
     *
     * @param s First word
     * @param t Second word
     * @return The distance between words
     */
    public static int getLevenshteinDistance(String s, String t) {
        int[][] d; // matrix
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        char si; // ith character of s
        char tj; // jth character of t
        int cost; // cost

        // Step 1
        n = s.length();
        m = t.length();
        if (n == 0)
            return m;
        if (m == 0)
            return n;
        d = new int[n+1][m+1];

        // Step 2
        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        // Step 3
        for (i = 1; i <= n; i++) {

            si = s.charAt(i - 1);

            // Step 4
            for (j = 1; j <= m; j++) {

                tj = t.charAt(j - 1);

                // Step 5
                if (si == tj) {
                    cost = 0;
                } else {
                    cost = 1;
                }

                // Step 6
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }

        // Step 7
        return d[n][m];
    }

    /**
     * Add a regular expression rule.
     * @param regExpr the regular expression to search for
     * @param replacement a string to replace with, which should match the expression.
     */
    public void addRegExprRule(String regExpr, String replacement) {
        rules.add(new RegExprRule(regExpr, replacement));
    }

    /**
     * Add a rule with synonym words.
     * @param words words which are synonyms
     */
    public void addSynonyms(String... words) {
        for (String word : words) {
            rules.add(new SynonymRule(word, words));
        }
    }

    /**
     * Check if two names are similar, but not identical. First both names will be "normalized".
     * Afterwards the Levenshtein distance will be calculated.<br>
     * Examples for normalization rules:<br>
     * <code>replaceAll("\\d+", "0")</code><br>
     * would cause similaryName("track 1", "track 2") = false, but similaryName("Track 1", "track 2") = true
     * @param name first name to compare
     * @param name2 second name to compare
     * @return true if the normalized names are different but only a "little bit"
     */
    public boolean similaryName(String name, String name2) {
        // check plain strings
        int distance = getLevenshteinDistance(name, name2);
        boolean similar = distance > 0 && distance <= 2;

        // check if only the case differs, so we don't consider large distance as different strings
        if (distance > 2 && name.length() == name2.length()) {
            similar = Utils.deAccent(name).equalsIgnoreCase(Utils.deAccent(name2));
        }

        // try all rules
        for (NormalizeRule rule : rules) {
            int levenshteinDistance = getLevenshteinDistance(rule.normalize(name), rule.normalize(name2));
            if (levenshteinDistance == 0)
                // one rule results in identical names: identical
                return false;
            else if (levenshteinDistance <= 2) {
                // 0 < distance <= 2
                similar = true;
            }
        }
        return similar;
    }

    /**
     * A normalization that is applied to names before testing them
     */
    @FunctionalInterface
    public interface NormalizeRule {

        /**
         * Normalize the string by replacing parts.
         * @param name name to normalize
         * @return normalized string
         */
        String normalize(String name);
    }

    /**
     * A rule to replace by regular expression,
     * so that all strings matching the regular expression are handled as if they were {@link RegExprRule#replacement}
     */
    public static class RegExprRule implements NormalizeRule {
        private final Pattern regExpr;
        private final String replacement;

        /**
         * Create a new rule to replace by regular expression
         * @param expression The regular expression
         * @param replacement The replacement
         */
        public RegExprRule(String expression, String replacement) {
            this.regExpr = Pattern.compile(expression);
            this.replacement = replacement;
        }

        @Override
        public String normalize(String name) {
            return regExpr.matcher(name).replaceAll(replacement);
        }

        @Override
        public String toString() {
            return "replaceAll(" + regExpr + ", " + replacement + ')';
        }
    }

    /**
     * A rule that registers synonyms to a given word
     */
    public static class SynonymRule implements NormalizeRule {

        private final String[] words;
        private final Pattern regExpr;
        private final String replacement;

        /**
         * Create a new {@link SynonymRule}
         * @param replacement The word to use instead
         * @param words The synonyms for that word
         */
        public SynonymRule(String replacement, String... words) {
            this.replacement = replacement.toLowerCase(Locale.ENGLISH);
            this.words = words;

            // build regular expression for other words (for fast match)
            StringBuilder expression = new StringBuilder();
            int maxLength = 0;
            for (int i = 0; i < words.length; i++) {
                if (words[i].length() > maxLength) {
                    maxLength = words[i].length();
                }
                if (expression.length() > 0) {
                    expression.append('|');
                }
                expression.append(Pattern.quote(words[i]));
            }
            this.regExpr = Pattern.compile(expression.toString(), CASE_INSENSITIVE + UNICODE_CASE);
        }

        @Override
        public String normalize(String name) {
            // find first match
            Matcher matcher = regExpr.matcher(name);
            if (!matcher.find())
                return name;

            int start = matcher.start();

            // which word matches?
            String part = "";
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                part = name.substring(start, start + word.length());
                if (word.equalsIgnoreCase(part)) {
                    break;
                }
            }

            // replace the word
            char[] newName = matcher.replaceFirst(replacement).toCharArray();

            // adjust case (replacement is not shorter than matching word!)
            int minLength = Math.min(replacement.length(), part.length());
            for (int i = 0; i < minLength; i++) {
                if (Character.isUpperCase(part.charAt(i))) {
                    newName[start + i] = Character.toUpperCase(newName[start + i]);
                }
            }

            return new String(newName);
        }

        @Override
        public String toString() {
            return "synonyms(" + replacement + ", " + Arrays.toString(words) + ')';
        }
    }
}
