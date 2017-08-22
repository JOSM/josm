// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * Dialog displayed to download OSM and/or GPS data from OSM server.
 */
public class DownloadDialog extends JDialog {
    private static final IntegerProperty DOWNLOAD_TAB = new IntegerProperty("download.tab", 0);

    private static final BooleanProperty DOWNLOAD_AUTORUN = new BooleanProperty("download.autorun", false);
    private static final BooleanProperty DOWNLOAD_OSM = new BooleanProperty("download.osm", true);
    private static final BooleanProperty DOWNLOAD_GPS = new BooleanProperty("download.gps", false);
    private static final BooleanProperty DOWNLOAD_NOTES = new BooleanProperty("download.notes", false);
    private static final BooleanProperty DOWNLOAD_NEWLAYER = new BooleanProperty("download.newlayer", false);
    private static final BooleanProperty DOWNLOAD_ZOOMTODATA = new BooleanProperty("download.zoomtodata", true);

    /** the unique instance of the download dialog */
    private static DownloadDialog instance;

    /**
     * Replies the unique instance of the download dialog
     *
     * @return the unique instance of the download dialog
     */
    public static synchronized DownloadDialog getInstance() {
        if (instance == null) {
            instance = new DownloadDialog(Main.parent);
        }
        return instance;
    }

    protected SlippyMapChooser slippyMapChooser;
    protected final transient List<DownloadSelection> downloadSelections = new ArrayList<>();
    protected final JTabbedPane tpDownloadAreaSelectors = new JTabbedPane();
    protected JCheckBox cbNewLayer;
    protected JCheckBox cbStartup;
    protected JCheckBox cbZoomToDownloadedData;
    protected final JLabel sizeCheck = new JLabel();
    protected transient Bounds currentBounds;
    protected boolean canceled;

    protected JCheckBox cbDownloadOsmData;
    protected JCheckBox cbDownloadGpxData;
    protected JCheckBox cbDownloadNotes;
    /** the download action and button */
    private final DownloadAction actDownload = new DownloadAction();
    protected final JButton btnDownload = new JButton(actDownload);

