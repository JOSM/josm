// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.StructUtils.StructEntry;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;

/**
 * Collection of utility methods for tag conflict resolution
 *
 */
public final class TagConflictResolutionUtil {

    /** The OSM key 'source' */
    private static final String KEY_SOURCE = "source";

    /** The group identifier for French Cadastre choices */
    private static final String GRP_FR_CADASTRE = "FR:cadastre";

    /** The group identifier for Canadian CANVEC choices */
    private static final String GRP_CA_CANVEC = "CA:canvec";

    /**
     * Default preferences for the list of AutomaticCombine tag conflict resolvers.
     */
    private static final Collection<AutomaticCombine> defaultAutomaticTagConflictCombines = Arrays.asList(
        new AutomaticCombine("tiger:tlid", "US TIGER tlid", false, ":", "Integer"),
        new AutomaticCombine("tiger:(?!tlid$).*", "US TIGER not tlid", true, ":", "String")
    );

    /**
     * Default preferences for the list of AutomaticChoice tag conflict resolvers.
     */
    private static final Collection<AutomaticChoice> defaultAutomaticTagConflictChoices = Arrays.asList(
        /* "source" "FR:cadastre" - https://wiki.openstreetmap.org/wiki/FR:WikiProject_France/Cadastre
         * List of choices for the "source" tag of data exported from the French cadastre,
         * which ends by the exported year generating many conflicts.
         * The generated score begins with the year number to select the most recent one.
         */
        new AutomaticChoice(KEY_SOURCE, GRP_FR_CADASTRE, "FR cadastre source, manual value", true,
                "cadastre", "0"),
        new AutomaticChoice(KEY_SOURCE, GRP_FR_CADASTRE, "FR cadastre source, initial format", true,
                "extraction vectorielle v1 cadastre-dgi-fr source : Direction G[eé]n[eé]rale des Imp[oô]ts"
                + " - Cadas\\. Mise [aà] jour : (2[0-9]{3})",
                "$1 1"),
        new AutomaticChoice(KEY_SOURCE, GRP_FR_CADASTRE, "FR cadastre source, last format", true,
                "(?:cadastre-dgi-fr source : )?Direction G[eé]n[eé]rale des (?:Imp[oô]ts|Finances Publiques)"
                + " - Cadas(?:tre)?(?:\\.| ;) [Mm]ise [aà] jour : (2[0-9]{3})",
                "$1 2"),
        /* "source" "CA:canvec" - https://wiki.openstreetmap.org/wiki/CanVec
         * List of choices for the "source" tag of data exported from Natural Resources Canada (NRCan)
         */
        new AutomaticChoice(KEY_SOURCE, GRP_CA_CANVEC, "CA canvec source, initial value", true,
                "CanVec_Import_2009", "00"),
        new AutomaticChoice(KEY_SOURCE, GRP_CA_CANVEC, "CA canvec source, 4.0/6.0 value", true,
                "CanVec ([1-9]).0 - NRCan", "0$1"),
        new AutomaticChoice(KEY_SOURCE, GRP_CA_CANVEC, "CA canvec source, 7.0/8.0 value", true,
                "NRCan-CanVec-([1-9]).0", "0$1"),
        new AutomaticChoice(KEY_SOURCE, GRP_CA_CANVEC, "CA canvec source, 10.0/12.0 value", true,
                "NRCan-CanVec-(1[012]).0", "$1")
    );

    private static volatile Collection<AutomaticTagConflictResolver> automaticTagConflictResolvers;

    private TagConflictResolutionUtil() {
        // no constructor, just static utility methods
    }

    /**
     * Normalizes the tags in the tag collection <code>tc</code> before resolving tag conflicts.
     *
     * Removes irrelevant tags like "created_by".
     *
     * For tags which are not present on at least one of the merged nodes, the empty value ""
     * is added to the list of values for this tag, but only if there are at least two
     * primitives with tags, and at least one tagged primitive do not have this tag.
     *
     * @param tc the tag collection
     * @param merged the collection of merged primitives
     */
    public static void normalizeTagCollectionBeforeEditing(TagCollection tc, Collection<? extends OsmPrimitive> merged) {
        // remove irrelevant tags
        //
        for (String key : OsmPrimitive.getDiscardableKeys()) {
            tc.removeByKey(key);
        }

        Collection<OsmPrimitive> taggedPrimitives = new ArrayList<>();
        for (OsmPrimitive p: merged) {
            if (p.isTagged()) {
                taggedPrimitives.add(p);
            }
        }
        if (taggedPrimitives.size() <= 1)
            return;

        for (String key: tc.getKeys()) {
            // make sure the empty value is in the tag set if a tag is not present
            // on all merged nodes
            //
            for (OsmPrimitive p: taggedPrimitives) {
                if (p.get(key) == null) {
                    tc.add(new Tag(key, "")); // add a tag with key and empty value
                }
            }
        }
    }

