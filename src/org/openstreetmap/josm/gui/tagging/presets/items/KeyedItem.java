// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import javax.swing.JPopupMenu;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.dialogs.properties.HelpTagAction;
import org.openstreetmap.josm.gui.dialogs.properties.TaginfoAction;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;

/**
 * Preset item associated to an OSM key.
 */
public abstract class KeyedItem extends TextItem {

    /** The constant value {@code "<different>"}. */
    protected static final String DIFFERENT = "<different>";
    /** Translation of {@code "<different>"}. */
    public static final String DIFFERENT_I18N = tr("<different>");

    /** True if the default value should also be set on primitives that already have tags.  */
    protected static final BooleanProperty PROP_FILL_DEFAULT = new BooleanProperty("taggingpreset.fill-default-for-tagged-primitives", false);

    /** Last value of each key used in presets, used for prefilling corresponding fields */
    static final Map<String, String> LAST_VALUES = new HashMap<>();

    /** This specifies the property key that will be modified by the item. */
    public String key; // NOSONAR
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
     *
     * TODO merge with {@link org.openstreetmap.josm.data.osm.TagCollection}
     */
    public static class Usage {
        /** Usage count for all values used for this key */
        public final SortedMap<String, Integer> map = new TreeMap<>();
        private boolean hadKeys;
        private boolean hadEmpty;
        private int selectedCount;

        /**
         * Check if there is exactly one value for this key.
         * @return <code>true</code> if there was exactly one value.
         */
        public boolean hasUniqueValue() {
            return map.size() == 1 && !hadEmpty;
        }

        /**
         * Check if this key was not used in any primitive
         * @return <code>true</code> if it was unused.
         */
        public boolean unused() {
            return map.isEmpty();
        }

        /**
         * Get the first value available.
         * @return The first value
         * @throws NoSuchElementException if there is no such value.
         */
        public String getFirst() {
            return map.firstKey();
        }

        /**
         * Check if we encountered any primitive that had any keys
         * @return <code>true</code> if any of the primitives had any tags.
         */
        public boolean hadKeys() {
            return hadKeys;
        }

        /**
         * Returns the number of primitives selected.
         * @return the number of primitives selected.
         */
        public int getSelectedCount() {
            return selectedCount;
        }

        /**
         * Splits multiple values and adds their usage counts as single value.
         * <p>
         * A value of {@code regional;pizza} will increment the count of {@code regional} and of
         * {@code pizza}.
         * @param delimiter The delimiter used for splitting.
         * @return A new usage object with the new counts.
         */
        public Usage splitValues(String delimiter) {
            Usage usage = new Usage();
            usage.hadEmpty = hadEmpty;
            usage.hadKeys = hadKeys;
            usage.selectedCount = selectedCount;
            map.forEach((value, count) -> {
                for (String v : value.split(String.valueOf(delimiter), -1)) {
                    usage.map.merge(v, count, Integer::sum);
                }
            });
            return usage;
        }
    }

    /**
     * Computes the tag usage for the given key from the given primitives
     * @param sel the primitives
     * @param key the key
     * @return the tag usage
     */
    public static Usage determineTextUsage(Collection<OsmPrimitive> sel, String key) {
        Usage returnValue = new Usage();
        returnValue.selectedCount = sel.size();
        for (OsmPrimitive s : sel) {
            String v = s.get(key);
            if (v != null) {
                returnValue.map.merge(v, 1, Integer::sum);
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
        returnValue.selectedCount = sel.size();
        for (OsmPrimitive s : sel) {
            String booleanValue = OsmUtils.getNamedOsmBoolean(s.get(key));
            if (booleanValue != null) {
                returnValue.map.merge(booleanValue, 1, Integer::sum);
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
        return MatchType.KEY_REQUIRED == type || MatchType.KEY_VALUE_REQUIRED == type;
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
    public Boolean matches(Map<String, String> tags) {
        switch (MatchType.ofString(match)) {
        case NONE:
            return null; // NOSONAR
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

    protected JPopupMenu getPopupMenu() {
        Tag tag = new Tag(key, null);
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(tr("Key: {0}", key)).setEnabled(false);
        popupMenu.add(new HelpTagAction(() -> tag));
        TaginfoAction taginfoAction = new TaginfoAction(() -> tag, () -> null);
        popupMenu.add(taginfoAction.toTagHistoryAction());
        popupMenu.add(taginfoAction);
        return popupMenu;
    }

    @Override
    public String toString() {
        return "KeyedItem [key=" + key + ", text=" + text
                + ", text_context=" + text_context + ", match=" + match
                + ']';
    }
}
