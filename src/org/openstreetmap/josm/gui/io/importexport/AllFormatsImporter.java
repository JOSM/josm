// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.util.Iterator;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

/**
 * Dummy importer that adds the "All Formats"-Filter when opening files
 */
public class AllFormatsImporter extends FileImporter {
    /**
     * Constructs a new {@code AllFormatsImporter}.
     */
    public AllFormatsImporter() {
        super(new ExtensionFileFilter(getAllExtensions(), "", tr("All Formats")
                + " (*.gpx *.osm *.nmea *.jpg ...)"));
    }

    @Override
    public boolean acceptFile(File pathname) {
        return false;
    }

    /**
     * Builds list of all supported extensions by the registered FileImporters.
     * @return String comma separated list of supported file extensions
     */
    private static String getAllExtensions() {
        Iterator<FileImporter> imp = ExtensionFileFilter.getImporters().iterator();
        StringBuilder ext = new StringBuilder();
        while (imp.hasNext()) {
            FileImporter fi = imp.next();
            if (fi instanceof AllFormatsImporter || fi.filter == null) {
                continue;
            }
            ext.append(fi.filter.getExtensions()).append(',');
        }
        // remove last comma
        return ext.substring(0, ext.length()-1);
    }
}
