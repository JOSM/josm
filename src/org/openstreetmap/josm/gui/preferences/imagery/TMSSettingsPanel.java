// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.data.imagery.TMSCachedTileLoader;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.imagery.TileSourceDisplaySettings;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

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

    private final JSpinner maxConcurrentDownloads;
    private final JSpinner maxDownloadsPerHost;


    /**
     * Constructs a new {@code TMSSettingsPanel}.
     */
    public TMSSettingsPanel() {
        super(new GridBagLayout());
        minZoomLvl = new JSpinner(new SpinnerNumberModel(
                Utils.clamp(TMSLayer.PROP_MIN_ZOOM_LVL.get().intValue(), TMSLayer.MIN_ZOOM, TMSLayer.MAX_ZOOM),
                TMSLayer.MIN_ZOOM,
                TMSLayer.MAX_ZOOM, 1));
        maxZoomLvl = new JSpinner(new SpinnerNumberModel(
                Utils.clamp(TMSLayer.PROP_MAX_ZOOM_LVL.get().intValue(), TMSLayer.MIN_ZOOM, TMSLayer.MAX_ZOOM),
                TMSLayer.MIN_ZOOM,
                TMSLayer.MAX_ZOOM, 1));
        maxConcurrentDownloads = new JSpinner(new SpinnerNumberModel(
                TMSCachedTileLoader.THREAD_LIMIT.get().intValue(), 0, Integer.MAX_VALUE, 1));
        maxDownloadsPerHost = new JSpinner(new SpinnerNumberModel(
                TMSCachedTileLoader.HOST_LIMIT.get().intValue(), 0, Integer.MAX_VALUE, 1));


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

        add(new JLabel(tr("Maximum concurrent downloads: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(maxConcurrentDownloads, GBC.eol());

        add(new JLabel(tr("Maximum concurrent downloads per host: ")), GBC.std());
        add(GBC.glue(5, 0), GBC.std());
        add(maxDownloadsPerHost, GBC.eol());

    }

    /**
     * Loads the TMS settings.
     */
    public void loadSettings() {
        this.autozoomActive.setSelected(TileSourceDisplaySettings.PROP_AUTO_ZOOM.get());
        this.autoloadTiles.setSelected(TileSourceDisplaySettings.PROP_AUTO_LOAD.get());
        this.addToSlippyMapChosser.setSelected(TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.get());
        this.maxZoomLvl.setValue(TMSLayer.getMaxZoomLvl(null));
        this.minZoomLvl.setValue(TMSLayer.getMinZoomLvl(null));
        this.maxConcurrentDownloads.setValue(TMSCachedTileLoader.THREAD_LIMIT.get());
        this.maxDownloadsPerHost.setValue(TMSCachedTileLoader.HOST_LIMIT.get());
    }

    /**
     * Saves the TMS settings.
     * @return true when restart is required
     */
    public boolean saveSettings() {
        boolean restartRequired = false;

        if (!TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.get().equals(this.addToSlippyMapChosser.isSelected())) {
            restartRequired = true;
        }
        TMSLayer.PROP_ADD_TO_SLIPPYMAP_CHOOSER.put(this.addToSlippyMapChosser.isSelected());
        TileSourceDisplaySettings.PROP_AUTO_ZOOM.put(this.autozoomActive.isSelected());
        TileSourceDisplaySettings.PROP_AUTO_LOAD.put(this.autoloadTiles.isSelected());
        TMSLayer.setMaxZoomLvl((Integer) this.maxZoomLvl.getValue());
        TMSLayer.setMinZoomLvl((Integer) this.minZoomLvl.getValue());

        if (!TMSCachedTileLoader.THREAD_LIMIT.get().equals(this.maxConcurrentDownloads.getValue())) {
            TMSCachedTileLoader.THREAD_LIMIT.put((Integer) this.maxConcurrentDownloads.getValue());
            restartRequired = true;
        }

        if (!TMSCachedTileLoader.HOST_LIMIT.get().equals(this.maxDownloadsPerHost.getValue())) {
            TMSCachedTileLoader.HOST_LIMIT.put((Integer) this.maxDownloadsPerHost.getValue());
            restartRequired = true;
        }

        return restartRequired;
    }
}
