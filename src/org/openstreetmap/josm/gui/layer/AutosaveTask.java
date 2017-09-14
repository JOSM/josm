// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.openstreetmap.josm.actions.OpenFileAction.OpenFileTask;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter.Listener;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.io.importexport.OsmExporter;
import org.openstreetmap.josm.gui.io.importexport.OsmImporter;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Saves data layers periodically so they can be recovered in case of a crash.
 *
 * There are 2 directories
 *  - autosave dir: copies of the currently open data layers are saved here every
 *      PROP_INTERVAL seconds. When a data layer is closed normally, the corresponding
 *      files are removed. If this dir is non-empty on start, JOSM assumes
 *      that it crashed last time.
 *  - deleted layers dir: "secondary archive" - when autosaved layers are restored
 *      they are copied to this directory. We cannot keep them in the autosave folder,
 *      but just deleting it would be dangerous: Maybe a feature inside the file
 *      caused JOSM to crash. If the data is valuable, the user can still try to
 *      open with another versions of JOSM or fix the problem manually.
 *
 *      The deleted layers dir keeps at most PROP_DELETED_LAYERS files.
 *
 * @since  3378 (creation)
 * @since 10386 (new LayerChangeListener interface)
 */
public class AutosaveTask extends TimerTask implements LayerChangeListener, Listener {

