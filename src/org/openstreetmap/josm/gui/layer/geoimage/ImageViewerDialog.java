// License: GPL. For details, see LICENSE file.
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

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel.Action;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Dialog to view and manipulate geo-tagged images from a {@link GeoImageLayer}.
 */
public final class ImageViewerDialog extends ToggleDialog implements LayerChangeListener, ActiveLayerChangeListener {

    private static final String COMMAND_ZOOM = "zoom";
    private static final String COMMAND_CENTERVIEW = "centre";
    private static final String COMMAND_NEXT = "next";
    private static final String COMMAND_REMOVE = "remove";
    private static final String COMMAND_REMOVE_FROM_DISK = "removefromdisk";
    private static final String COMMAND_PREVIOUS = "previous";
    private static final String COMMAND_COLLAPSE = "collapse";
    private static final String COMMAND_FIRST = "first";
    private static final String COMMAND_LAST = "last";
    private static final String COMMAND_COPY_PATH = "copypath";

    private final ImageDisplay imgDisplay = new ImageDisplay();
    private boolean centerView;

    // Only one instance of that class is present at one time
    private static volatile ImageViewerDialog dialog;

    private boolean collapseButtonClicked;

    static void newInstance() {
        dialog = new ImageViewerDialog();
    }

    /**
     * Replies the unique instance of this dialog
     * @return the unique instance
     */
    public static ImageViewerDialog getInstance() {
        if (dialog == null)
            throw new AssertionError("a new instance needs to be created first");
        return dialog;
    }

    private JButton btnNext;
    private JButton btnPrevious;
    private JButton btnCollapse;
    private JToggleButton tbCentre;

    private ImageViewerDialog() {
        super(tr("Geotagged Images"), "geoimage", tr("Display geotagged images"), Shortcut.registerShortcut("tools:geotagged",
        tr("Tool: {0}", tr("Display geotagged images")), KeyEvent.VK_Y, Shortcut.DIRECT), 200);
        build();
        MainApplication.getLayerManager().addActiveLayerChangeListener(this);
        MainApplication.getLayerManager().addLayerChangeListener(this);
    }

    private void build() {
        JPanel content = new JPanel(new BorderLayout());

        content.add(imgDisplay, BorderLayout.CENTER);

        Dimension buttonDim = new Dimension(26, 26);

        ImageAction prevAction = new ImageAction(COMMAND_PREVIOUS, ImageProvider.get("dialogs", "previous"), tr("Previous"));
        btnPrevious = new JButton(prevAction);
        btnPrevious.setPreferredSize(buttonDim);
        Shortcut scPrev = Shortcut.registerShortcut(
                "geoimage:previous", tr("Geoimage: {0}", tr("Show previous Image")), KeyEvent.VK_PAGE_UP, Shortcut.DIRECT);
        final String previousImage = "Previous Image";
        MainApplication.registerActionShortcut(prevAction, scPrev);
        btnPrevious.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scPrev.getKeyStroke(), previousImage);
        btnPrevious.getActionMap().put(previousImage, prevAction);
        btnPrevious.setEnabled(false);

