// License: GPL. See LICENSE file for details.
// Copyright 2007 by Christian Gallioz (aka khris78)
// Parts of code from Geotagged plugin (by Rob Neild)
// and the core JOSM source code (by Immanuel Scholz and others)

package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer.ImageEntry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

public class ImageViewerDialog extends ToggleDialog implements ActionListener {

    private static final String COMMAND_ZOOM = "zoom";
    private static final String COMMAND_CENTERVIEW = "centre";
    private static final String COMMAND_NEXT = "next";
    private static final String COMMAND_REMOVE = "remove";
    private static final String COMMAND_PREVIOUS = "previous";

    private ImageDisplay imgDisplay = new ImageDisplay();
    private boolean centerView = false;

    // Only one instance of that class
    static private ImageViewerDialog INSTANCE = null;

    public static ImageViewerDialog getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ImageViewerDialog();
        }
        return INSTANCE;
    }
    
    private JButton btnNext;
    private JButton btnPrevious;

    private ImageViewerDialog() {
        super(tr("Geotagged Images"), "geoimage", tr("Display geotagged images"), Shortcut.registerShortcut("tools:geotagged", tr("Tool: {0}", tr("Display geotagged images")), KeyEvent.VK_Y, Shortcut.GROUP_EDIT), 200);

        if (INSTANCE != null) {
            throw new IllegalStateException("Image viewer dialog should not be instanciated twice !");
        }

        INSTANCE = this;
        
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        content.add(imgDisplay, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());

        JButton button;

        Dimension buttonDim = new Dimension(26,26);
        button = new JButton();
        button.setIcon(ImageProvider.get("dialogs", "previous"));
        button.setActionCommand(COMMAND_PREVIOUS);
        button.setToolTipText(tr("Previous"));
        button.addActionListener(this);
        button.setPreferredSize(buttonDim);
        buttons.add(button);
        btnPrevious = button; //FIX

        button = new JButton();
        button.setIcon(ImageProvider.get("dialogs", "delete"));
        button.setActionCommand(COMMAND_REMOVE);
        button.setToolTipText(tr("Remove photo from layer"));
        button.addActionListener(this);
        button.setPreferredSize(buttonDim);
        buttons.add(button);

        button = new JButton();
        button.setIcon(ImageProvider.get("dialogs", "next"));
        button.setActionCommand(COMMAND_NEXT);
        button.setToolTipText(tr("Next"));
        button.addActionListener(this);
        button.setPreferredSize(buttonDim);
        buttons.add(button);
        btnNext = button;

        JToggleButton tb = new JToggleButton();
        tb.setIcon(ImageProvider.get("dialogs", "centreview"));
        tb.setActionCommand(COMMAND_CENTERVIEW);
        tb.setToolTipText(tr("Center view"));
        tb.addActionListener(this);
        tb.setPreferredSize(buttonDim);
        buttons.add(tb);

        button = new JButton();
        button.setIcon(ImageProvider.get("dialogs", "zoom-best-fit"));
        button.setActionCommand(COMMAND_ZOOM);
        button.setToolTipText(tr("Zoom best fit and 1:1"));
        button.addActionListener(this);
        button.setPreferredSize(buttonDim);
        buttons.add(button);

        content.add(buttons, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);

    }

    public void actionPerformed(ActionEvent e) {
        if (COMMAND_NEXT.equals(e.getActionCommand())) {
            if (currentLayer != null) {
                currentLayer.showNextPhoto();
            }
        } else if (COMMAND_PREVIOUS.equals(e.getActionCommand())) {
            if (currentLayer != null) {
                currentLayer.showPreviousPhoto();
            }

        } else if (COMMAND_CENTERVIEW.equals(e.getActionCommand())) {
            centerView = ((JToggleButton) e.getSource()).isSelected();
            if (centerView && currentEntry != null && currentEntry.pos != null) {
                Main.map.mapView.zoomTo(currentEntry.pos);
            }

        } else if (COMMAND_ZOOM.equals(e.getActionCommand())) {
            imgDisplay.zoomBestFitOrOne();

        } else if (COMMAND_REMOVE.equals(e.getActionCommand())) {
            if (currentLayer != null) {
               currentLayer.removeCurrentPhoto();
            }
        }

    }

    public static void showImage(GeoImageLayer layer, ImageEntry entry) {
        getInstance().displayImage(layer, entry);
        layer.checkPreviousNextButtons();
    }
    public static void setPreviousEnabled(Boolean value) {
        getInstance().btnPrevious.setEnabled(value);
    }
    public static void setNextEnabled(Boolean value) {
        getInstance().btnNext.setEnabled(value);
    }


    private GeoImageLayer currentLayer = null;
    private ImageEntry currentEntry = null;

    public void displayImage(GeoImageLayer layer, ImageEntry entry) {
        synchronized(this) {
            if (currentLayer == layer && currentEntry == entry) {
                repaint();
                return;
            }

            if (centerView && Main.map != null && entry != null && entry.pos != null) {
                Main.map.mapView.zoomTo(entry.pos);
            }

            currentLayer = layer;
            currentEntry = entry;
        }

        if (entry != null) {
            imgDisplay.setImage(entry.file);
            StringBuffer osd = new StringBuffer(entry.file != null ? entry.file.getName() : "");
            if (entry.elevation != null) {
                osd.append(tr("\nAltitude: {0} m", entry.elevation.longValue()));
            }
            if (entry.speed != null) {
                osd.append(tr("\n{0} km/h", Math.round(entry.speed)));
            }
            imgDisplay.setOsdText(osd.toString());
        } else {
            imgDisplay.setImage(null);
            imgDisplay.setOsdText("");
        }
    }
    
    /**
     * Returns whether an image is currently displayed
     * @return If image is currently displayed
     */
    public boolean hasImage() {
        return currentEntry != null;
    }
}
