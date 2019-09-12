// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.ImageData;
import org.openstreetmap.josm.data.ImageData.ImageDataUpdateListener;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
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
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Dialog to view and manipulate geo-tagged images from a {@link GeoImageLayer}.
 */
public final class ImageViewerDialog extends ToggleDialog implements LayerChangeListener, ActiveLayerChangeListener, ImageDataUpdateListener {

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

    static void createInstance() {
        if (dialog != null)
            throw new IllegalStateException("ImageViewerDialog instance was already created");
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

    private JButton btnLast;
    private JButton btnNext;
    private JButton btnPrevious;
    private JButton btnFirst;
    private JButton btnCollapse;
    private JButton btnDelete;
    private JButton btnCopyPath;
    private JButton btnDeleteFromDisk;
    private JToggleButton tbCentre;

    private ImageViewerDialog() {
        super(tr("Geotagged Images"), "geoimage", tr("Display geotagged images"), Shortcut.registerShortcut("tools:geotagged",
        tr("Tool: {0}", tr("Display geotagged images")), KeyEvent.VK_Y, Shortcut.DIRECT), 200);
        build();
        MainApplication.getLayerManager().addActiveLayerChangeListener(this);
        MainApplication.getLayerManager().addLayerChangeListener(this);
        for (Layer l: MainApplication.getLayerManager().getLayers()) {
            registerOnLayer(l);
        }
    }

    private static JButton createNavigationButton(JosmAction action, Dimension buttonDim) {
        JButton btn = new JButton(action);
        btn.setPreferredSize(buttonDim);
        btn.setEnabled(false);
        return btn;
    }

    private void build() {
        JPanel content = new JPanel(new BorderLayout());

        content.add(imgDisplay, BorderLayout.CENTER);

        Dimension buttonDim = new Dimension(26, 26);

        btnFirst = createNavigationButton(imageFirstAction, buttonDim);
        btnPrevious = createNavigationButton(imagePreviousAction, buttonDim);

        btnDelete = new JButton(imageRemoveAction);
        btnDelete.setPreferredSize(buttonDim);

        btnDeleteFromDisk = new JButton(imageRemoveFromDiskAction);
        btnDeleteFromDisk.setPreferredSize(buttonDim);

        btnCopyPath = new JButton(imageCopyPathAction);
        btnCopyPath.setPreferredSize(buttonDim);

        btnNext = createNavigationButton(imageNextAction, buttonDim);
        btnLast = createNavigationButton(imageLastAction, buttonDim);

        tbCentre = new JToggleButton(imageCenterViewAction);
        tbCentre.setPreferredSize(buttonDim);

        JButton btnZoomBestFit = new JButton(imageZoomAction);
        btnZoomBestFit.setPreferredSize(buttonDim);

        btnCollapse = new JButton(imageCollapseAction);
        btnCollapse.setPreferredSize(new Dimension(20, 20));
        btnCollapse.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel buttons = new JPanel();
        buttons.add(btnFirst);
        buttons.add(btnPrevious);
        buttons.add(btnNext);
        buttons.add(btnLast);
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
            if (currentData != null) {
                currentData.selectNextImage();
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
            if (currentData != null) {
                currentData.selectPreviousImage();
            }
        }
    }

    private class ImageFirstAction extends JosmAction {
        ImageFirstAction() {
            super(null, new ImageProvider("dialogs", "first"), tr("First"), Shortcut.registerShortcut(
                    "geoimage:first", tr("Geoimage: {0}", tr("Show first Image")), KeyEvent.VK_HOME, Shortcut.DIRECT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentData != null) {
                currentData.selectFirstImage();
            }
        }
    }

    private class ImageLastAction extends JosmAction {
        ImageLastAction() {
            super(null, new ImageProvider("dialogs", "last"), tr("Last"), Shortcut.registerShortcut(
                    "geoimage:last", tr("Geoimage: {0}", tr("Show last Image")), KeyEvent.VK_END, Shortcut.DIRECT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentData != null) {
                currentData.selectLastImage();
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
            super(null, new ImageProvider("dialogs", "delete"), tr("Remove photo(s) from layer"), Shortcut.registerShortcut(
                    "geoimage:deleteimagefromlayer", tr("Geoimage: {0}", tr("Remove photo(s) from layer")), KeyEvent.VK_DELETE, Shortcut.SHIFT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentData != null) {
                currentData.removeSelectedImages();
            }
        }
    }

    private class ImageRemoveFromDiskAction extends JosmAction {
        ImageRemoveFromDiskAction() {
            super(null, new ImageProvider("dialogs", "geoimage/deletefromdisk"), tr("Delete photo file(s) from disk"),
                  Shortcut.registerShortcut(
                    "geoimage:deletefilefromdisk", tr("Geoimage: {0}", tr("Delete file(s) from disk")), KeyEvent.VK_DELETE, Shortcut.CTRL_SHIFT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentData != null && currentData.getSelectedImage() != null) {
                List<ImageEntry> toDelete = currentData.getSelectedImages();
                int size = toDelete.size();

                int result = new ExtendedDialog(
                        MainApplication.getMainFrame(),
                        tr("Delete image file from disk"),
                        tr("Cancel"), tr("Delete"))
                        .setButtonIcons("cancel", "dialogs/delete")
                        .setContent(new JLabel("<html><h3>"
                                + trn("Delete the file from disk?",
                                      "Delete the {0} files from disk?", size, size)
                                + "<p>" + trn("The image file will be permanently lost!",
                                              "The images files will be permanently lost!", size) + "</h3></html>",
                                ImageProvider.get("dialogs/geoimage/deletefromdisk"), SwingConstants.LEFT))
                        .toggleEnable("geoimage.deleteimagefromdisk")
                        .setCancelButton(1)
                        .setDefaultButton(2)
                        .showDialog()
                        .getValue();

                if (result == 2) {
                    currentData.removeSelectedImages();
                    for (ImageEntry delete : toDelete) {
                        if (Utils.deleteFile(delete.getFile())) {
                            Logging.info("File " + delete.getFile() + " deleted.");
                        } else {
                            JOptionPane.showMessageDialog(
                                    MainApplication.getMainFrame(),
                                    tr("Image file could not be deleted."),
                                    tr("Error"),
                                    JOptionPane.ERROR_MESSAGE
                                    );
                        }
                    }
                }
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
            if (currentData != null) {
                ClipboardUtils.copyString(currentData.getSelectedImage().getFile().toString());
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

    /**
     * Displays image for the given data.
     * @param data geo image data
     * @param entry image entry
     * @deprecated Use {@link #displayImage}
     */
    @Deprecated
    public static void showImage(ImageData data, ImageEntry entry) {
        getInstance().displayImage(data, entry);
    }

    /**
     * Enables (or disables) the "Previous" button.
     * @param value {@code true} to enable the button, {@code false} otherwise
     */
    public void setPreviousEnabled(boolean value) {
        btnFirst.setEnabled(value);
        btnPrevious.setEnabled(value);
    }

    /**
     * Enables (or disables) the "Next" button.
     * @param value {@code true} to enable the button, {@code false} otherwise
     */
    public void setNextEnabled(boolean value) {
        btnNext.setEnabled(value);
        btnLast.setEnabled(value);
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

    private transient ImageData currentData;
    private transient ImageEntry currentEntry;

    /**
     * Displays a single image for the given layer.
     * @param data the image data
     * @param entry image entry
     * @see #displayImages
     */
    public void displayImage(ImageData data, ImageEntry entry) {
        displayImages(data, Collections.singletonList(entry));
    }

    /**
     * Displays images for the given layer.
     * @param data the image data
     * @param entries image entries
     * @since 15333
     */
    public void displayImages(ImageData data, List<ImageEntry> entries) {
        boolean imageChanged;
        ImageEntry entry = entries != null && entries.size() == 1 ? entries.get(0) : null;

        synchronized (this) {
            // TODO: pop up image dialog but don't load image again

            imageChanged = currentEntry != entry;

            if (centerView && entry != null && MainApplication.isDisplayingMapView() && entry.getPos() != null) {
                MainApplication.getMap().mapView.zoomTo(entry.getPos());
            }

            currentData = data;
            currentEntry = entry;
        }

        if (entry != null) {
            setNextEnabled(data.hasNextImage());
            setPreviousEnabled(data.hasPreviousImage());
            btnDelete.setEnabled(true);
            btnDeleteFromDisk.setEnabled(true);
            btnCopyPath.setEnabled(true);

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
            Optional.ofNullable(entry.getIptcCaption()).map(s -> tr("\nCaption: {0}", s)).ifPresent(osd::append);
            Optional.ofNullable(entry.getIptcHeadline()).map(s -> tr("\nHeadline: {0}", s)).ifPresent(osd::append);
            Optional.ofNullable(entry.getIptcKeywords()).map(s -> tr("\nKeywords: {0}", s)).ifPresent(osd::append);
            Optional.ofNullable(entry.getIptcObjectName()).map(s -> tr("\nObject name: {0}", s)).ifPresent(osd::append);

            imgDisplay.setOsdText(osd.toString());
        } else {
            boolean hasMultipleImages = entries != null && entries.size() > 1;
            // if this method is called to reinitialize dialog content with a blank image,
            // do not actually show the dialog again with a blank image if currently hidden (fix #10672)
            setTitle(tr("Geotagged Images"));
            imgDisplay.setImage(null);
            imgDisplay.setOsdText("");
            setNextEnabled(false);
            setPreviousEnabled(false);
            btnDelete.setEnabled(hasMultipleImages);
            btnDeleteFromDisk.setEnabled(hasMultipleImages);
            btnCopyPath.setEnabled(false);
            if (hasMultipleImages) {
                imgDisplay.setEmptyText(tr("Multiple images selected"));
                btnFirst.setEnabled(!isFirstImageSelected(data));
                btnLast.setEnabled(!isLastImageSelected(data));
            }
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

    private static boolean isLastImageSelected(ImageData data) {
        return data.isImageSelected(data.getImages().get(data.getImages().size() - 1));
    }

    private static boolean isFirstImageSelected(ImageData data) {
        return data.isImageSelected(data.getImages().get(0));
    }

    /**
     * When an image is closed, really close it and do not pop
     * up the side dialog.
     */
    @Override
    protected boolean dockWhenClosingDetachedDlg() {
        if (collapseButtonClicked) {
            collapseButtonClicked = false;
            return super.dockWhenClosingDetachedDlg();
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
     * Returns whether the center view is currently active.
     * @return {@code true} if the center view is active, {@code false} otherwise
     * @since 9416
     */
    public static boolean isCenterView() {
        return getInstance().centerView;
    }

    @Override
    public void layerAdded(LayerAddEvent e) {
        registerOnLayer(e.getAddedLayer());
        showLayer(e.getAddedLayer());
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        if (e.getRemovedLayer() instanceof GeoImageLayer) {
            ImageData removedData = ((GeoImageLayer) e.getRemovedLayer()).getImageData();
            if (removedData == currentData) {
                displayImages(null, null);
            }
            removedData.removeImageDataUpdateListener(this);
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

    private void registerOnLayer(Layer layer) {
        if (layer instanceof GeoImageLayer) {
            ((GeoImageLayer) layer).getImageData().addImageDataUpdateListener(this);
        }
    }

    private void showLayer(Layer newLayer) {
        if (currentData == null && newLayer instanceof GeoImageLayer) {
            ((GeoImageLayer) newLayer).getImageData().selectFirstImage();
        }
    }

    @Override
    public void selectedImageChanged(ImageData data) {
        displayImages(data, data.getSelectedImages());
    }

    @Override
    public void imageDataUpdated(ImageData data) {
        displayImages(data, data.getSelectedImages());
    }
}
