// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.io.imagery.HTMLGrabber;
import org.openstreetmap.josm.tools.GBC;

/**
 * {@code JPanel} giving access to WMS settings.
 * @since 5465
 */
public class WMSSettingsPanel extends JPanel {

    // WMS Settings
    private final JCheckBox autozoomActive;
    private final JosmComboBox browser;
    private final JCheckBox overlapCheckBox;
    private final JSpinner spinEast;
    private final JSpinner spinNorth;
    private final JSpinner spinSimConn;

    /**
     * Constructs a new {@code WMSSettingsPanel}.
     */
    public WMSSettingsPanel() {
        super(new GridBagLayout());

        // Auto zoom
        autozoomActive = new JCheckBox();
        add(new JLabel(tr("Auto zoom by default: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(autozoomActive, GBC.eol().fill(GBC.HORIZONTAL));

        // Downloader
        browser = new JosmComboBox(new String[] {
                "webkit-image {0}",
                "gnome-web-photo --mode=photo --format=png {0} /dev/stdout",
                "gnome-web-photo-fixed {0}",
        "webkit-image-gtk {0}"});
        browser.setEditable(true);
        add(new JLabel(tr("Downloader:")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(browser, GBC.eol().fill(GBC.HORIZONTAL));

        // Simultaneous connections
        add(Box.createHorizontalGlue(), GBC.eol().fill(GBC.HORIZONTAL));
        JLabel labelSimConn = new JLabel(tr("Simultaneous connections:"));
        spinSimConn = new JSpinner(new SpinnerNumberModel(WMSLayer.PROP_SIMULTANEOUS_CONNECTIONS.get().intValue(), 1, 30, 1));
        add(labelSimConn, GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(spinSimConn, GBC.eol());

        // Overlap
        add(Box.createHorizontalGlue(), GBC.eol().fill(GBC.HORIZONTAL));

        overlapCheckBox = new JCheckBox(tr("Overlap tiles"));
        JLabel labelEast = new JLabel(tr("% of east:"));
        JLabel labelNorth = new JLabel(tr("% of north:"));
        spinEast = new JSpinner(new SpinnerNumberModel(WMSLayer.PROP_OVERLAP_EAST.get().intValue(), 1, 50, 1));
        spinNorth = new JSpinner(new SpinnerNumberModel(WMSLayer.PROP_OVERLAP_NORTH.get().intValue(), 1, 50, 1));

        JPanel overlapPanel = new JPanel(new FlowLayout());
        overlapPanel.add(overlapCheckBox);
        overlapPanel.add(labelEast);
        overlapPanel.add(spinEast);
        overlapPanel.add(labelNorth);
        overlapPanel.add(spinNorth);

        add(overlapPanel, GBC.eop());
    }
    
    /**
     * Loads the WMS settings.
     */
    public void loadSettings() {
        this.autozoomActive.setSelected(WMSLayer.PROP_DEFAULT_AUTOZOOM.get());
        this.browser.setSelectedItem(HTMLGrabber.PROP_BROWSER.get());
        this.overlapCheckBox.setSelected(WMSLayer.PROP_OVERLAP.get());
        this.spinEast.setValue(WMSLayer.PROP_OVERLAP_EAST.get());
        this.spinNorth.setValue(WMSLayer.PROP_OVERLAP_NORTH.get());
        this.spinSimConn.setValue(WMSLayer.PROP_SIMULTANEOUS_CONNECTIONS.get());
    }
    
    /**
     * Saves the WMS settings.
     * @return true when restart is required
     */
    public boolean saveSettings() {
        WMSLayer.PROP_DEFAULT_AUTOZOOM.put(this.autozoomActive.isSelected());
        WMSLayer.PROP_OVERLAP.put(overlapCheckBox.getModel().isSelected());
        WMSLayer.PROP_OVERLAP_EAST.put((Integer) spinEast.getModel().getValue());
        WMSLayer.PROP_OVERLAP_NORTH.put((Integer) spinNorth.getModel().getValue());
        WMSLayer.PROP_SIMULTANEOUS_CONNECTIONS.put((Integer) spinSimConn.getModel().getValue());

        HTMLGrabber.PROP_BROWSER.put(browser.getEditor().getItem().toString());
        
        return false;
    }
}
