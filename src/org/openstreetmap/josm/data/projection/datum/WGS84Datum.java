// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.projection.Ellipsoid;

/**
 * WGS84 datum. Transformation from and to WGS84 datum is a no-op.
 * @since 4285
 */
public final class WGS84Datum extends NullDatum {

    /**
     * The unique instance.
     */
    public static final WGS84Datum INSTANCE = new WGS84Datum();

    private WGS84Datum() {
        super(tr("WGS84"), Ellipsoid.WGS84);
    }
}
