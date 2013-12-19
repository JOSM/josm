// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.BooleanProperty;

import java.awt.event.ActionEvent;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * This class toggles hatched background rendering of areas outside of the downloaded areas.
 */
public class HatchAreaOutsideDownloadAction extends PreferenceToggleAction {

    private static final BooleanProperty PROP = new BooleanProperty("mappaint.hatch-outside-download-area", true);

    /**
     * Constructs a new {@link HatchAreaOutsideDownloadAction}.
     */
    public HatchAreaOutsideDownloadAction() {
        super(tr("Hatch area outside download"),
                tr("Enable/disable hatched background rendering of areas outside of the downloaded areas."),
                PROP
        );
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.main.hasEditLayer());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (Main.isDisplayingMapView()) {
            Main.map.mapView.repaint();
        }
    }

    public static boolean isHatchEnabled() {
        return PROP.get();
    }
}
