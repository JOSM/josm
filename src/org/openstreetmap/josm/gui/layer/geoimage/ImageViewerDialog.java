// License: GPL. See LICENSE file for details.
// Copyright 2007 by Christian Gallioz (aka khris78)
// Parts of code from Geotagged plugin (by Rob Neild)
// and the core JOSM source code (by Immanuel Scholz and others)

package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel.Action;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

public class ImageViewerDialog extends ToggleDialog {

    private static final String COMMAND_ZOOM = "zoom";
    private static final String COMMAND_CENTERVIEW = "centre";
    private static final String COMMAND_NEXT = "next";
    private static final String COMMAND_REMOVE = "remove";
    private static final String COMMAND_REMOVE_FROM_DISK = "removefromdisk";
    private static final String COMMAND_PREVIOUS = "previous";
    private static final String COMMAND_COLLAPSE = "collapse";

    private ImageDisplay imgDisplay = new ImageDisplay();
    private boolean centerView = false;

    // Only one instance of that class
    static private ImageViewerDialog INSTANCE = null;

    private boolean collapseButtonClicked = false;

    public static ImageViewerDialog getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ImageViewerDialog();
        }
        return INSTANCE;
    }

    private JButton btnNext;
    private JButton btnPrevious;
    private JButton btnCollapse;

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

        Dimension buttonDim = new Dimension(26,26);

        ImageAction prevAction = new ImageAction(COMMAND_PREVIOUS, ImageProvider.get("dialogs", "previous"), tr("Previous"));
        btnPrevious = new JButton(prevAction);
        btnPrevious.setPreferredSize(buttonDim);
        Shortcut scPrev = Shortcut.registerShortcut(
            "geoimage:previous", tr("Geoimage: {0}", tr("Show previous Image")), KeyEvent.VK_PAGE_UP, Shortcut.GROUP_DIRECT);
        final String APREVIOUS = "Previous Image";
        Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scPrev.getKeyStroke(), APREVIOUS);
        Main.contentPane.getActionMap().put(APREVIOUS, prevAction);
        btnPrevious.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scPrev.getKeyStroke(), APREVIOUS);
        btnPrevious.getActionMap().put(APREVIOUS, prevAction);

        final String DELETE_TEXT = tr("Remove photo from layer");
        ImageAction delAction = new ImageAction(COMMAND_REMOVE, ImageProvider.get("dialogs", "delete"), DELETE_TEXT);
        JButton btnDelete = new JButton(delAction);
        btnDelete.setPreferredSize(buttonDim);
        Shortcut scDelete = Shortcut.registerShortcut(
            "geoimage:deleteimagefromlayer", tr("Geoimage: {0}", DELETE_TEXT), KeyEvent.VK_DELETE, Shortcut.GROUP_DIRECT, Shortcut.SHIFT_DEFAULT);
        Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scDelete.getKeyStroke(), DELETE_TEXT);
        Main.contentPane.getActionMap().put(DELETE_TEXT, delAction);
        btnDelete.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scDelete.getKeyStroke(), DELETE_TEXT);
        btnDelete.getActionMap().put(DELETE_TEXT, delAction);

        ImageAction delFromDiskAction = new ImageAction(COMMAND_REMOVE_FROM_DISK, ImageProvider.get("dialogs", "geoimage/deletefromdisk"), tr("Delete image file from disk"));
        JButton btnDeleteFromDisk = new JButton(delFromDiskAction);
        btnDeleteFromDisk.setPreferredSize(buttonDim);
        Shortcut scDeleteFromDisk = Shortcut.registerShortcut(
            "geoimage:deletefilefromdisk", tr("Geoimage: {0}", tr("Delete File from disk")), KeyEvent.VK_DELETE, Shortcut.GROUP_DIRECT, Shortcut.GROUP_MENU + Shortcut.SHIFT_DEFAULT);
        final String ADELFROMDISK = "Delete image file from disk";
        Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scDeleteFromDisk.getKeyStroke(), ADELFROMDISK);
        Main.contentPane.getActionMap().put(ADELFROMDISK, delFromDiskAction);
        btnDeleteFromDisk.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scDeleteFromDisk.getKeyStroke(), ADELFROMDISK);
        btnDeleteFromDisk.getActionMap().put(ADELFROMDISK, delFromDiskAction);

        ImageAction nextAction = new ImageAction(COMMAND_NEXT, ImageProvider.get("dialogs", "next"), tr("Next"));
        btnNext = new JButton(nextAction);
        btnNext.setPreferredSize(buttonDim);
        Shortcut scNext = Shortcut.registerShortcut(
            "geoimage:next", tr("Geoimage: {0}", tr("Show next Image")), KeyEvent.VK_PAGE_DOWN, Shortcut.GROUP_DIRECT);
        final String ANEXT = "Next Image";
        Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scNext.getKeyStroke(), ANEXT);
        Main.contentPane.getActionMap().put(ANEXT, nextAction);
        btnNext.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scNext.getKeyStroke(), ANEXT);
        btnNext.getActionMap().put(ANEXT, nextAction);

        JToggleButton tbCentre = new JToggleButton(new ImageAction(COMMAND_CENTERVIEW, ImageProvider.get("dialogs", "centreview"), tr("Center view")));
        tbCentre.setPreferredSize(buttonDim);

        JButton btnZoomBestFit = new JButton(new ImageAction(COMMAND_ZOOM, ImageProvider.get("dialogs", "zoom-best-fit"), tr("Zoom best fit and 1:1")));
        btnZoomBestFit.setPreferredSize(buttonDim);

        btnCollapse = new JButton(new ImageAction(COMMAND_COLLAPSE, ImageProvider.get("dialogs", "collapse"), tr("Move dialog to the side pane")));
        btnCollapse.setPreferredSize(new Dimension(20,20));
        btnCollapse.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel buttons = new JPanel();
        buttons.add(btnPrevious);
        buttons.add(btnNext);
        buttons.add(Box.createRigidArea(new Dimension(14, 0)));
        buttons.add(tbCentre);
        buttons.add(btnZoomBestFit);
        buttons.add(Box.createRigidArea(new Dimension(14, 0)));
        buttons.add(btnDelete);
        buttons.add(btnDeleteFromDisk);

        JPanel bottomPane = new JPanel();
        bottomPane.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 1;
        bottomPane.add(buttons, gc);

        gc.gridx = 1;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.PAGE_END;
        gc.weightx = 0;
        bottomPane.add(btnCollapse, gc);

        content.add(bottomPane, BorderLayout.SOUTH);

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
                if (centerView && currentEntry != null && currentEntry.getPos() != null) {
                    Main.map.mapView.zoomTo(currentEntry.getPos());
                }

            } else if (COMMAND_ZOOM.equals(action)) {
                imgDisplay.zoomBestFitOrOne();

            } else if (COMMAND_REMOVE.equals(action)) {
                if (currentLayer != null) {
                   currentLayer.removeCurrentPhoto();
                }
            } else if (COMMAND_REMOVE_FROM_DISK.equals(action)) {
                if (currentLayer != null) {
                    currentLayer.removeCurrentPhotoFromDisk();
                }
            } else if (COMMAND_COLLAPSE.equals(action)) {
                collapseButtonClicked = true;
                detachedDialog.getToolkit().getSystemEventQueue().postEvent(new WindowEvent(detachedDialog, WindowEvent.WINDOW_CLOSING));
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
//            if (currentLayer == layer && currentEntry == entry) {
//                repaint();
//                return;
//            }                     TODO: pop up image dialog but don't load image again

            if (centerView && Main.map != null && entry != null && entry.getPos() != null) {
                Main.map.mapView.zoomTo(entry.getPos());
            }

            currentLayer = layer;
            currentEntry = entry;
        }

        if (entry != null) {
            imgDisplay.setImage(entry.file);
            titleBar.setTitle("Geotagged Images" + (entry.file != null ? " - " + entry.file.getName() : ""));
            StringBuffer osd = new StringBuffer(entry.file != null ? entry.file.getName() : "");
            if (entry.getElevation() != null) {
                osd.append(tr("\nAltitude: {0} m", entry.getElevation().longValue()));
            }
            if (entry.getSpeed() != null) {
                osd.append(tr("\n{0} km/h", Math.round(entry.getSpeed())));
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
        if (collapseButtonClicked) {
            collapseButtonClicked = false;
            return true;
        }
        return false;
    }

    @Override
    protected void stateChanged() {
        super.stateChanged();
        if (btnCollapse != null) {
            btnCollapse.setVisible(!isDocked);
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
