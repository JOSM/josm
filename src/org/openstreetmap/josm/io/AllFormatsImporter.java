// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

/**
 * Dummy importer that adds the "All Formats"-Filter when opening files
 */
public class AllFormatsImporter extends FileImporter {
    public AllFormatsImporter() {
        super(new ExtensionFileFilter("osm,xml,osm.gz,osm.bz2,osm.bz,gpx,gpx.gz,nmea,nme,nma,log,txt,wms,jpg", "", tr("All Formats")
                    + " (*.gpx *.osm *.nmea *.jpg ...)"));
    }
    @Override public boolean acceptFile(File pathname) {
        return false;
    }
}