    protected final JPanel buildMainPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());

        // size check depends on selected data source
        final ChangeListener checkboxChangeListener = e -> updateSizeCheck();

        // adding the download tasks
        pnl.add(new JLabel(tr("Data Sources and Types:")), GBC.std().insets(5, 5, 1, 5));
        cbDownloadOsmData = new JCheckBox(tr("OpenStreetMap data"), true);
        cbDownloadOsmData.setToolTipText(tr("Select to download OSM data in the selected download area."));
        cbDownloadOsmData.getModel().addChangeListener(checkboxChangeListener);
        pnl.add(cbDownloadOsmData, GBC.std().insets(1, 5, 1, 5));
        cbDownloadGpxData = new JCheckBox(tr("Raw GPS data"));
        cbDownloadGpxData.setToolTipText(tr("Select to download GPS traces in the selected download area."));
        cbDownloadGpxData.getModel().addChangeListener(checkboxChangeListener);
        pnl.add(cbDownloadGpxData, GBC.std().insets(5, 5, 1, 5));
        cbDownloadNotes = new JCheckBox(tr("Notes"));
        cbDownloadNotes.setToolTipText(tr("Select to download notes in the selected download area."));
        cbDownloadNotes.getModel().addChangeListener(checkboxChangeListener);
        pnl.add(cbDownloadNotes, GBC.eol().insets(50, 5, 1, 5));

        // must be created before hook
        slippyMapChooser = new SlippyMapChooser();

        // hook for subclasses
        buildMainPanelAboveDownloadSelections(pnl);

        // predefined download selections
        downloadSelections.add(slippyMapChooser);
        downloadSelections.add(new BookmarkSelection());
        downloadSelections.add(new BoundingBoxSelection());
        downloadSelections.add(new PlaceSelection());
        downloadSelections.add(new TileSelection());

        // add selections from plugins
        PluginHandler.addDownloadSelection(downloadSelections);

        // now everybody may add their tab to the tabbed pane
        // (not done right away to allow plugins to remove one of
        // the default selectors!)
        for (DownloadSelection s : downloadSelections) {
            s.addGui(this);
        }

        pnl.add(tpDownloadAreaSelectors, GBC.eol().fill());

        try {
            tpDownloadAreaSelectors.setSelectedIndex(DOWNLOAD_TAB.get());
        } catch (IndexOutOfBoundsException ex) {
            Logging.trace(ex);
            DOWNLOAD_TAB.put(0);
        }

        Font labelFont = sizeCheck.getFont();
        sizeCheck.setFont(labelFont.deriveFont(Font.PLAIN, labelFont.getSize()));

        cbNewLayer = new JCheckBox(tr("Download as new layer"));
        cbNewLayer.setToolTipText(tr("<html>Select to download data into a new data layer.<br>"
                +"Unselect to download into the currently active data layer.</html>"));

        cbStartup = new JCheckBox(tr("Open this dialog on startup"));
        cbStartup.setToolTipText(
                tr("<html>Autostart ''Download from OSM'' dialog every time JOSM is started.<br>" +
                        "You can open it manually from File menu or toolbar.</html>"));
        cbStartup.addActionListener(e -> DOWNLOAD_AUTORUN.put(cbStartup.isSelected()));

        cbZoomToDownloadedData = new JCheckBox(tr("Zoom to downloaded data"));
        cbZoomToDownloadedData.setToolTipText(tr("Select to zoom to entire newly downloaded data."));

        pnl.add(cbNewLayer, GBC.std().anchor(GBC.WEST).insets(5, 5, 5, 5));
        pnl.add(cbStartup, GBC.std().anchor(GBC.WEST).insets(15, 5, 5, 5));
        pnl.add(cbZoomToDownloadedData, GBC.std().anchor(GBC.WEST).insets(15, 5, 5, 5));

        ExpertToggleAction.addVisibilitySwitcher(cbZoomToDownloadedData);

        pnl.add(sizeCheck, GBC.eol().anchor(GBC.EAST).insets(5, 5, 5, 2));

        if (!ExpertToggleAction.isExpert()) {
            JLabel infoLabel = new JLabel(
                    tr("Use left click&drag to select area, arrows or right mouse button to scroll map, wheel or +/- to zoom."));
            pnl.add(infoLabel, GBC.eol().anchor(GBC.SOUTH).insets(0, 0, 0, 0));
        }
        return pnl;
    }

    /* This should not be necessary, but if not here, repaint is not always correct in SlippyMap! */
    @Override
    public void paint(Graphics g) {
        tpDownloadAreaSelectors.getSelectedComponent().paint(g);
        super.paint(g);
    }

    protected final JPanel buildButtonPanel() {
        JPanel pnl = new JPanel(new FlowLayout());

        // -- download button
        pnl.add(btnDownload);
        InputMapUtils.enableEnter(btnDownload);

        InputMapUtils.addEnterActionWhenAncestor(cbDownloadGpxData, actDownload);
        InputMapUtils.addEnterActionWhenAncestor(cbDownloadOsmData, actDownload);
        InputMapUtils.addEnterActionWhenAncestor(cbDownloadNotes, actDownload);
        InputMapUtils.addEnterActionWhenAncestor(cbNewLayer, actDownload);
        InputMapUtils.addEnterActionWhenAncestor(cbStartup, actDownload);
        InputMapUtils.addEnterActionWhenAncestor(cbZoomToDownloadedData, actDownload);

        // -- cancel button
        JButton btnCancel;
        CancelAction actCancel = new CancelAction();
        btnCancel = new JButton(actCancel);
        pnl.add(btnCancel);
        InputMapUtils.enableEnter(btnCancel);

        // -- cancel on ESC
        InputMapUtils.addEscapeAction(getRootPane(), actCancel);

        // -- help button
        JButton btnHelp = new JButton(new ContextSensitiveHelpAction(getRootPane().getClientProperty("help").toString()));
        pnl.add(btnHelp);
        InputMapUtils.enableEnter(btnHelp);

        return pnl;
    }

    /**
     * Constructs a new {@code DownloadDialog}.
     * @param parent the parent component
     */
    public DownloadDialog(Component parent) {
        this(parent, ht("/Action/Download"));
    }

    /**
     * Constructs a new {@code DownloadDialog}.
     * @param parent the parent component
     * @param helpTopic the help topic to assign
     */
    public DownloadDialog(Component parent, String helpTopic) {
        super(GuiHelper.getFrameForComponent(parent), tr("Download"), ModalityType.DOCUMENT_MODAL);
        HelpUtil.setHelpContext(getRootPane(), helpTopic);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildMainPanel(), BorderLayout.CENTER);
        getContentPane().add(buildButtonPanel(), BorderLayout.SOUTH);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "checkClipboardContents");

        getRootPane().getActionMap().put("checkClipboardContents", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String clip = ClipboardUtils.getClipboardStringContent();
                if (clip == null) {
                    return;
                }
                Bounds b = OsmUrlToBounds.parse(clip);
                if (b != null) {
                    boundingBoxChanged(new Bounds(b), null);
                }
            }
        });
        addWindowListener(new WindowEventHandler());
        restoreSettings();
    }

    protected void updateSizeCheck() {
        boolean isAreaTooLarge = false;
        if (currentBounds == null) {
            sizeCheck.setText(tr("No area selected yet"));
            sizeCheck.setForeground(Color.darkGray);
        } else if (isDownloadNotes() && !isDownloadOsmData() && !isDownloadGpxData()) {
            // see max_note_request_area in https://github.com/openstreetmap/openstreetmap-website/blob/master/config/example.application.yml
            isAreaTooLarge = currentBounds.getArea() > Main.pref.getDouble("osm-server.max-request-area-notes", 25);
        } else {
            // see max_request_area in https://github.com/openstreetmap/openstreetmap-website/blob/master/config/example.application.yml
            isAreaTooLarge = currentBounds.getArea() > Main.pref.getDouble("osm-server.max-request-area", 0.25);
        }
        displaySizeCheckResult(isAreaTooLarge);
    }

    protected void displaySizeCheckResult(boolean isAreaTooLarge) {
        if (isAreaTooLarge) {
            sizeCheck.setText(tr("Download area too large; will probably be rejected by server"));
            sizeCheck.setForeground(Color.red);
        } else {
            sizeCheck.setText(tr("Download area ok, size probably acceptable to server"));
            sizeCheck.setForeground(Color.darkGray);
        }
    }

    /**
     * Distributes a "bounding box changed" from one DownloadSelection
     * object to the others, so they may update or clear their input fields.
     * @param b new current bounds
     *
     * @param eventSource - the DownloadSelection object that fired this notification.
     */
    public void boundingBoxChanged(Bounds b, DownloadSelection eventSource) {
        this.currentBounds = b;
        for (DownloadSelection s : downloadSelections) {
            if (s != eventSource) {
                s.setDownloadArea(currentBounds);
            }
        }
        updateSizeCheck();
    }

    /**
     * Starts download for the given bounding box
     * @param b bounding box to download
     */
    public void startDownload(Bounds b) {
        this.currentBounds = b;
        actDownload.run();
    }

    /**
     * Replies true if the user selected to download OSM data
     *
     * @return true if the user selected to download OSM data
     */
    public boolean isDownloadOsmData() {
        return cbDownloadOsmData.isSelected();
    }

    /**
     * Replies true if the user selected to download GPX data
     *
     * @return true if the user selected to download GPX data
     */
    public boolean isDownloadGpxData() {
        return cbDownloadGpxData.isSelected();
    }

    /**
     * Replies true if user selected to download notes
     *
     * @return true if user selected to download notes
     */
    public boolean isDownloadNotes() {
        return cbDownloadNotes.isSelected();
    }

    /**
     * Replies true if the user requires to download into a new layer
     *
     * @return true if the user requires to download into a new layer
     */
    public boolean isNewLayerRequired() {
        return cbNewLayer.isSelected();
    }

    /**
     * Replies true if the user requires to zoom to new downloaded data
     *
     * @return true if the user requires to zoom to new downloaded data
     * @since 11658
     */
    public boolean isZoomToDownloadedDataRequired() {
        return cbZoomToDownloadedData.isSelected();
    }

    /**
     * Adds a new download area selector to the download dialog
     *
     * @param selector the download are selector
     * @param displayName the display name of the selector
     */
    public void addDownloadAreaSelector(JPanel selector, String displayName) {
        tpDownloadAreaSelectors.add(displayName, selector);
    }

    /**
     * Refreshes the tile sources
     * @since 6364
     */
    public final void refreshTileSources() {
        if (slippyMapChooser != null) {
            slippyMapChooser.refreshTileSources();
        }
    }

    /**
     * Remembers the current settings in the download dialog.
     */
    public void rememberSettings() {
        DOWNLOAD_TAB.put(tpDownloadAreaSelectors.getSelectedIndex());
        DOWNLOAD_OSM.put(cbDownloadOsmData.isSelected());
        DOWNLOAD_GPS.put(cbDownloadGpxData.isSelected());
        DOWNLOAD_NOTES.put(cbDownloadNotes.isSelected());
        DOWNLOAD_NEWLAYER.put(cbNewLayer.isSelected());
        DOWNLOAD_ZOOMTODATA.put(cbZoomToDownloadedData.isSelected());
        if (currentBounds != null) {
            Main.pref.put("osm-download.bounds", currentBounds.encodeAsString(";"));
        }
    }

    /**
     * Restores the previous settings in the download dialog.
     */
    public void restoreSettings() {
        cbDownloadOsmData.setSelected(DOWNLOAD_OSM.get());
        cbDownloadGpxData.setSelected(DOWNLOAD_GPS.get());
        cbDownloadNotes.setSelected(DOWNLOAD_NOTES.get());
        cbNewLayer.setSelected(DOWNLOAD_NEWLAYER.get());
        cbStartup.setSelected(isAutorunEnabled());
        cbZoomToDownloadedData.setSelected(DOWNLOAD_ZOOMTODATA.get());
        int idx = Utils.clamp(DOWNLOAD_TAB.get(), 0, tpDownloadAreaSelectors.getTabCount() - 1);
        tpDownloadAreaSelectors.setSelectedIndex(idx);

        if (Main.isDisplayingMapView()) {
            MapView mv = Main.map.mapView;
            currentBounds = new Bounds(
                    mv.getLatLon(0, mv.getHeight()),
                    mv.getLatLon(mv.getWidth(), 0)
            );
            boundingBoxChanged(currentBounds, null);
        } else {
            Bounds bounds = getSavedDownloadBounds();
            if (bounds != null) {
                currentBounds = bounds;
                boundingBoxChanged(currentBounds, null);
            }
        }
    }

    /**
     * Returns the previously saved bounding box from preferences.
     * @return The bounding box saved in preferences if any, {@code null} otherwise
     * @since 6509
     */
    public static Bounds getSavedDownloadBounds() {
        String value = Main.pref.get("osm-download.bounds");
        if (!value.isEmpty()) {
            try {
                return new Bounds(value, ";");
            } catch (IllegalArgumentException e) {
                Logging.warn(e);
            }
        }
        return null;
    }

    /**
     * Determines if the dialog autorun is enabled in preferences.
     * @return {@code true} if the download dialog must be open at startup, {@code false} otherwise
     */
    public static boolean isAutorunEnabled() {
        return DOWNLOAD_AUTORUN.get();
    }

    /**
     * Automatically opens the download dialog, if autorun is enabled.
     * @see #isAutorunEnabled
     */
    public static void autostartIfNeeded() {
        if (isAutorunEnabled()) {
            Main.main.menu.download.actionPerformed(null);
        }
    }

    /**
     * Returns an {@link Optional} of the currently selected download area.
     * @return An {@link Optional} of the currently selected download area.
     * @since 12574 Return type changed to optional
     */
    public Optional<Bounds> getSelectedDownloadArea() {
        return Optional.ofNullable(currentBounds);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            getParent(),
                            new Dimension(1000, 600)
                    )
            ).applySafe(this);
        } else if (isShowing()) { // Avoid IllegalComponentStateException like in #8775
            new WindowGeometry(this).remember(getClass().getName() + ".geometry");
        }
        super.setVisible(visible);
    }

    /**
     * Replies true if the dialog was canceled
     *
     * @return true if the dialog was canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    protected void buildMainPanelAboveDownloadSelections(JPanel pnl) {
        // Do nothing
    }

    class CancelAction extends AbstractAction {
        CancelAction() {
            putValue(NAME, tr("Cancel"));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Click to close the dialog and to abort downloading"));
        }

        public void run() {
            setCanceled(true);
            setVisible(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            run();
        }
    }

    class DownloadAction extends AbstractAction {
        DownloadAction() {
            putValue(NAME, tr("Download"));
            new ImageProvider("download").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Click to download the currently selected area"));
            setEnabled(!Main.isOffline(OnlineResource.OSM_API));
        }

        public void run() {
            /*
             * Checks if the user selected the type of data to download. At least one the following
             * must be chosen : raw osm data, gpx data, notes.
             * If none of those are selected, then the corresponding dialog is shown to inform the user.
             */
            if (!isDownloadOsmData() && !isDownloadGpxData() && !isDownloadNotes()) {
                JOptionPane.showMessageDialog(
                        DownloadDialog.this,
                        tr("<html>Neither <strong>{0}</strong> nor <strong>{1}</strong> nor <strong>{2}</strong> is enabled.<br>"
                                        + "Please choose to either download OSM data, or GPX data, or Notes, or all.</html>",
                                cbDownloadOsmData.getText(),
                                cbDownloadGpxData.getText(),
                                cbDownloadNotes.getText()
                        ),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            setCanceled(false);
            setVisible(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            run();
        }
    }

    class WindowEventHandler extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            new CancelAction().run();
        }

        @Override
        public void windowActivated(WindowEvent e) {
            btnDownload.requestFocusInWindow();
        }
    }
}
