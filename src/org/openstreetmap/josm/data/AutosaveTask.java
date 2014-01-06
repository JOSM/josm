// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
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
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.OpenFileAction.OpenFileTask;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter.Listener;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmExporter;
import org.openstreetmap.josm.io.OsmImporter;
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
 */
public class AutosaveTask extends TimerTask implements LayerChangeListener, Listener {

    private static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };
    private static final String AUTOSAVE_DIR = "autosave";
    private static final String DELETED_LAYERS_DIR = "autosave/deleted_layers";

    public static final BooleanProperty PROP_AUTOSAVE_ENABLED = new BooleanProperty("autosave.enabled", true);
    public static final IntegerProperty PROP_FILES_PER_LAYER = new IntegerProperty("autosave.filesPerLayer", 1);
    public static final IntegerProperty PROP_DELETED_LAYERS = new IntegerProperty("autosave.deletedLayersBackupCount", 5);
    public static final IntegerProperty PROP_INTERVAL = new IntegerProperty("autosave.interval", 5 * 60);
    public static final IntegerProperty PROP_INDEX_LIMIT = new IntegerProperty("autosave.index-limit", 1000);
    /** Defines if a notification should be displayed after each autosave */
    public static final BooleanProperty PROP_NOTIFICATION = new BooleanProperty("autosave.notification", false);

    private static class AutosaveLayerInfo {
        OsmDataLayer layer;
        String layerName;
        String layerFileName;
        final Deque<File> backupFiles = new LinkedList<File>();
    }

    private final DataSetListenerAdapter datasetAdapter = new DataSetListenerAdapter(this);
    private final Set<DataSet> changedDatasets = new HashSet<DataSet>();
    private final List<AutosaveLayerInfo> layersInfo = new ArrayList<AutosaveLayerInfo>();
    private Timer timer;
    private final Object layersLock = new Object();
    private final Deque<File> deletedLayers = new LinkedList<File>();

    private final File autosaveDir = new File(Main.pref.getPreferencesDir() + AUTOSAVE_DIR);
    private final File deletedLayersDir = new File(Main.pref.getPreferencesDir() + DELETED_LAYERS_DIR);

    public void schedule() {
        if (PROP_INTERVAL.get() > 0) {

            if (!autosaveDir.exists() && !autosaveDir.mkdirs()) {
                Main.warn(tr("Unable to create directory {0}, autosave will be disabled", autosaveDir.getAbsolutePath()));
                return;
            }
            if (!deletedLayersDir.exists() && !deletedLayersDir.mkdirs()) {
                Main.warn(tr("Unable to create directory {0}, autosave will be disabled", deletedLayersDir.getAbsolutePath()));
                return;
            }

            for (File f: deletedLayersDir.listFiles()) {
                deletedLayers.add(f); // FIXME: sort by mtime
            }

            timer = new Timer(true);
            timer.schedule(this, 1000, PROP_INTERVAL.get() * 1000);
            MapView.addLayerChangeListener(this);
            if (Main.isDisplayingMapView()) {
                for (OsmDataLayer l: Main.map.mapView.getLayersOfType(OsmDataLayer.class)) {
                    registerNewlayer(l);
                }
            }
        }
    }

    private String getFileName(String layerName, int index) {
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

    private File getNewLayerFile(AutosaveLayerInfo layer) {
        int index = 0;
        Date now = new Date();
        while (true) {
            String filename = String.format("%1$s_%2$tY%2$tm%2$td_%2$tH%2$tM%3$s", layer.layerFileName, now, index == 0?"":"_" + index);
            File result = new File(autosaveDir, filename+".osm");
            try {
                if (result.createNewFile()) {
                    try {
                        File pidFile = new File(autosaveDir, filename+".pid");
                        PrintStream ps = new PrintStream(pidFile);
                        ps.println(ManagementFactory.getRuntimeMXBean().getName());
                        Utils.close(ps);
                    } catch (Throwable t) {
                        Main.error(t);
                    }
                    return result;
                } else {
                    Main.warn(tr("Unable to create file {0}, other filename will be used", result.getAbsolutePath()));
                    if (index > PROP_INDEX_LIMIT.get())
                        throw new IOException("index limit exceeded");
                }
            } catch (IOException e) {
                Main.error(tr("IOError while creating file, autosave will be skipped: {0}", e.getMessage()));
                return null;
            }
            index++;
        }
    }

    private void savelayer(AutosaveLayerInfo info) throws IOException {
        if (!info.layer.getName().equals(info.layerName)) {
            setLayerFileName(info);
            info.layerName = info.layer.getName();
        }
        if (changedDatasets.remove(info.layer.data)) {
            File file = getNewLayerFile(info);
            if (file != null) {
                info.backupFiles.add(file);
                new OsmExporter().exportData(file, info.layer, true /* no backup with appended ~ */);
            }
        }
        while (info.backupFiles.size() > PROP_FILES_PER_LAYER.get()) {
            File oldFile = info.backupFiles.remove();
            if (!oldFile.delete()) {
                Main.warn(tr("Unable to delete old backup file {0}", oldFile.getAbsolutePath()));
            } else {
                getPidFile(oldFile).delete();
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
                    displayNotification();
                }
            } catch (Throwable t) {
                // Don't let exception stop time thread
                Main.error("Autosave failed:");
                Main.error(t);
            }
        }
    }

    protected void displayNotification() {
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                new Notification(tr("Your work has been saved automatically."))
                .setDuration(Notification.TIME_SHORT)
                .show();
            }
        });
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        // Do nothing
    }

    private void registerNewlayer(OsmDataLayer layer) {
        synchronized (layersLock) {
            layer.data.addDataSetListener(datasetAdapter);
            AutosaveLayerInfo info = new AutosaveLayerInfo();
            info.layer = layer;
            layersInfo.add(info);
        }
    }

    @Override
    public void layerAdded(Layer newLayer) {
        if (newLayer instanceof OsmDataLayer) {
            registerNewlayer((OsmDataLayer) newLayer);
        }
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        if (oldLayer instanceof OsmDataLayer) {
            synchronized (layersLock) {
                OsmDataLayer osmLayer = (OsmDataLayer) oldLayer;
                osmLayer.data.removeDataSetListener(datasetAdapter);
                Iterator<AutosaveLayerInfo> it = layersInfo.iterator();
                while (it.hasNext()) {
                    AutosaveLayerInfo info = it.next();
                    if (info.layer == osmLayer) {

                        try {
                            savelayer(info);
                            File lastFile = info.backupFiles.pollLast();
                            if (lastFile != null) {
                                moveToDeletedLayersFolder(lastFile);
                            }
                            for (File file: info.backupFiles) {
                                if (file.delete()) {
                                    getPidFile(file).delete();
                                }
                            }
                        } catch (IOException e) {
                            Main.error(tr("Error while creating backup of removed layer: {0}", e.getMessage()));
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

    private final File getPidFile(File osmFile) {
        return new File(autosaveDir, osmFile.getName().replaceFirst("[.][^.]+$", ".pid"));
    }

    /**
     * Replies the list of .osm files still present in autosave dir, that are not currently managed by another instance of JOSM.
     * These files are hence unsaved layers from an old instance of JOSM that crashed and may be recovered by this instance.
     * @return The list of .osm files still present in autosave dir, that are not currently managed by another instance of JOSM
     */
    public List<File> getUnsavedLayersFiles() {
        List<File> result = new ArrayList<File>();
        File[] files = autosaveDir.listFiles(OsmImporter.FILE_FILTER);
        if (files == null)
            return result;
        for (File file: files) {
            if (file.isFile()) {
                boolean skipFile = false;
                File pidFile = getPidFile(file);
                if (pidFile.exists()) {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(pidFile)));
                        try {
                            String jvmId = reader.readLine();
                            if (jvmId != null) {
                                String pid = jvmId.split("@")[0];
                                skipFile = jvmPerfDataFileExists(pid);
                            }
                        } catch (Throwable t) {
                            Main.error(t);
                        } finally {
                            Utils.close(reader);
                        }
                    } catch (Throwable t) {
                        Main.error(t);
                    }
                }
                if (!skipFile) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    private boolean jvmPerfDataFileExists(final String jvmId) {
        File jvmDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "hsperfdata_" + System.getProperty("user.name"));
        if (jvmDir.exists() && jvmDir.canRead()) {
            File[] files = jvmDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().equals(jvmId) && file.isFile();
                }
            });
            return files != null && files.length == 1;
        }
        return false;
    }

    public void recoverUnsavedLayers() {
        List<File> files = getUnsavedLayersFiles();
        final OpenFileTask openFileTsk = new OpenFileTask(files, null, tr("Restoring files"));
        Main.worker.submit(openFileTsk);
        Main.worker.submit(new Runnable() {
            @Override
            public void run() {
                for (File f: openFileTsk.getSuccessfullyOpenedFiles()) {
                    moveToDeletedLayersFolder(f);
                }
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
            if (!backupFile.delete()) {
                Main.warn(String.format("Could not delete old backup file %s", backupFile));
            }
        }
        if (f.renameTo(backupFile)) {
            deletedLayers.add(backupFile);
            pidFile.delete();
        } else {
            Main.warn(String.format("Could not move autosaved file %s to %s folder", f.getName(), deletedLayersDir.getName()));
            // we cannot move to deleted folder, so just try to delete it directly
            if (!f.delete()) {
                Main.warn(String.format("Could not delete backup file %s", f));
            } else {
                pidFile.delete();
            }
        }
        while (deletedLayers.size() > PROP_DELETED_LAYERS.get()) {
            File next = deletedLayers.remove();
            if (next == null) {
                break;
            }
            if (!next.delete()) {
                Main.warn(String.format("Could not delete archived backup file %s", next));
            }
        }
    }

    public void dicardUnsavedLayers() {
        for (File f: getUnsavedLayersFiles()) {
            moveToDeletedLayersFolder(f);
        }
    }
}
