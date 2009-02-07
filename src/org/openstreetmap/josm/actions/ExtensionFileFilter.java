// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * A file filter that filters after the extension. Also includes a list of file
 * filters used in JOSM.
 *
 * @author imi
 */
public class ExtensionFileFilter extends FileFilter {
    private final String extension;
    private final String description;
    public final String defaultExtension;

    public static final int OSM = 0;
    public static final int GPX = 1;
    public static final int NMEA = 2;

    public static ExtensionFileFilter[] filters = {
        new ExtensionFileFilter("osm,xml", "osm", tr("OSM Server Files")+ " (*.osm *.xml)"),
        new ExtensionFileFilter("gpx,gpx.gz", "gpx", tr("GPX Files") + " (*.gpx *.gpx.gz)"),
        new ExtensionFileFilter("nmea,nme,nma,txt", "nmea", tr("NMEA-0183 Files") + " (*.nmea *.nme *.nma *.txt)"),
    };

    /**
     * Construct an extension file filter by giving the extension to check after.
     *
     */
    public ExtensionFileFilter(String extension, String defExt, String description) {
        this.extension = extension;
        defaultExtension = defExt;
        this.description = description;
    }

    public boolean acceptName(String filename) {
        String name = filename.toLowerCase();
        for (String ext : extension.split(","))
            if (name.endsWith("."+ext))
                return true;
        return false;
    }

    @Override public boolean accept(File pathname) {
        if (pathname.isDirectory())
            return true;
        return acceptName(pathname.getName());
    }

    @Override public String getDescription() {
        return description;
    }
}
