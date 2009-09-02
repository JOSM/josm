// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFileChooser;
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
 */
public class ExtensionFileFilter extends FileFilter {

    /**
     * list of supported formats
     */
    public static ArrayList<FileImporter> importers = new ArrayList<FileImporter>(Arrays.asList(new OsmImporter(),
            new OsmGzipImporter(), new OsmBzip2Importer(), new GpxImporter(), new NMEAImporter(), new AllFormatsImporter()));

    public static ArrayList<FileExporter> exporters = new ArrayList<FileExporter>(Arrays.asList(new GpxExporter(),
            new OsmExporter(), new OsmGzipExporter(), new OsmBzip2Exporter()));


    private final String extensions;
    private final String description;
    /**
     * @deprecated use {@see #getDefaultExtension()
     */
    @Deprecated
    public final String defaultExtension;


    static protected void sort(List<ExtensionFileFilter> filters) {
        Collections.sort(
                filters,
                new Comparator<ExtensionFileFilter>() {
                    private AllFormatsImporter all = new AllFormatsImporter();
                    public int compare(ExtensionFileFilter o1, ExtensionFileFilter o2) {
                        if (o1.getDescription().equals(all.filter.getDescription())) return 1;
                        if (o2.getDescription().equals(all.filter.getDescription())) return -1;
                        return o1.getDescription().compareTo(o2.getDescription());
                    }
                }
        );
    }

    /**
     * Replies an ordered list of {@see ExtensionFileFilter}s for importing.
     * The list is ordered according to their description, an {@see AllFormatsImporter}
     * is append at the end.
     * 
     * @return an ordered list of {@see ExtensionFileFilter}s for importing.
     */
    public static List<ExtensionFileFilter> getImportExtensionFileFilters() {
        LinkedList<ExtensionFileFilter> filters = new LinkedList<ExtensionFileFilter>();
        for (FileImporter importer : importers) {
            if (filters.contains(importer.filter)) {
                continue;
            }
            filters.add(importer.filter);
        }
        sort(filters);
        return filters;
    }

    /**
     * Replies an ordered list of {@see ExtensionFileFilter}s for exporting.
     * The list is ordered according to their description, an {@see AllFormatsImporter}
     * is append at the end.
     * 
     * @return an ordered list of {@see ExtensionFileFilter}s for exporting.
     */
    public static List<ExtensionFileFilter> getExportExtensionFileFilters() {
        LinkedList<ExtensionFileFilter> filters = new LinkedList<ExtensionFileFilter>();
        for (FileExporter exporter : exporters) {
            if (filters.contains(exporter.filter)) {
                continue;
            }
            filters.add(exporter.filter);
        }
        sort(filters);
        return filters;
    }

    /**
     * Replies the default {@see ExtensionFileFilter} for a given extension
     * 
     * @param extension the extension
     * @return the default {@see ExtensionFileFilter} for a given extension
     */
    public static ExtensionFileFilter getDefaultImportExtensionFileFilter(String extension) {
        if (extension == null) return new AllFormatsImporter().filter;
        for (FileImporter importer : importers) {
            if (extension.equals(importer.filter.getDefaultExtension()))
                return importer.filter;
        }
        return new AllFormatsImporter().filter;
    }

    /**
     * Replies the default {@see ExtensionFileFilter} for a given extension
     * 
     * @param extension the extension
     * @return the default {@see ExtensionFileFilter} for a given extension
     */
    public static ExtensionFileFilter getDefaultExportExtensionFileFilter(String extension) {
        if (extension == null) return new AllFormatsImporter().filter;
        for (FileExporter exporter : exporters) {
            if (extension.equals(exporter.filter.getDefaultExtension()))
                return exporter.filter;
        }
        return new AllFormatsImporter().filter;
    }

    /**
     * Applies the choosable {@see FileFilter} to a {@see JFileChooser} before using the
     * file chooser for selecting a file for reading.
     * 
     * @param fileChooser the file chooser
     * @param extension the default extension
     */
    public static void applyChoosableImportFileFilters(JFileChooser fileChooser, String extension) {
        for (ExtensionFileFilter filter: getImportExtensionFileFilters()) {
            fileChooser.addChoosableFileFilter(filter);
        }
        fileChooser.setFileFilter(getDefaultImportExtensionFileFilter(extension));
    }

    /**
     * Applies the choosable {@see FileFilter} to a {@see JFileChooser} before using the
     * file chooser for selecting a file for writing.
     * 
     * @param fileChooser the file chooser
     * @param extension the default extension
     */
    public static void applyChoosableExportFileFilters(JFileChooser fileChooser, String extension) {
        for (ExtensionFileFilter filter: getExportExtensionFileFilters()) {
            fileChooser.addChoosableFileFilter(filter);
        }
        fileChooser.setFileFilter(getDefaultExportExtensionFileFilter(extension));
    }

    /**
     * Construct an extension file filter by giving the extension to check after.
     */
    public ExtensionFileFilter(String extension, String defaultExtension, String description) {
        this.extensions = extension;
        this.defaultExtension = defaultExtension;
        this.description = description;
    }

    public boolean acceptName(String filename) {
        String name = filename.toLowerCase();
        for (String ext : extensions.split(","))
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

    public String getDefaultExtension() {
        return defaultExtension;
    }

    /**
     * Dummy importer that adds the "All Formats"-Filter when opening files
     */
    public static class AllFormatsImporter extends FileImporter {
        public AllFormatsImporter() {
            super(
                    new ExtensionFileFilter("osm,xml,osm.gz,osm.bz2,osm.bz,gpx,gpx.gz,nmea,nme,nma,txt,wms", "", tr("All Formats")
                            + " (*.gpx *.osm *.nmea ...)"));
        }
        @Override public boolean acceptFile(File pathname) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((defaultExtension == null) ? 0 : defaultExtension.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((extensions == null) ? 0 : extensions.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExtensionFileFilter other = (ExtensionFileFilter) obj;
        if (defaultExtension == null) {
            if (other.defaultExtension != null)
                return false;
        } else if (!defaultExtension.equals(other.defaultExtension))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (extensions == null) {
            if (other.extensions != null)
                return false;
        } else if (!extensions.equals(other.extensions))
            return false;
        return true;
    }
}
