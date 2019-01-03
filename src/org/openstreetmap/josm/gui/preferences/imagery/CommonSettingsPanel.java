// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * {@code JPanel} giving access to common imagery settings.
 * @since 5465
 */
public class CommonSettingsPanel extends JPanel {

    // Common Settings
    private final JosmComboBox<String> sharpen;
    private final JSlider tilesZoom = new JSlider(-2, 2, 0);


    /**
     * Constructs a new {@code CommonSettingsPanel}.
     */
    public CommonSettingsPanel() {
        super(new GridBagLayout());

        this.sharpen = new JosmComboBox<>(new String[] {
                tr("None"),
                tr("Soft"),
                tr("Strong")});
        add(new JLabel(tr("Sharpen (requires layer re-add): ")));
        add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
        add(this.sharpen, GBC.eol().fill(GBC.HORIZONTAL));

        this.tilesZoom.setPaintLabels(true);
        this.tilesZoom.setMajorTickSpacing(2);
        this.tilesZoom.setMinorTickSpacing(1);
        this.tilesZoom.setPaintTicks(true);
        add(new JLabel(tr("Tiles zoom offset:")));
        add(GBC.glue(5, 0), GBC.std());
        add(this.tilesZoom, GBC.eol());
    }

    /**
     * Loads the common settings.
     */
    public void loadSettings() {
        this.sharpen.setSelectedIndex(Utils.clamp(ImageryLayer.PROP_SHARPEN_LEVEL.get(), 0, 2));
        this.tilesZoom.setValue(AbstractTileSourceLayer.ZOOM_OFFSET.get());
    }

    /**
     * Saves the common settings.
     * @return true when restart is required
     */
    public boolean saveSettings() {
        ImageryLayer.PROP_SHARPEN_LEVEL.put(sharpen.getSelectedIndex());

        boolean restartRequired = false;
        if (!AbstractTileSourceLayer.ZOOM_OFFSET.get().equals(this.tilesZoom.getValue())) {
            // TODO: make warning about too small MEMORY_CACHE_SIZE?
            AbstractTileSourceLayer.ZOOM_OFFSET.put(this.tilesZoom.getValue());
            restartRequired = true;
        }
        return restartRequired;
    }
}
