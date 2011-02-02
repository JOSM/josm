// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.preferences.BooleanProperty;

public interface OffsetServer {
    public static BooleanProperty PROP_SERVER_ENABLED = new BooleanProperty("imagery.offsetserver.enabled",false);
    abstract boolean isLayerSupported(ImageryInfo info);
    abstract EastNorth getOffset(ImageryInfo info, EastNorth en);
}
