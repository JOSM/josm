// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.data.imagery.WMSCachedTileLoaderJob;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.tools.GBC;

/**
 * {@code JPanel} giving access to WMS settings.
 * @since 5465
 */
public class WMSSettingsPanel extends JPanel {

    // WMS Settings
    private final JCheckBox autozoomActive;
    private final JSpinner spinSimConn;
    private final JSpinner tileSize;

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

        // Simultaneous connections
        add(Box.createHorizontalGlue(), GBC.eol().fill(GBC.HORIZONTAL));
        JLabel labelSimConn = new JLabel(tr("Simultaneous connections:"));
        spinSimConn = new JSpinner(new SpinnerNumberModel(WMSCachedTileLoaderJob.THREAD_LIMIT.get().intValue(), 1, 30, 1));
        labelSimConn.setLabelFor(spinSimConn);
        add(labelSimConn, GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(spinSimConn, GBC.eol());

        // Tile size
        JLabel labelTileSize = new JLabel(tr("Tile size:"));
        tileSize = new JSpinner(new SpinnerNumberModel(WMSLayer.PROP_IMAGE_SIZE.get().intValue(), 1, 4096, 128));
        labelTileSize.setLabelFor(tileSize);
        add(labelTileSize, GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(tileSize, GBC.eol());
    }

    /**
     * Loads the WMS settings.
     */
    public void loadSettings() {
        this.autozoomActive.setSelected(WMSLayer.PROP_DEFAULT_AUTOZOOM.get());
        this.spinSimConn.setValue(WMSCachedTileLoaderJob.THREAD_LIMIT.get());
        this.tileSize.setValue(WMSLayer.PROP_IMAGE_SIZE.get());
    }

    /**
     * Saves the WMS settings.
     * @return true when restart is required
     */
    public boolean saveSettings() {
        WMSLayer.PROP_DEFAULT_AUTOZOOM.put(this.autozoomActive.isSelected());
        WMSCachedTileLoaderJob.THREAD_LIMIT.put((Integer) spinSimConn.getModel().getValue());
        WMSLayer.PROP_IMAGE_SIZE.put((Integer) this.tileSize.getModel().getValue());

        return false;
    }
}
