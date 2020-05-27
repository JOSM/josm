// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.actions.downloadtasks.AbstractDownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadNotesTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.ViewportData;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;

/**
 * Class defines the way data is fetched from the OSM server.
 * @since 12652
 */
public class OSMDownloadSource implements DownloadSource<List<IDownloadSourceType>> {
    /**
     * The simple name for the {@link OSMDownloadSourcePanel}
     * @since 12706
     */
    public static final String SIMPLE_NAME = "osmdownloadpanel";

    /** The possible methods to get data */
    static final List<IDownloadSourceType> DOWNLOAD_SOURCES = new ArrayList<>();
    static {
        // Order is important (determines button order, and what gets zoomed to)
        DOWNLOAD_SOURCES.add(new OsmDataDownloadType());
        DOWNLOAD_SOURCES.add(new GpsDataDownloadType());
        DOWNLOAD_SOURCES.add(new NotesDataDownloadType());
    }

    @Override
    public AbstractDownloadSourcePanel<List<IDownloadSourceType>> createPanel(DownloadDialog dialog) {
        return new OSMDownloadSourcePanel(this, dialog);
    }

    @Override
    public void doDownload(List<IDownloadSourceType> data, DownloadSettings settings) {
        Bounds bbox = settings.getDownloadBounds()
                .orElseThrow(() -> new IllegalArgumentException("OSM downloads requires bounds"));
        boolean zoom = settings.zoomToData();
        boolean newLayer = settings.asNewLayer();
        final List<Pair<AbstractDownloadTask<?>, Future<?>>> tasks = new ArrayList<>();
        IDownloadSourceType zoomTask = zoom ? data.stream().findFirst().orElse(null) : null;
        data.stream().filter(IDownloadSourceType::isEnabled).forEach(type -> {
            try {
                AbstractDownloadTask<?> task = type.getDownloadClass().getDeclaredConstructor().newInstance();
                task.setZoomAfterDownload(type.equals(zoomTask));
                Future<?> future = task.download(new DownloadParams().withNewLayer(newLayer), bbox, null);
                MainApplication.worker.submit(new PostDownloadHandler(task, future));
                if (zoom) {
                    tasks.add(new Pair<AbstractDownloadTask<?>, Future<?>>(task, future));
                }
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                Logging.error(e);
            }
        });

        if (zoom && tasks.size() > 1) {
            MainApplication.worker.submit(() -> {
                ProjectionBounds bounds = null;
                // Wait for completion of download jobs
                for (Pair<AbstractDownloadTask<?>, Future<?>> p : tasks) {
                    try {
                        p.b.get();
                        ProjectionBounds b = p.a.getDownloadProjectionBounds();
                        if (bounds == null) {
                            bounds = b;
                        } else if (b != null) {
                            bounds.extend(b);
                        }
                    } catch (InterruptedException | ExecutionException ex) {
                        Logging.warn(ex);
                    }
                }
                MapFrame map = MainApplication.getMap();
                // Zoom to the larger download bounds
                if (map != null && bounds != null) {
                    final ProjectionBounds pb = bounds;
                    GuiHelper.runInEDTAndWait(() -> map.mapView.zoomTo(new ViewportData(pb)));
                }
            });
        }
    }

    @Override
    public String getLabel() {
        return tr("Download from OSM");
    }

    @Override
    public boolean onlyExpert() {
        return false;
    }

    /**
     * @return The possible downloads that JOSM can make in the default Download
     *         screen
     * @since 16503
     */
    public static List<IDownloadSourceType> getDownloadTypes() {
        return Collections.unmodifiableList(DOWNLOAD_SOURCES);
    }

    /**
     * Get the instance of a data download type
     *
     * @param <T> The type to get
     * @param typeClazz The class of the type
     * @return The type instance
     * @since 16503
     */
    public static <T extends IDownloadSourceType> T getDownloadType(Class<T> typeClazz) {
        return DOWNLOAD_SOURCES.stream().filter(typeClazz::isInstance).map(typeClazz::cast).findFirst().orElse(null);
    }

