// This code has been adapted and copied from code that has been written by Immanuel Scholz and others for JOSM.
// License: GPL. Copyright 2007 by Tim Haussmann
package org.openstreetmap.josm.gui.download;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * This class controls the user input by listening to mouse and key events.
 * Currently implemented is: - zooming in and out with scrollwheel - zooming in
 * and centering by double clicking - selecting an area by clicking and dragging
 * the mouse
 * 
 * @author Tim Haussmann
 */
public class OsmMapControl extends MouseAdapter implements MouseMotionListener, MouseListener {

    // start and end point of selection rectangle
    private Point iStartSelectionPoint;
    private Point iEndSelectionPoint;

    // the SlippyMapChooserComponent
    private final SlippyMapChooser iSlippyMapChooser;

    private SizeButton iSizeButton = null;
    private SourceButton iSourceButton = null;

    /**
     * Create a new OsmMapControl
     */
    public OsmMapControl(SlippyMapChooser navComp, JPanel contentPane, SizeButton sizeButton, SourceButton sourceButton) {
        this.iSlippyMapChooser = navComp;
        iSlippyMapChooser.addMouseListener(this);
        iSlippyMapChooser.addMouseMotionListener(this);

        String[] n = { ",", ".", "up", "right", "down", "left" };
        int[] k =
                { KeyEvent.VK_COMMA, KeyEvent.VK_PERIOD, KeyEvent.VK_UP, KeyEvent.VK_RIGHT,
                        KeyEvent.VK_DOWN, KeyEvent.VK_LEFT };

        if (contentPane != null) {
            for (int i = 0; i < n.length; ++i) {
                contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                        KeyStroke.getKeyStroke(k[i], KeyEvent.CTRL_DOWN_MASK),
                        "MapMover.Zoomer." + n[i]);
            }
        }
        iSizeButton = sizeButton;
        iSourceButton = sourceButton;
    }

    /**
     * Start drawing the selection rectangle if it was the 1st button (left
     * button)
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (!iSizeButton.hit(e.getPoint())) {
                iStartSelectionPoint = e.getPoint();
                iEndSelectionPoint = e.getPoint();
            }
        }
        
    }

    public void mouseDragged(MouseEvent e) {        
        if((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK){
            if (iStartSelectionPoint != null) {             
                iEndSelectionPoint = e.getPoint();
                iSlippyMapChooser.setSelection(iStartSelectionPoint, iEndSelectionPoint);
            }
        }
    }

    /**
     * When dragging the map change the cursor back to it's pre-move cursor. If
     * a double-click occurs center and zoom the map on the clicked location.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            
            int sourceButton = iSourceButton.hit(e.getPoint());
            
            if (iSizeButton.hit(e.getPoint())) {
                iSizeButton.toggle();
                iSlippyMapChooser.resizeSlippyMap();
            }
            else if(sourceButton == SourceButton.HIDE_OR_SHOW) {
                iSourceButton.toggle();
                iSlippyMapChooser.repaint();
                
            }else if(sourceButton == SourceButton.MAPNIK || sourceButton == SourceButton.OSMARENDER || sourceButton == SourceButton.CYCLEMAP) {
                iSlippyMapChooser.toggleMapSource(sourceButton);
            }
            else {
                if (e.getClickCount() == 1) {
                    iSlippyMapChooser.setSelection(iStartSelectionPoint, e.getPoint());

                    // reset the selections start and end
                    iEndSelectionPoint = null;
                    iStartSelectionPoint = null;
                }
            }
            
        }
    }

    public void mouseMoved(MouseEvent e) {
    }

}
