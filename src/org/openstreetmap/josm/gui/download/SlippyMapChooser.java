// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Toolkit;
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
public class SlippyMapChooser extends JPanel implements DownloadSelection, PropertyChangeListener {

    private DownloadDialog iGui;
    private SlippyMapBBoxChooser pnlSlippyMapBBoxChooser;
    // standard dimension
    private Dimension iDownloadDialogDimension;

    /**
     * Create the chooser component.
     */
    public SlippyMapChooser() {
        pnlSlippyMapBBoxChooser = new SlippyMapBBoxChooser();
        pnlSlippyMapBBoxChooser.addPropertyChangeListener(this);
    }

    @Override
    public void addGui(final DownloadDialog gui) {
        iGui = gui;
        iGui.addDownloadAreaSelector(pnlSlippyMapBBoxChooser, tr("Slippy map"));
    }

    @Override
    public void setDownloadArea(Bounds area) {
        pnlSlippyMapBBoxChooser.setBoundingBox(area);
        repaint();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(BBoxChooser.BBOX_PROP)) {
            if (iGui != null) {
                iGui.boundingBoxChanged((Bounds)evt.getNewValue(), this);
            }
        } else if(evt.getPropertyName().equals(SlippyMapBBoxChooser.RESIZE_PROP)) {
            int w, h;

            // retrieve the size of the display
            Dimension iScreenSize = Toolkit.getDefaultToolkit().getScreenSize();

            // enlarge
            if(iDownloadDialogDimension == null) {
                // make the each dimension 90% of the absolute display size
                w = iScreenSize.width * 90 / 100;
                h = iScreenSize.height * 90 / 100;
                iDownloadDialogDimension = iGui.getSize();
            }
            // shrink
            else {
                // set the size back to the initial dimensions
                w = iDownloadDialogDimension.width;
                h = iDownloadDialogDimension.height;
                iDownloadDialogDimension = null;
            }

            // resize and center the DownloadDialog
            iGui.setBounds((iScreenSize.width - w) / 2, (iScreenSize.height - h) / 2, w, h);
            repaint();
        }
    }
    
    /**
     * Refreshes the tile sources
     * @since 6364
     */
    public final void refreshTileSources() {
        if (pnlSlippyMapBBoxChooser != null) {
            pnlSlippyMapBBoxChooser.refreshTileSources();
        }
    }
}