    /**
     * @param type The IDownloadSourceType object to remove
     * @return true See {@link List#remove}, but it also returns false if the
     * parameter is a class from JOSM core.
     * @since 16503
     */
    public static boolean removeDownloadType(IDownloadSourceType type) {
        boolean modified = false;
        if (!(type instanceof OsmDataDownloadType) && !(type instanceof GpsDataDownloadType)
                && !(type instanceof NotesDataDownloadType)) {
            modified = DOWNLOAD_SOURCES.remove(type);
        }
        return modified;
    }

    /**
     * Add a download type to the default JOSM download window
     *
     * @param type The initialized type to download
     * @return See {@link List#add}, but it also returns false if the class
     * already has an instance in the list or it is a class from JOSM core.
     * @since 16503
     */
    public static boolean addDownloadType(IDownloadSourceType type) {
        boolean modified = false;
        if (!(type instanceof OsmDataDownloadType) && !(type instanceof GpsDataDownloadType)
                && !(type instanceof NotesDataDownloadType)
                || DOWNLOAD_SOURCES.stream()
                        .noneMatch(possibility -> type.getClass().isInstance(possibility))) {
            modified = DOWNLOAD_SOURCES.add(type);
        } else {
            throw new IllegalArgumentException("There can only be one instance of a class added, and it cannot be a built-in class.");
        }
        return modified;
    }

    /**
     * The GUI representation of the OSM download source.
     * @since 12652
     */
    public static class OSMDownloadSourcePanel extends AbstractDownloadSourcePanel<List<IDownloadSourceType>> {
        private final JLabel sizeCheck = new JLabel();

        /** This is used to keep track of the components for download sources, and to dynamically update/remove them */
        private JPanel downloadSourcesPanel;

        private ChangeListener checkboxChangeListener;

        /**
         * Label used in front of data types available for download. Made public for reuse in other download dialogs.
         * @since 16155
         */
        public static final String DATA_SOURCES_AND_TYPES = marktr("Data Sources and Types:");

        /**
         * Creates a new {@link OSMDownloadSourcePanel}.
         * @param dialog the parent download dialog, as {@code DownloadDialog.getInstance()} might not be initialized yet
         * @param ds The osm download source the panel is for.
         * @since 12900
         */
        public OSMDownloadSourcePanel(OSMDownloadSource ds, DownloadDialog dialog) {
            super(ds);
            setLayout(new GridBagLayout());

            // size check depends on selected data source
            checkboxChangeListener = e ->
                    dialog.getSelectedDownloadArea().ifPresent(this::updateSizeCheck);

            // adding the download tasks
            add(new JLabel(tr(DATA_SOURCES_AND_TYPES)), GBC.std().insets(5, 5, 1, 5).anchor(GBC.CENTER));
            Font labelFont = sizeCheck.getFont();
            sizeCheck.setFont(labelFont.deriveFont(Font.PLAIN, labelFont.getSize()));

            downloadSourcesPanel = new JPanel();
            add(downloadSourcesPanel, GBC.eol().anchor(GBC.EAST));
            updateSources();
            add(sizeCheck, GBC.eol().anchor(GBC.EAST).insets(5, 5, 5, 2));

            setMinimumSize(new Dimension(450, 115));
        }

        /**
         * Update the source list for downloading data
         */
        protected void updateSources() {
            downloadSourcesPanel.removeAll();
            DOWNLOAD_SOURCES
                .forEach(obj -> downloadSourcesPanel.add(obj.getCheckBox(checkboxChangeListener), GBC.std().insets(1, 5, 1, 5)));
        }

        @Override
        public List<IDownloadSourceType> getData() {
            return DOWNLOAD_SOURCES;
        }

        @Override
        public void rememberSettings() {
            DOWNLOAD_SOURCES.forEach(type -> type.getBooleanProperty().put(type.getCheckBox().isSelected()));
        }

        @Override
        public void restoreSettings() {
            updateSources();
            DOWNLOAD_SOURCES.forEach(type -> type.getCheckBox().setSelected(type.isEnabled()));
        }

