// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor.conversion;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.coor.ILatLon;

/**
 * Coordinate format that converts coordinates to simple floating point decimal format.
 * @since 12735
 */
public class DecimalDegreesCoordinateFormat extends AbstractCoordinateFormat {

    public static final DecimalDegreesCoordinateFormat INSTANCE = new DecimalDegreesCoordinateFormat();

    protected DecimalDegreesCoordinateFormat() {
        super("DECIMAL_DEGREES", tr("Decimal Degrees"));
    }

    @Override
    public String latToString(ILatLon ll) {
        return cDdFormatter.format(ll.lat());
    }

    @Override
    public String lonToString(ILatLon ll) {
        return cDdFormatter.format(ll.lon());
    }
}
