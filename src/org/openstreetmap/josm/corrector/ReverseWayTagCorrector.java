// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.correction.RoleCorrection;
import org.openstreetmap.josm.data.correction.TagCorrection;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.UserCancelException;

/**
 * A ReverseWayTagCorrector handles necessary corrections of tags
 * when a way is reversed. E.g. oneway=yes needs to be changed
 * to oneway=-1 and vice versa.
 *
 * The Corrector offers the automatic resolution in an dialog
 * for the user to confirm.
 */
public class ReverseWayTagCorrector extends TagCorrector<Way> {

    private static final String SEPARATOR = "[:_]";

    private static Pattern getPatternFor(String s) {
        return getPatternFor(s, false);
    }

    private static Pattern getPatternFor(String s, boolean exactMatch) {
        if (exactMatch) {
            return Pattern.compile("(^)(" + s + ")($)");
        } else {
            return Pattern.compile("(^|.*" + SEPARATOR + ")(" + s + ")(" + SEPARATOR + ".*|$)",
                    Pattern.CASE_INSENSITIVE);
        }
    }

    private static final Collection<Pattern> IGNORED_KEYS = new ArrayList<>();
    static {
        for (String s : AbstractPrimitive.getUninterestingKeys()) {
            IGNORED_KEYS.add(getPatternFor(s));
        }
        for (String s : new String[]{"name", "ref", "tiger:county"}) {
            IGNORED_KEYS.add(getPatternFor(s, false));
        }
        for (String s : new String[]{"tiger:county", "turn:lanes", "change:lanes", "placement"}) {
            IGNORED_KEYS.add(getPatternFor(s, true));
        }
    }

    private interface IStringSwitcher extends Function<String, String> {

        static IStringSwitcher combined(IStringSwitcher... switchers) {
            return key -> {
                for (IStringSwitcher switcher : switchers) {
                    final String newKey = switcher.apply(key);
                    if (!key.equals(newKey)) {
                        return newKey;
                    }
                }
                return key;
            };
        }
    }

    private static class StringSwitcher implements IStringSwitcher {

        private final String a;
        private final String b;
        private final Pattern pattern;

        StringSwitcher(String a, String b) {
            this.a = a;
            this.b = b;
            this.pattern = getPatternFor(a + '|' + b);
        }

        @Override
        public String apply(String text) {
            Matcher m = pattern.matcher(text);

            if (m.lookingAt()) {
                String leftRight = m.group(2).toLowerCase(Locale.ENGLISH);

                StringBuilder result = new StringBuilder();
                result.append(text.substring(0, m.start(2)))
                      .append(leftRight.equals(a) ? b : a)
                      .append(text.substring(m.end(2)));

                return result.toString();
            }
            return text;
        }
    }

    /**
     * Reverses a given tag.
     * @since 5787
     */
    public static final class TagSwitcher {

        private TagSwitcher() {
            // Hide implicit public constructor for utility class
        }

        /**
         * Reverses a given tag.
         * @param tag The tag to reverse
         * @return The reversed tag (is equal to <code>tag</code> if no change is needed)
         */
        public static Tag apply(final Tag tag) {
            return apply(tag.getKey(), tag.getValue());
        }

        /**
         * Reverses a given tag (key=value).
         * @param key The tag key
         * @param value The tag value
         * @return The reversed tag (is equal to <code>key=value</code> if no change is needed)
         */
        public static Tag apply(final String key, final String value) {
            String newKey = key;
            String newValue = value;

            if (key.startsWith("oneway") || key.endsWith("oneway")) {
                if (OsmUtils.isReversed(value)) {
                    newValue = OsmUtils.TRUE_VALUE;
                } else if (OsmUtils.isTrue(value)) {
                    newValue = OsmUtils.REVERSE_VALUE;
                }
                newKey = COMBINED_SWITCHERS.apply(key);
            } else if (key.startsWith("incline") || key.endsWith("incline")) {
                newValue = UP_DOWN.apply(value);
                if (newValue.equals(value)) {
                    newValue = invertNumber(value);
                }
            } else if (key.startsWith("direction") || key.endsWith("direction")) {
                newValue = COMBINED_SWITCHERS.apply(value);
            } else if (key.endsWith(":forward") || key.endsWith(":backward")) {
                // Change key but not left/right value (fix #8518)
                newKey = FORWARD_BACKWARD.apply(key);
            } else if (!ignoreKeyForCorrection(key)) {
                newKey = COMBINED_SWITCHERS.apply(key);
                newValue = COMBINED_SWITCHERS.apply(value);
            }
            return new Tag(newKey, newValue);
        }
    }