        @Override
        public void setVisible(boolean aFlag) {
            super.setVisible(aFlag);
            updateSources();
        }

        @Override
        public boolean checkDownload(DownloadSettings settings) {
            /*
             * It is mandatory to specify the area to download from OSM.
             */
            if (!settings.getDownloadBounds().isPresent()) {
                JOptionPane.showMessageDialog(
                        this.getParent(),
                        tr("Please select a download area first."),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );

                return false;
            }

            /*
             * Checks if the user selected the type of data to download. At least one the following
             * must be chosen : raw osm data, gpx data, notes.
             * If none of those are selected, then the corresponding dialog is shown to inform the user.
             */
            if (DOWNLOAD_SOURCES.stream().noneMatch(IDownloadSourceType::isEnabled)) {
                StringBuilder line1 = new StringBuilder("<html>").append(tr("None of"));
                StringBuilder line2 = new StringBuilder(tr("Please choose to either download"));

                DOWNLOAD_SOURCES.forEach(type -> {
                    line1.append(" <strong>").append(type.getCheckBox().getText()).append("</strong> ");
                    line2.append(' ').append(type.getCheckBox().getText()).append(tr(", or"));
                });
                line1.append(tr("is enabled.")).append("<br>");
                line2.append(tr(" all.")).append("</html>");
                JOptionPane.showMessageDialog(
                        this.getParent(),
                        line1.append(line2).toString(),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );

                return false;
            }

            this.rememberSettings();

            return true;
        }

        /**
         * Replies true if the user selected to download OSM data
         *
         * @return true if the user selected to download OSM data
         * @deprecated since xxx -- use {@link OSMDownloadSource#getDownloadTypes} with
         *             {@code get(0).getCheckBox().isSelected()}
         */
        @Deprecated
        public boolean isDownloadOsmData() {
            return DOWNLOAD_SOURCES.get(0).getCheckBox().isSelected();
        }

        /**
         * Replies true if the user selected to download GPX data
         *
         * @return true if the user selected to download GPX data
         * @deprecated since xxx -- use {@link OSMDownloadSource#getDownloadTypes} with
         *             {@code get(1).getCheckBox().isSelected()}
         */
        @Deprecated
        public boolean isDownloadGpxData() {
            return DOWNLOAD_SOURCES.get(1).getCheckBox().isSelected();
        }

        /**
         * Replies true if user selected to download notes
         *
         * @return true if user selected to download notes
         * @deprecated since xxx -- use {@link OSMDownloadSource#getDownloadTypes} with
         *             {@code get(2).getCheckBox().isSelected()}
         */
        @Deprecated
        public boolean isDownloadNotes() {
            return DOWNLOAD_SOURCES.get(2).getCheckBox().isSelected();
        }

        @Override
        public Icon getIcon() {
            return ImageProvider.get("download");
        }

        @Override
        public void boundingBoxChanged(Bounds bbox) {
            updateSizeCheck(bbox);
        }

        @Override
        public String getSimpleName() {
            return SIMPLE_NAME;
        }

        private void updateSizeCheck(Bounds bbox) {
            if (bbox == null) {
                sizeCheck.setText(tr("No area selected yet"));
                sizeCheck.setForeground(Color.darkGray);
                return;
            }

            displaySizeCheckResult(DOWNLOAD_SOURCES.stream()
                    .anyMatch(type -> type.isDownloadAreaTooLarge(bbox)));
        }

        private void displaySizeCheckResult(boolean isAreaTooLarge) {
            if (isAreaTooLarge) {
                sizeCheck.setText(tr("Download area too large; will probably be rejected by server"));
                sizeCheck.setForeground(Color.red);
            } else {
                sizeCheck.setText(tr("Download area ok, size probably acceptable to server"));
                sizeCheck.setForeground(Color.darkGray);
            }
        }

    }

    /**
     * Encapsulates data that is required to download from the OSM server.
     */
    static class OSMDownloadData {

        private List<IDownloadSourceType> downloadPossibilities;

