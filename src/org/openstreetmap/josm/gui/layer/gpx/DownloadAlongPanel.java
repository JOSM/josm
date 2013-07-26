// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Panel displayed in "Download along..." dialogs
 * @since 6054
 */
public class DownloadAlongPanel extends JPanel {
    
    // Preferences keys
    private final String prefOsm;
    private final String prefGps;
    private final String prefDist;
    private final String prefArea;
    private final String prefNear;

    // Data types to download
    private final JCheckBox cbDownloadOsmData;
    private final JCheckBox cbDownloadGpxData;

    // Legacy list of values
    private static final Integer[] dist = { 5000, 500, 50 };
    private static final Integer[] area = { 20, 10, 5, 1 };
    
    private final JList buffer;
    private final JList maxRect;
    private final JList downloadNear;
    
    /**
     * Constructs a new {@code DownloadPanel}.
     * @param prefOsm Preference key determining if OSM data should be downloaded
     * @param prefGps Preference key determining if GPS data should be downloaded
     * @param prefDist Preference key determining maximum distance
     * @param prefArea Preference key determining maximum area
     * @param prefNear Preference key determining "near" parameter. Can be {@code null}
     */
    public DownloadAlongPanel(String prefOsm, String prefGps, String prefDist, String prefArea, String prefNear) {
        super(new GridBagLayout());
        
        this.prefOsm = prefOsm;
        this.prefGps = prefGps;
        this.prefDist = prefDist;
        this.prefArea = prefArea;
        this.prefNear = prefNear;

        cbDownloadOsmData = new JCheckBox(tr("OpenStreetMap data"), Main.pref.getBoolean(prefOsm, true));
        cbDownloadOsmData.setToolTipText(tr("Select to download OSM data."));
        add(cbDownloadOsmData,  GBC.std().insets(1,5,1,5));
        cbDownloadGpxData = new JCheckBox(tr("Raw GPS data"), Main.pref.getBoolean(prefGps, false));
        cbDownloadGpxData.setToolTipText(tr("Select to download GPS traces."));
        add(cbDownloadGpxData,  GBC.eol().insets(5,5,1,5));
        
        add(new JLabel(tr("Download everything within:")), GBC.eol());
        String[] s = new String[dist.length];
        for (int i = 0; i < dist.length; ++i) {
            s[i] = tr("{0} meters", dist[i]);
        }
        buffer = new JList(s);
        
        double distanceValue = Main.pref.getDouble(prefDist, dist[0]);
        int distanceLegacyIndex = 0;
        for (int i = 0; i < dist.length; i++) {
            if (dist[i] == (int)distanceValue) {
                distanceLegacyIndex = i;
                break;
            }
        }
        
        buffer.setSelectedIndex(distanceLegacyIndex);
        add(buffer, GBC.eol());

        add(new JLabel(tr("Maximum area per request:")), GBC.eol());
        s = new String[area.length];
        for (int i = 0; i < area.length; ++i) {
            s[i] = tr("{0} sq km", area[i]);
        }
        maxRect = new JList(s);

        double areaValue = Main.pref.getDouble(prefArea, area[0]);
        int areaLegacyIndex = 0;
        for (int i = 0; i < area.length; i++) {
            if (area[i] == (int)areaValue) {
                areaLegacyIndex = i;
                break;
            }
        }
        
        maxRect.setSelectedIndex(areaLegacyIndex);
        add(maxRect, GBC.eol());
        
        if (prefNear != null) {
            add(new JLabel(tr("Download near:")), GBC.eol());
            downloadNear = new JList(new String[]{tr("track only"), tr("waypoints only"), tr("track and waypoints")});
            downloadNear.setSelectedIndex(Main.pref.getInteger(prefNear, 0));
            add(downloadNear, GBC.eol());
        } else {
            downloadNear = null;
        }
    }
    
    /**
     * Gets the maximum distance in meters
     * @return The maximum distance, in meters
     */
    public final double getDistance() {
        return dist[buffer.getSelectedIndex()];
    }

    /**
     * Gets the maximum area in squared kilometers
     * @return The maximum distance, in squared kilometers
     */
    public final double getArea() {
        return area[maxRect.getSelectedIndex()];
    }
    
    /**
     * Gets the "download near" choosen value
     * @return the "download near" choosen value (0: track only, 1: waypoints only, 2: both)
     */
    public final int getNear() {
        return downloadNear.getSelectedIndex();
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
     * Remembers the current settings in the download panel
     */
    protected final void rememberSettings() {
        Main.pref.put(prefOsm, isDownloadOsmData());
        Main.pref.put(prefGps, isDownloadGpxData());
        Main.pref.putDouble(prefDist, getDistance());
        Main.pref.putDouble(prefArea, getArea());
        if (prefNear != null) {
            Main.pref.putInteger(prefNear, getNear());
        }
    }
    
    /**
     * Adds a change listener to comboboxes
     * @param listener The listener that will be notified of each combobox change
     */
    protected final void addChangeListener(ChangeListener listener) {
        cbDownloadGpxData.addChangeListener(listener);
        cbDownloadOsmData.addChangeListener(listener);
    }

    /**
     * Show this panel in a new "Download along" help-aware dialog
     * @param title The dialog title
     * @param helpTopic The dialog help topic
     * @return The selected button index (0 for download, 1 for cancel, 2 for dialog closure)
     */
    public int showInDownloadDialog(String title, String helpTopic) {
        final ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Download"),
                        ImageProvider.get("download"),
                        tr("Click to download"),
                        null // no specific help text
                ),
                new ButtonSpec(
                        tr("Cancel"),
                        ImageProvider.get("cancel"),
                        tr("Click to cancel"),
                        null // no specific help text
                )
        };
        
        addChangeListener(new ChangeListener() {
            @Override public void stateChanged(ChangeEvent e) {
                options[0].setEnabled(isDownloadOsmData() || isDownloadGpxData());
            }
        });

        int ret = HelpAwareOptionPane.showOptionDialog(Main.parent, this, title,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0], helpTopic);
        if (0 == ret) {
            rememberSettings();
        }

        return ret;
    }
}