    /**
     * Completes tags in the tag collection <code>tc</code> with the empty value
     * for each tag. If the empty value is present the tag conflict resolution dialog
     * will offer an option for removing the tag and not only options for selecting
     * one of the current values of the tag.
     *
     * @param tc the tag collection
     */
    public static void completeTagCollectionForEditing(TagCollection tc) {
        for (String key: tc.getKeys()) {
            // make sure the empty value is in the tag set such that we can delete the tag
            // in the conflict dialog if necessary
            tc.add(new Tag(key, ""));
        }
    }

    /**
     * Automatically resolve some tag conflicts.
     * The list of automatic resolution is taken from the preferences.
     * @param tc the tag collection
     * @since 11606
     */
    public static void applyAutomaticTagConflictResolution(TagCollection tc) {
        try {
            applyAutomaticTagConflictResolution(tc, getAutomaticTagConflictResolvers());
        } catch (JosmRuntimeException e) {
            Logging.log(Logging.LEVEL_ERROR, "Unable to automatically resolve tag conflicts", e);
        }
    }

    /**
     * Get the AutomaticTagConflictResolvers configured in the Preferences or the default ones.
     * @return the configured AutomaticTagConflictResolvers.
     * @since 11606
     */
    public static Collection<AutomaticTagConflictResolver> getAutomaticTagConflictResolvers() {
        if (automaticTagConflictResolvers == null) {
            Collection<AutomaticCombine> automaticTagConflictCombines = StructUtils.getListOfStructs(
                            Config.getPref(),
                            "automatic-tag-conflict-resolution.combine",
                            defaultAutomaticTagConflictCombines, AutomaticCombine.class);
            Collection<AutomaticChoiceGroup> automaticTagConflictChoiceGroups =
                    AutomaticChoiceGroup.groupChoices(StructUtils.getListOfStructs(
                            Config.getPref(),
                            "automatic-tag-conflict-resolution.choice",
                            defaultAutomaticTagConflictChoices, AutomaticChoice.class));
            // Use a tmp variable to fully construct the collection before setting
            // the volatile variable automaticTagConflictResolvers.
            ArrayList<AutomaticTagConflictResolver> tmp = new ArrayList<>();
            tmp.addAll(automaticTagConflictCombines);
            tmp.addAll(automaticTagConflictChoiceGroups);
            automaticTagConflictResolvers = tmp;
        }
        return Collections.unmodifiableCollection(automaticTagConflictResolvers);
    }

    /**
     * An automatic tag conflict resolver interface.
     * @since 11606
     */
    interface AutomaticTagConflictResolver {
        /**
         * Check if this resolution apply to the given Tag key.
         * @param key The Tag key to match.
         * @return true if this automatic resolution apply to the given Tag key.
         */
        boolean matchesKey(String key);

        /**
         * Try to resolve a conflict between a set of values for a Tag
         * @param values the set of conflicting values for the Tag.
         * @return the resolved value or null if resolution was not possible.
         */
        String resolve(Set<String> values);
    }

    /**
     * Automatically resolve some given conflicts using the given resolvers.
     * @param tc the tag collection.
     * @param resolvers the list of automatic tag conflict resolvers to apply.
     * @since 11606
     */
    public static void applyAutomaticTagConflictResolution(TagCollection tc,
            Collection<AutomaticTagConflictResolver> resolvers) {
        for (String key: tc.getKeysWithMultipleValues()) {
            for (AutomaticTagConflictResolver resolver : resolvers) {
                try {
                    if (resolver.matchesKey(key)) {
                        String result = resolver.resolve(tc.getValues(key));
                        if (result != null) {
                            tc.setUniqueForKey(key, result);
                            break;
                        }
                    }
                } catch (PatternSyntaxException e) {
                    // Can happen if a particular resolver has an invalid regular expression pattern
                    // but it should not stop the other automatic tag conflict resolution.
                    Logging.error(e);
                }
            }
        }
    }

