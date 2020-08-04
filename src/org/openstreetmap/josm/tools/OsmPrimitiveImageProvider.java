// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.StyleElementList;
import org.openstreetmap.josm.gui.mappaint.styleelement.MapImage;
import org.openstreetmap.josm.gui.mappaint.styleelement.NodeElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;

/**
 * An {@link ImageProvider} for {@link org.openstreetmap.josm.data.osm.OsmPrimitive}
 *
 * @since 16838 (extracted from ImageProvider)
 */
public final class OsmPrimitiveImageProvider {

    private OsmPrimitiveImageProvider() {
        // private constructor
    }

    /**
     * Returns an {@link ImageIcon} for the given OSM object, at the specified size.
     * This is a slow operation.
     * @param primitive Object for which an icon shall be fetched. The icon is chosen based on tags.
     * @param options zero or more {@linkplain Options options}.
     * @return Icon for {@code primitive} that fits in cell or {@code null}.
     * @since 15889
     */
    public static ImageResource getResource(OsmPrimitive primitive, Collection<Options> options) {
        // Check if the current styles have special icon for tagged objects.
        if (primitive.isTagged()) {
            ImageResource icon = getResourceFromMapPaintStyles(primitive, options);
            if (icon != null) {
                return icon;
            }
        }

        // Check if the presets have icons for nodes/relations.
        if (primitive.isTagged() && (!options.contains(Options.NO_WAY_PRESETS) || OsmPrimitiveType.WAY != primitive.getType())) {
            final Optional<ImageResource> icon = TaggingPresets.getMatchingPresets(primitive).stream()
                    .sorted(Comparator.comparing(p -> (p.iconName != null && p.iconName.contains("multipolygon"))
                            || p.types == null || p.types.isEmpty() ? Integer.MAX_VALUE : p.types.size()))
                    .map(TaggingPreset::getImageResource)
                    .filter(Objects::nonNull)
                    .findFirst();
            if (icon.isPresent()) {
                return icon.get();
            }
        }

        // Use generic default icon.
        return options.contains(Options.NO_DEFAULT)
                ? null
                : new ImageProvider("data", primitive.getDisplayType().getAPIName()).getResource();
    }

    /**
     * Computes a new padded icon for the given tagged primitive, using map paint styles.
     * This is a slow operation.
     * @param primitive tagged OSM primitive
     * @param options zero or more {@linkplain Options options}.
     * @return a new padded icon for the given tagged primitive, or null
     */
    private static ImageResource getResourceFromMapPaintStyles(OsmPrimitive primitive, Collection<Options> options) {
        Pair<StyleElementList, Range> nodeStyles;
        DataSet ds = primitive.getDataSet();
        if (ds != null) {
            ds.getReadLock().lock();
        }
        try {
            nodeStyles = MapPaintStyles.getStyles().generateStyles(primitive, 100, false);
        } finally {
            if (ds != null) {
                ds.getReadLock().unlock();
            }
        }
        for (StyleElement style : nodeStyles.a) {
            if (style instanceof NodeElement) {
                NodeElement nodeStyle = (NodeElement) style;
                MapImage icon = nodeStyle.mapImage;
                if (icon != null && icon.getImageResource() != null &&
                        (icon.name == null || !options.contains(Options.NO_DEPRECATED) || !icon.name.contains("deprecated"))) {
                    return icon.getImageResource();
                }
            }
        }
        return null;
    }

    /**
     * Searches for an icon for the given key/value and primitiveType
     * @param key The tag key
     * @param value The tag value
     * @param primitiveType The type of the primitive
     * @return an icon for the given key/value and primitiveType
     */
    public static Optional<ImageResource> getResource(String key, String value, OsmPrimitiveType primitiveType) {
        final OsmPrimitive virtual = primitiveType
                .newInstance(0, false);
        if (virtual instanceof INode) {
            ((INode) virtual).setCoor(LatLon.ZERO);
        }
        virtual.put(key, value);
        try {
            final ImageResource padded = getResource(virtual, EnumSet.of(Options.NO_DEFAULT, Options.NO_DEPRECATED));
            return Optional.ofNullable(padded);
        } catch (Exception e) {
            Logging.warn("Failed to find icon for {0} {1}={2}", virtual.getType(), key, value);
            Logging.warn(e);
            return Optional.empty();
        }
    }

    /**
     * Options used in {@link #getResource(OsmPrimitive, Collection)}.
     * @since 15889
     */
    public enum Options {
        /**
         * Exclude icon indicating deprecated tag usage.
         */
        NO_DEPRECATED,
        /**
         * Exclude default icon for {@link OsmPrimitiveType} from {@link ImageProvider#get(OsmPrimitiveType)}
         */
        NO_DEFAULT,
        /**
         * Exclude tagging preset icons.
         */
        NO_PRESETS,
        /**
         * Exclude tagging preset icons for {@linkplain OsmPrimitiveType#WAY ways}.
         */
        NO_WAY_PRESETS;

        static final Collection<Options> DEFAULT = Collections.singleton(Options.NO_WAY_PRESETS);
    }
}