        /**
         * @param downloadPossibilities A list of DataDownloadTypes (instantiated, with
         *                              options set)
         */
        OSMDownloadData(List<IDownloadSourceType> downloadPossibilities) {
            this.downloadPossibilities = downloadPossibilities;
        }

        /**
         * @return A list of DataDownloadTypes (instantiated, with options set)
         */
        public List<IDownloadSourceType> getDownloadPossibilities() {
            return downloadPossibilities;
        }
    }

    private static class OsmDataDownloadType implements IDownloadSourceType {
        static final BooleanProperty IS_ENABLED = new BooleanProperty("download.osm.data", true);
        JCheckBox cbDownloadOsmData;

        @Override
        public JCheckBox getCheckBox(ChangeListener checkboxChangeListener) {
            if (cbDownloadOsmData == null) {
                cbDownloadOsmData = new JCheckBox(tr("OpenStreetMap data"), true);
                cbDownloadOsmData.setToolTipText(tr("Select to download OSM data in the selected download area."));
                cbDownloadOsmData.getModel().addChangeListener(checkboxChangeListener);
            }
            if (checkboxChangeListener != null) {
                cbDownloadOsmData.getModel().addChangeListener(checkboxChangeListener);
            }
            return cbDownloadOsmData;
        }

        @Override
        public Class<? extends AbstractDownloadTask<DataSet>> getDownloadClass() {
            return DownloadOsmTask.class;
        }

        @Override
        public BooleanProperty getBooleanProperty() {
            return IS_ENABLED;
        }

        @Override
        public boolean isDownloadAreaTooLarge(Bounds bound) {
            // see max_request_area in
            // https://github.com/openstreetmap/openstreetmap-website/blob/master/config/example.application.yml
            return bound.getArea() > Config.getPref().getDouble("osm-server.max-request-area", 0.25);
        }
    }

    private static class GpsDataDownloadType implements IDownloadSourceType {
        static final BooleanProperty IS_ENABLED = new BooleanProperty("download.osm.gps", false);
        private JCheckBox cbDownloadGpxData;

        @Override
        public JCheckBox getCheckBox(ChangeListener checkboxChangeListener) {
            if (cbDownloadGpxData == null) {
                cbDownloadGpxData = new JCheckBox(tr("Raw GPS data"));
                cbDownloadGpxData.setToolTipText(tr("Select to download GPS traces in the selected download area."));
            }
            if (checkboxChangeListener != null) {
                cbDownloadGpxData.getModel().addChangeListener(checkboxChangeListener);
            }

            return cbDownloadGpxData;
        }

        @Override
        public Class<? extends AbstractDownloadTask<GpxData>> getDownloadClass() {
            return DownloadGpsTask.class;
        }

        @Override
        public BooleanProperty getBooleanProperty() {
            return IS_ENABLED;
        }

        @Override
        public boolean isDownloadAreaTooLarge(Bounds bound) {
            return false;
        }
    }

    private static class NotesDataDownloadType implements IDownloadSourceType {
        static final BooleanProperty IS_ENABLED = new BooleanProperty("download.osm.notes", false);
        private JCheckBox cbDownloadNotes;

        @Override
        public JCheckBox getCheckBox(ChangeListener checkboxChangeListener) {
            if (cbDownloadNotes == null) {
                cbDownloadNotes = new JCheckBox(tr("Notes"));
                cbDownloadNotes.setToolTipText(tr("Select to download notes in the selected download area."));
            }
            if (checkboxChangeListener != null) {
                cbDownloadNotes.getModel().addChangeListener(checkboxChangeListener);
            }

            return cbDownloadNotes;
        }

        @Override
        public Class<? extends AbstractDownloadTask<NoteData>> getDownloadClass() {
            return DownloadNotesTask.class;
        }

        @Override
        public BooleanProperty getBooleanProperty() {
            return IS_ENABLED;
        }

        @Override
        public boolean isDownloadAreaTooLarge(Bounds bound) {
            // see max_note_request_area in
            // https://github.com/openstreetmap/openstreetmap-website/blob/master/config/example.application.yml
            return bound.getArea() > Config.getPref().getDouble("osm-server.max-request-area-notes", 25);
        }
    }
}