    /**
     * Preference for automatic tag-conflict resolver by combining the tag values using a separator.
     * @since 11606
     */
    public static class AutomaticCombine implements AutomaticTagConflictResolver {

        /** The Tag key to match */
        @StructEntry public String key;

        /** A free description */
        @StructEntry public String description = "";

        /** If regular expression must be used to match the Tag key or the value. */
        @StructEntry public boolean isRegex;

        /** The separator to use to combine the values. */
        @StructEntry public String separator = ";";

        /** If the combined values must be sorted.
         * Possible values:
         * <ul>
         * <li> Integer - Sort using Integer natural order.</li>
         * <li> String - Sort using String natural order.</li>
         * <li> * - No ordering.</li>
         * </ul>
         */
        @StructEntry public String sort;

        /** Default constructor. */
        public AutomaticCombine() {
            // needed for instantiation from Preferences
        }

        /** Instantiate an automatic tag-conflict resolver which combining the values using a separator.
         * @param key The Tag key to match.
         * @param description A free description.
         * @param isRegex If regular expression must be used to match the Tag key or the value.
         * @param separator The separator to use to combine the values.
         * @param sort If the combined values must be sorted.
         */
        public AutomaticCombine(String key, String description, boolean isRegex, String separator, String sort) {
            this.key = key;
            this.description = description;
            this.isRegex = isRegex;
            this.separator = separator;
            this.sort = sort;
        }

        @Override
        public boolean matchesKey(String k) {
            if (isRegex) {
                return Pattern.matches(this.key, k);
            } else {
                return this.key.equals(k);
            }
        }

        Set<String> instantiateSortedSet() {
            if ("String".equals(sort)) {
                return new TreeSet<>();
            } else if ("Integer".equals(sort)) {
                return new TreeSet<>((String v1, String v2) -> Long.valueOf(v1).compareTo(Long.valueOf(v2)));
            } else {
                return new LinkedHashSet<>();
            }
        }

        @Override
        public String resolve(Set<String> values) {
            Set<String> results = instantiateSortedSet();
            for (String value: values) {
                for (String part: value.split(Pattern.quote(separator))) {
                    results.add(part);
                }
            }
            return String.join(separator, results);
        }

        @Override
        public String toString() {
            return AutomaticCombine.class.getSimpleName()
                    + "(key='" + key + "', description='" + description + "', isRegex="
                    + isRegex + ", separator='" + separator + "', sort='" + sort + "')";
        }
    }

    /**
     * Preference for a particular choice from a group for automatic tag conflict resolution.
     * {@code AutomaticChoice}s are grouped into {@link AutomaticChoiceGroup}.
     * @since 11606
     */
    public static class AutomaticChoice {

        /** The Tag key to match. */
        @StructEntry public String key;

        /** The name of the {link AutomaticChoice group} this choice belongs to. */
        @StructEntry public String group;

        /** A free description. */
        @StructEntry public String description = "";

        /** If regular expression must be used to match the Tag key or the value. */
        @StructEntry public boolean isRegex;

        /** The Tag value to match. */
        @StructEntry public String value;

        /**
         * The score to give to this choice in order to choose the best value
         * Natural String ordering is used to identify the best score.
         */
        @StructEntry public String score;

        /** Default constructor. */
        public AutomaticChoice() {
            // needed for instantiation from Preferences
        }

        /**
         * Instantiate a particular choice from a group for automatic tag conflict resolution.
         * @param key The Tag key to match.
         * @param group The name of the {link AutomaticChoice group} this choice belongs to.
         * @param description A free description.
         * @param isRegex If regular expression must be used to match the Tag key or the value.
         * @param value The Tag value to match.
         * @param score The score to give to this choice in order to choose the best value.
         */
        public AutomaticChoice(String key, String group, String description, boolean isRegex, String value, String score) {
            this.key = key;
            this.group = group;
            this.description = description;
            this.isRegex = isRegex;
            this.value = value;
            this.score = score;
        }

