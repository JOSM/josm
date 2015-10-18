// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceConfigurationError;

import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.io.AllFormatsImporter;
import org.openstreetmap.josm.io.FileExporter;
import org.openstreetmap.josm.io.FileImporter;
import org.openstreetmap.josm.tools.Utils;

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

        importers = new ArrayList<>();

        final List<Class<? extends FileImporter>> importerNames = Arrays.asList(
                org.openstreetmap.josm.io.OsmImporter.class,
                org.openstreetmap.josm.io.OsmGzipImporter.class,
                org.openstreetmap.josm.io.OsmZipImporter.class,
                org.openstreetmap.josm.io.OsmChangeImporter.class,
                org.openstreetmap.josm.io.GpxImporter.class,
                org.openstreetmap.josm.io.NMEAImporter.class,
                org.openstreetmap.josm.io.NoteImporter.class,
                org.openstreetmap.josm.io.OsmBzip2Importer.class,
                org.openstreetmap.josm.io.JpgImporter.class,
                org.openstreetmap.josm.io.WMSLayerImporter.class,
                org.openstreetmap.josm.io.AllFormatsImporter.class,
                org.openstreetmap.josm.io.session.SessionImporter.class
        );

        for (final Class<? extends FileImporter> importerClass : importerNames) {
            try {
                FileImporter importer = importerClass.newInstance();
                importers.add(importer);
                MapView.addLayerChangeListener(importer);
            } catch (Exception e) {
                if (Main.isDebugEnabled()) {
                    Main.debug(e.getMessage());
                }
            } catch (ServiceConfigurationError e) {
                // error seen while initializing WMSLayerImporter in plugin unit tests:
                // -
                // ServiceConfigurationError: javax.imageio.spi.ImageWriterSpi:
                // Provider com.sun.media.imageioimpl.plugins.jpeg.CLibJPEGImageWriterSpi could not be instantiated
                // Caused by: java.lang.IllegalArgumentException: vendorName == null!
                //      at javax.imageio.spi.IIOServiceProvider.<init>(IIOServiceProvider.java:76)
                //      at javax.imageio.spi.ImageReaderWriterSpi.<init>(ImageReaderWriterSpi.java:231)
                //      at javax.imageio.spi.ImageWriterSpi.<init>(ImageWriterSpi.java:213)
                //      at com.sun.media.imageioimpl.plugins.jpeg.CLibJPEGImageWriterSpi.<init>(CLibJPEGImageWriterSpi.java:84)
                // -
                // This is a very strange behaviour of JAI:
                // http://thierrywasyl.wordpress.com/2009/07/24/jai-how-to-solve-vendorname-null-exception/
                // -
                // that can lead to various problems, see #8583 comments
                Main.error(e);
            }
        }

        exporters = new ArrayList<>();

        final List<Class<? extends FileExporter>> exporterClasses = Arrays.asList(
                org.openstreetmap.josm.io.GpxExporter.class,
                org.openstreetmap.josm.io.OsmExporter.class,
                org.openstreetmap.josm.io.OsmGzipExporter.class,
                org.openstreetmap.josm.io.OsmBzip2Exporter.class,
                org.openstreetmap.josm.io.GeoJSONExporter.CurrentProjection.class, // needs to be considered earlier than GeoJSONExporter
                org.openstreetmap.josm.io.GeoJSONExporter.class,
                org.openstreetmap.josm.io.WMSLayerExporter.class,
                org.openstreetmap.josm.io.NoteExporter.class
        );

        for (final Class<? extends FileExporter> exporterClass : exporterClasses) {
            try {
                FileExporter exporter = exporterClass.newInstance();
                exporters.add(exporter);
                MapView.addLayerChangeListener(exporter);
            } catch (Exception e) {
                if (Main.isDebugEnabled()) {
                    Main.debug(e.getMessage());
                }
            } catch (ServiceConfigurationError e) {
                // see above in importers initialization
                Main.error(e);
            }
        }
    }

    private final String extensions;
    private final String description;
    private final String defaultExtension;

    protected static void sort(List<ExtensionFileFilter> filters) {
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
        for (int i = 0; i < importers.size(); i++) {
            if (importers.get(i) instanceof AllFormatsImporter) {
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
        List<ExtensionFileFilter> filters = new LinkedList<>();
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
        List<ExtensionFileFilter> filters = new LinkedList<>();
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
     * Applies the choosable {@link FileFilter} to a {@link AbstractFileChooser} before using the
     * file chooser for selecting a file for reading.
     *
     * @param fileChooser the file chooser
     * @param extension the default extension
     * @param allTypes If true, all the files types known by JOSM will be proposed in the "file type" combobox.
     *                 If false, only the file filters that include {@code extension} will be proposed
     * @since 5438
     */
    public static void applyChoosableImportFileFilters(AbstractFileChooser fileChooser, String extension, boolean allTypes) {
        for (ExtensionFileFilter filter: getImportExtensionFileFilters()) {
            if (allTypes || filter.acceptName("file."+extension)) {
                fileChooser.addChoosableFileFilter(filter);
            }
        }
        fileChooser.setFileFilter(getDefaultImportExtensionFileFilter(extension));
    }

    /**
     * Applies the choosable {@link FileFilter} to a {@link AbstractFileChooser} before using the
     * file chooser for selecting a file for writing.
     *
     * @param fileChooser the file chooser
     * @param extension the default extension
     * @param allTypes If true, all the files types known by JOSM will be proposed in the "file type" combobox.
     *                 If false, only the file filters that include {@code extension} will be proposed
     * @since 5438
     */
    public static void applyChoosableExportFileFilters(AbstractFileChooser fileChooser, String extension, boolean allTypes) {
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
     * Construct an extension file filter with the extensions supported by {@link org.openstreetmap.josm.io.Compression}
     * automatically added to the {@code extensions}. The specified {@code extensions} will be added to the description
     * in the form {@code old-description (*.ext1, *.ext2)}.
     * @param extensions The comma-separated list of file extensions
     * @param defaultExtension The default extension
     * @param description A short textual description of the file type without supported extensions in parentheses
     * @param addArchiveExtensionsToDescription Whether to also add the archive extensions to the description
     * @return The constructed filter
     */
    public static ExtensionFileFilter newFilterWithArchiveExtensions(
            String extensions, String defaultExtension, String description, boolean addArchiveExtensionsToDescription) {
        final Collection<String> extensionsPlusArchive = new LinkedHashSet<>();
        final Collection<String> extensionsForDescription = new LinkedHashSet<>();
        for (String e : extensions.split(",")) {
            extensionsPlusArchive.add(e);
            extensionsPlusArchive.add(e + ".gz");
            extensionsPlusArchive.add(e + ".bz2");
            extensionsForDescription.add("*." + e);
            if (addArchiveExtensionsToDescription) {
                extensionsForDescription.add("*." + e + ".gz");
                extensionsForDescription.add("*." + e + ".bz2");
            }
        }
        return new ExtensionFileFilter(Utils.join(",", extensionsPlusArchive), defaultExtension,
                description + " (" + Utils.join(", ", extensionsForDescription) + ")");
    }

    /**
     * Returns true if this file filter accepts the given filename.
     * @param filename The filename to check after
     * @return true if this file filter accepts the given filename (i.e if this filename ends with one of the extensions)
     * @since 1169
     */
    public boolean acceptName(String filename) {
        return Utils.hasExtension(filename, extensions.split(","));
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
