// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class JpgImporter extends FileImporter {
    private GpxLayer gpx;

    public JpgImporter() {
        super(new ExtensionFileFilter("jpg", "jpg", tr("Image Files") + " (*.jpg, "+ tr("folder")+")"));
    }

    public JpgImporter(GpxLayer gpx) {
        this();
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
            List<File> files = new ArrayList<File>();
            Set<String> visitedDirs = new HashSet<String>();
            addRecursiveFiles(files, visitedDirs, sel, progressMonitor.createSubTaskMonitor(1, true));

            if (progressMonitor.isCancelled())
                return;

            if (files.isEmpty())
                throw new IOException(tr("No image files found."));

            GeoImageLayer.create(files, gpx);
        } finally {
            progressMonitor.finishTask();
        }
    }

    private void addRecursiveFiles(List<File> files, Set<String> visitedDirs, List<File> sel, ProgressMonitor progressMonitor) throws IOException {

        if (progressMonitor.isCancelled())
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
                    if (f.getName().toLowerCase().endsWith(".jpg")) {
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