        /**
         * Check if this choice match the given Tag value.
         * @param v the Tag value to match.
         * @return true if this choice correspond to the given tag value.
         */
        public boolean matchesValue(String v) {
            if (isRegex) {
                return Pattern.matches(this.value, v);
            } else {
                return this.value.equals(v);
            }
        }

        /**
         * Return the score associated to this choice for the given Tag value.
         * For the result to be valid the given tag value must {@link #matchesValue(String) match} this choice.
         * @param v the Tag value of which to get the score.
         * @return the score associated to the given Tag value.
         * @throws PatternSyntaxException if the regular expression syntax is invalid
         */
        public String computeScoreFromValue(String v) {
            if (isRegex) {
                return v.replaceAll("^" + this.value + "$", this.score);
            } else {
                return this.score;
            }
        }

        @Override
        public String toString() {
            return AutomaticChoice.class.getSimpleName()
                    + "(key='" + key + "', group='" + group + "', description='" + description
                    + "', isRegex=" + isRegex + ", value='" + value + "', score='" + score + "')";
        }
    }

    /**
     * Preference for an automatic tag conflict resolver which choose from
     * a group of possible {@link AutomaticChoice choice} values.
     * @since 11606
     */
    public static class AutomaticChoiceGroup implements AutomaticTagConflictResolver {

        /** The Tag key to match. */
        @StructEntry public String key;

        /** The name of the group. */
        final String group;

        /** If regular expression must be used to match the Tag key. */
        @StructEntry public boolean isRegex;

        /** The list of choice to choose from. */
        final List<AutomaticChoice> choices;

        /** Instantiate an automatic tag conflict resolver which choose from
         * a given list of {@link AutomaticChoice choice} values.
         *
         * @param key The Tag key to match.
         * @param group The name of the group.
         * @param isRegex If regular expression must be used to match the Tag key.
         * @param choices The list of choice to choose from.
         */
        public AutomaticChoiceGroup(String key, String group, boolean isRegex, List<AutomaticChoice> choices) {
            this.key = key;
            this.group = group;
            this.isRegex = isRegex;
            this.choices = choices;
        }

        /**
         * Group a given list of {@link AutomaticChoice} by the Tag key and the choice group name.
         * @param choices the list of {@link AutomaticChoice choices} to group.
         * @return the resulting list of group.
         */
        public static Collection<AutomaticChoiceGroup> groupChoices(Collection<AutomaticChoice> choices) {
            HashMap<Pair<String, String>, AutomaticChoiceGroup> results = new HashMap<>();
            for (AutomaticChoice choice: choices) {
                Pair<String, String> id = new Pair<>(choice.key, choice.group);
                AutomaticChoiceGroup group = results.get(id);
                if (group == null) {
                    boolean isRegex = choice.isRegex && !Pattern.quote(choice.key).equals(choice.key);
                    group = new AutomaticChoiceGroup(choice.key, choice.group, isRegex, new ArrayList<>());
                    results.put(id, group);
                }
                group.choices.add(choice);
            }
            return results.values();
        }

        @Override
        public boolean matchesKey(String k) {
            if (isRegex) {
                return Pattern.matches(this.key, k);
            } else {
                return this.key.equals(k);
            }
        }

        @Override
        public String resolve(Set<String> values) {
            String bestScore = "";
            String bestValue = "";
            for (String value : values) {
                String score = null;
                for (AutomaticChoice choice : choices) {
                    if (choice.matchesValue(value)) {
                        score = choice.computeScoreFromValue(value);
                    }
                }
                if (score == null) {
                    // This value is not matched in this group
                    // so we can not choose from this group for this key.
                    return null;
                }
                if (score.compareTo(bestScore) >= 0) {
                    bestScore = score;
                    bestValue = value;
                }
            }
            return bestValue;
        }

        @Override
        public String toString() {
            Collection<String> stringChoices = choices.stream().map(AutomaticChoice::toString).collect(Collectors.toCollection(ArrayList::new));
            return AutomaticChoiceGroup.class.getSimpleName() + "(key='" + key + "', group='" + group +
                    "', isRegex=" + isRegex + ", choices=(\n  " + String.join(",\n  ", stringChoices) + "))";
        }
    }
}
