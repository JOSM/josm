// This code has been adapted and copied from code that has been written by Immanuel Scholz and others for JOSM.
// License: GPL. Copyright 2007 by Tim Haussmann
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.bbox.BBoxChooser;
import org.openstreetmap.josm.gui.bbox.SlippyMapBBoxChooser;

/**
 * JComponent that displays the slippy map tiles
 *
 * @author Tim Haussmann
 *
 */
public class SlippyMapChooser extends JPanel implements DownloadSelection, PropertyChangeListener{

    private DownloadDialog iGui;
    private SlippyMapBBoxChooser pnlSlippyMapBBoxChooser;

    /**
     * Create the chooser component.
     */
    public SlippyMapChooser() {
        pnlSlippyMapBBoxChooser = new SlippyMapBBoxChooser();
        pnlSlippyMapBBoxChooser.addPropertyChangeListener(this);
    }

    public void addGui(final DownloadDialog gui) {
        iGui = gui;
        iGui.addDownloadAreaSelector(pnlSlippyMapBBoxChooser, tr("Slippy map"));
    }


    public void setDownloadArea(Bounds area) {
        pnlSlippyMapBBoxChooser.setBoundingBox(area);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(BBoxChooser.BBOX_PROP)) {
            if (iGui != null) {
                iGui.boundingBoxChanged((Bounds)evt.getNewValue(), this);
            }
        }
    }
}
