// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.CoordinateFormatManager;
import org.openstreetmap.josm.data.coor.conversion.DMSCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.ICoordinateFormat;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.ImageLabel;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 * Dialog displayed to the user to download mapping data.
 */
public class DownloadDialog extends JDialog {

    private static final IntegerProperty DOWNLOAD_TAB = new IntegerProperty("download.tab", 0);
    private static final StringProperty DOWNLOAD_SOURCE_TAB = new StringProperty("download.source.tab", OSMDownloadSource.SIMPLE_NAME);
    private static final BooleanProperty DOWNLOAD_AUTORUN = new BooleanProperty("download.autorun", false);
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
            instance = new DownloadDialog(MainApplication.getMainFrame());
        }
        return instance;
    }

    private static final ListenerList<DownloadSourceListener> downloadSourcesListeners = ListenerList.create();
    private static final List<DownloadSource<?>> downloadSources = new ArrayList<>();
    static {
        // add default download sources
        addDownloadSource(new OSMDownloadSource());
        addDownloadSource(new OverpassDownloadSource());
    }

    protected final transient List<DownloadSelection> downloadSelections = new ArrayList<>();
    protected final JTabbedPane tpDownloadAreaSelectors = new JTabbedPane();
    protected final DownloadSourceTabs downloadSourcesTab = new DownloadSourceTabs();

    private final ImageLabel latText;
    private final ImageLabel lonText;
    private final ImageLabel bboxText;
    {
        final LatLon sample = new LatLon(90, 180);
        final ICoordinateFormat sampleFormat = DMSCoordinateFormat.INSTANCE;
        final Color background = new JPanel().getBackground();
        latText = new ImageLabel("lat", null, sampleFormat.latToString(sample).length(), background);
        lonText = new ImageLabel("lon", null, sampleFormat.lonToString(sample).length(), background);
        bboxText = new ImageLabel("bbox", null, sampleFormat.toString(sample, "").length() * 2, background);
    }

    protected JCheckBox cbStartup;
    protected JCheckBox cbZoomToDownloadedData;
    protected SlippyMapChooser slippyMapChooser;
    protected JPanel mainPanel;
    protected DownloadDialogSplitPane dialogSplit;

    /*
     * Keep the reference globally to avoid having it garbage collected
     */
    protected final transient ExpertToggleAction.ExpertModeChangeListener expertListener =
            getExpertModeListenerForDownloadSources();
    protected transient Bounds currentBounds;
    protected boolean canceled;

    protected JButton btnDownload;
    protected JButton btnDownloadNewLayer;
    protected JButton btnCancel;
    protected JButton btnHelp;

    /**
     * Builds the main panel of the dialog.
     * @return The panel of the dialog.
     */
    protected final JPanel buildMainPanel() {
        mainPanel = new JPanel(new GridBagLayout());

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

        // allow to collapse the panes, but reserve some space for tabs
        downloadSourcesTab.setMinimumSize(new Dimension(0, 25));
        tpDownloadAreaSelectors.setMinimumSize(new Dimension(0, 0));

        dialogSplit = new DownloadDialogSplitPane(
                downloadSourcesTab,
                tpDownloadAreaSelectors);

        ChangeListener tabChangedListener = getDownloadSourceTabChangeListener();
        tabChangedListener.stateChanged(new ChangeEvent(downloadSourcesTab));
        downloadSourcesTab.addChangeListener(tabChangedListener);

        mainPanel.add(dialogSplit, GBC.eol().fill());

        JPanel statusBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusBarPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        statusBarPanel.add(latText);
        statusBarPanel.add(lonText);
        statusBarPanel.add(bboxText);
        mainPanel.add(statusBarPanel, GBC.eol().fill(GBC.HORIZONTAL));
        ExpertToggleAction.addVisibilitySwitcher(statusBarPanel);

        cbStartup = new JCheckBox(tr("Open this dialog on startup"));
        cbStartup.setToolTipText(
                tr("<html>Autostart ''Download from OSM'' dialog every time JOSM is started.<br>" +
                        "You can open it manually from File menu or toolbar.</html>"));
        cbStartup.addActionListener(e -> DOWNLOAD_AUTORUN.put(cbStartup.isSelected()));

        JLabel iconZoomToDownloadedData = new JLabel(ImageProvider.get("dialogs/autoscale/download", ImageProvider.ImageSizes.SMALLICON));
        cbZoomToDownloadedData = new JCheckBox(tr("Zoom to downloaded data"));
        cbZoomToDownloadedData.setToolTipText(tr("Select to zoom to entire newly downloaded data."));

        JPanel checkboxPanel = new JPanel(new FlowLayout());
        checkboxPanel.add(cbStartup);
        checkboxPanel.add(Box.createHorizontalStrut(6));
        checkboxPanel.add(iconZoomToDownloadedData);
        checkboxPanel.add(cbZoomToDownloadedData);
        mainPanel.add(checkboxPanel, GBC.eol());

        ExpertToggleAction.addVisibilitySwitcher(cbZoomToDownloadedData);
        ExpertToggleAction.addVisibilitySwitcher(iconZoomToDownloadedData);

        JLabel infoLabel = new JLabel(
                tr("Use left click&drag to select area, arrows or right mouse button to scroll map, wheel or +/- to zoom."));
        mainPanel.add(infoLabel, GBC.eol().anchor(GBC.CENTER).insets(0, 0, 0, 0));

        ExpertToggleAction.addExpertModeChangeListener(isExpert -> infoLabel.setVisible(!isExpert), true);

        return mainPanel;
    }

    /**
     * Builds the button pane of the dialog.
     * @return The button panel of the dialog.
     */
    protected final JPanel buildButtonPanel() {
        btnDownload = new JButton(new DownloadAction(false));
        btnDownloadNewLayer = new JButton(new DownloadAction(true));
        btnCancel = new JButton(new CancelAction());
        btnHelp = new JButton(
                new ContextSensitiveHelpAction(getRootPane().getClientProperty("help").toString()));

        JPanel pnl = new JPanel(new FlowLayout());

        pnl.add(btnDownload);
        pnl.add(btnDownloadNewLayer);
        pnl.add(btnCancel);
        pnl.add(btnHelp);

        InputMapUtils.enableEnter(btnDownload);
        InputMapUtils.enableEnter(btnCancel);
        InputMapUtils.addEscapeAction(getRootPane(), btnCancel.getAction());
        InputMapUtils.enableEnter(btnHelp);

        InputMapUtils.addEnterActionWhenAncestor(cbStartup, btnDownload.getAction());
        InputMapUtils.addEnterActionWhenAncestor(cbZoomToDownloadedData, btnDownload.getAction());
        InputMapUtils.addCtrlEnterAction(pnl, btnDownload.getAction());

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

        // if no bounding box is selected make sure it is still propagated.
        if (currentBounds == null) {
            boundingBoxChanged(null, null);
        }
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

        for (AbstractDownloadSourcePanel<?> ds : downloadSourcesTab.getAllPanels()) {
            ds.boundingBoxChanged(b);
        }

        bboxText.setText(b == null ? "" : String.join("  ",
                CoordinateFormatManager.getDefaultFormat().toString(b.getMin(), " "),
                CoordinateFormatManager.getDefaultFormat().toString(b.getMax(), " ")));
    }

    /**
     * Updates the coordinates after moving the mouse cursor
     * @param latLon the coordinates under the mouse cursor
     */
    public void mapCursorChanged(ILatLon latLon) {
        latText.setText(CoordinateFormatManager.getDefaultFormat().latToString(latLon));
        lonText.setText(CoordinateFormatManager.getDefaultFormat().lonToString(latLon));
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
     * @return {@code true} if the download dialog must be open at startup, {@code false} otherwise.
     */
    public static boolean isAutorunEnabled() {
        return DOWNLOAD_AUTORUN.get();
    }

    /**
     * Adds a new download area selector to the download dialog.
     *
     * @param selector the download are selector.
     * @param displayName the display name of the selector.
     */
    public void addDownloadAreaSelector(JPanel selector, String displayName) {
        tpDownloadAreaSelectors.add(displayName, selector);
    }

    /**
     * Add a listener to get events from the DownloadSelection window
     *
     * @param selection The DownloadSelection object to send the Bounds to
     * @return See {@link List#add}
     * @since 16684
     */
    public boolean addDownloadAreaListener(DownloadSelection selection) {
        return downloadSelections.add(selection);
    }

    /**
     * Remove a listener that was getting events from the DownloadSelection window
     *
     * @param selection The DownloadSelection object to not send the Bounds to
     * @return See {@link List#remove}
     * @since 16684
     */
    public boolean removeDownloadAreaListener(DownloadSelection selection) {
        return downloadSelections.remove(selection);
    }

    /**
     * Adds a new download source to the download dialog if it is not added.
     *
     * @param downloadSource The download source to be added.
     * @param <T> The type of the download data.
     * @throws JosmRuntimeException If the download source is already added. Note, download sources are
     * compared by their reference.
     * @since 12878
     */
    public static <T> void addDownloadSource(DownloadSource<T> downloadSource) {
        if (downloadSources.contains(downloadSource)) {
            throw new JosmRuntimeException("The download source you are trying to add already exists.");
        }

        downloadSources.add(downloadSource);
        downloadSourcesListeners.fireEvent(l -> l.downloadSourceAdded(downloadSource));
    }

    /**
     * Remove a download source from the download dialog
     *
     * @param downloadSource The download source to be removed.
     * @return see {@link List#remove}
     * @since 15542
     */
    public static boolean removeDownloadSource(DownloadSource<?> downloadSource) {
        if (downloadSources.contains(downloadSource)) {
            return downloadSources.remove(downloadSource);
        }
        return false;
    }

    /**
     * Refreshes the tile sources.
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
        downloadSourcesTab.getAllPanels().forEach(AbstractDownloadSourcePanel::rememberSettings);
        downloadSourcesTab.getSelectedPanel().ifPresent(panel -> DOWNLOAD_SOURCE_TAB.put(panel.getSimpleName()));
        DOWNLOAD_ZOOMTODATA.put(cbZoomToDownloadedData.isSelected());
        if (currentBounds != null) {
            Config.getPref().put("osm-download.bounds", currentBounds.encodeAsString(";"));
        }
    }

    /**
     * Restores the previous settings in the download dialog.
     */
    public void restoreSettings() {
        cbStartup.setSelected(isAutorunEnabled());
        cbZoomToDownloadedData.setSelected(DOWNLOAD_ZOOMTODATA.get());

        try {
            tpDownloadAreaSelectors.setSelectedIndex(DOWNLOAD_TAB.get());
        } catch (IndexOutOfBoundsException e) {
            Logging.trace(e);
            tpDownloadAreaSelectors.setSelectedIndex(0);
        }

        downloadSourcesTab.getAllPanels().forEach(AbstractDownloadSourcePanel::restoreSettings);
        downloadSourcesTab.setSelected(DOWNLOAD_SOURCE_TAB.get());

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
     * @return The bounding box saved in preferences if any, {@code null} otherwise.
     * @since 6509
     */
    public static Bounds getSavedDownloadBounds() {
        String value = Config.getPref().get("osm-download.bounds");
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
            btnDownloadNewLayer.setEnabled(
                    !MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class).isEmpty());
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
     * @param newLayer The flag defining if a new layer must be created for the downloaded data.
     * @return The {@link DownloadSettings} object that describes the current state of
     * the download dialog.
     */
    public DownloadSettings getDownloadSettings(boolean newLayer) {
        final Component areaSelector = tpDownloadAreaSelectors.getSelectedComponent();
        final Bounds slippyMapBounds = areaSelector instanceof SlippyMapBBoxChooser
                ? ((SlippyMapBBoxChooser) areaSelector).getVisibleMapArea()
                : null;
        return new DownloadSettings(currentBounds, slippyMapBounds, newLayer, isZoomToDownloadedDataRequired());
    }

    protected void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Adds the download source to the download sources tab.
     * @param downloadSource The download source to be added.
     * @param <T> The type of the download data.
     */
    protected <T> void addNewDownloadSourceTab(DownloadSource<T> downloadSource) {
        downloadSourcesTab.addPanel(downloadSource.createPanel(this));
    }

    /**
     * Creates listener that removes/adds download sources from/to {@code downloadSourcesTab}
     * depending on the current mode.
     * @return The expert mode listener.
     */
    private ExpertToggleAction.ExpertModeChangeListener getExpertModeListenerForDownloadSources() {
        return downloadSourcesTab::updateExpert;
    }

    /**
     * Creates a listener that reacts on tab switches for {@code downloadSourcesTab} in order
     * to adjust proper division of the dialog according to user saved preferences or minimal size
     * of the panel.
     * @return A listener to adjust dialog division.
     */
    private ChangeListener getDownloadSourceTabChangeListener() {
        return ec -> downloadSourcesTab.getSelectedPanel().ifPresent(
                panel -> dialogSplit.setPolicy(panel.getSizingPolicy()));
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
            rememberSettings();
            setCanceled(true);
            setVisible(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Optional<AbstractDownloadSourcePanel<?>> panel = downloadSourcesTab.getSelectedPanel();
            run();
            panel.ifPresent(AbstractDownloadSourcePanel::checkCancel);
        }
    }

    /**
     * Action that is executed when the download button is pressed.
     */
    class DownloadAction extends AbstractAction {
        final boolean newLayer;
        DownloadAction(boolean newLayer) {
            this.newLayer = newLayer;
            if (!newLayer) {
                putValue(NAME, tr("Download"));
                putValue(SHORT_DESCRIPTION, tr("Click to download the currently selected area"));
                new ImageProvider("download").getResource().attachImageIcon(this);
            } else {
                putValue(NAME, tr("Download as new layer"));
                putValue(SHORT_DESCRIPTION, tr("Click to download the currently selected area into a new data layer"));
                new ImageProvider("download_new_layer").getResource().attachImageIcon(this);
            }
            setEnabled(!NetworkManager.isOffline(OnlineResource.OSM_API));
        }

        /**
         * Starts the download and closes the dialog, if all requirements for the current download source are met.
         * Otherwise the download is not started and the dialog remains visible.
         */
        public void run() {
            rememberSettings();
            downloadSourcesTab.getSelectedPanel().ifPresent(panel -> {
                DownloadSettings downloadSettings = getDownloadSettings(newLayer);
                if (panel.checkDownload(downloadSettings)) {
                    setCanceled(false);
                    setVisible(false);
                    panel.triggerDownload(downloadSettings);
                }
            });
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

    /**
     * A special tabbed pane for {@link AbstractDownloadSourcePanel}s
     * @author Michael Zangl
     * @since 12706
     */
    private class DownloadSourceTabs extends JTabbedPane implements DownloadSourceListener {
        private final List<AbstractDownloadSourcePanel<?>> allPanels = new ArrayList<>();

        DownloadSourceTabs() {
            downloadSources.forEach(this::downloadSourceAdded);
            downloadSourcesListeners.addListener(this);
        }

        List<AbstractDownloadSourcePanel<?>> getAllPanels() {
            return allPanels;
        }

        List<AbstractDownloadSourcePanel<?>> getVisiblePanels() {
            return IntStream.range(0, getTabCount())
                    .mapToObj(this::getComponentAt)
                    .map(p -> (AbstractDownloadSourcePanel<?>) p)
                    .collect(Collectors.toList());
        }

        void setSelected(String simpleName) {
            getVisiblePanels().stream()
                .filter(panel -> simpleName.equals(panel.getSimpleName()))
                .findFirst()
                .ifPresent(this::setSelectedComponent);
        }

        void updateExpert(boolean isExpert) {
            updateTabs();
        }

        void addPanel(AbstractDownloadSourcePanel<?> panel) {
            allPanels.add(panel);
            updateTabs();
        }

        private void updateTabs() {
            // Not the best performance, but we don't do it often
            removeAll();

            boolean isExpert = ExpertToggleAction.isExpert();
            allPanels.stream()
                .filter(panel -> isExpert || !panel.getDownloadSource().onlyExpert())
                .forEach(panel -> addTab(panel.getDownloadSource().getLabel(), panel.getIcon(), panel));
        }

        Optional<AbstractDownloadSourcePanel<?>> getSelectedPanel() {
            return Optional.ofNullable((AbstractDownloadSourcePanel<?>) getSelectedComponent());
        }

        @Override
        public void insertTab(String title, Icon icon, Component component, String tip, int index) {
            if (!(component instanceof AbstractDownloadSourcePanel)) {
                throw new IllegalArgumentException("Can only add AbstractDownloadSourcePanels");
            }
            super.insertTab(title, icon, component, tip, index);
        }

        @Override
        public void downloadSourceAdded(DownloadSource<?> source) {
            addPanel(source.createPanel(DownloadDialog.this));
        }
    }

    /**
     * A special split pane that acts according to a {@link DownloadSourceSizingPolicy}
     *
     * It attempts to size the top tab content correctly.
     *
     * @author Michael Zangl
     * @since 12705
     */
    private static class DownloadDialogSplitPane extends JSplitPane {
        private DownloadSourceSizingPolicy policy;
        private final JTabbedPane topComponent;
        /**
         * If the height was explicitly set by the user.
         */
        private boolean heightAdjustedExplicitly;

        DownloadDialogSplitPane(JTabbedPane newTopComponent, Component newBottomComponent) {
            super(VERTICAL_SPLIT, newTopComponent, newBottomComponent);
            this.topComponent = newTopComponent;

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    // doLayout is called automatically when the component size decreases
                    // This seems to be the only way to call doLayout when the component size increases
                    // We need this since we sometimes want to increase the top component size.
                    revalidate();
                }
            });

            addPropertyChangeListener(DIVIDER_LOCATION_PROPERTY, e -> heightAdjustedExplicitly = true);
        }

        public void setPolicy(DownloadSourceSizingPolicy policy) {
            this.policy = policy;

            super.setDividerLocation(policy.getComponentHeight() + computeOffset());
            setDividerSize(policy.isHeightAdjustable() ? 10 : 0);
            setEnabled(policy.isHeightAdjustable());
        }

        @Override
        public void doLayout() {
            // We need to force this height before the layout manager is run.
            // We cannot do this in the setDividerLocation, since the offset cannot be computed there.
            int offset = computeOffset();
            if (policy.isHeightAdjustable() && heightAdjustedExplicitly) {
                policy.storeHeight(Math.max(getDividerLocation() - offset, 0));
            }
            // At least 30 pixel for map, if we have enough space
            int maxValidDividerLocation = getHeight() > 150 ? getHeight() - 40 : getHeight();

            super.setDividerLocation(Math.min(policy.getComponentHeight() + offset, maxValidDividerLocation));
            super.doLayout();
            // Order is important (set this after setDividerLocation/doLayout called the listener)
            this.heightAdjustedExplicitly = false;
        }

        /**
         * @return The difference between the content height and the divider location
         */
        private int computeOffset() {
            Component selectedComponent = topComponent.getSelectedComponent();
            return topComponent.getHeight() - (selectedComponent == null ? 0 : selectedComponent.getHeight());
        }
    }
}
