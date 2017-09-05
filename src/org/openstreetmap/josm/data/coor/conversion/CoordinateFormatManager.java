// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor.conversion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Class that manages the available coordinate formats.
 * @since 12735
 */
public final class CoordinateFormatManager {

    private static final List<ICoordinateFormat> formats = new ArrayList<>();

    static {
        registerCoordinateFormat(DecimalDegreesCoordinateFormat.INSTANCE);
        registerCoordinateFormat(DMSCoordinateFormat.INSTANCE);
        registerCoordinateFormat(NauticalCoordinateFormat.INSTANCE);
        registerCoordinateFormat(ProjectedCoordinateFormat.INSTANCE);
    }

    private CoordinateFormatManager() {
        // hide constructor
    }

    /**
     * Register a coordinate format.
     * <p>
     * It will be available as a choice in the preferences.
     * @param format the coordinate format
     */
    public static void registerCoordinateFormat(ICoordinateFormat format) {
        formats.add(format);
    }

    /**
     * Get the list of all registered coordinate formats.
     * @return the list of all registered coordinate formats
     */
    public static List<ICoordinateFormat> getCoordinateFormats() {
        return Collections.unmodifiableList(formats);
    }

    private static volatile ICoordinateFormat defaultCoordinateFormat = DecimalDegreesCoordinateFormat.INSTANCE;

    /**
     * Replies the default coordinate format to be use
     *
     * @return the default coordinate format
     */
    public static ICoordinateFormat getDefaultFormat() {
        return defaultCoordinateFormat;
    }

    /**
     * Sets the default coordinate format
     *
     * @param format the default coordinate format
     */
    public static void setCoordinateFormat(ICoordinateFormat format) {
        if (format != null) {
            defaultCoordinateFormat = format;
        }
    }

    /**
     * Get registered coordinate format by id
     *
     * @param id id of the coordinate format to get
     * @return the registered {@link ICoordinateFormat} with given id, or <code>null</code>
     * if no match is found
     */
    public static ICoordinateFormat getCoordinateFormat(String id) {
        CheckParameterUtil.ensureParameterNotNull(id, "id");
        return formats.stream().filter(format -> id.equals(format.getId())).findFirst().orElse(null);
    }
}
