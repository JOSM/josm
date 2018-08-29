// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor.conversion;

import static org.openstreetmap.josm.tools.I18n.trc;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Abstract base class for {@link ICoordinateFormat} implementations.
 * @since 12735
 */
public abstract class AbstractCoordinateFormat implements ICoordinateFormat {

    protected final String id;
    protected final String displayName;

    /** The normal number format for server precision coordinates */
    protected static final DecimalFormat cDdFormatter = newUnlocalizedDecimalFormat("###0.0######");
    /** Character denoting South, as string */
    protected static final String SOUTH = trc("compass", "S");
    /** Character denoting North, as string */
    protected static final String NORTH = trc("compass", "N");
    /** Character denoting West, as string */
    protected static final String WEST = trc("compass", "W");
    /** Character denoting East, as string */
    protected static final String EAST = trc("compass", "E");

    protected AbstractCoordinateFormat(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /**
     * Creates a new unlocalized {@link DecimalFormat}.
     * By not using the localized decimal separator, we can present a comma separated list of coordinates.
     * @param pattern decimal format pattern
     * @return {@code DecimalFormat} using dot as decimal separator
     * @see DecimalFormat#applyPattern
     * @since 14203
     */
    public static DecimalFormat newUnlocalizedDecimalFormat(String pattern) {
        DecimalFormat format = (DecimalFormat) NumberFormat.getInstance(Locale.UK);
        format.applyPattern(pattern);
        return format;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