    private static final StringSwitcher FORWARD_BACKWARD = new StringSwitcher("forward", "backward");
    private static final StringSwitcher UP_DOWN = new StringSwitcher("up", "down");
    private static final IStringSwitcher COMBINED_SWITCHERS = IStringSwitcher.combined(
        new StringSwitcher("left", "right"),
        new StringSwitcher("forwards", "backwards"),
        new StringSwitcher("east", "west"),
        new StringSwitcher("north", "south"),
        FORWARD_BACKWARD, UP_DOWN
    );

    /**
     * Tests whether way can be reversed without semantic change, i.e., whether tags have to be changed.
     * Looks for keys like oneway, oneway:bicycle, cycleway:right:oneway, left/right.
     * @param way way to test
     * @return false if tags should be changed to keep semantic, true otherwise.
     */
    public static boolean isReversible(Way way) {
        for (Tag tag : TagCollection.from(way)) {
            if (!tag.equals(TagSwitcher.apply(tag))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the subset of irreversible ways.
     * @param ways all ways
     * @return the subset of irreversible ways
     * @see #isReversible(Way)
     */
    public static List<Way> irreversibleWays(List<Way> ways) {
        List<Way> newWays = new ArrayList<>(ways);
        for (Way way : ways) {
            if (isReversible(way)) {
                newWays.remove(way);
            }
        }
        return newWays;
    }

    /**
     * Inverts sign of a numeric value.
     * @param value numeric value
     * @return opposite numeric value
     */
    public static String invertNumber(String value) {
        Pattern pattern = Pattern.compile("^([+-]?)(\\d.*)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()) return value;
        String sign = matcher.group(1);
        String rest = matcher.group(2);
        sign = "-".equals(sign) ? "" : "-";
        return sign + rest;
    }

    static List<TagCorrection> getTagCorrections(Tagged way) {
        List<TagCorrection> tagCorrections = new ArrayList<>();
        for (Map.Entry<String, String> entry : way.getKeys().entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            Tag newTag = TagSwitcher.apply(key, value);
            String newKey = newTag.getKey();
            String newValue = newTag.getValue();

            boolean needsCorrection = !key.equals(newKey);
            if (way.get(newKey) != null && way.get(newKey).equals(newValue)) {
                needsCorrection = false;
            }
            if (!value.equals(newValue)) {
                needsCorrection = true;
            }

            if (needsCorrection) {
                tagCorrections.add(new TagCorrection(key, value, newKey, newValue));
            }
        }
        return tagCorrections;
    }

    static List<RoleCorrection> getRoleCorrections(Way oldway) {
        List<RoleCorrection> roleCorrections = new ArrayList<>();

        Collection<OsmPrimitive> referrers = oldway.getReferrers();
        for (OsmPrimitive referrer: referrers) {
            if (!(referrer instanceof Relation)) {
                continue;
            }
            Relation relation = (Relation) referrer;
            int position = 0;
            for (RelationMember member : relation.getMembers()) {
                if (!member.getMember().hasEqualSemanticAttributes(oldway)
                        || !member.hasRole()) {
                    position++;
                    continue;
                }

                final String newRole = COMBINED_SWITCHERS.apply(member.getRole());
                if (!member.getRole().equals(newRole)) {
                    roleCorrections.add(new RoleCorrection(relation, position, member, newRole));
                }

                position++;
            }
        }
        return roleCorrections;
    }

    static Map<OsmPrimitive, List<TagCorrection>> getTagCorrectionsMap(Way way) {
        Map<OsmPrimitive, List<TagCorrection>> tagCorrectionsMap = new HashMap<>();
        List<TagCorrection> tagCorrections = getTagCorrections(way);
        if (!tagCorrections.isEmpty()) {
            tagCorrectionsMap.put(way, tagCorrections);
        }
        for (Node node : way.getNodes()) {
            final List<TagCorrection> corrections = getTagCorrections(node);
            if (!corrections.isEmpty()) {
                tagCorrectionsMap.put(node, corrections);
            }
        }
        return tagCorrectionsMap;
    }

    @Override
    public Collection<Command> execute(Way oldway, Way way) throws UserCancelException {
        Map<OsmPrimitive, List<TagCorrection>> tagCorrectionsMap = getTagCorrectionsMap(way);

        Map<OsmPrimitive, List<RoleCorrection>> roleCorrectionMap = new HashMap<>();
        List<RoleCorrection> roleCorrections = getRoleCorrections(oldway);
        if (!roleCorrections.isEmpty()) {
            roleCorrectionMap.put(way, roleCorrections);
        }

        return applyCorrections(oldway.getDataSet(), tagCorrectionsMap, roleCorrectionMap,
                tr("When reversing this way, the following changes are suggested in order to maintain data consistency."));
    }

    private static boolean ignoreKeyForCorrection(String key) {
        for (Pattern ignoredKey : IGNORED_KEYS) {
            if (ignoredKey.matcher(key).matches()) {
                return true;
            }
        }
        return false;
    }
}
