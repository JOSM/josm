// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 * Dialog displayed to the user to download mapping data.
 */
public class DownloadDialog extends JDialog {

    /**
     * Preference properties
     */
    private static final IntegerProperty DOWNLOAD_TAB = new IntegerProperty("download.tab", 0);
    private static final IntegerProperty DOWNLOAD_SOURCE_TAB = new IntegerProperty("download-source.tab", 0);
    private static final IntegerProperty DIALOG_SPLIT = new IntegerProperty("download.split", 200);
    private static final BooleanProperty DOWNLOAD_AUTORUN = new BooleanProperty("download.autorun", false);
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

    protected final transient List<DownloadSource<?>> downloadSources = new ArrayList<>();
    protected final transient List<DownloadSelection> downloadSelections = new ArrayList<>();
    protected final JTabbedPane tpDownloadAreaSelectors = new JTabbedPane();
    protected final JTabbedPane downloadSourcesTab = new JTabbedPane();

    protected JCheckBox cbNewLayer;
    protected JCheckBox cbStartup;
    protected JCheckBox cbZoomToDownloadedData;
    protected SlippyMapChooser slippyMapChooser;
    protected JPanel mainPanel;
    protected JSplitPane dialogSplit;

    /*
     * Keep the reference globally to avoid having it garbage collected
     */
    protected final transient ExpertToggleAction.ExpertModeChangeListener expertListener =
            getExpertModeListenerForDownloadSources();
    protected transient Bounds currentBounds;
    protected boolean canceled;

    protected JButton btnDownload;
    protected JButton btnCancel;
    protected JButton btnHelp;

    /**
     * Builds the main panel of the dialog.
     * @return The panel of the dialog.
     */
    protected final JPanel buildMainPanel() {
        mainPanel = new JPanel(new GridBagLayout());

        downloadSources.add(new OSMDownloadSource());
        downloadSources.add(new OverpassDownloadSource());

        // register all default download sources
        for (int i = 0; i < downloadSources.size(); i++) {
            downloadSources.get(i).addGui(this);
        }

        // must be created before hook
        slippyMapChooser = new SlippyMapChooser();

        // predefined download selections
        downloadSelections.add(slippyMapChooser);
        downloadSelections.add(new BookmarkSelection());
        downloadSelections.add(new BoundingBoxSelection());
        downloadSelections.add(new PlaceSelection());
        downloadSelections.add(new TileSelection());

        // add selections from plugins
        PluginHandler.addDownloadSelection(downloadSelections);

        // register all default download selections
        for (DownloadSelection s : downloadSelections) {
            s.addGui(this);
        }

        // allow to collapse the panes completely
        downloadSourcesTab.setMinimumSize(new Dimension(0, 0));
        tpDownloadAreaSelectors.setMinimumSize(new Dimension(0, 0));

        dialogSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                downloadSourcesTab,
                tpDownloadAreaSelectors);

        mainPanel.add(dialogSplit, GBC.eol().fill());

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

        mainPanel.add(cbNewLayer, GBC.std().anchor(GBC.WEST).insets(5, 5, 5, 5));
        mainPanel.add(cbStartup, GBC.std().anchor(GBC.WEST).insets(15, 5, 5, 5));
        mainPanel.add(cbZoomToDownloadedData, GBC.std().anchor(GBC.WEST).insets(15, 5, 5, 5));

        ExpertToggleAction.addVisibilitySwitcher(cbZoomToDownloadedData);

