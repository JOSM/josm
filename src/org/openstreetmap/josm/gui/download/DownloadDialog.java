// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 * Main download dialog.
 *
 * Can be extended by plugins in two ways:
 * (1) by adding download tasks that are then called with the selected bounding box
 * (2) by adding "DownloadSelection" objects that implement different ways of selecting a bounding box
 *
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class DownloadDialog extends JPanel {
    static private final Logger logger = Logger.getLogger(DownloadDialog.class.getName());

    private final List<DownloadSelection> downloadSelections = new ArrayList<DownloadSelection>();
    private final JTabbedPane tpDownloadAreaSelectors = new JTabbedPane();
    private final JCheckBox cbNewLayer;
    private final JLabel sizeCheck = new JLabel();

    private Bounds currentBounds = null;

    private JCheckBox cbDownloadOsmData = new JCheckBox(tr("OpenStreetMap data"), true);
    private JCheckBox cbDownloadGpxData = new JCheckBox(tr("Raw GPS data"));


    public DownloadDialog() {
        setLayout(new GridBagLayout());

        // adding the download tasks
        add(new JLabel(tr("Data Sources and Types")), GBC.eol().insets(0,5,0,0));
        add(cbDownloadOsmData,  GBC.eol().insets(20,0,0,0));
        add(cbDownloadGpxData,  GBC.eol().insets(20,0,0,0));
        
        // predefined download selections
        downloadSelections.add(new SlippyMapChooser());
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
   
        cbNewLayer = new JCheckBox(tr("Download as new layer"));
        add(cbNewLayer, GBC.eol().insets(0,5,0,0));

        add(new JLabel(tr("Download Area")), GBC.eol().insets(0,5,0,0));
        add(tpDownloadAreaSelectors, GBC.eol().fill());

        try {
            tpDownloadAreaSelectors.setSelectedIndex(Main.pref.getInteger("download.tab", 0));
        } catch (Exception ex) {
            Main.pref.putInteger("download.tab", 0);
        }

        Font labelFont = sizeCheck.getFont();
        sizeCheck.setFont(labelFont.deriveFont(Font.PLAIN, labelFont.getSize()));
        add(sizeCheck, GBC.eop().insets(0,5,5,10));

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK), "checkClipboardContents");

        getActionMap().put("checkClipboardContents", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                checkClipboardContents();
            }
        });
        
        restoreSettings();
    }

    private void checkClipboardContents() {
        String result = "";
        Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

        if(contents == null || !contents.isDataFlavorSupported(DataFlavor.stringFlavor))
            return;

        try {
            result = (String)contents.getTransferData(DataFlavor.stringFlavor);
        }
        catch(Exception ex) {
            return;
        }

        Bounds b = OsmUrlToBounds.parse(result);
        if (b != null) {
            boundingBoxChanged(new Bounds(b),null);
        }
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
                s.boundingBoxChanged(this);
            }
        }
        updateSizeCheck();
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
     * Remembers the current settings in the download dialog 
     * 
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
    
    public void restoreSettings() {
        cbDownloadOsmData.setSelected(Main.pref.getBoolean("download.osm", true));
        cbDownloadGpxData.setSelected(Main.pref.getBoolean("download.gps", false));
        cbNewLayer.setSelected(Main.pref.getBoolean("download.newlayer", false));
        int idx = Main.pref.getInteger("download.tab", 0);
        if (idx < 0 || idx > tpDownloadAreaSelectors.getTabCount()) {
            idx = 0;
        }
        tpDownloadAreaSelectors.setSelectedIndex(idx);
        
        if (Main.map != null) {
            MapView mv = Main.map.mapView;
            currentBounds = new Bounds(
                    mv.getLatLon(0, mv.getHeight()),
                    mv.getLatLon(mv.getWidth(), 0)                    
                    );
            boundingBoxChanged(currentBounds,null);
        }
        else if (Main.pref.hasKey("osm-download.bounds")) {
            // read the bounding box from the preferences
            try {
                currentBounds = new Bounds(Main.pref.get("osm-download.bounds"), ";");
                boundingBoxChanged(currentBounds,null);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Replies the currently selected download area. May be null, if no download area is selected
     * yet.
     */
    public Bounds getSelectedDownloadArea() {
        return currentBounds;
    }
    
}
