// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.MapGridPaintable;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This action toggles the display of the grid over the map view.
 * @see MapGridPaintable
 * @since 19640
 */
public class ShowGridAction extends PreferenceToggleAction {

    /**
     * Constructs a new {@link ShowGridAction}.
     */
    public ShowGridAction() {
        super(tr("Show"),
                new ImageProvider("grid"),
                tr("Enable/disable the grid drawn over the map. Its spacing and orientation are set in the display preferences."),
                MapGridPaintable.ENABLED
        );
        setHelpId(ht("/MapView#Grid"));
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(MainApplication.isDisplayingMapView());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (MainApplication.isDisplayingMapView()) {
            MainApplication.getMap().mapView.repaint();
        }
    }
}
