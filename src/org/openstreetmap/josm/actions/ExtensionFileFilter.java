// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.io.FileExporter;
import org.openstreetmap.josm.io.FileImporter;
import org.openstreetmap.josm.io.GpxExporter;
import org.openstreetmap.josm.io.GpxImporter;
import org.openstreetmap.josm.io.NMEAImporter;
import org.openstreetmap.josm.io.OsmBzip2Exporter;
import org.openstreetmap.josm.io.OsmBzip2Importer;
import org.openstreetmap.josm.io.OsmExporter;
import org.openstreetmap.josm.io.OsmGzipExporter;
import org.openstreetmap.josm.io.OsmGzipImporter;
import org.openstreetmap.josm.io.OsmImporter;

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

    /**
     * list of supported formats
     */
    public static ArrayList<FileImporter> importers = new ArrayList<FileImporter>(Arrays.asList(new OsmImporter(),
            new OsmGzipImporter(), new OsmBzip2Importer(), new GpxImporter(), new NMEAImporter(), new AllFormatsImporter()));

    public static ArrayList<FileExporter> exporters = new ArrayList<FileExporter>(Arrays.asList(new GpxExporter(),
            new OsmExporter(), new OsmGzipExporter(), new OsmBzip2Exporter()));
    
    /**
     * Construct an extension file filter by giving the extension to check after.
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
    
    /**
     * Dummy importer that adds the "All Formats"-Filter when opening files
     */
    public static class AllFormatsImporter extends FileImporter {
        public AllFormatsImporter() {
            super(
                new ExtensionFileFilter("osm,xml,osm.gz,osm.bz2,osm.bz,gpx,gpx.gz,nmea,nme,nma,txt", "", tr("All Formats")
                        + " (*.gpx *.osm *.nmea ...)"));
        }
        @Override public boolean acceptFile(File pathname) {
            return false;
        }
    }
}
