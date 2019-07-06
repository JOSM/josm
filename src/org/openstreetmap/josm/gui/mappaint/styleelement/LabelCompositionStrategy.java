// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.LanguageInfo;

/**
 * <p>Provides an abstract parent class and three concrete sub classes for various
 * strategies on how to compose the text label which can be rendered close to a node
 * or within an area in an OSM map.</p>
 *
 * <p>The three strategies below support three rules for composing a label:
 * <ul>
 *   <li>{@link StaticLabelCompositionStrategy} - the label is given by a static text
 *   specified in the MapCSS style file</li>
 *
 *   <li>{@link TagLookupCompositionStrategy} - the label is given by the content of a
 *   tag whose name specified in the MapCSS style file</li>
 *
 *   <li>{@link DeriveLabelFromNameTagsCompositionStrategy} - the label is given by the value
 *   of one of the configured "name tags". The list of relevant name tags can be configured
 *   in the JOSM preferences
 *   see the preference options <code>mappaint.nameOrder</code> and <code>mappaint.nameComplementOrder</code>.</li>
 * </ul>
 * @since  3987 (creation)
 * @since 10599 (functional interface)
 */
@FunctionalInterface
public interface LabelCompositionStrategy {

    /**
     * Replies the text value to be rendered as label for the primitive {@code primitive}.
     *
     * @param primitive the primitive
     *
     * @return the text value to be rendered or null, if primitive is null or
     * if no suitable value could be composed
     */
    String compose(IPrimitive primitive);

    /**
     * Strategy where the label is given by a static text specified in the MapCSS style file.
     */
    class StaticLabelCompositionStrategy implements LabelCompositionStrategy {
        private final String defaultLabel;

        public StaticLabelCompositionStrategy(String defaultLabel) {
            this.defaultLabel = defaultLabel;
        }

        @Override
        public String compose(IPrimitive primitive) {
            return defaultLabel;
        }

        public String getDefaultLabel() {
            return defaultLabel;
        }

        @Override
        public String toString() {
            return '{' + getClass().getSimpleName() + " defaultLabel=" + defaultLabel + '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(defaultLabel);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            StaticLabelCompositionStrategy that = (StaticLabelCompositionStrategy) obj;
            return Objects.equals(defaultLabel, that.defaultLabel);
        }
    }

    /**
     * Strategy where the label is given by the content of a tag whose name specified in the MapCSS style file.
     */
    class TagLookupCompositionStrategy implements LabelCompositionStrategy {

        private final String defaultLabelTag;

        public TagLookupCompositionStrategy(String defaultLabelTag) {
            if (defaultLabelTag != null) {
                defaultLabelTag = defaultLabelTag.trim();
                if (defaultLabelTag.isEmpty()) {
                    defaultLabelTag = null;
                }
            }
            this.defaultLabelTag = defaultLabelTag;
        }

        @Override
        public String compose(IPrimitive primitive) {
            if (defaultLabelTag == null) return null;
            if (primitive == null) return null;
            return primitive.get(defaultLabelTag);
        }

        public String getDefaultLabelTag() {
            return defaultLabelTag;
        }

        @Override
        public String toString() {
            return '{' + getClass().getSimpleName() + " defaultLabelTag=" + defaultLabelTag + '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(defaultLabelTag);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TagLookupCompositionStrategy that = (TagLookupCompositionStrategy) obj;
            return Objects.equals(defaultLabelTag, that.defaultLabelTag);
        }
    }

    /**
     * Strategy where the label is given by the value of one of the configured "name tags".
     * The list of relevant name tags can be configured in the JOSM preferences
     * see the preference options <code>mappaint.nameOrder</code> and <code>mappaint.nameComplementOrder</code>
     */
    class DeriveLabelFromNameTagsCompositionStrategy implements LabelCompositionStrategy, PreferenceChangedListener {

        /**
         * The list of default name tags from which a label candidate is derived.
         */
        private static final String[] DEFAULT_NAME_TAGS = {
            "name:" + LanguageInfo.getJOSMLocaleCode(),
            "name",
            "int_name",
            "distance",
            "ref",
            "operator",
            "brand",
            "addr:housenumber"
        };