        if (!ExpertToggleAction.isExpert()) {
            JLabel infoLabel = new JLabel(
                    tr("Use left click&drag to select area, arrows or right mouse button to scroll map, wheel or +/- to zoom."));
            mainPanel.add(infoLabel, GBC.eol().anchor(GBC.SOUTH).insets(0, 0, 0, 0));
        }
        return mainPanel;
    }

    /* This should not be necessary, but if not here, repaint is not always correct in SlippyMap! */
    @Override
    public void paint(Graphics g) {
        tpDownloadAreaSelectors.getSelectedComponent().paint(g);
        super.paint(g);
    }

    /**
     * Builds the button pane of the dialog.
     * @return The button panel of the dialog.
     */
    protected final JPanel buildButtonPanel() {
        btnDownload = new JButton(new DownloadAction());
        btnCancel = new JButton(new CancelAction());
        btnHelp = new JButton(
                new ContextSensitiveHelpAction(getRootPane().getClientProperty("help").toString()));

        JPanel pnl = new JPanel(new FlowLayout());

        pnl.add(btnDownload);
        pnl.add(btnCancel);
        pnl.add(btnHelp);

        InputMapUtils.enableEnter(btnDownload);
        InputMapUtils.enableEnter(btnCancel);
        InputMapUtils.addEscapeAction(getRootPane(), btnCancel.getAction());
        InputMapUtils.enableEnter(btnHelp);

        InputMapUtils.addEnterActionWhenAncestor(cbNewLayer, btnDownload.getAction());
        InputMapUtils.addEnterActionWhenAncestor(cbStartup, btnDownload.getAction());
        InputMapUtils.addEnterActionWhenAncestor(cbZoomToDownloadedData, btnDownload.getAction());

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
        ExpertToggleAction.addExpertModeChangeListener(expertListener);
        restoreSettings();
    }

    /**
     * Distributes a "bounding box changed" from one DownloadSelection
     * object to the others, so they may update or clear their input fields. Also informs
     * download sources about the change, so they can react on it.
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

        for (Component ds : downloadSourcesTab.getComponents()) {
            if (ds instanceof AbstractDownloadSourcePanel) {
                ((AbstractDownloadSourcePanel<?>) ds).boudingBoxChanged(b);
            }
        }
    }

    /**
     * Starts download for the given bounding box
     * @param b bounding box to download
     */
    public void startDownload(Bounds b) {
        this.currentBounds = b;
        startDownload();
    }

    /**
     * Starts download.
     */
    public void startDownload() {
        btnDownload.doClick();
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
     * Determines if the dialog autorun is enabled in preferences.
     * @return {@code true} if the download dialog must be open at startup, {@code false} otherwise
     */
    public static boolean isAutorunEnabled() {
        return DOWNLOAD_AUTORUN.get();
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
     * Adds a new download source to the download dialog
     *
     * @param downloadSource The download source to be added.
     * @param <T> The type of the download data.
     */
    public <T> void addDownloadSource(DownloadSource<T> downloadSource) {
        if ((ExpertToggleAction.isExpert() && downloadSource.onlyExpert()) || !downloadSource.onlyExpert()) {
            addNewDownloadSourceTab(downloadSource);
        }
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
        DOWNLOAD_SOURCE_TAB.put(downloadSourcesTab.getSelectedIndex());
        DIALOG_SPLIT.put(dialogSplit.getDividerLocation());
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
        cbNewLayer.setSelected(DOWNLOAD_NEWLAYER.get());
        cbStartup.setSelected(isAutorunEnabled());
        cbZoomToDownloadedData.setSelected(DOWNLOAD_ZOOMTODATA.get());
        dialogSplit.setDividerLocation(DIALOG_SPLIT.get());

        try {
            tpDownloadAreaSelectors.setSelectedIndex(DOWNLOAD_TAB.get());
        } catch (IndexOutOfBoundsException e) {
            Logging.trace(e);
            tpDownloadAreaSelectors.setSelectedIndex(0);
        }

        try {
            downloadSourcesTab.setSelectedIndex(DOWNLOAD_SOURCE_TAB.get());
        } catch (IndexOutOfBoundsException e) {
            Logging.trace(e);
            downloadSourcesTab.setSelectedIndex(0);
        }

        if (MainApplication.isDisplayingMapView()) {
            MapView mv = MainApplication.getMap().mapView;
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
     * Automatically opens the download dialog, if autorun is enabled.
     * @see #isAutorunEnabled
     */
    public static void autostartIfNeeded() {
        if (isAutorunEnabled()) {
            MainApplication.getMenu().download.actionPerformed(null);
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

    /**
     * Gets the global settings of the download dialog.
     * @return The {@link DownloadSettings} object that describes the current state of
     * the download dialog.
     */
    public DownloadSettings getDownloadSettings() {
        return new DownloadSettings(currentBounds, isNewLayerRequired(), isZoomToDownloadedDataRequired());
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Returns position of the download source in the tabbed pane.
     * @param downloadSource The download source.
     * @return The index of the download source, or -1 if it not in the pane.
     */
    protected int getDownloadSourceIndex(DownloadSource<?> downloadSource) {
        return Arrays.stream(downloadSourcesTab.getComponents())
                .filter(it -> it instanceof AbstractDownloadSourcePanel)
                .map(it -> (AbstractDownloadSourcePanel<?>) it)
                .filter(it -> it.getDownloadSource().equals(downloadSource))
                .findAny()
                .map(downloadSourcesTab::indexOfComponent)
                .orElse(-1);
    }

    /**
     * Adds the download source to the download sources tab.
     * @param downloadSource The download source to be added.
     * @param <T> The type of the download data.
     */
    private <T> void addNewDownloadSourceTab(DownloadSource<T> downloadSource) {
        AbstractDownloadSourcePanel<T> panel = downloadSource.createPanel();
        downloadSourcesTab.add(panel, downloadSource.getLabel());
        Icon icon = panel.getIcon();
        if (icon != null) {
            int idx = getDownloadSourceIndex(downloadSource);
            downloadSourcesTab.setIconAt(
                    idx != -1 ? idx : downloadSourcesTab.getTabCount() - 1,
                    icon);
        }
    }

    /**
     * Creates listener that removes/adds download sources from/to {@code downloadSourcesTab}
     * depending on the current mode.
     * @return The expert mode listener.
     */
    private ExpertToggleAction.ExpertModeChangeListener getExpertModeListenerForDownloadSources() {
        return isExpert -> {
            if (isExpert) {
                downloadSources.stream()
                        .filter(DownloadSource::onlyExpert)
                        .filter(it -> getDownloadSourceIndex(it) == -1)
                        .forEach(this::addNewDownloadSourceTab);
            } else {
                IntStream.range(0, downloadSourcesTab.getTabCount())
                        .mapToObj(downloadSourcesTab::getComponentAt)
                        .filter(it -> it instanceof AbstractDownloadSourcePanel)
                        .map(it -> (AbstractDownloadSourcePanel<?>) it)
                        .filter(it -> it.getDownloadSource().onlyExpert())
                        .forEach(downloadSourcesTab::remove);
            }
        };
    }

    /**
     * Action that is executed when the cancel button is pressed.
     */
    class CancelAction extends AbstractAction {
        CancelAction() {
            putValue(NAME, tr("Cancel"));
            new ImageProvider("cancel").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Click to close the dialog and to abort downloading"));
        }

        /**
         * Cancels the download
         */
        public void run() {
            setCanceled(true);
            setVisible(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            AbstractDownloadSourcePanel<?> pnl = (AbstractDownloadSourcePanel<?>) downloadSourcesTab.getSelectedComponent();
            run();
            pnl.checkCancel();
        }
    }

    /**
     * Action that is executed when the download button is pressed.
     */
    class DownloadAction extends AbstractAction {
        DownloadAction() {
            putValue(NAME, tr("Download"));
            new ImageProvider("download").getResource().attachImageIcon(this);
            putValue(SHORT_DESCRIPTION, tr("Click to download the currently selected area"));
            setEnabled(!Main.isOffline(OnlineResource.OSM_API));
        }

        /**
         * Starts the download, if possible
         */
        public void run() {
            Component panel = downloadSourcesTab.getSelectedComponent();
            if (panel instanceof AbstractDownloadSourcePanel) {
                AbstractDownloadSourcePanel<?> pnl = (AbstractDownloadSourcePanel<?>) panel;
                DownloadSettings downloadSettings = getDownloadSettings();
                if (pnl.checkDownload(downloadSettings)) {
                    rememberSettings();
                    setCanceled(false);
                    setVisible(false);
                    pnl.triggerDownload(downloadSettings);
                }
            }
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
