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
import java.text.SimpleDateFormat;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.actions.JosmAction;
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

    private final ImageZoomAction imageZoomAction = new ImageZoomAction();
    private final ImageCenterViewAction imageCenterViewAction = new ImageCenterViewAction();
    private final ImageNextAction imageNextAction = new ImageNextAction();
    private final ImageRemoveAction imageRemoveAction = new ImageRemoveAction();
    private final ImageRemoveFromDiskAction imageRemoveFromDiskAction = new ImageRemoveFromDiskAction();
    private final ImagePreviousAction imagePreviousAction = new ImagePreviousAction();
    private final ImageCollapseAction imageCollapseAction = new ImageCollapseAction();
    private final ImageFirstAction imageFirstAction = new ImageFirstAction();
    private final ImageLastAction imageLastAction = new ImageLastAction();
    private final ImageCopyPathAction imageCopyPathAction = new ImageCopyPathAction();

    private final ImageDisplay imgDisplay = new ImageDisplay();
    private boolean centerView;

    // Only one instance of that class is present at one time
    private static volatile ImageViewerDialog dialog;

    private boolean collapseButtonClicked;

    /**
     * Replies the unique instance of this dialog
     * @return the unique instance
     */
    public static ImageViewerDialog getInstance() {
        if (dialog == null)
            dialog = new ImageViewerDialog();
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

        btnPrevious = new JButton(imagePreviousAction);
        btnPrevious.setPreferredSize(buttonDim);
        btnPrevious.setEnabled(false);

        JButton btnDelete = new JButton(imageRemoveAction);
        btnDelete.setPreferredSize(buttonDim);

        JButton btnDeleteFromDisk = new JButton(imageRemoveFromDiskAction);
        btnDeleteFromDisk.setPreferredSize(buttonDim);

        JButton btnCopyPath = new JButton(imageCopyPathAction);
        btnCopyPath.setPreferredSize(buttonDim);

        btnNext = new JButton(imageNextAction);
        btnNext.setPreferredSize(buttonDim);
        btnNext.setEnabled(false);

        tbCentre = new JToggleButton(imageCenterViewAction);
        tbCentre.setPreferredSize(buttonDim);

        JButton btnZoomBestFit = new JButton(imageZoomAction);
        btnZoomBestFit.setPreferredSize(buttonDim);

        btnCollapse = new JButton(imageCollapseAction);
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
        // Manually destroy actions until JButtons are replaced by standard SideButtons
        imageFirstAction.destroy();
        imageLastAction.destroy();
        imagePreviousAction.destroy();
        imageNextAction.destroy();
        imageCenterViewAction.destroy();
        imageCollapseAction.destroy();
        imageCopyPathAction.destroy();
        imageRemoveAction.destroy();
        imageRemoveFromDiskAction.destroy();
        imageZoomAction.destroy();
        super.destroy();
        dialog = null;
    }

    private class ImageNextAction extends JosmAction {
        ImageNextAction() {
            super(null, new ImageProvider("dialogs", "next"), tr("Next"), Shortcut.registerShortcut(
                    "geoimage:next", tr("Geoimage: {0}", tr("Show next Image")), KeyEvent.VK_PAGE_DOWN, Shortcut.DIRECT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentLayer != null) {
                currentLayer.showNextPhoto();
            }
        }
    }

    private class ImagePreviousAction extends JosmAction {
        ImagePreviousAction() {
            super(null, new ImageProvider("dialogs", "previous"), tr("Previous"), Shortcut.registerShortcut(
                    "geoimage:previous", tr("Geoimage: {0}", tr("Show previous Image")), KeyEvent.VK_PAGE_UP, Shortcut.DIRECT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentLayer != null) {
                currentLayer.showPreviousPhoto();
            }
        }
    }

    private class ImageFirstAction extends JosmAction {
        ImageFirstAction() {
            super(null, (ImageProvider) null, null, Shortcut.registerShortcut(
                    "geoimage:first", tr("Geoimage: {0}", tr("Show first Image")), KeyEvent.VK_HOME, Shortcut.DIRECT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentLayer != null) {
                currentLayer.showFirstPhoto();
            }
        }
    }

    private class ImageLastAction extends JosmAction {
        ImageLastAction() {
            super(null, (ImageProvider) null, null, Shortcut.registerShortcut(
                    "geoimage:last", tr("Geoimage: {0}", tr("Show last Image")), KeyEvent.VK_END, Shortcut.DIRECT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentLayer != null) {
                currentLayer.showLastPhoto();
            }
        }
    }

    private class ImageCenterViewAction extends JosmAction {
        ImageCenterViewAction() {
            super(null, new ImageProvider("dialogs", "centreview"), tr("Center view"), null,
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final JToggleButton button = (JToggleButton) e.getSource();
            centerView = button.isEnabled() && button.isSelected();
            if (centerView && currentEntry != null && currentEntry.getPos() != null) {
                MainApplication.getMap().mapView.zoomTo(currentEntry.getPos());
            }
        }
    }

    private class ImageZoomAction extends JosmAction {
        ImageZoomAction() {
            super(null, new ImageProvider("dialogs", "zoom-best-fit"), tr("Zoom best fit and 1:1"), null,
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            imgDisplay.zoomBestFitOrOne();
        }
    }

    private class ImageRemoveAction extends JosmAction {
        ImageRemoveAction() {
            super(null, new ImageProvider("dialogs", "delete"), tr("Remove photo from layer"), Shortcut.registerShortcut(
                    "geoimage:deleteimagefromlayer", tr("Geoimage: {0}", tr("Remove photo from layer")), KeyEvent.VK_DELETE, Shortcut.SHIFT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentLayer != null) {
                currentLayer.removeCurrentPhoto();
            }
        }
    }

    private class ImageRemoveFromDiskAction extends JosmAction {
        ImageRemoveFromDiskAction() {
            super(null, new ImageProvider("dialogs", "geoimage/deletefromdisk"), tr("Delete image file from disk"),
                  Shortcut.registerShortcut(
                    "geoimage:deletefilefromdisk", tr("Geoimage: {0}", tr("Delete File from disk")), KeyEvent.VK_DELETE, Shortcut.CTRL_SHIFT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentLayer != null) {
                currentLayer.removeCurrentPhotoFromDisk();
            }
        }
    }

    private class ImageCopyPathAction extends JosmAction {
        ImageCopyPathAction() {
            super(null, new ImageProvider("copy"), tr("Copy image path"), Shortcut.registerShortcut(
                    "geoimage:copypath", tr("Geoimage: {0}", tr("Copy image path")), KeyEvent.VK_C, Shortcut.ALT_CTRL_SHIFT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentLayer != null) {
                currentLayer.copyCurrentPhotoPath();
            }
        }
    }

    private class ImageCollapseAction extends JosmAction {
        ImageCollapseAction() {
            super(null, new ImageProvider("dialogs", "collapse"), tr("Move dialog to the side pane"), null,
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            collapseButtonClicked = true;
            detachedDialog.getToolkit().getSystemEventQueue().postEvent(new WindowEvent(detachedDialog, WindowEvent.WINDOW_CLOSING));
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
                imgDisplay.setImage(entry);
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
            // Make sure date/time format includes milliseconds
            if (dtf instanceof SimpleDateFormat) {
                String pattern = ((SimpleDateFormat) dtf).toPattern();
                if (!pattern.contains(".SSS")) {
                    dtf = new SimpleDateFormat(pattern.replace(":ss", ":ss.SSS"));
                }
            }
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
            imgDisplay.setImage(null);
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
