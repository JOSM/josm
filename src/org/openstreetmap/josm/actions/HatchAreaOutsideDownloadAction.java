// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * This class toggles hatched background rendering of areas outside of the downloaded areas.
 *
 * @since 14388
 */
public class HatchAreaOutsideDownloadAction extends PreferenceToggleAction {

    private static final BooleanProperty PROP = new BooleanProperty("mappaint.hatch-outside-download-area", true);

    /**
     * Constructs a new {@link HatchAreaOutsideDownloadAction}.
     */
    public HatchAreaOutsideDownloadAction() {
        super(tr("Hatch area outside download"),
                tr("Enable/disable hatched background rendering of areas outside of the downloaded areas."),
                PROP.getKey(), PROP.getDefaultValue()
        );
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

    /**
     * Determines whether hatched background rendering is enabled
     *
     * @return whether hatched background rendering is enabled
     */
    public static boolean isHatchEnabled() {
        return PROP.get();
    }
}
