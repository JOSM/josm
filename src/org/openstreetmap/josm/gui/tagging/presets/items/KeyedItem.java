// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
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
    protected static final Map<String, String> LAST_VALUES = new HashMap<>();

    public String key;
    /** The text to display */
    public String text;
    /** The context used for translating {@link #text} */
    public String text_context;
    public String match = getDefaultMatch().getValue();

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

    protected static class Usage {
        public SortedSet<String> values;
        private boolean hadKeys;
        private boolean hadEmpty;

        public boolean hasUniqueValue() {
            return values.size() == 1 && !hadEmpty;
        }

        public boolean unused() {
            return values.isEmpty();
        }

        public String getFirst() {
            return values.first();
        }

        public boolean hadKeys() {
            return hadKeys;
        }
    }

    protected static Usage determineTextUsage(Collection<OsmPrimitive> sel, String key) {
        Usage returnValue = new Usage();
        returnValue.values = new TreeSet<>();
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
        returnValue.values = new TreeSet<>();
        for (OsmPrimitive s : sel) {
            String booleanValue = OsmUtils.getNamedOsmBoolean(s.get(key));
            if (booleanValue != null) {
                returnValue.values.add(booleanValue);
            }
        }
        return returnValue;
    }

    public abstract MatchType getDefaultMatch();

    public abstract Collection<String> getValues();

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
