// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;

/**
 * File importer allowing to import geotagged images
 * @since 17548
 */
public class ImageImporter extends FileImporter {
    private GpxLayer gpx;

    /**
     * The supported image file types on the current system
     */
    public static final List<String> SUPPORTED_FILE_TYPES = Collections
            .unmodifiableList(Arrays.stream(ImageIO.getReaderFileSuffixes()).sorted().collect(Collectors.toList()));

    /**
     * The default file filter
     */
    public static final ExtensionFileFilter FILE_FILTER = getFileFilters(false);

    /**
     * An alternate file filter that also includes folders.
     */
    public static final ExtensionFileFilter FILE_FILTER_WITH_FOLDERS = getFileFilters(true);

    private static ExtensionFileFilter getFileFilters(boolean folder) {
        String typeStr = String.join(",", SUPPORTED_FILE_TYPES);
        String humanStr = tr("Image Files") + " (*." + String.join(", *.", SUPPORTED_FILE_TYPES);
        if (folder) {
            humanStr += ", " + tr("folder");
        }
        humanStr += ")";

        return new ExtensionFileFilter(typeStr, "jpg", humanStr);
    }

    /**
     * Constructs a new {@code ImageImporter}.
     */
    public ImageImporter() {
        this(false);
    }

    /**
     * Constructs a new {@code ImageImporter} with folders selection, if wanted.
     * @param includeFolders If true, includes folders in the file filter
     */
    public ImageImporter(boolean includeFolders) {
        super(includeFolders ? FILE_FILTER_WITH_FOLDERS : FILE_FILTER);
    }

    /**
     * Constructs a new {@code ImageImporter} for the given GPX layer. Folders selection is allowed.
     * @param gpx The GPX layer
     */
    public ImageImporter(GpxLayer gpx) {
        this(true);
        this.gpx = gpx;
    }

    @Override
    public boolean acceptFile(File pathname) {
        return super.acceptFile(pathname) || pathname.isDirectory();
    }

    @Override
    public void importData(List<File> sel, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        progressMonitor.beginTask(tr("Looking for image files"), 1);
        try {
            List<File> files = new ArrayList<>();
            Set<String> visitedDirs = new HashSet<>();
            addRecursiveFiles(files, visitedDirs, sel, progressMonitor.createSubTaskMonitor(1, true));

            if (progressMonitor.isCanceled())
                return;

            if (files.isEmpty())
                throw new IOException(tr("No image files found."));

            GeoImageLayer.create(files, gpx);
        } finally {
            progressMonitor.finishTask();
        }
    }

    static void addRecursiveFiles(List<File> files, Set<String> visitedDirs, List<File> sel, ProgressMonitor progressMonitor)
            throws IOException {

        if (progressMonitor.isCanceled())
            return;

        progressMonitor.beginTask(null, sel.size());
        try {
            for (File f : sel) {
                if (f.isDirectory()) {
                    if (visitedDirs.add(f.getCanonicalPath())) { // Do not loop over symlinks
                        File[] dirFiles = f.listFiles(); // Can be null for some strange directories (like lost+found)
                        if (dirFiles != null) {
                            addRecursiveFiles(files, visitedDirs, Arrays.asList(dirFiles), progressMonitor.createSubTaskMonitor(1, true));
                        }
                    } else {
                        progressMonitor.worked(1);
                    }
                } else {
                    if (FILE_FILTER.accept(f)) {
                        files.add(f);
                    }
                    progressMonitor.worked(1);
                }
            }
        } finally {
            progressMonitor.finishTask();
        }
    }

    @Override
    public boolean isBatchImporter() {
        return true;
    }

    /**
     * Needs to be the last, to avoid problems.
     */
    @Override
    public double getPriority() {
        return -1000;
    }
}
