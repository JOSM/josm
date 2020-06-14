// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.AllFormatsImporter;
import org.openstreetmap.josm.gui.io.importexport.FileExporter;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.io.importexport.GeoJSONImporter;
import org.openstreetmap.josm.gui.io.importexport.GpxImporter;
import org.openstreetmap.josm.gui.io.importexport.JpgImporter;
import org.openstreetmap.josm.gui.io.importexport.NMEAImporter;
import org.openstreetmap.josm.gui.io.importexport.NoteImporter;
import org.openstreetmap.josm.gui.io.importexport.OsmChangeImporter;
import org.openstreetmap.josm.gui.io.importexport.OsmImporter;
import org.openstreetmap.josm.gui.io.importexport.RtkLibImporter;
import org.openstreetmap.josm.gui.io.importexport.WMSLayerImporter;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.io.session.SessionImporter;
import org.openstreetmap.josm.tools.Logging;
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
    private static final ArrayList<FileImporter> importers;

    /**
     * List of supported formats for export.
     * @since 4869
     */
    private static final ArrayList<FileExporter> exporters;

    // add some file types only if the relevant classes are there.
    // this gives us the option to painlessly drop them from the .jar
    // and build JOSM versions without support for these formats

    static {

        importers = new ArrayList<>();

        final List<Class<? extends FileImporter>> importerNames = Arrays.asList(
                OsmImporter.class,
                OsmChangeImporter.class,
                GeoJSONImporter.class,
                GpxImporter.class,
                NMEAImporter.class,
                RtkLibImporter.class,
                NoteImporter.class,
                JpgImporter.class,
                WMSLayerImporter.class,
                AllFormatsImporter.class,
                SessionImporter.class
        );

        for (final Class<? extends FileImporter> importerClass : importerNames) {
            try {
                FileImporter importer = importerClass.getConstructor().newInstance();
                importers.add(importer);
            } catch (ReflectiveOperationException e) {
                Logging.debug(e);
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
                Logging.error(e);
            }
        }

        exporters = new ArrayList<>();

        final List<Class<? extends FileExporter>> exporterClasses = Arrays.asList(
                org.openstreetmap.josm.gui.io.importexport.GpxExporter.class,
                org.openstreetmap.josm.gui.io.importexport.OsmExporter.class,
                org.openstreetmap.josm.gui.io.importexport.OsmGzipExporter.class,
                org.openstreetmap.josm.gui.io.importexport.OsmBzip2Exporter.class,
                org.openstreetmap.josm.gui.io.importexport.OsmXzExporter.class,
                org.openstreetmap.josm.gui.io.importexport.GeoJSONExporter.class,
                org.openstreetmap.josm.gui.io.importexport.WMSLayerExporter.class,
                org.openstreetmap.josm.gui.io.importexport.NoteExporter.class,
                org.openstreetmap.josm.gui.io.importexport.ValidatorErrorExporter.class
        );

        for (final Class<? extends FileExporter> exporterClass : exporterClasses) {
            try {
                FileExporter exporter = exporterClass.getConstructor().newInstance();
                exporters.add(exporter);
                MainApplication.getLayerManager().addAndFireActiveLayerChangeListener(exporter);
            } catch (ReflectiveOperationException e) {
                Logging.debug(e);
            } catch (ServiceConfigurationError e) {
                // see above in importers initialization
                Logging.error(e);
            }
        }
    }

    private final String extensions;
    private final String description;
    private final String defaultExtension;

    protected static Comparator<ExtensionFileFilter> comparator() {
        AllFormatsImporter all = new AllFormatsImporter();
        return (o1, o2) -> {
            if (o1.getDescription().equals(all.filter.getDescription())) return 1;
            if (o2.getDescription().equals(all.filter.getDescription())) return -1;
            return o1.getDescription().compareTo(o2.getDescription());
        };
    }

    /**
     * Strategy to determine if extensions must be added to the description.
     */
    public enum AddArchiveExtension {
        /** No extension is added */
        NONE,
        /** Only base extension is added */
        BASE,
        /** All extensions are added (base + archives) */
        ALL
    }

    /**
     * Adds a new file importer at the end of the global list. This importer will be evaluated after core ones.
     * @param importer new file importer
     * @since 10407
     */
    public static void addImporter(FileImporter importer) {
        if (importer != null) {
            importers.add(importer);
        }
    }

    /**
     * Adds a new file importer at the beginning of the global list. This importer will be evaluated before core ones.
     * @param importer new file importer
     * @since 10407
     */
    public static void addImporterFirst(FileImporter importer) {
        if (importer != null) {
            importers.add(0, importer);
        }
    }

    /**
     * Adds a new file exporter at the end of the global list. This exporter will be evaluated after core ones.
     * @param exporter new file exporter
     * @since 10407
     */
    public static void addExporter(FileExporter exporter) {
        if (exporter != null) {
            exporters.add(exporter);
        }
    }

    /**
     * Adds a new file exporter at the beginning of the global list. This exporter will be evaluated before core ones.
     * @param exporter new file exporter
     * @since 10407
     */
    public static void addExporterFirst(FileExporter exporter) {
        if (exporter != null) {
            exporters.add(0, exporter);
        }
    }

    /**
     * Returns the list of file importers.
     * @return unmodifiable list of file importers
     * @since 10407
     */
    public static List<FileImporter> getImporters() {
        return Collections.unmodifiableList(importers);
    }

    /**
     * Returns the list of file exporters.
     * @return unmodifiable list of file exporters
     * @since 10407
     */
    public static List<FileExporter> getExporters() {
        return Collections.unmodifiableList(exporters);
    }

    /**
     * Updates the {@link AllFormatsImporter} that is contained in the importers list. If
     * you do not use the importers variable directly, you don't need to call this.
     * <p>
     * Updating the AllFormatsImporter is required when plugins add new importers that
     * support new file extensions. The old AllFormatsImporter doesn't include the new
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
        return importers.stream()
                .map(importer -> importer.filter)
                .sorted(comparator())
                .collect(Collectors.toList());
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
        return exporters.stream()
                .filter(FileExporter::isEnabled)
                .map(exporter -> exporter.filter)
                .distinct()
                .sorted(comparator())
                .collect(Collectors.toList());
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
        return importers.stream()
                .filter(importer -> extension.equals(importer.filter.getDefaultExtension()))
                .findFirst().map(importer -> importer.filter)
                .orElseGet(() -> new AllFormatsImporter().filter);
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
        // if extension did not match defaultExtension of any exporter,
        // scan all supported extensions
        File file = new File("file." + extension);
        for (FileExporter exporter : exporters) {
            if (exporter.filter.accept(file))
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
     * @param additionalTypes matching types will additionally be added to the "file type" combobox.
     * @since 14668 (signature)
     */
    public static void applyChoosableImportFileFilters(
            AbstractFileChooser fileChooser, String extension, Predicate<ExtensionFileFilter> additionalTypes) {
        getImportExtensionFileFilters().stream()
                .filter(filter -> additionalTypes.test(filter) || filter.acceptName("file."+extension))
                .forEach(fileChooser::addChoosableFileFilter);
        fileChooser.setFileFilter(getDefaultImportExtensionFileFilter(extension));
    }

    /**
     * Applies the choosable {@link FileFilter} to a {@link AbstractFileChooser} before using the
     * file chooser for selecting a file for writing.
     *
     * @param fileChooser the file chooser
     * @param extension the default extension
     * @param additionalTypes matching types will additionally be added to the "file type" combobox.
     * @since 14668 (signature)
     */
    public static void applyChoosableExportFileFilters(
            AbstractFileChooser fileChooser, String extension, Predicate<ExtensionFileFilter> additionalTypes) {
        for (ExtensionFileFilter filter: getExportExtensionFileFilters()) {
            if (additionalTypes.test(filter) || filter.acceptName("file."+extension)) {
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
     * @param addArchiveExtension Whether to also add the archive extensions to the description
     * @param archiveExtensions List of extensions to be added
     * @return The constructed filter
     */
    public static ExtensionFileFilter newFilterWithArchiveExtensions(String extensions, String defaultExtension,
            String description, AddArchiveExtension addArchiveExtension, List<String> archiveExtensions) {
        final Collection<String> extensionsPlusArchive = new LinkedHashSet<>();
        final Collection<String> extensionsForDescription = new LinkedHashSet<>();
        for (String e : extensions.split(",", -1)) {
            extensionsPlusArchive.add(e);
            if (addArchiveExtension != AddArchiveExtension.NONE) {
                extensionsForDescription.add("*." + e);
            }
            for (String extension : archiveExtensions) {
                extensionsPlusArchive.add(e + '.' + extension);
                if (addArchiveExtension == AddArchiveExtension.ALL) {
                    extensionsForDescription.add("*." + e + '.' + extension);
                }
            }
        }
        return new ExtensionFileFilter(
            String.join(",", extensionsPlusArchive),
            defaultExtension,
            description + (!extensionsForDescription.isEmpty()
                ? (" (" + String.join(", ", extensionsForDescription) + ')')
                : "")
            );
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

        List<String> archiveExtensions = Arrays.asList("gz", "bz", "bz2", "xz", "zip");
        return newFilterWithArchiveExtensions(
            extensions,
            defaultExtension,
            description,
            addArchiveExtensionsToDescription ? AddArchiveExtension.ALL : AddArchiveExtension.BASE,
            archiveExtensions
        );
    }

    /**
     * Returns true if this file filter accepts the given filename.
     * @param filename The filename to check after
     * @return true if this file filter accepts the given filename (i.e if this filename ends with one of the extensions)
     * @since 1169
     */
    public boolean acceptName(String filename) {
        return Utils.hasExtension(filename, extensions.split(",", -1));
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
        return Objects.hash(extensions, description, defaultExtension);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ExtensionFileFilter that = (ExtensionFileFilter) obj;
        return Objects.equals(extensions, that.extensions) &&
                Objects.equals(description, that.description) &&
                Objects.equals(defaultExtension, that.defaultExtension);
    }
}