    private static final char[] ILLEGAL_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};
    private static final String AUTOSAVE_DIR = "autosave";
    private static final String DELETED_LAYERS_DIR = "autosave/deleted_layers";

    /**
     * If autosave is enabled
     */
    public static final BooleanProperty PROP_AUTOSAVE_ENABLED = new BooleanProperty("autosave.enabled", true);
    /**
     * The number of files to store per layer
     */
    public static final IntegerProperty PROP_FILES_PER_LAYER = new IntegerProperty("autosave.filesPerLayer", 1);
    /**
     * How many deleted layers should be stored
     */
    public static final IntegerProperty PROP_DELETED_LAYERS = new IntegerProperty("autosave.deletedLayersBackupCount", 5);
    /**
     * The autosave interval, in seconds
     */
    public static final IntegerProperty PROP_INTERVAL = new IntegerProperty("autosave.interval", (int) TimeUnit.MINUTES.toSeconds(5));
    /**
     * The maximum number of autosave files to store
     */
    public static final IntegerProperty PROP_INDEX_LIMIT = new IntegerProperty("autosave.index-limit", 1000);
    /**
     * Defines if a notification should be displayed after each autosave
     */
    public static final BooleanProperty PROP_NOTIFICATION = new BooleanProperty("autosave.notification", false);

    protected static final class AutosaveLayerInfo {
        private final OsmDataLayer layer;
        private String layerName;
        private String layerFileName;
        private final Deque<File> backupFiles = new LinkedList<>();

        AutosaveLayerInfo(OsmDataLayer layer) {
            this.layer = layer;
        }
    }

    private final DataSetListenerAdapter datasetAdapter = new DataSetListenerAdapter(this);
    private final Set<DataSet> changedDatasets = new HashSet<>();
    private final List<AutosaveLayerInfo> layersInfo = new ArrayList<>();
    private final Object layersLock = new Object();
    private final Deque<File> deletedLayers = new LinkedList<>();

    private final File autosaveDir = new File(Config.getDirs().getUserDataDirectory(), AUTOSAVE_DIR);
    private final File deletedLayersDir = new File(Config.getDirs().getUserDataDirectory(), DELETED_LAYERS_DIR);

    /**
     * Replies the autosave directory.
     * @return the autosave directory
     * @since 10299
     */
    public final Path getAutosaveDir() {
        return autosaveDir.toPath();
    }

    /**
     * Starts the autosave background task.
     */
    public void schedule() {
        if (PROP_INTERVAL.get() > 0) {

            if (!autosaveDir.exists() && !autosaveDir.mkdirs()) {
                Logging.warn(tr("Unable to create directory {0}, autosave will be disabled", autosaveDir.getAbsolutePath()));
                return;
            }
            if (!deletedLayersDir.exists() && !deletedLayersDir.mkdirs()) {
                Logging.warn(tr("Unable to create directory {0}, autosave will be disabled", deletedLayersDir.getAbsolutePath()));
                return;
            }

            File[] files = deletedLayersDir.listFiles();
            if (files != null) {
                for (File f: files) {
                    deletedLayers.add(f); // FIXME: sort by mtime
                }
            }

            new Timer(true).schedule(this, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(PROP_INTERVAL.get()));
            MainApplication.getLayerManager().addAndFireLayerChangeListener(this);
        }
    }

    private static String getFileName(String layerName, int index) {
        String result = layerName;
        for (char illegalCharacter : ILLEGAL_CHARACTERS) {
            result = result.replaceAll(Pattern.quote(String.valueOf(illegalCharacter)),
                    '&' + String.valueOf((int) illegalCharacter) + ';');
        }
        if (index != 0) {
            result = result + '_' + index;
        }
        return result;
    }

    private void setLayerFileName(AutosaveLayerInfo layer) {
        int index = 0;
        while (true) {
            String filename = getFileName(layer.layer.getName(), index);
            boolean foundTheSame = false;
            for (AutosaveLayerInfo info: layersInfo) {
                if (info != layer && filename.equals(info.layerFileName)) {
                    foundTheSame = true;
                    break;
                }
            }

            if (!foundTheSame) {
                layer.layerFileName = filename;
                return;
            }

            index++;
        }
    }

    protected File getNewLayerFile(AutosaveLayerInfo layer, Date now, int startIndex) {
        int index = startIndex;
        while (true) {
            String filename = String.format("%1$s_%2$tY%2$tm%2$td_%2$tH%2$tM%2$tS%2$tL%3$s",
                    layer.layerFileName, now, index == 0 ? "" : ('_' + Integer.toString(index)));
            File result = new File(autosaveDir, filename + '.' + Config.getPref().get("autosave.extension", "osm"));
            try {
                if (index > PROP_INDEX_LIMIT.get())
                    throw new IOException("index limit exceeded");
                if (result.createNewFile()) {
                    createNewPidFile(autosaveDir, filename);
                    return result;
                } else {
                    Logging.warn(tr("Unable to create file {0}, other filename will be used", result.getAbsolutePath()));
                }
            } catch (IOException e) {
                Logging.log(Logging.LEVEL_ERROR, tr("IOError while creating file, autosave will be skipped: {0}", e.getMessage()), e);
                return null;
            }
            index++;
        }
    }

    private static void createNewPidFile(File autosaveDir, String filename) {
        File pidFile = new File(autosaveDir, filename+".pid");
        try (PrintStream ps = new PrintStream(pidFile, "UTF-8")) {
            ps.println(ManagementFactory.getRuntimeMXBean().getName());
        } catch (IOException | SecurityException t) {
            Logging.error(t);
        }
    }

    private void savelayer(AutosaveLayerInfo info) {
        if (!info.layer.getName().equals(info.layerName)) {
            setLayerFileName(info);
            info.layerName = info.layer.getName();
        }
        if (changedDatasets.remove(info.layer.data)) {
            File file = getNewLayerFile(info, new Date(), 0);
            if (file != null) {
                info.backupFiles.add(file);
                new OsmExporter().exportData(file, info.layer, true /* no backup with appended ~ */);
            }
        }
        while (info.backupFiles.size() > PROP_FILES_PER_LAYER.get()) {
            File oldFile = info.backupFiles.remove();
            if (Utils.deleteFile(oldFile, marktr("Unable to delete old backup file {0}"))) {
                Utils.deleteFile(getPidFile(oldFile), marktr("Unable to delete old backup file {0}"));
            }
        }
    }

    @Override
    public void run() {
        synchronized (layersLock) {
            try {
                for (AutosaveLayerInfo info: layersInfo) {
                    savelayer(info);
                }
                changedDatasets.clear();
                if (PROP_NOTIFICATION.get() && !layersInfo.isEmpty()) {
                    GuiHelper.runInEDT(this::displayNotification);
                }
            } catch (RuntimeException t) { // NOPMD
                // Don't let exception stop time thread
                Logging.error("Autosave failed:");
                Logging.error(t);
            }
        }
    }

    protected void displayNotification() {
        new Notification(tr("Your work has been saved automatically."))
        .setDuration(Notification.TIME_SHORT)
        .show();
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // Do nothing
    }

    private void registerNewlayer(OsmDataLayer layer) {
        synchronized (layersLock) {
            layer.data.addDataSetListener(datasetAdapter);
            layersInfo.add(new AutosaveLayerInfo(layer));
        }
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        if (e.getAddedLayer() instanceof OsmDataLayer) {
            registerNewlayer((OsmDataLayer) e.getAddedLayer());
        }
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        if (e.getRemovedLayer() instanceof OsmDataLayer) {
            synchronized (layersLock) {
                OsmDataLayer osmLayer = (OsmDataLayer) e.getRemovedLayer();
                osmLayer.data.removeDataSetListener(datasetAdapter);
                Iterator<AutosaveLayerInfo> it = layersInfo.iterator();
                while (it.hasNext()) {
                    AutosaveLayerInfo info = it.next();
                    if (info.layer == osmLayer) {

                        savelayer(info);
                        File lastFile = info.backupFiles.pollLast();
                        if (lastFile != null) {
                            moveToDeletedLayersFolder(lastFile);
                        }
                        for (File file: info.backupFiles) {
                            if (Utils.deleteFile(file)) {
                                Utils.deleteFile(getPidFile(file));
                            }
                        }

                        it.remove();
                    }
                }
            }
        }
    }

    @Override
    public void processDatasetEvent(AbstractDatasetChangedEvent event) {
        changedDatasets.add(event.getDataset());
    }

    protected File getPidFile(File osmFile) {
        return new File(autosaveDir, osmFile.getName().replaceFirst("[.][^.]+$", ".pid"));
    }

    /**
     * Replies the list of .osm files still present in autosave dir, that are not currently managed by another instance of JOSM.
     * These files are hence unsaved layers from an old instance of JOSM that crashed and may be recovered by this instance.
     * @return The list of .osm files still present in autosave dir, that are not currently managed by another instance of JOSM
     */
    public List<File> getUnsavedLayersFiles() {
        List<File> result = new ArrayList<>();
        File[] files = autosaveDir.listFiles(OsmImporter.FILE_FILTER);
        if (files == null)
            return result;
        for (File file: files) {
            if (file.isFile()) {
                boolean skipFile = false;
                File pidFile = getPidFile(file);
                if (pidFile.exists()) {
                    try (BufferedReader reader = Files.newBufferedReader(pidFile.toPath(), StandardCharsets.UTF_8)) {
                        String jvmId = reader.readLine();
                        if (jvmId != null) {
                            String pid = jvmId.split("@")[0];
                            skipFile = jvmPerfDataFileExists(pid);
                        }
                    } catch (IOException | SecurityException t) {
                        Logging.error(t);
                    }
                }
                if (!skipFile) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    private static boolean jvmPerfDataFileExists(final String jvmId) {
        File jvmDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "hsperfdata_" + System.getProperty("user.name"));
        if (jvmDir.exists() && jvmDir.canRead()) {
            File[] files = jvmDir.listFiles((FileFilter) file -> file.getName().equals(jvmId) && file.isFile());
            return files != null && files.length == 1;
        }
        return false;
    }

    /**
     * Recover the unsaved layers and open them asynchronously.
     * @return A future that can be used to wait for the completion of this task.
     */
    public Future<?> recoverUnsavedLayers() {
        List<File> files = getUnsavedLayersFiles();
        final OpenFileTask openFileTsk = new OpenFileTask(files, null, tr("Restoring files"));
        final Future<?> openFilesFuture = MainApplication.worker.submit(openFileTsk);
        return MainApplication.worker.submit(() -> {
            try {
                // Wait for opened tasks to be generated.
                openFilesFuture.get();
                for (File f: openFileTsk.getSuccessfullyOpenedFiles()) {
                    moveToDeletedLayersFolder(f);
                }
            } catch (InterruptedException | ExecutionException e) {
                Logging.error(e);
            }
        });
    }

    /**
     * Move file to the deleted layers directory.
     * If moving does not work, it will try to delete the file directly.
     * Afterwards, if the number of deleted layers gets larger than PROP_DELETED_LAYERS,
     * some files in the deleted layers directory will be removed.
     *
     * @param f the file, usually from the autosave dir
     */
    private void moveToDeletedLayersFolder(File f) {
        File backupFile = new File(deletedLayersDir, f.getName());
        File pidFile = getPidFile(f);

        if (backupFile.exists()) {
            deletedLayers.remove(backupFile);
            Utils.deleteFile(backupFile, marktr("Unable to delete old backup file {0}"));
        }
        if (f.renameTo(backupFile)) {
            deletedLayers.add(backupFile);
            Utils.deleteFile(pidFile);
        } else {
            Logging.warn(String.format("Could not move autosaved file %s to %s folder", f.getName(), deletedLayersDir.getName()));
            // we cannot move to deleted folder, so just try to delete it directly
            if (Utils.deleteFile(f, marktr("Unable to delete backup file {0}"))) {
                Utils.deleteFile(pidFile, marktr("Unable to delete PID file {0}"));
            }
        }
        while (deletedLayers.size() > PROP_DELETED_LAYERS.get()) {
            File next = deletedLayers.remove();
            if (next == null) {
                break;
            }
            Utils.deleteFile(next, marktr("Unable to delete archived backup file {0}"));
        }
    }

    /**
     * Mark all unsaved layers as deleted. They are still preserved in the deleted layers folder.
     */
    public void discardUnsavedLayers() {
        for (File f: getUnsavedLayersFiles()) {
            moveToDeletedLayersFolder(f);
        }
    }
}