        /**
         * The list of default name complement tags from which a label candidate is derived.
         */
        private static final String[] DEFAULT_NAME_COMPLEMENT_TAGS = {
            "capacity"
        };

        private List<String> nameTags = new ArrayList<>();
        private List<String> nameComplementTags = new ArrayList<>();

        /**
         * <p>Creates the strategy and initializes its name tags from the preferences.</p>
         */
        public DeriveLabelFromNameTagsCompositionStrategy() {
            Config.getPref().addPreferenceChangeListener(this);
            initNameTagsFromPreferences();
        }

        private static List<String> buildNameTags(List<String> nameTags) {
            List<String> result = new ArrayList<>();
            if (nameTags != null) {
                for (String tag: nameTags) {
                    if (tag == null) {
                        continue;
                    }
                    tag = tag.trim();
                    if (tag.isEmpty()) {
                        continue;
                    }
                    result.add(tag);
                }
            }
            return result;
        }

        /**
         * Sets the name tags to be looked up in order to build up the label.
         *
         * @param nameTags the name tags. null values are ignored.
         */
        public void setNameTags(List<String> nameTags) {
            this.nameTags = buildNameTags(nameTags);
        }

        /**
         * Sets the name complement tags to be looked up in order to build up the label.
         *
         * @param nameComplementTags the name complement tags. null values are ignored.
         * @since 6541
         */
        public void setNameComplementTags(List<String> nameComplementTags) {
            this.nameComplementTags = buildNameTags(nameComplementTags);
        }

        /**
         * Replies an unmodifiable list of the name tags used to compose the label.
         *
         * @return the list of name tags
         */
        public List<String> getNameTags() {
            return Collections.unmodifiableList(nameTags);
        }

        /**
         * Replies an unmodifiable list of the name complement tags used to compose the label.
         *
         * @return the list of name complement tags
         * @since 6541
         */
        public List<String> getNameComplementTags() {
            return Collections.unmodifiableList(nameComplementTags);
        }

        /**
         * Initializes the name tags to use from a list of default name tags (see
         * {@link #DEFAULT_NAME_TAGS} and {@link #DEFAULT_NAME_COMPLEMENT_TAGS})
         * and from name tags configured in the preferences using the keys
         * <code>mappaint.nameOrder</code> and <code>mappaint.nameComplementOrder</code>.
         */
        public final void initNameTagsFromPreferences() {
            if (Config.getPref() == null) {
                this.nameTags = new ArrayList<>(Arrays.asList(DEFAULT_NAME_TAGS));
                this.nameComplementTags = new ArrayList<>(Arrays.asList(DEFAULT_NAME_COMPLEMENT_TAGS));
            } else {
                this.nameTags = new ArrayList<>(
                        Config.getPref().getList("mappaint.nameOrder", Arrays.asList(DEFAULT_NAME_TAGS))
                );
                this.nameComplementTags = new ArrayList<>(
                        Config.getPref().getList("mappaint.nameComplementOrder", Arrays.asList(DEFAULT_NAME_COMPLEMENT_TAGS))
                );
            }
        }

        private String getPrimitiveName(IPrimitive n) {
            StringBuilder name = new StringBuilder();
            if (!n.hasKeys()) return null;
            for (String rn : nameTags) {
                String val = n.get(rn);
                if (val != null) {
                    name.append(val);
                    break;
                }
            }
            for (String rn : nameComplementTags) {
                String comp = n.get(rn);
                if (comp != null) {
                    if (name.length() == 0) {
                        name.append(comp);
                    } else {
                        name.append(" (").append(comp).append(')');
                    }
                    break;
                }
            }
            return name.toString();
        }

        @Override
        public String compose(IPrimitive primitive) {
            if (primitive == null) return null;
            return getPrimitiveName(primitive);
        }

        @Override
        public String toString() {
            return '{' + getClass().getSimpleName() + '}';
        }

        @Override
        public void preferenceChanged(PreferenceChangeEvent e) {
            if (e.getKey() != null && e.getKey().startsWith("mappaint.name")) {
                initNameTagsFromPreferences();
            }
        }
    }
}
