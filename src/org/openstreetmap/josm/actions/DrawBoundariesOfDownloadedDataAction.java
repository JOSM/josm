// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.display.DrawingPreference;

/**
 * This class toggles whether to draw boundaries of downloaded data.
 *
 * @since 14648
 */
public class DrawBoundariesOfDownloadedDataAction extends PreferenceToggleAction {

    /**
     * Constructs a new {@link DrawBoundariesOfDownloadedDataAction}.
     */
    public DrawBoundariesOfDownloadedDataAction() {
        super(tr("Draw boundaries of downloaded data"),
                tr("Enable/disable hatched background rendering of areas outside of the downloaded areas."),
                DrawingPreference.SOURCE_BOUNDS_PROP.getKey(),
                DrawingPreference.SOURCE_BOUNDS_PROP.getDefaultValue()
        );
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(MainApplication.getLayerManager().getEditLayer() != null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (MainApplication.isDisplayingMapView()) {
            MainApplication.getMap().mapView.repaint();
        }
    }

}
