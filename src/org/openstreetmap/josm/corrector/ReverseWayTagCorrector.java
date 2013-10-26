// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;

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

    private static final Pattern getPatternFor(String s) {
        return getPatternFor(s, false);
    }

    private static final Pattern getPatternFor(String s, boolean exactMatch) {
        if (exactMatch) {
            return Pattern.compile("(^)(" + s + ")($)");
        } else {
            return Pattern.compile("(^|.*" + SEPARATOR + ")(" + s + ")(" + SEPARATOR + ".*|$)",
                    Pattern.CASE_INSENSITIVE);
        }
    }

    private static final Collection<Pattern> ignoredKeys = new ArrayList<Pattern>();
    static {
        for (String s : OsmPrimitive.getUninterestingKeys()) {
            ignoredKeys.add(getPatternFor(s));
        }
        for (String s : new String[]{"name", "ref", "tiger:county"}) {
            ignoredKeys.add(getPatternFor(s, false));
        }
        for (String s : new String[]{"tiger:county", "turn:lanes", "change:lanes", "placement"}) {
            ignoredKeys.add(getPatternFor(s, true));
        }
    }

    private static class StringSwitcher {

        private final String a;
        private final String b;
        private final Pattern pattern;

        public StringSwitcher(String a, String b) {
            this.a = a;
            this.b = b;
            this.pattern = getPatternFor(a + "|" + b);
        }

        public String apply(String text) {
            Matcher m = pattern.matcher(text);

            if (m.lookingAt()) {
                String leftRight = m.group(2).toLowerCase();

                StringBuilder result = new StringBuilder();
                result.append(text.substring(0, m.start(2)));
                result.append(leftRight.equals(a) ? b : a);
                result.append(text.substring(m.end(2)));

                return result.toString();
            }
            return text;
        }
    }

    /**
     * Reverses a given tag.
     * @since 5787
     */
    public static class TagSwitcher {

        /**
         * Reverses a given tag.
         * @param tag The tag to reverse
         * @return The reversed tag (is equal to <code>tag</code> if no change is needed)
         */
        public static final Tag apply(final Tag tag) {
            return apply(tag.getKey(), tag.getValue());
        }

        /**
         * Reverses a given tag (key=value).
         * @param key The tag key
         * @param value The tag value
         * @return The reversed tag (is equal to <code>key=value</code> if no change is needed)
         */
        public static final Tag apply(final String key, final String value) {
            String newKey = key;
            String newValue = value;

            if (key.startsWith("oneway") || key.endsWith("oneway")) {
                if (OsmUtils.isReversed(value)) {
                    newValue = OsmUtils.trueval;
                } else if (OsmUtils.isTrue(value)) {
                    newValue = OsmUtils.reverseval;
                }
            } else if (key.startsWith("incline") || key.endsWith("incline")
                    || key.startsWith("direction") || key.endsWith("direction")) {
                newValue = UP_DOWN.apply(value);
                if (newValue.equals(value)) {
                    newValue = invertNumber(value);
                }
            } else if (key.endsWith(":forward") || key.endsWith(":backward")) {
                // Change key but not left/right value (fix #8518)
                newKey = FORWARD_BACKWARD.apply(key);

            } else if (!ignoreKeyForCorrection(key)) {
                for (StringSwitcher prefixSuffixSwitcher : stringSwitchers) {
                    newKey = prefixSuffixSwitcher.apply(key);
                    if (!key.equals(newKey)) {
                        break;
                    }
                    newValue = prefixSuffixSwitcher.apply(value);
                    if (!value.equals(newValue)) {
                        break;
                    }
                }
            }
            return new Tag(newKey, newValue);
        }
    }

    private static final StringSwitcher FORWARD_BACKWARD = new StringSwitcher("forward", "backward");
    private static final StringSwitcher UP_DOWN = new StringSwitcher("up", "down");

    private static final StringSwitcher[] stringSwitchers = new StringSwitcher[] {
        new StringSwitcher("left", "right"),
        new StringSwitcher("forwards", "backwards"),
        new StringSwitcher("east", "west"),
        new StringSwitcher("north", "south"),
        FORWARD_BACKWARD, UP_DOWN
    };

    /**
     * Tests whether way can be reversed without semantic change, i.e., whether tags have to be changed.
     * Looks for keys like oneway, oneway:bicycle, cycleway:right:oneway, left/right.
     * @param way
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

    public static List<Way> irreversibleWays(List<Way> ways) {
        List<Way> newWays = new ArrayList<Way>(ways);
        for (Way way : ways) {
            if (isReversible(way)) {
                newWays.remove(way);
            }
        }
        return newWays;
    }

    public static String invertNumber(String value) {
        Pattern pattern = Pattern.compile("^([+-]?)(\\d.*)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()) return value;
        String sign = matcher.group(1);
        String rest = matcher.group(2);
        sign = sign.equals("-") ? "" : "-";
        return sign + rest;
    }

    @Override
    public Collection<Command> execute(Way oldway, Way way) throws UserCancelException {
        Map<OsmPrimitive, List<TagCorrection>> tagCorrectionsMap =
            new HashMap<OsmPrimitive, List<TagCorrection>>();

        List<TagCorrection> tagCorrections = new ArrayList<TagCorrection>();
        for (String key : way.keySet()) {
            String value = way.get(key);
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
        if (!tagCorrections.isEmpty()) {
            tagCorrectionsMap.put(way, tagCorrections);
        }

        Map<OsmPrimitive, List<RoleCorrection>> roleCorrectionMap =
            new HashMap<OsmPrimitive, List<RoleCorrection>>();
        List<RoleCorrection> roleCorrections = new ArrayList<RoleCorrection>();

        Collection<OsmPrimitive> referrers = oldway.getReferrers();
        for (OsmPrimitive referrer: referrers) {
            if (! (referrer instanceof Relation)) {
                continue;
            }
            Relation relation = (Relation)referrer;
            int position = 0;
            for (RelationMember member : relation.getMembers()) {
                if (!member.getMember().hasEqualSemanticAttributes(oldway)
                        || !member.hasRole()) {
                    position++;
                    continue;
                }

                boolean found = false;
                String newRole = null;
                for (StringSwitcher prefixSuffixSwitcher : stringSwitchers) {
                    newRole = prefixSuffixSwitcher.apply(member.getRole());
                    if (!newRole.equals(member.getRole())) {
                        found = true;
                        break;
                    }
                }

                if (found) {
                    roleCorrections.add(new RoleCorrection(relation, position, member, newRole));
                }

                position++;
            }
        }
        if (!roleCorrections.isEmpty()) {
            roleCorrectionMap.put(way, roleCorrections);
        }

        return applyCorrections(tagCorrectionsMap, roleCorrectionMap,
                tr("When reversing this way, the following changes are suggested in order to maintain data consistency."));
    }

    private static boolean ignoreKeyForCorrection(String key) {
        for (Pattern ignoredKey : ignoredKeys) {
            if (ignoredKey.matcher(key).matches()) {
                return true;
            }
        }
        return false;
    }
}
