// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.gui.widgets.JosmTextField;

/**
 * {@code JPanel} giving access to TMS settings.
 * @since 5465
 */
public class TMSSettingsPanel extends JPanel {

    // TMS Settings
    private final JCheckBox autozoomActive = new JCheckBox();
    private final JCheckBox autoloadTiles = new JCheckBox();
    private final JSpinner minZoomLvl;
    private final JSpinner maxZoomLvl;
    private final JCheckBox addToSlippyMapChosser = new JCheckBox();
    private final JosmTextField tilecacheDir = new JosmTextField();

    /**
     * Constructs a new {@code TMSSettingsPanel}.
     */
    public TMSSettingsPanel() {
        super(new GridBagLayout());
        minZoomLvl = new JSpinner(new SpinnerNumberModel(TMSLayer.DEFAULT_MIN_ZOOM, TMSLayer.MIN_ZOOM, TMSLayer.MAX_ZOOM, 1));
        maxZoomLvl = new JSpinner(new SpinnerNumberModel(TMSLayer.DEFAULT_MAX_ZOOM, TMSLayer.MIN_ZOOM, TMSLayer.MAX_ZOOM, 1));

        add(new JLabel(tr("Auto zoom by default: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(autozoomActive, GBC.eol().fill(GBC.HORIZONTAL));

        add(new JLabel(tr("Autoload tiles by default: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(autoloadTiles, GBC.eol().fill(GBC.HORIZONTAL));

        add(new JLabel(tr("Min. zoom level: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(this.minZoomLvl, GBC.eol());

        add(new JLabel(tr("Max. zoom level: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(this.maxZoomLvl, GBC.eol());

        add(new JLabel(tr("Add to slippymap chooser: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(addToSlippyMapChosser, GBC.eol().fill(GBC.HORIZONTAL));

        add(new JLabel(tr("Tile cache directory: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(tilecacheDir, GBC.eol().fill(GBC.HORIZONTAL));
    }
    
    /**
     * Loads the TMS settings.
     */
    public void loadSettings() {
        this.autozoomActive.setSelected(TMSLayer.PROP_DEFAULT_AUTOZOOM.get());
        this.autoloadTiles.setSelected(TMSLayer.PROP_DEFAULT_AUTOLOAD.get());
        this.addToSlippyMapChosser.setSelected(TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.get());
        this.maxZoomLvl.setValue(TMSLayer.getMaxZoomLvl(null));
        this.minZoomLvl.setValue(TMSLayer.getMinZoomLvl(null));
        this.tilecacheDir.setText(TMSLayer.PROP_TILECACHE_DIR.get());
    }
    
    /**
     * Saves the TMS settings.
     * @return true when restart is required
     */
    public boolean saveSettings() {
        boolean restartRequired = false;
        
        if (TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.get() != this.addToSlippyMapChosser.isSelected()) {
            restartRequired = true;
        }
        TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.put(this.addToSlippyMapChosser.isSelected());
        TMSLayer.PROP_DEFAULT_AUTOZOOM.put(this.autozoomActive.isSelected());
        TMSLayer.PROP_DEFAULT_AUTOLOAD.put(this.autoloadTiles.isSelected());
        TMSLayer.setMaxZoomLvl((Integer)this.maxZoomLvl.getValue());
        TMSLayer.setMinZoomLvl((Integer)this.minZoomLvl.getValue());
        TMSLayer.PROP_TILECACHE_DIR.put(this.tilecacheDir.getText());
        
        return restartRequired;
    }
}
