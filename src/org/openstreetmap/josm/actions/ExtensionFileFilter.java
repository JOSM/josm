// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.io.AllFormatsImporter;
import org.openstreetmap.josm.io.FileExporter;
import org.openstreetmap.josm.io.FileImporter;

/**
 * A file filter that filters after the extension. Also includes a list of file
 * filters used in JOSM.
 * @since 32
 */
public class ExtensionFileFilter extends FileFilter implements java.io.FileFilter {

    /**
     * List of supported formats for import.
     * @since 4869
     */
    public static final ArrayList<FileImporter> importers;

    /**
     * List of supported formats for export.
     * @since 4869
     */
    public static final ArrayList<FileExporter> exporters;

    // add some file types only if the relevant classes are there.
    // this gives us the option to painlessly drop them from the .jar
    // and build JOSM versions without support for these formats

    static {

        importers = new ArrayList<FileImporter>();

        String[] importerNames = {
                "org.openstreetmap.josm.io.OsmImporter",
                "org.openstreetmap.josm.io.OsmGzipImporter",
                "org.openstreetmap.josm.io.OsmChangeImporter",
                "org.openstreetmap.josm.io.GpxImporter",
                "org.openstreetmap.josm.io.NMEAImporter",
                "org.openstreetmap.josm.io.OsmBzip2Importer",
                "org.openstreetmap.josm.io.JpgImporter",
                "org.openstreetmap.josm.io.WMSLayerImporter",
                "org.openstreetmap.josm.io.AllFormatsImporter",
                "org.openstreetmap.josm.io.session.SessionImporter"
        };

        for (String classname : importerNames) {
            try {
                FileImporter importer = (FileImporter) Class.forName(classname).newInstance();
                importers.add(importer);
                MapView.addLayerChangeListener(importer);
            } catch (Throwable t) {
                Main.debug(t.getMessage());
            }
        }

        exporters = new ArrayList<FileExporter>();

        String[] exporterNames = {
                "org.openstreetmap.josm.io.GpxExporter",
                "org.openstreetmap.josm.io.OsmExporter",
                "org.openstreetmap.josm.io.OsmGzipExporter",
                "org.openstreetmap.josm.io.OsmBzip2Exporter",
                "org.openstreetmap.josm.io.GeoJSONExporter",
                "org.openstreetmap.josm.io.WMSLayerExporter"
        };

        for (String classname : exporterNames) {
            try {
                FileExporter exporter = (FileExporter)Class.forName(classname).newInstance();
                exporters.add(exporter);
                MapView.addLayerChangeListener(exporter);
            } catch (Throwable t) {
                Main.debug(t.getMessage());
            }
        }
    }

    private final String extensions;
    private final String description;
    private final String defaultExtension;

    static protected void sort(List<ExtensionFileFilter> filters) {
        Collections.sort(
                filters,
                new Comparator<ExtensionFileFilter>() {
                    private AllFormatsImporter all = new AllFormatsImporter();
                    @Override
                    public int compare(ExtensionFileFilter o1, ExtensionFileFilter o2) {
                        if (o1.getDescription().equals(all.filter.getDescription())) return 1;
                        if (o2.getDescription().equals(all.filter.getDescription())) return -1;
                        return o1.getDescription().compareTo(o2.getDescription());
                    }
                }
        );
    }

    /**
     * Updates the {@link AllFormatsImporter} that is contained in the importers list. If
     * you do not use the importers variable directly, you don’t need to call this.
     * <p>
     * Updating the AllFormatsImporter is required when plugins add new importers that
     * support new file extensions. The old AllFormatsImporter doesn’t include the new
     * extensions and thus will not display these files.
     *
     * @since 5131
     */
    public static void updateAllFormatsImporter() {
        for(int i=0; i < importers.size(); i++) {
            if(importers.get(i) instanceof AllFormatsImporter) {
                importers.set(i, new AllFormatsImporter());
            }
        }
    }

    /**
     * Replies an ordered list of {@link ExtensionFileFilter}s for importing.
     * The list is ordered according to their description, an {@link AllFormatsImporter}
     * is append at the end.
     *
     * @return an ordered list of {@link ExtensionFileFilter}s for importing.
     * @since 2029
     */
    public static List<ExtensionFileFilter> getImportExtensionFileFilters() {
        updateAllFormatsImporter();
        LinkedList<ExtensionFileFilter> filters = new LinkedList<ExtensionFileFilter>();
        for (FileImporter importer : importers) {
            filters.add(importer.filter);
        }
        sort(filters);
        return filters;
    }

