// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerAction;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Toggle the image display between thumbnails and symbols.
 * @since 7935
 */
public class ShowThumbnailAction extends AbstractAction implements LayerAction {

    private final transient GeoImageLayer layer;

    /**
     * Constructs a new {@code ToggleGeoImageThumbAction} action.
     * @param layer image layer
     */
    public ShowThumbnailAction(GeoImageLayer layer) {
        super(tr("Show thumbnails"));
        new ImageProvider("dialogs/geoimage/togglegit").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, tr("Show image thumbnails instead of icons."));
        this.layer = layer;
    }

    /**
     * This is called after the menu entry was selected.
     * @param e action event
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        layer.setUseThumbs(!layer.isUseThumbs());
    }

    /**
     * Check if there is any suitable image to be toggled.
     * @param layer image layer
     * @return {@code true} if there are images to be toggled,
     *         {@code false} otherwise
     */
    private static boolean enabled(GeoImageLayer layer) {
        return layer.data != null && !layer.data.isEmpty();
    }

    /** Create actual menu entry and define if it is enabled or not. */
    @Override
    public Component createMenuComponent() {
        JCheckBoxMenuItem toggleItem = new JCheckBoxMenuItem(this);
        toggleItem.setEnabled(enabled(layer));
        toggleItem.setState(layer.isUseThumbs());
        return toggleItem;
    }

    /** Check if the current layer is supported. */
    @Override
    public boolean supportLayers(List<Layer> layers) {
        return layers.size() == 1 && layers.get(0) instanceof GeoImageLayer;
    }
}
