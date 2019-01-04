// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;

/**
 * Preset item associated to an OSM key.
 */
public abstract class KeyedItem extends TaggingPresetItem {

    /** Translatation of "&lt;different&gt;". Use in combo boxes to display an entry matching several different values. */
    protected static final String DIFFERENT = tr("<different>");

    protected static final BooleanProperty PROP_FILL_DEFAULT = new BooleanProperty("taggingpreset.fill-default-for-tagged-primitives", false);

    /** Last value of each key used in presets, used for prefilling corresponding fields */
    static final Map<String, String> LAST_VALUES = new HashMap<>();

    /** This specifies the property key that will be modified by the item. */
    public String key; // NOSONAR
    /** The text to display */
    public String text; // NOSONAR
    /** The context used for translating {@link #text} */
    public String text_context; // NOSONAR
    /**
     * Allows to change the matching process, i.e., determining whether the tags of an OSM object fit into this preset.
     * If a preset fits then it is linked in the Tags/Membership dialog.<ul>
     * <li>none: neutral, i.e., do not consider this item for matching</li>
     * <li>key: positive if key matches, neutral otherwise</li>
     * <li>key!: positive if key matches, negative otherwise</li>
     * <li>keyvalue: positive if key and value matches, neutral otherwise</li>
     * <li>keyvalue!: positive if key and value matches, negative otherwise</li></ul>
     * Note that for a match, at least one positive and no negative is required.
     * Default is "keyvalue!" for {@link Key} and "none" for {@link Text}, {@link Combo}, {@link MultiSelect} and {@link Check}.
     */
    public String match = getDefaultMatch().getValue(); // NOSONAR

    /**
     * Enum denoting how a match (see {@link TaggingPresetItem#matches}) is performed.
     */
    protected enum MatchType {

        /** Neutral, i.e., do not consider this item for matching. */
        NONE("none"),
        /** Positive if key matches, neutral otherwise. */
        KEY("key"),
        /** Positive if key matches, negative otherwise. */
        KEY_REQUIRED("key!"),
        /** Positive if key and value matches, neutral otherwise. */
        KEY_VALUE("keyvalue"),
        /** Positive if key and value matches, negative otherwise. */
        KEY_VALUE_REQUIRED("keyvalue!");

        private final String value;

        MatchType(String value) {
            this.value = value;
        }

        /**
         * Replies the associated textual value.
         * @return the associated textual value
         */
        public String getValue() {
            return value;
        }

        /**
         * Determines the {@code MatchType} for the given textual value.
         * @param type the textual value
         * @return the {@code MatchType} for the given textual value
         */
        public static MatchType ofString(String type) {
            for (MatchType i : EnumSet.allOf(MatchType.class)) {
                if (i.getValue().equals(type))
                    return i;
            }
            throw new IllegalArgumentException(type + " is not allowed");
        }
    }

    /**
     * Usage information on a key
     */
    protected static class Usage {
        /**
         * A set of values that were used for this key.
         */
        public final SortedSet<String> values = new TreeSet<>(); // NOSONAR
        private boolean hadKeys;
        private boolean hadEmpty;

        /**
         * Check if there is exactly one value for this key.
         * @return <code>true</code> if there was exactly one value.
         */
        public boolean hasUniqueValue() {
            return values.size() == 1 && !hadEmpty;
        }

        /**
         * Check if this key was not used in any primitive
         * @return <code>true</code> if it was unused.
         */
        public boolean unused() {
            return values.isEmpty();
        }

        /**
         * Get the first value available.
         * @return The first value
         * @throws NoSuchElementException if there is no such value.
         */
        public String getFirst() {
            return values.first();
        }

        /**
         * Check if we encountered any primitive that had any keys
         * @return <code>true</code> if any of the primtives had any tags.
         */
        public boolean hadKeys() {
            return hadKeys;
        }
    }

    protected static Usage determineTextUsage(Collection<OsmPrimitive> sel, String key) {
        Usage returnValue = new Usage();
        for (OsmPrimitive s : sel) {
            String v = s.get(key);
            if (v != null) {
                returnValue.values.add(v);
            } else {
                returnValue.hadEmpty = true;
            }
            if (s.hasKeys()) {
                returnValue.hadKeys = true;
            }
        }
        return returnValue;
    }

    protected static Usage determineBooleanUsage(Collection<OsmPrimitive> sel, String key) {
        Usage returnValue = new Usage();
        for (OsmPrimitive s : sel) {
            String booleanValue = OsmUtils.getNamedOsmBoolean(s.get(key));
            if (booleanValue != null) {
                returnValue.values.add(booleanValue);
            }
        }
        return returnValue;
    }

    /**
     * Determines whether key or key+value are required.
     * @return whether key or key+value are required
     */
    public boolean isKeyRequired() {
        final MatchType type = MatchType.ofString(match);
        return MatchType.KEY_REQUIRED.equals(type) || MatchType.KEY_VALUE_REQUIRED.equals(type);
    }

    /**
     * Returns the default match.
     * @return the default match
     */
    public abstract MatchType getDefaultMatch();

    /**
     * Returns the list of values.
     * @return the list of values
     */
    public abstract Collection<String> getValues();

    protected String getKeyTooltipText() {
        return tr("This corresponds to the key ''{0}''", key);
    }

    @Override
    protected Boolean matches(Map<String, String> tags) {
        switch (MatchType.ofString(match)) {
        case NONE:
            return null;
        case KEY:
            return tags.containsKey(key) ? Boolean.TRUE : null;
        case KEY_REQUIRED:
            return tags.containsKey(key);
        case KEY_VALUE:
            return tags.containsKey(key) && getValues().contains(tags.get(key)) ? Boolean.TRUE : null;
        case KEY_VALUE_REQUIRED:
            return tags.containsKey(key) && getValues().contains(tags.get(key));
        default:
            throw new IllegalStateException();
        }
    }

    @Override
    public String toString() {
        return "KeyedItem [key=" + key + ", text=" + text
                + ", text_context=" + text_context + ", match=" + match
                + ']';
    }
}
