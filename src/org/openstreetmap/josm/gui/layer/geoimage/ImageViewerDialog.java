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

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel.Action;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer.ImageEntry;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

public class ImageViewerDialog extends ToggleDialog {

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

        /* Don't show a detached dialog right from the start. */
        if (isShowing && !isDocked) {
            setIsShowing(false);
        }

        INSTANCE = this;

        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        content.add(imgDisplay, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());

        Dimension buttonDim = new Dimension(26,26);
        
        ImageAction prevAction = new ImageAction(COMMAND_PREVIOUS, ImageProvider.get("dialogs", "previous"), tr("Previous"));
        btnPrevious = new JButton(prevAction);
        btnPrevious.setPreferredSize(buttonDim);
        buttons.add(btnPrevious);
        Shortcut scPrev = Shortcut.registerShortcut(
            "geoimage:previous", tr("Geoimage: {0}", tr("Show previous Image")), KeyEvent.VK_PAGE_UP, Shortcut.GROUP_DIRECT);
        final String APREVIOUS = "Previous Image";
        Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scPrev.getKeyStroke(), APREVIOUS);
        Main.contentPane.getActionMap().put(APREVIOUS, prevAction);
        btnPrevious.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scPrev.getKeyStroke(), APREVIOUS);
        btnPrevious.getActionMap().put(APREVIOUS, prevAction);

        JButton btnDelete = new JButton(new ImageAction(COMMAND_REMOVE, ImageProvider.get("dialogs", "delete"), tr("Remove photo from layer")));
        btnDelete.setPreferredSize(buttonDim);
        buttons.add(btnDelete);
      
        ImageAction nextAction = new ImageAction(COMMAND_NEXT, ImageProvider.get("dialogs", "next"), tr("Next"));
        btnNext = new JButton(nextAction);
        btnNext.setPreferredSize(buttonDim);
        buttons.add(btnNext);
        Shortcut scNext = Shortcut.registerShortcut(
            "geoimage:next", tr("Geoimage: {0}", tr("Show next Image")), KeyEvent.VK_PAGE_DOWN, Shortcut.GROUP_DIRECT);
        final String ANEXT = "Next Image";
        Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scNext.getKeyStroke(), ANEXT);
        Main.contentPane.getActionMap().put(ANEXT, nextAction);
        btnNext.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scNext.getKeyStroke(), ANEXT);
        btnNext.getActionMap().put(ANEXT, nextAction);

        JToggleButton tbCentre = new JToggleButton(new ImageAction(COMMAND_CENTERVIEW, ImageProvider.get("dialogs", "centreview"), tr("Center view")));
        tbCentre.setPreferredSize(buttonDim);
        buttons.add(tbCentre);
       
        JButton btnZoomBestFit = new JButton(new ImageAction(COMMAND_ZOOM, ImageProvider.get("dialogs", "zoom-best-fit"), tr("Zoom best fit and 1:1")));
        btnZoomBestFit.setPreferredSize(buttonDim);
        buttons.add(btnZoomBestFit);

        content.add(buttons, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    class ImageAction extends AbstractAction {
        private final String action;
        public ImageAction(String action, ImageIcon icon, String toolTipText) {
            this.action = action;
            putValue(SHORT_DESCRIPTION, toolTipText);
            putValue(SMALL_ICON, icon);
        }

        public void actionPerformed(ActionEvent e) {
            if (COMMAND_NEXT.equals(action)) {
                if (currentLayer != null) {
                    currentLayer.showNextPhoto();
                }
            } else if (COMMAND_PREVIOUS.equals(action)) {
                if (currentLayer != null) {
                    currentLayer.showPreviousPhoto();
                }

            } else if (COMMAND_CENTERVIEW.equals(action)) {
                centerView = ((JToggleButton) e.getSource()).isSelected();
                if (centerView && currentEntry != null && currentEntry.pos != null) {
                    Main.map.mapView.zoomTo(currentEntry.pos);
                }

            } else if (COMMAND_ZOOM.equals(action)) {
                imgDisplay.zoomBestFitOrOne();

            } else if (COMMAND_REMOVE.equals(action)) {
                if (currentLayer != null) {
                   currentLayer.removeCurrentPhoto();
                }
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
        if (! isDialogShowing()) {
            setIsDocked(false);     // always open a detached window when an image is clicked and dialog is closed
            showDialog();
        } else {
            if (isDocked && isCollapsed) {
                expand();
                dialogsPanel.reconstruct(Action.COLLAPSED_TO_DEFAULT, this);
            }
        }

    }

    /**
     * When pressing the Toggle button always show the docked dialog.
     */
    @Override
    protected void toggleButtonHook() {
        if (! isShowing) {
            setIsDocked(true);
            setIsCollapsed(false);
        }
    }

    /**
     * When an image is closed, really close it and do not pop
     * up the side dialog.
     */
    @Override
    protected boolean dockWhenClosingDetachedDlg() {
        return false;
    }

    /**
     * Returns whether an image is currently displayed
     * @return If image is currently displayed
     */
    public boolean hasImage() {
        return currentEntry != null;
    }
}
