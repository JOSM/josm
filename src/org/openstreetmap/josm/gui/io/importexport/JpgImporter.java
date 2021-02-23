// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.IllegalDataException;

/**
 * File importer allowing to import geotagged images (*.jpg files).
 *
 */
public class JpgImporter extends FileImporter {
    /** Check if the filename starts with a borked path ({@link java.io.File#File} drops consecutive {@code /} characters). */
    private static final Pattern URL_START_BAD = Pattern.compile("^(https?:/)([^/].*)$");
    /** Check for the beginning of a "good" url */
    private static final Pattern URL_START_GOOD = Pattern.compile("^https?://.*$");

    private GpxLayer gpx;

    /**
     * The default file filter (only *.jpg files).
     */
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "jpg,jpeg", "jpg", tr("Image Files") + " (*.jpg)");

    /**
     * An alternate file filter that also includes folders.
     * @since 5438
     */
    public static final ExtensionFileFilter FILE_FILTER_WITH_FOLDERS = new ExtensionFileFilter(
            "jpg,jpeg", "jpg", tr("Image Files") + " (*.jpg, "+ tr("folder")+')');

    /**
     * Constructs a new {@code JpgImporter}.
     */
    public JpgImporter() {
        this(false);
    }

    /**
     * Constructs a new {@code JpgImporter} with folders selection, if wanted.
     * @param includeFolders If true, includes folders in the file filter
     * @since 5438
     */
    public JpgImporter(boolean includeFolders) {
        super(includeFolders ? FILE_FILTER_WITH_FOLDERS : FILE_FILTER);
    }

    /**
     * Constructs a new {@code JpgImporter} for the given GPX layer. Folders selection is allowed.
     * @param gpx The GPX layer
     */
    public JpgImporter(GpxLayer gpx) {
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
            addRecursiveFiles(this.options, files, visitedDirs, sel, progressMonitor.createSubTaskMonitor(1, true));

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
        addRecursiveFiles(EnumSet.noneOf(Options.class), files, visitedDirs, sel, progressMonitor);
    }

    static void addRecursiveFiles(Set<Options> options, List<File> files, Set<String> visitedDirs, List<File> sel,
            ProgressMonitor progressMonitor) throws IOException {

        if (progressMonitor.isCanceled())
            return;

        progressMonitor.beginTask(null, sel.size());
        try {
            for (File f : sel) {
                if (f.isDirectory()) {
                    if (visitedDirs.add(f.getCanonicalPath())) { // Do not loop over symlinks
                        File[] dirFiles = f.listFiles(); // Can be null for some strange directories (like lost+found)
                        if (dirFiles != null) {
                            addRecursiveFiles(options, files, visitedDirs, Arrays.asList(dirFiles),
                                    progressMonitor.createSubTaskMonitor(1, true));
                        }
                    } else {
                        progressMonitor.worked(1);
                    }
                } else {
                    /* Check if the path is a web path, and if so, ensure that it is "correct" */
                    final String path = f.getPath();
                    Matcher matcherBad = URL_START_BAD.matcher(path);
                    final String realPath;
                    if (matcherBad.matches()) {
                        realPath = matcherBad.replaceFirst(matcherBad.group(1) + "/" + matcherBad.group(2));
                    } else {
                        realPath = path;
                    }
                    if (URL_START_GOOD.matcher(realPath).matches() && FILE_FILTER.accept(f)
                            && options.contains(Options.ALLOW_WEB_RESOURCES)) {
                        try (CachedFile cachedFile = new CachedFile(realPath)) {
                            files.add(cachedFile.getFile());
                        }
                    } else if (FILE_FILTER.accept(f)) {
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
