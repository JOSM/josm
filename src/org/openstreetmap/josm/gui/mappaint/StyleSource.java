// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.io.File;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.preferences.SourceEntry;

abstract public class StyleSource extends SourceEntry {
    public boolean hasError = false;
    public File zipIcons;

    public StyleSource(String url, String name, String shortdescription) {
        super(url, name, shortdescription, true);
    }

    public StyleSource(SourceEntry entry) {
        super(entry.url, entry.name, entry.shortdescription, entry.active);
    }

    abstract public void apply(MultiCascade mc, OsmPrimitive osm, double scale, OsmPrimitive multipolyOuterWay, boolean pretendWayIsClosed);

    abstract public void loadStyleSource();
}
