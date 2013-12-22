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
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.ContextSensitiveHelpAction;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 *
 */
public class DownloadDialog extends JDialog  {
    /** the unique instance of the download dialog */
    static private DownloadDialog instance;

    /**
     * Replies the unique instance of the download dialog
     *
     * @return the unique instance of the download dialog
     */
    static public DownloadDialog getInstance() {
        if (instance == null) {
            instance = new DownloadDialog(Main.parent);
        }
        return instance;
    }

    protected SlippyMapChooser slippyMapChooser;
    protected final List<DownloadSelection> downloadSelections = new ArrayList<DownloadSelection>();
    protected final JTabbedPane tpDownloadAreaSelectors = new JTabbedPane();
    protected JCheckBox cbNewLayer;
    protected JCheckBox cbStartup;
    protected final JLabel sizeCheck = new JLabel();
    protected Bounds currentBounds = null;
    protected boolean canceled;

    protected JCheckBox cbDownloadOsmData;
    protected JCheckBox cbDownloadGpxData;
    /** the download action and button */
    private DownloadAction actDownload;
    protected SideButton btnDownload;

    private void makeCheckBoxRespondToEnter(JCheckBox cb) {
        cb.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "doDownload");
        cb.getActionMap().put("doDownload", actDownload);
    }

    protected JPanel buildMainPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new GridBagLayout());

        // adding the download tasks
        pnl.add(new JLabel(tr("Data Sources and Types:")), GBC.std().insets(5,5,1,5));
        cbDownloadOsmData = new JCheckBox(tr("OpenStreetMap data"), true);
        cbDownloadOsmData.setToolTipText(tr("Select to download OSM data in the selected download area."));
        pnl.add(cbDownloadOsmData,  GBC.std().insets(1,5,1,5));
        cbDownloadGpxData = new JCheckBox(tr("Raw GPS data"));
        cbDownloadGpxData.setToolTipText(tr("Select to download GPS traces in the selected download area."));
        pnl.add(cbDownloadGpxData,  GBC.eol().insets(5,5,1,5));

        // hook for subclasses
        buildMainPanelAboveDownloadSelections(pnl);

        slippyMapChooser = new SlippyMapChooser();
        
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
            tpDownloadAreaSelectors.setSelectedIndex(Main.pref.getInteger("download.tab", 0));
        } catch (Exception ex) {
            Main.pref.putInteger("download.tab", 0);
        }

        Font labelFont = sizeCheck.getFont();
        sizeCheck.setFont(labelFont.deriveFont(Font.PLAIN, labelFont.getSize()));

        cbNewLayer = new JCheckBox(tr("Download as new layer"));
        cbNewLayer.setToolTipText(tr("<html>Select to download data into a new data layer.<br>"
                +"Unselect to download into the currently active data layer.</html>"));

        cbStartup = new JCheckBox(tr("Open this dialog on startup"));
        cbStartup.setToolTipText(tr("<html>Autostart ''Download from OSM'' dialog every time JOSM is started.<br>You can open it manually from File menu or toolbar.</html>"));
        cbStartup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                 Main.pref.put("download.autorun", cbStartup.isSelected());
            }});

        pnl.add(cbNewLayer, GBC.std().anchor(GBC.WEST).insets(5,5,5,5));
        pnl.add(cbStartup, GBC.std().anchor(GBC.WEST).insets(15,5,5,5));

        pnl.add(sizeCheck,  GBC.eol().anchor(GBC.EAST).insets(5,5,5,2));

        if (!ExpertToggleAction.isExpert()) {
            JLabel infoLabel  = new JLabel(tr("Use left click&drag to select area, arrows or right mouse button to scroll map, wheel or +/- to zoom."));
            pnl.add(infoLabel,GBC.eol().anchor(GBC.SOUTH).insets(0,0,0,0));
        }
        return pnl;
    }

    /* This should not be necessary, but if not here, repaint is not always correct in SlippyMap! */
    @Override
    public void paint(Graphics g) {
        tpDownloadAreaSelectors.getSelectedComponent().paint(g);
        super.paint(g);
    }

    protected JPanel buildButtonPanel() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new FlowLayout());

        // -- download button
        pnl.add(btnDownload = new SideButton(actDownload = new DownloadAction()));
        InputMapUtils.enableEnter(btnDownload);

        makeCheckBoxRespondToEnter(cbDownloadGpxData);
        makeCheckBoxRespondToEnter(cbDownloadOsmData);
        makeCheckBoxRespondToEnter(cbNewLayer);

        // -- cancel button
        SideButton btnCancel;
        CancelAction actCancel = new CancelAction();
        pnl.add(btnCancel = new SideButton(actCancel));
        InputMapUtils.enableEnter(btnCancel);

        // -- cancel on ESC
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), "cancel");
        getRootPane().getActionMap().put("cancel", actCancel);

        // -- help button
        SideButton btnHelp;
        pnl.add(btnHelp = new SideButton(new ContextSensitiveHelpAction(ht("/Action/Download"))));
        InputMapUtils.enableEnter(btnHelp);

        return pnl;
    }

    public DownloadDialog(Component parent) {
        super(JOptionPane.getFrameForComponent(parent),tr("Download"), ModalityType.DOCUMENT_MODAL);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildMainPanel(), BorderLayout.CENTER);
        getContentPane().add(buildButtonPanel(), BorderLayout.SOUTH);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), "checkClipboardContents");

        getRootPane().getActionMap().put("checkClipboardContents", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String clip = Utils.getClipboardContent();
                if (clip == null) {
                    return;
                }
                Bounds b = OsmUrlToBounds.parse(clip);
                if (b != null) {
                    boundingBoxChanged(new Bounds(b), null);
                }
            }
        });
        HelpUtil.setHelpContext(getRootPane(), ht("/Action/Download"));
        addWindowListener(new WindowEventHandler());
        restoreSettings();
    }

    private void updateSizeCheck() {
        if (currentBounds == null) {
            sizeCheck.setText(tr("No area selected yet"));
            sizeCheck.setForeground(Color.darkGray);
        } else if (currentBounds.getArea() > Main.pref.getDouble("osm-server.max-request-area", 0.25)) {
            sizeCheck.setText(tr("Download area too large; will probably be rejected by server"));
            sizeCheck.setForeground(Color.red);
        } else {
            sizeCheck.setText(tr("Download area ok, size probably acceptable to server"));
            sizeCheck.setForeground(Color.darkGray);
        }
    }

    /**
     * Distributes a "bounding box changed" from one DownloadSelection
     * object to the others, so they may update or clear their input
     * fields.
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
     * Invoked by
     * @param b
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
     * Replies true if the user requires to download into a new layer
     *
     * @return true if the user requires to download into a new layer
     */
    public boolean isNewLayerRequired() {
        return cbNewLayer.isSelected();
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
        Main.pref.put("download.tab", Integer.toString(tpDownloadAreaSelectors.getSelectedIndex()));
        Main.pref.put("download.osm", cbDownloadOsmData.isSelected());
        Main.pref.put("download.gps", cbDownloadGpxData.isSelected());
        Main.pref.put("download.newlayer", cbNewLayer.isSelected());
        if (currentBounds != null) {
            Main.pref.put("osm-download.bounds", currentBounds.encodeAsString(";"));
        }
    }

    /**
     * Restores the previous settings in the download dialog.
     */
    public void restoreSettings() {
        cbDownloadOsmData.setSelected(Main.pref.getBoolean("download.osm", true));
        cbDownloadGpxData.setSelected(Main.pref.getBoolean("download.gps", false));
        cbNewLayer.setSelected(Main.pref.getBoolean("download.newlayer", false));
        cbStartup.setSelected( isAutorunEnabled() );
        int idx = Main.pref.getInteger("download.tab", 0);
        if (idx < 0 || idx > tpDownloadAreaSelectors.getTabCount()) {
            idx = 0;
        }
        tpDownloadAreaSelectors.setSelectedIndex(idx);

        if (Main.isDisplayingMapView()) {
            MapView mv = Main.map.mapView;
            currentBounds = new Bounds(
                    mv.getLatLon(0, mv.getHeight()),
                    mv.getLatLon(mv.getWidth(), 0)
            );
            boundingBoxChanged(currentBounds,null);
        }
        else {
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
                Main.warn(e);
            }
        }
        return null;
    }

    /**
     * Determines if the dialog autorun is enabled in preferences.
     * @return {@code true} if the download dialog must be open at startup, {@code false} otherwise
     */
    public static boolean isAutorunEnabled() {
        return Main.pref.getBoolean("download.autorun",false);
    }

    public static void autostartIfNeeded() {
        if (isAutorunEnabled()) {
            Main.main.menu.download.actionPerformed(null);
        }
    }

    /**
     * Replies the currently selected download area.
     * @return the currently selected download area. May be {@code null}, if no download area is selected yet.
     */
    public Bounds getSelectedDownloadArea() {
        return currentBounds;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            new WindowGeometry(
                    getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(
                            getParent(),
                            new Dimension(1000,600)
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
    }

    class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(NAME, tr("Cancel"));
            putValue(SMALL_ICON, ImageProvider.get("cancel"));
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
        public DownloadAction() {
            putValue(NAME, tr("Download"));
            putValue(SMALL_ICON, ImageProvider.get("download"));
            putValue(SHORT_DESCRIPTION, tr("Click to download the currently selected area"));
        }

        public void run() {
            if (currentBounds == null) {
                JOptionPane.showMessageDialog(
                        DownloadDialog.this,
                        tr("Please select a download area first."),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            if (!isDownloadOsmData() && !isDownloadGpxData()) {
                JOptionPane.showMessageDialog(
                        DownloadDialog.this,
                        tr("<html>Neither <strong>{0}</strong> nor <strong>{1}</strong> is enabled.<br>"
                                + "Please choose to either download OSM data, or GPX data, or both.</html>",
                                cbDownloadOsmData.getText(),
                                cbDownloadGpxData.getText()
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