        final String removePhoto = tr("Remove photo from layer");
        ImageAction delAction = new ImageAction(COMMAND_REMOVE, ImageProvider.get("dialogs", "delete"), removePhoto);
        JButton btnDelete = new JButton(delAction);
        btnDelete.setPreferredSize(buttonDim);
        Shortcut scDelete = Shortcut.registerShortcut(
                "geoimage:deleteimagefromlayer", tr("Geoimage: {0}", tr("Remove photo from layer")), KeyEvent.VK_DELETE, Shortcut.SHIFT);
        MainApplication.registerActionShortcut(delAction, scDelete);
        btnDelete.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scDelete.getKeyStroke(), removePhoto);
        btnDelete.getActionMap().put(removePhoto, delAction);

        ImageAction delFromDiskAction = new ImageAction(COMMAND_REMOVE_FROM_DISK,
                ImageProvider.get("dialogs", "geoimage/deletefromdisk"), tr("Delete image file from disk"));
        JButton btnDeleteFromDisk = new JButton(delFromDiskAction);
        btnDeleteFromDisk.setPreferredSize(buttonDim);
        Shortcut scDeleteFromDisk = Shortcut.registerShortcut(
                "geoimage:deletefilefromdisk", tr("Geoimage: {0}", tr("Delete File from disk")), KeyEvent.VK_DELETE, Shortcut.CTRL_SHIFT);
        final String deleteImage = "Delete image file from disk";
        MainApplication.registerActionShortcut(delFromDiskAction, scDeleteFromDisk);
        btnDeleteFromDisk.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scDeleteFromDisk.getKeyStroke(), deleteImage);
        btnDeleteFromDisk.getActionMap().put(deleteImage, delFromDiskAction);

        ImageAction copyPathAction = new ImageAction(COMMAND_COPY_PATH, ImageProvider.get("copy"), tr("Copy image path"));
        JButton btnCopyPath = new JButton(copyPathAction);
        btnCopyPath.setPreferredSize(buttonDim);
        Shortcut scCopyPath = Shortcut.registerShortcut(
                "geoimage:copypath", tr("Geoimage: {0}", tr("Copy image path")), KeyEvent.VK_C, Shortcut.ALT_CTRL_SHIFT);
        final String copyImage = "Copy image path";
        MainApplication.registerActionShortcut(copyPathAction, scCopyPath);
        btnCopyPath.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scCopyPath.getKeyStroke(), copyImage);
        btnCopyPath.getActionMap().put(copyImage, copyPathAction);

        ImageAction nextAction = new ImageAction(COMMAND_NEXT, ImageProvider.get("dialogs", "next"), tr("Next"));
        btnNext = new JButton(nextAction);
        btnNext.setPreferredSize(buttonDim);
        Shortcut scNext = Shortcut.registerShortcut(
                "geoimage:next", tr("Geoimage: {0}", tr("Show next Image")), KeyEvent.VK_PAGE_DOWN, Shortcut.DIRECT);
        final String nextImage = "Next Image";
        MainApplication.registerActionShortcut(nextAction, scNext);
        btnNext.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scNext.getKeyStroke(), nextImage);
        btnNext.getActionMap().put(nextImage, nextAction);
        btnNext.setEnabled(false);

        MainApplication.registerActionShortcut(
                new ImageAction(COMMAND_FIRST, null, null),
                Shortcut.registerShortcut(
                        "geoimage:first", tr("Geoimage: {0}", tr("Show first Image")), KeyEvent.VK_HOME, Shortcut.DIRECT)
        );
        MainApplication.registerActionShortcut(
                new ImageAction(COMMAND_LAST, null, null),
                Shortcut.registerShortcut(
                        "geoimage:last", tr("Geoimage: {0}", tr("Show last Image")), KeyEvent.VK_END, Shortcut.DIRECT)
        );

        tbCentre = new JToggleButton(new ImageAction(COMMAND_CENTERVIEW,
                ImageProvider.get("dialogs", "centreview"), tr("Center view")));
        tbCentre.setPreferredSize(buttonDim);

        JButton btnZoomBestFit = new JButton(new ImageAction(COMMAND_ZOOM,
                ImageProvider.get("dialogs", "zoom-best-fit"), tr("Zoom best fit and 1:1")));
        btnZoomBestFit.setPreferredSize(buttonDim);

        btnCollapse = new JButton(new ImageAction(COMMAND_COLLAPSE,
                ImageProvider.get("dialogs", "collapse"), tr("Move dialog to the side pane")));
        btnCollapse.setPreferredSize(new Dimension(20, 20));
        btnCollapse.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel buttons = new JPanel();
        buttons.add(btnPrevious);
        buttons.add(btnNext);
        buttons.add(Box.createRigidArea(new Dimension(7, 0)));
        buttons.add(tbCentre);
        buttons.add(btnZoomBestFit);
        buttons.add(Box.createRigidArea(new Dimension(7, 0)));
        buttons.add(btnDelete);
        buttons.add(btnDeleteFromDisk);
        buttons.add(Box.createRigidArea(new Dimension(7, 0)));
        buttons.add(btnCopyPath);

        JPanel bottomPane = new JPanel(new GridBagLayout());
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

        createLayout(content, false, null);
    }

    @Override
    public void destroy() {
        MainApplication.getLayerManager().removeActiveLayerChangeListener(this);
        MainApplication.getLayerManager().removeLayerChangeListener(this);
        super.destroy();
    }

    class ImageAction extends AbstractAction {
        private final String action;

        ImageAction(String action, ImageIcon icon, String toolTipText) {
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
                final JToggleButton button = (JToggleButton) e.getSource();
                centerView = button.isEnabled() && button.isSelected();
                if (centerView && currentEntry != null && currentEntry.getPos() != null) {
                    MainApplication.getMap().mapView.zoomTo(currentEntry.getPos());
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
            } else if (COMMAND_COPY_PATH.equals(action)) {
                if (currentLayer != null) {
                    currentLayer.copyCurrentPhotoPath();
                }
            } else if (COMMAND_COLLAPSE.equals(action)) {
                collapseButtonClicked = true;
                detachedDialog.getToolkit().getSystemEventQueue().postEvent(new WindowEvent(detachedDialog, WindowEvent.WINDOW_CLOSING));
            }
        }
    }

    public static void showImage(GeoImageLayer layer, ImageEntry entry) {
        getInstance().displayImage(layer, entry);
        if (layer != null) {
            layer.checkPreviousNextButtons();
        } else {
            setPreviousEnabled(false);
            setNextEnabled(false);
        }
    }

    /**
     * Enables (or disables) the "Previous" button.
     * @param value {@code true} to enable the button, {@code false} otherwise
     */
    public static void setPreviousEnabled(boolean value) {
        getInstance().btnPrevious.setEnabled(value);
    }

    /**
     * Enables (or disables) the "Next" button.
     * @param value {@code true} to enable the button, {@code false} otherwise
     */
    public static void setNextEnabled(boolean value) {
        getInstance().btnNext.setEnabled(value);
    }

    /**
     * Enables (or disables) the "Center view" button.
     * @param value {@code true} to enable the button, {@code false} otherwise
     * @return the old enabled value. Can be used to restore the original enable state
     */
    public static synchronized boolean setCentreEnabled(boolean value) {
        final ImageViewerDialog instance = getInstance();
        final boolean wasEnabled = instance.tbCentre.isEnabled();
        instance.tbCentre.setEnabled(value);
        instance.tbCentre.getAction().actionPerformed(new ActionEvent(instance.tbCentre, 0, null));
        return wasEnabled;
    }

    private transient GeoImageLayer currentLayer;
    private transient ImageEntry currentEntry;

    public void displayImage(GeoImageLayer layer, ImageEntry entry) {
        boolean imageChanged;

        synchronized (this) {
            // TODO: pop up image dialog but don't load image again

            imageChanged = currentEntry != entry;

            if (centerView && entry != null && MainApplication.isDisplayingMapView() && entry.getPos() != null) {
                MainApplication.getMap().mapView.zoomTo(entry.getPos());
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
            setTitle(tr("Geotagged Images") + (entry.getFile() != null ? " - " + entry.getFile().getName() : ""));
            StringBuilder osd = new StringBuilder(entry.getFile() != null ? entry.getFile().getName() : "");
            if (entry.getElevation() != null) {
                osd.append(tr("\nAltitude: {0} m", Math.round(entry.getElevation())));
            }
            if (entry.getSpeed() != null) {
                osd.append(tr("\nSpeed: {0} km/h", Math.round(entry.getSpeed())));
            }
            if (entry.getExifImgDir() != null) {
                osd.append(tr("\nDirection {0}\u00b0", Math.round(entry.getExifImgDir())));
            }
            DateFormat dtf = DateUtils.getDateTimeFormat(DateFormat.SHORT, DateFormat.MEDIUM);
            if (entry.hasExifTime()) {
                osd.append(tr("\nEXIF time: {0}", dtf.format(entry.getExifTime())));
            }
            if (entry.hasGpsTime()) {
                osd.append(tr("\nGPS time: {0}", dtf.format(entry.getGpsTime())));
            }

            imgDisplay.setOsdText(osd.toString());
        } else {
            // if this method is called to reinitialize dialog content with a blank image,
            // do not actually show the dialog again with a blank image if currently hidden (fix #10672)
            setTitle(tr("Geotagged Images"));
            imgDisplay.setImage(null, null);
            imgDisplay.setOsdText("");
            return;
        }
        if (!isDialogShowing()) {
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

    /**
     * Returns whether the center view is currently active.
     * @return {@code true} if the center view is active, {@code false} otherwise
     * @since 9416
     */
    public static boolean isCenterView() {
        return getInstance().centerView;
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        showLayer(e.getAddedLayer());
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        // Clear current image and layer if current layer is deleted
        if (currentLayer != null && currentLayer.equals(e.getRemovedLayer())) {
            showImage(null, null);
        }
        // Check buttons state in case of layer merging
        if (currentLayer != null && e.getRemovedLayer() instanceof GeoImageLayer) {
            currentLayer.checkPreviousNextButtons();
        }
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        // ignored
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        showLayer(e.getSource().getActiveLayer());
    }

    private void showLayer(Layer newLayer) {
        if (currentLayer == null && newLayer instanceof GeoImageLayer) {
            ((GeoImageLayer) newLayer).showFirstPhoto();
        }
    }

}
