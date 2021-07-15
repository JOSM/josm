// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.io.importexport.ImageImporter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Loads a set of images, while displaying a dialog that indicates what the plugin is currently doing.
 * In facts, this object is instantiated with a list of files. These files may be JPEG files or
 * directories. In case of directories, they are scanned to find all the images they contain.
 * Then all the images that have be found are loaded as ImageEntry instances.
 *
 * @since 18035 (extracted from GeoImageLayer)
 */
final class ImagesLoader extends PleaseWaitRunnable {

    private boolean canceled;
    private GeoImageLayer layer;
    private final Collection<File> selection;
    private final Set<String> loadedDirectories = new HashSet<>();
    private final Set<String> errorMessages;
    private final GpxLayer gpxLayer;

    /**
     * Constructs a new {@code ImagesLoader}.
     * @param selection
     * @param gpxLayer
     */
    public ImagesLoader(Collection<File> selection, GpxLayer gpxLayer) {
        super(tr("Extracting GPS locations from EXIF"));
        this.selection = selection;
        this.gpxLayer = gpxLayer;
        errorMessages = new LinkedHashSet<>();
    }

    private void rememberError(String message) {
        this.errorMessages.add(message);
    }

    @Override
    protected void realRun() throws IOException {
        progressMonitor.subTask(tr("Starting directory scan"));
        Collection<File> files = new ArrayList<>();
        try {
            addRecursiveFiles(files, selection);
        } catch (IllegalStateException e) {
            Logging.debug(e);
            rememberError(e.getMessage());
        }

        if (canceled)
            return;
        progressMonitor.subTask(tr("Read photos..."));
        progressMonitor.setTicksCount(files.size());

        // read the image files
        List<ImageEntry> entries = new ArrayList<>(files.size());

        for (File f : files) {

            if (canceled) {
                break;
            }

            progressMonitor.subTask(tr("Reading {0}...", f.getName()));
            progressMonitor.worked(1);

            ImageEntry e = new ImageEntry(f);
            e.extractExif();
            entries.add(e);
        }
        layer = new GeoImageLayer(entries, gpxLayer);
        files.clear();
    }

    private void addRecursiveFiles(Collection<File> files, Collection<File> sel) {
        boolean nullFile = false;

        for (File f : sel) {

            if (canceled) {
                break;
            }

            if (f == null) {
                nullFile = true;

            } else if (f.isDirectory()) {
                String canonical = null;
                try {
                    canonical = f.getCanonicalPath();
                } catch (IOException e) {
                    Logging.error(e);
                    rememberError(tr("Unable to get canonical path for directory {0}\n",
                            f.getAbsolutePath()));
                }

                if (canonical == null || loadedDirectories.contains(canonical)) {
                    continue;
                } else {
                    loadedDirectories.add(canonical);
                }

                File[] children = f.listFiles(ImageImporter.FILE_FILTER_WITH_FOLDERS);
                if (children != null) {
                    progressMonitor.subTask(tr("Scanning directory {0}", f.getPath()));
                    addRecursiveFiles(files, Arrays.asList(children));
                } else {
                    rememberError(tr("Error while getting files from directory {0}\n", f.getPath()));
                }

            } else {
                files.add(f);
            }
        }

        if (nullFile) {
            throw new IllegalStateException(tr("One of the selected files was null"));
        }
    }

    private String formatErrorMessages() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        if (errorMessages.size() == 1) {
            sb.append(Utils.escapeReservedCharactersHTML(errorMessages.iterator().next()));
        } else {
            sb.append(Utils.joinAsHtmlUnorderedList(errorMessages));
        }
        sb.append("</html>");
        return sb.toString();
    }

    @Override
    protected void finish() {
        if (!errorMessages.isEmpty()) {
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    formatErrorMessages(),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
                    );
        }
        if (layer != null) {
            MainApplication.getLayerManager().addLayer(layer);

            if (!canceled && !layer.getImageData().getImages().isEmpty()) {
                boolean noGeotagFound = true;
                for (ImageEntry e : layer.getImageData().getImages()) {
                    if (e.getPos() != null) {
                        noGeotagFound = false;
                    }
                }
                if (noGeotagFound) {
                    new CorrelateGpxWithImages(layer).actionPerformed(null);
                }
            }
        }
    }

    @Override
    protected void cancel() {
        canceled = true;
    }
}