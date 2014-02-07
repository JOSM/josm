// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.text.DateFormat;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel.Action;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

public final class ImageViewerDialog extends ToggleDialog {

    private static final String COMMAND_ZOOM = "zoom";
    private static final String COMMAND_CENTERVIEW = "centre";
    private static final String COMMAND_NEXT = "next";
    private static final String COMMAND_REMOVE = "remove";
    private static final String COMMAND_REMOVE_FROM_DISK = "removefromdisk";
    private static final String COMMAND_PREVIOUS = "previous";
    private static final String COMMAND_COLLAPSE = "collapse";
    private static final String COMMAND_FIRST = "first";
    private static final String COMMAND_LAST = "last";

    private ImageDisplay imgDisplay = new ImageDisplay();
    private boolean centerView = false;

    // Only one instance of that class is present at one time
    private static ImageViewerDialog dialog;

    private boolean collapseButtonClicked = false;

    static void newInstance() {
        dialog = new ImageViewerDialog();
    }

    public static ImageViewerDialog getInstance() {
        if (dialog == null)
            throw new AssertionError("a new instance needs to be created first"); 
        return dialog;
    }

    private JButton btnNext;
    private JButton btnPrevious;
    private JButton btnCollapse;

    private ImageViewerDialog() {
        super(tr("Geotagged Images"), "geoimage", tr("Display geotagged images"), Shortcut.registerShortcut("tools:geotagged",
        tr("Tool: {0}", tr("Display geotagged images")), KeyEvent.VK_Y, Shortcut.DIRECT), 200);

        // Don't show a detached dialog right from the start.
        if (isShowing && !isDocked) {
            setIsShowing(false);
        }

        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        content.add(imgDisplay, BorderLayout.CENTER);

        Dimension buttonDim = new Dimension(26,26);

        ImageAction prevAction = new ImageAction(COMMAND_PREVIOUS, ImageProvider.get("dialogs", "previous"), tr("Previous"));
        btnPrevious = new JButton(prevAction);
        btnPrevious.setPreferredSize(buttonDim);
        Shortcut scPrev = Shortcut.registerShortcut(
                "geoimage:previous", tr("Geoimage: {0}", tr("Show previous Image")), KeyEvent.VK_PAGE_UP, Shortcut.DIRECT);
        final String APREVIOUS = "Previous Image";
        Main.registerActionShortcut(prevAction, scPrev);
        btnPrevious.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scPrev.getKeyStroke(), APREVIOUS);
        btnPrevious.getActionMap().put(APREVIOUS, prevAction);