    /**
     * Replies an ordered list of enabled {@link ExtensionFileFilter}s for exporting.
     * The list is ordered according to their description, an {@link AllFormatsImporter}
     * is append at the end.
     *
     * @return an ordered list of enabled {@link ExtensionFileFilter}s for exporting.
     * @since 2029
     */
    public static List<ExtensionFileFilter> getExportExtensionFileFilters() {
        LinkedList<ExtensionFileFilter> filters = new LinkedList<ExtensionFileFilter>();
        for (FileExporter exporter : exporters) {
            if (filters.contains(exporter.filter) || !exporter.isEnabled()) {
                continue;
            }
            filters.add(exporter.filter);
        }
        sort(filters);
        return filters;
    }

    /**
     * Replies the default {@link ExtensionFileFilter} for a given extension
     *
     * @param extension the extension
     * @return the default {@link ExtensionFileFilter} for a given extension
     * @since 2029
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
     * Replies the default {@link ExtensionFileFilter} for a given extension
     *
     * @param extension the extension
     * @return the default {@link ExtensionFileFilter} for a given extension
     * @since 2029
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
     * Applies the choosable {@link FileFilter} to a {@link JFileChooser} before using the
     * file chooser for selecting a file for reading.
     *
     * @param fileChooser the file chooser
     * @param extension the default extension
     * @param allTypes If true, all the files types known by JOSM will be proposed in the "file type" combobox.
     *                 If false, only the file filters that include {@code extension} will be proposed
     * @since 5438
     */
    public static void applyChoosableImportFileFilters(JFileChooser fileChooser, String extension, boolean allTypes) {
        for (ExtensionFileFilter filter: getImportExtensionFileFilters()) {
            if (allTypes || filter.acceptName("file."+extension)) {
                fileChooser.addChoosableFileFilter(filter);
            }
        }
        fileChooser.setFileFilter(getDefaultImportExtensionFileFilter(extension));
    }

    /**
     * Applies the choosable {@link FileFilter} to a {@link JFileChooser} before using the
     * file chooser for selecting a file for writing.
     *
     * @param fileChooser the file chooser
     * @param extension the default extension
     * @param allTypes If true, all the files types known by JOSM will be proposed in the "file type" combobox.
     *                 If false, only the file filters that include {@code extension} will be proposed
     * @since 5438
     */
    public static void applyChoosableExportFileFilters(JFileChooser fileChooser, String extension, boolean allTypes) {
        for (ExtensionFileFilter filter: getExportExtensionFileFilters()) {
            if (allTypes || filter.acceptName("file."+extension)) {
                fileChooser.addChoosableFileFilter(filter);
            }
        }
        fileChooser.setFileFilter(getDefaultExportExtensionFileFilter(extension));
    }

    /**
     * Construct an extension file filter by giving the extension to check after.
     * @param extension The comma-separated list of file extensions
     * @param defaultExtension The default extension
     * @param description A short textual description of the file type
     * @since 1169
     */
    public ExtensionFileFilter(String extension, String defaultExtension, String description) {
        this.extensions = extension;
        this.defaultExtension = defaultExtension;
        this.description = description;
    }

    /**
     * Returns true if this file filter accepts the given filename.
     * @param filename The filename to check after
     * @return true if this file filter accepts the given filename (i.e if this filename ends with one of the extensions)
     * @since 1169
     */
    public boolean acceptName(String filename) {
        String name = filename.toLowerCase();
        for (String ext : extensions.split(","))
            if (name.endsWith("."+ext))
                return true;
        return false;
    }

    @Override
    public boolean accept(File pathname) {
        if (pathname.isDirectory())
            return true;
        return acceptName(pathname.getName());
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Replies the comma-separated list of file extensions of this file filter.
     * @return the comma-separated list of file extensions of this file filter, as a String
     * @since 5131
     */
    public String getExtensions() {
        return extensions;
    }

    /**
     * Replies the default file extension of this file filter.
     * @return the default file extension of this file filter
     * @since 2029
     */
    public String getDefaultExtension() {
        return defaultExtension;
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