        final String DELETE_TEXT = tr("Remove photo from layer");
        ImageAction delAction = new ImageAction(COMMAND_REMOVE, ImageProvider.get("dialogs", "delete"), DELETE_TEXT);
        JButton btnDelete = new JButton(delAction);
        btnDelete.setPreferredSize(buttonDim);
        Shortcut scDelete = Shortcut.registerShortcut(
                "geoimage:deleteimagefromlayer", tr("Geoimage: {0}", tr("Remove photo from layer")), KeyEvent.VK_DELETE, Shortcut.SHIFT);
        Main.registerActionShortcut(delAction, scDelete);
        btnDelete.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scDelete.getKeyStroke(), DELETE_TEXT);
        btnDelete.getActionMap().put(DELETE_TEXT, delAction);

        ImageAction delFromDiskAction = new ImageAction(COMMAND_REMOVE_FROM_DISK, ImageProvider.get("dialogs", "geoimage/deletefromdisk"), tr("Delete image file from disk"));
        JButton btnDeleteFromDisk = new JButton(delFromDiskAction);
        btnDeleteFromDisk.setPreferredSize(buttonDim);
        Shortcut scDeleteFromDisk = Shortcut.registerShortcut(
                "geoimage:deletefilefromdisk", tr("Geoimage: {0}", tr("Delete File from disk")), KeyEvent.VK_DELETE, Shortcut.CTRL_SHIFT);
        final String ADELFROMDISK = "Delete image file from disk";
        Main.registerActionShortcut(delFromDiskAction, scDeleteFromDisk);
        btnDeleteFromDisk.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scDeleteFromDisk.getKeyStroke(), ADELFROMDISK);
        btnDeleteFromDisk.getActionMap().put(ADELFROMDISK, delFromDiskAction);

        ImageAction nextAction = new ImageAction(COMMAND_NEXT, ImageProvider.get("dialogs", "next"), tr("Next"));
        btnNext = new JButton(nextAction);
        btnNext.setPreferredSize(buttonDim);
        Shortcut scNext = Shortcut.registerShortcut(
                "geoimage:next", tr("Geoimage: {0}", tr("Show next Image")), KeyEvent.VK_PAGE_DOWN, Shortcut.DIRECT);
        final String ANEXT = "Next Image";
        Main.registerActionShortcut(nextAction, scNext);
        btnNext.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scNext.getKeyStroke(), ANEXT);
        btnNext.getActionMap().put(ANEXT, nextAction);

        Main.registerActionShortcut(
                new ImageAction(COMMAND_FIRST, null, null),
                Shortcut.registerShortcut(
                        "geoimage:first", tr("Geoimage: {0}", tr("Show first Image")), KeyEvent.VK_HOME, Shortcut.DIRECT)
        );
        Main.registerActionShortcut(
                new ImageAction(COMMAND_LAST, null, null),
                Shortcut.registerShortcut(
                        "geoimage:last", tr("Geoimage: {0}", tr("Show last Image")), KeyEvent.VK_END, Shortcut.DIRECT)
        );

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

        @Override
        public void actionPerformed(ActionEvent e) {
            if (COMMAND_NEXT.equals(action)) {
                if (currentLayer != null) {
                    currentLayer.showNextPhoto();
                }
            } else if (COMMAND_PREVIOUS.equals(action)) {
                if (currentLayer != null) {
                    currentLayer.showPreviousPhoto();
                }
            } else if (COMMAND_FIRST.equals(action) && currentLayer != null) {
                currentLayer.showFirstPhoto();
            } else if (COMMAND_LAST.equals(action) && currentLayer != null) {
                currentLayer.showLastPhoto();

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
        boolean imageChanged;

        synchronized(this) {
            // TODO: pop up image dialog but don't load image again

            imageChanged = currentEntry != entry;

            if (centerView && Main.isDisplayingMapView() && entry != null && entry.getPos() != null) {
                Main.map.mapView.zoomTo(entry.getPos());
            }

            currentLayer = layer;
            currentEntry = entry;
        }

        if (entry != null) {
            if (imageChanged) {
                // Set only if the image is new to preserve zoom and position if the same image is redisplayed 
                // (e.g. to update the OSD).
                imgDisplay.setImage(entry.getFile(), entry.getExifOrientation());
            }
            setTitle("Geotagged Images" + (entry.getFile() != null ? " - " + entry.getFile().getName() : ""));
            StringBuilder osd = new StringBuilder(entry.getFile() != null ? entry.getFile().getName() : "");
            if (entry.getElevation() != null) {
                osd.append(tr("\nAltitude: {0} m", entry.getElevation().longValue()));
            }
            if (entry.getSpeed() != null) {
                osd.append(tr("\n{0} km/h", Math.round(entry.getSpeed())));
            }
            if (entry.getExifImgDir() != null) {
                osd.append(tr("\nDirection {0}\u00b0", Math.round(entry.getExifImgDir())));
            }
            DateFormat dtf = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            if (entry.hasExifTime()) {
                osd.append(tr("\nEXIF time: {0}", dtf.format(entry.getExifTime())));
            }
            if (entry.hasGpsTime()) {
                osd.append(tr("\nGPS time: {0}", dtf.format(entry.getGpsTime())));
            }

            imgDisplay.setOsdText(osd.toString());
        } else {
            imgDisplay.setImage(null, null);
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

    /**
     * Returns the currently displayed image.
     * @return Currently displayed image or {@code null}
     * @since 6392
     */
    public static ImageEntry getCurrentImage() {
        return getInstance().currentEntry;
    }

    /**
     * Returns the layer associated with the image.
     * @return Layer associated with the image
     * @since 6392
     */
    public static GeoImageLayer getCurrentLayer() {
        return getInstance().currentLayer;
    }
}
