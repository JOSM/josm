// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.ImageData;
import org.openstreetmap.josm.data.ImageData.ImageDataUpdateListener;
import org.openstreetmap.josm.data.imagery.street_level.IImageEntry;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.dialogs.DialogsPanel;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.dialogs.layer.LayerVisibilityAction;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.imagery.Vector3D;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Dialog to view and manipulate geo-tagged images from a {@link GeoImageLayer}.
 */
public final class ImageViewerDialog extends ToggleDialog implements LayerChangeListener, ActiveLayerChangeListener, ImageDataUpdateListener {
    private static final String GEOIMAGE_FILLER = marktr("Geoimage: {0}");
    private static final String DIALOG_FOLDER = "dialogs";

    private final ImageryFilterSettings imageryFilterSettings = new ImageryFilterSettings();

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
    private final ImageOpenExternalAction imageOpenExternalAction = new ImageOpenExternalAction();
    private final LayerVisibilityAction visibilityAction = new LayerVisibilityAction(Collections::emptyList,
            () -> Collections.singleton(imageryFilterSettings));

    private final ImageDisplay imgDisplay = new ImageDisplay(imageryFilterSettings);
    private Future<?> imgLoadingFuture;
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
    private JButton btnOpenExternal;
    private JButton btnDeleteFromDisk;
    private JToggleButton tbCentre;
    /** The layer tab (used to select images when multiple layers provide images, makes for easy switching) */
    private JPanel layers;

    private ImageViewerDialog() {
        super(tr("Geotagged Images"), "geoimage", tr("Display geotagged images"), Shortcut.registerShortcut("tools:geotagged",
        tr("Windows: {0}", tr("Geotagged Images")), KeyEvent.VK_Y, Shortcut.DIRECT), 200);
        build();
        MainApplication.getLayerManager().addActiveLayerChangeListener(this);
        MainApplication.getLayerManager().addLayerChangeListener(this);
        for (Layer l: MainApplication.getLayerManager().getLayers()) {
            registerOnLayer(l);
        }
    }

    private static JButton createButton(AbstractAction action, Dimension buttonDim) {
        JButton btn = new JButton(action);
        btn.setPreferredSize(buttonDim);
        btn.addPropertyChangeListener("enabled", e -> action.setEnabled(Boolean.TRUE.equals(e.getNewValue())));
        return btn;
    }

    private static JButton createNavigationButton(AbstractAction action, Dimension buttonDim) {
        JButton btn = createButton(action, buttonDim);
        btn.setEnabled(false);
        action.addPropertyChangeListener(l -> {
            if ("enabled".equals(l.getPropertyName())) {
                btn.setEnabled(action.isEnabled());
            }
        });
        return btn;
    }

    private void build() {
        JPanel content = new JPanel(new BorderLayout());
        this.layers = new JPanel(new GridBagLayout());
        content.add(layers, BorderLayout.NORTH);

        content.add(imgDisplay, BorderLayout.CENTER);

        Dimension buttonDim = new Dimension(26, 26);

        btnFirst = createNavigationButton(imageFirstAction, buttonDim);
        btnPrevious = createNavigationButton(imagePreviousAction, buttonDim);

        btnDelete = createButton(imageRemoveAction, buttonDim);
        btnDeleteFromDisk = createButton(imageRemoveFromDiskAction, buttonDim);
        btnCopyPath = createButton(imageCopyPathAction, buttonDim);
        btnOpenExternal = createButton(imageOpenExternalAction, buttonDim);

        btnNext = createNavigationButton(imageNextAction, buttonDim);
        btnLast = createNavigationButton(imageLastAction, buttonDim);

        tbCentre = new JToggleButton(imageCenterViewAction);
        tbCentre.setPreferredSize(buttonDim);

        JButton btnZoomBestFit = new JButton(imageZoomAction);
        btnZoomBestFit.setPreferredSize(buttonDim);

        btnCollapse = createButton(imageCollapseAction, new Dimension(20, 20));
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
        buttons.add(btnOpenExternal);
        buttons.add(Box.createRigidArea(new Dimension(7, 0)));
        buttons.add(createButton(visibilityAction, buttonDim));

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

    private void updateLayers() {
        if (this.tabbedEntries.size() <= 1) {
            this.layers.setVisible(false);
            this.layers.removeAll();
        } else {
            this.layers.setVisible(true);
            // Remove all old components
            this.layers.removeAll();
            MainLayerManager layerManager = MainApplication.getLayerManager();
            List<Layer> invalidLayers = this.tabbedEntries.keySet().stream().filter(layer -> !layerManager.containsLayer(layer))
                    .collect(Collectors.toList());
            // `null` is for anything using the old methods, without telling us what layer it comes from.
            invalidLayers.remove(null);
            // We need to do multiple calls to avoid ConcurrentModificationExceptions
            invalidLayers.forEach(this.tabbedEntries::remove);
            addButtonsForImageLayers();
        }
        this.revalidate();
    }

    /**
     * Add the buttons for image layers
     */
    private void addButtonsForImageLayers() {
        final IImageEntry<?> current;
        synchronized (this) {
            current = this.currentEntry;
        }
        List<JButton> layerButtons = new ArrayList<>(this.tabbedEntries.size());
        if (this.tabbedEntries.containsKey(null)) {
            List<IImageEntry<?>> nullEntries = this.tabbedEntries.get(null);
            JButton layerButton = createImageLayerButton(null, nullEntries);
            layerButtons.add(layerButton);
            layerButton.setEnabled(!nullEntries.contains(current));
        }
        for (Map.Entry<Layer, List<IImageEntry<?>>> entry :
                this.tabbedEntries.entrySet().stream().filter(entry -> entry.getKey() != null)
                        .sorted(Comparator.comparing(entry -> entry.getKey().getName())).collect(Collectors.toList())) {
            JButton layerButton = createImageLayerButton(entry.getKey(), entry.getValue());
            layerButtons.add(layerButton);
            layerButton.setEnabled(!entry.getValue().contains(current));
        }
        layerButtons.forEach(this.layers::add);
    }

    /**
     * Create a button for a specific layer and its entries
     *
     * @param layer     The layer to switch to
     * @param entries   The entries to display
     * @return The button to use to switch to the specified layer
     */
    private static JButton createImageLayerButton(Layer layer, List<IImageEntry<?>> entries) {
        final JButton layerButton = new JButton();
        layerButton.addActionListener(new ImageActionListener(layer, entries));
        layerButton.setText(layer != null ? layer.getLabel() : tr("Default"));
        return layerButton;
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
        imageOpenExternalAction.destroy();
        imageRemoveAction.destroy();
        imageRemoveFromDiskAction.destroy();
        imageZoomAction.destroy();
        cancelLoadingImage();
        super.destroy();
        dialog = null;
    }

    /**
     * This literally exists to silence sonarlint complaints.
     * @param <I> the type of the operand and result of the operator
     */
    @FunctionalInterface
    private interface SerializableUnaryOperator<I> extends UnaryOperator<I>, Serializable {
    }

    private abstract class ImageAction extends JosmAction {
        final SerializableUnaryOperator<IImageEntry<?>> supplier;
        ImageAction(String name, ImageProvider icon, String tooltip, Shortcut shortcut,
                boolean registerInToolbar, String toolbarId, boolean installAdaptors,
                final SerializableUnaryOperator<IImageEntry<?>> supplier) {
            super(name, icon, tooltip, shortcut, registerInToolbar, toolbarId, installAdaptors);
            Objects.requireNonNull(supplier);
            this.supplier = supplier;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            final IImageEntry<?> entry = ImageViewerDialog.this.currentEntry;
            if (entry != null) {
                IImageEntry<?> nextEntry = this.getSupplier().apply(entry);
                entry.selectImage(ImageViewerDialog.this, nextEntry);
            }
            this.resetRememberActions();
        }

        void resetRememberActions() {
            for (ImageRememberAction action : Arrays.asList(ImageViewerDialog.this.imageLastAction, ImageViewerDialog.this.imageFirstAction)) {
                action.last = null;
                action.updateEnabledState();
            }
        }

        SerializableUnaryOperator<IImageEntry<?>> getSupplier() {
            return this.supplier;
        }

        @Override
        protected void updateEnabledState() {
            final IImageEntry<?> entry = ImageViewerDialog.this.currentEntry;
            this.setEnabled(entry != null && this.getSupplier().apply(entry) != null);
        }
    }

    private class ImageNextAction extends ImageAction {
        ImageNextAction() {
            super(null, new ImageProvider(DIALOG_FOLDER, "next"), tr("Next"), Shortcut.registerShortcut(
                    "geoimage:next", tr(GEOIMAGE_FILLER, tr("Show next Image")), KeyEvent.VK_PAGE_DOWN, Shortcut.DIRECT),
                  false, null, false, IImageEntry::getNextImage);
        }
    }

    private class ImagePreviousAction extends ImageAction {
        ImagePreviousAction() {
            super(null, new ImageProvider(DIALOG_FOLDER, "previous"), tr("Previous"), Shortcut.registerShortcut(
                    "geoimage:previous", tr(GEOIMAGE_FILLER, tr("Show previous Image")), KeyEvent.VK_PAGE_UP, Shortcut.DIRECT),
                  false, null, false, IImageEntry::getPreviousImage);
        }
    }

    /** This class exists to remember the last entry, and go back if clicked again when it would not otherwise be enabled */
    private abstract class ImageRememberAction extends ImageAction {
        private final ImageProvider defaultIcon;
        transient IImageEntry<?> last;
        ImageRememberAction(String name, ImageProvider icon, String tooltip, Shortcut shortcut,
                boolean registerInToolbar, String toolbarId, boolean installAdaptors, SerializableUnaryOperator<IImageEntry<?>> supplier) {
            super(name, icon, tooltip, shortcut, registerInToolbar, toolbarId, installAdaptors, supplier);
            this.defaultIcon = icon;
        }

        /**
         * Update the icon for this action
         */
        public void updateIcon() {
            if (this.last != null) {
                new ImageProvider(DIALOG_FOLDER, "history").getResource().attachImageIcon(this, true);
            } else {
                this.defaultIcon.getResource().attachImageIcon(this, true);
            }
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            final IImageEntry<?> current = ImageViewerDialog.this.currentEntry;
            final IImageEntry<?> expected = this.supplier.apply(current);
            if (current != null) {
                IImageEntry<?> nextEntry = this.getSupplier().apply(current);
                current.selectImage(ImageViewerDialog.this, nextEntry);
            }
            this.resetRememberActions();
            if (!Objects.equals(current, expected)) {
                this.last = current;
            } else {
                this.last = null;
            }
            this.updateEnabledState();
        }

        @Override
        protected void updateEnabledState() {
            final IImageEntry<?> current = ImageViewerDialog.this.currentEntry;
            final IImageEntry<?> nextEntry = current != null ? this.getSupplier().apply(current) : null;
            if (this.last == null && nextEntry != null && nextEntry.equals(current)) {
                this.setEnabled(false);
            } else {
                super.updateEnabledState();
            }
            this.updateIcon();
        }

        @Override
        SerializableUnaryOperator<IImageEntry<?>> getSupplier() {
            if (this.last != null) {
                return entry -> this.last;
            }
            return super.getSupplier();
        }
    }

    /**
     * A listener that is called to change the viewing layer
     */
    private static class ImageActionListener implements ActionListener {

        private final Layer layer;
        private final List<IImageEntry<?>> entries;

        ImageActionListener(Layer layer, List<IImageEntry<?>> entries) {
            this.layer = layer;
            this.entries = entries;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ImageViewerDialog.getInstance().displayImages(this.layer, this.entries);
        }
    }

    private class ImageFirstAction extends ImageRememberAction {
        ImageFirstAction() {
            super(null, new ImageProvider(DIALOG_FOLDER, "first"), tr("First"), Shortcut.registerShortcut(
                    "geoimage:first", tr(GEOIMAGE_FILLER, tr("Show first Image")), KeyEvent.VK_HOME, Shortcut.DIRECT),
                  false, null, false, IImageEntry::getFirstImage);
        }
    }

    private class ImageLastAction extends ImageRememberAction {
        ImageLastAction() {
            super(null, new ImageProvider(DIALOG_FOLDER, "last"), tr("Last"), Shortcut.registerShortcut(
                    "geoimage:last", tr(GEOIMAGE_FILLER, tr("Show last Image")), KeyEvent.VK_END, Shortcut.DIRECT),
                  false, null, false, IImageEntry::getLastImage);
        }
    }

    private class ImageCenterViewAction extends JosmAction {
        ImageCenterViewAction() {
            super(null, new ImageProvider("dialogs/autoscale", "selection"), tr("Center view"), null,
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
            super(null, new ImageProvider(DIALOG_FOLDER, "zoom-best-fit"), tr("Zoom best fit and 1:1"), null,
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            imgDisplay.zoomBestFitOrOne();
        }
    }

    private class ImageRemoveAction extends JosmAction {
        ImageRemoveAction() {
            super(null, new ImageProvider(DIALOG_FOLDER, "delete"), tr("Remove photo from layer"), Shortcut.registerShortcut(
                    "geoimage:deleteimagefromlayer", tr(GEOIMAGE_FILLER, tr("Remove photo from layer")), KeyEvent.VK_DELETE, Shortcut.SHIFT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (ImageViewerDialog.this.currentEntry != null) {
                IImageEntry<?> imageEntry = ImageViewerDialog.this.currentEntry;
                if (imageEntry.isRemoveSupported()) {
                    imageEntry.remove();
                }
            }
        }
    }

    private class ImageRemoveFromDiskAction extends JosmAction {
        ImageRemoveFromDiskAction() {
            super(null, new ImageProvider(DIALOG_FOLDER, "geoimage/deletefromdisk"), tr("Delete image file from disk"),
                    Shortcut.registerShortcut("geoimage:deletefilefromdisk",
                            tr(GEOIMAGE_FILLER, tr("Delete image file from disk")), KeyEvent.VK_DELETE, Shortcut.CTRL_SHIFT),
                    false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentEntry != null) {
                List<IImageEntry<?>> toDelete = currentEntry instanceof ImageEntry ?
                        new ArrayList<>(((ImageEntry) currentEntry).getDataSet().getSelectedImages())
                        : Collections.singletonList(currentEntry);
                int size = toDelete.size();

                int result = new ExtendedDialog(
                        MainApplication.getMainFrame(),
                        tr("Delete image file from disk"),
                        tr("Cancel"), tr("Delete"))
                        .setButtonIcons("cancel", "dialogs/geoimage/deletefromdisk")
                        .setContent(new JLabel("<html><h3>"
                                + trn("Delete the file from disk?",
                                      "Delete the {0} files from disk?", size, size)
                                + "<p>" + trn("The image file will be permanently lost!",
                                              "The images files will be permanently lost!", size) + "</h3></html>",
                                ImageProvider.get("dialogs/geoimage/deletefromdisk"), SwingConstants.LEADING))
                        .toggleEnable("geoimage.deleteimagefromdisk")
                        .setCancelButton(1)
                        .setDefaultButton(2)
                        .showDialog()
                        .getValue();

                if (result == 2) {
                    final List<ImageData> imageDataCollection = toDelete.stream().filter(ImageEntry.class::isInstance)
                            .map(ImageEntry.class::cast).map(ImageEntry::getDataSet).distinct().collect(Collectors.toList());
                    for (IImageEntry<?> delete : toDelete) {
                        // We have to be able to remove the image from the layer and the image from its storage location
                        // If either are false, then don't remove the image.
                        if (delete.isRemoveSupported() && delete.isDeleteSupported() && delete.remove() && delete.delete()) {
                            Logging.info("File {0} deleted.", delete.getFile());
                        } else {
                            JOptionPane.showMessageDialog(
                                    MainApplication.getMainFrame(),
                                    tr("Image file could not be deleted."),
                                    tr("Error"),
                                    JOptionPane.ERROR_MESSAGE
                                    );
                        }
                    }
                    imageDataCollection.forEach(data -> {
                        data.notifyImageUpdate();
                        data.updateSelectedImage();
                    });
                }
            }
        }
    }

    private class ImageCopyPathAction extends JosmAction {
        ImageCopyPathAction() {
            super(null, new ImageProvider("copy"), tr("Copy image path"), Shortcut.registerShortcut(
                    "geoimage:copypath", tr(GEOIMAGE_FILLER, tr("Copy image path")), KeyEvent.VK_C, Shortcut.ALT_CTRL_SHIFT),
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentEntry != null) {
                ClipboardUtils.copyString(String.valueOf(currentEntry.getImageURI()));
            }
        }
    }

    private class ImageCollapseAction extends JosmAction {
        ImageCollapseAction() {
            super(null, new ImageProvider(DIALOG_FOLDER, "collapse"), tr("Move dialog to the side pane"), null,
                  false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            collapseButtonClicked = true;
            detachedDialog.getToolkit().getSystemEventQueue().postEvent(new WindowEvent(detachedDialog, WindowEvent.WINDOW_CLOSING));
        }
    }

    private class ImageOpenExternalAction extends JosmAction {
        ImageOpenExternalAction() {
            super(null, new ImageProvider("external-link"), tr("Open image in external viewer"), null, false, null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentEntry != null) {
                try {
                    PlatformManager.getPlatform().openUrl(currentEntry.getImageURI().toURL().toExternalForm());
                } catch (IOException ex) {
                    Logging.error(ex);
                }
            }
        }
    }

    /**
     * Enables (or disables) the "Previous" button.
     * @param value {@code true} to enable the button, {@code false} otherwise
     */
    public void setPreviousEnabled(boolean value) {
        this.imageFirstAction.updateEnabledState();
        this.btnFirst.setEnabled(value || this.imageFirstAction.isEnabled());
        btnPrevious.setEnabled(value);
    }

    /**
     * Enables (or disables) the "Next" button.
     * @param value {@code true} to enable the button, {@code false} otherwise
     */
    public void setNextEnabled(boolean value) {
        btnNext.setEnabled(value);
        this.imageLastAction.updateEnabledState();
        this.btnLast.setEnabled(value || this.imageLastAction.isEnabled());
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

    /** Used for tabbed panes */
    private final transient Map<Layer, List<IImageEntry<?>>> tabbedEntries = new HashMap<>();
    private transient IImageEntry<? extends IImageEntry<?>> currentEntry;

    /**
     * Displays a single image for the given layer.
     * @param ignoredData the image data
     * @param entry image entry
     * @see #displayImages
     */
    public void displayImage(ImageData ignoredData, ImageEntry entry) {
        displayImages(Collections.singletonList(entry));
    }

    /**
     * Displays a single image for the given layer.
     * @param entry image entry
     * @see #displayImages
     */
    public void displayImage(IImageEntry<?> entry) {
        this.displayImages(Collections.singletonList(entry));
    }

    /**
     * Displays images for the given layer.
     * @param entries image entries
     * @since 18246
     */
    public void displayImages(List<IImageEntry<?>> entries) {
        this.displayImages((Layer) null, entries);
    }

    /**
     * Displays images for the given layer.
     * @param layer The layer to use for the tab ui
     * @param entries image entries
     * @since 18591
     */
    public void displayImages(Layer layer, List<IImageEntry<?>> entries) {
        boolean imageChanged;
        IImageEntry<?> entry = entries != null && entries.size() == 1 ? entries.get(0) : null;

        synchronized (this) {
            // TODO: pop up image dialog but don't load image again

            imageChanged = currentEntry != entry;

            if (centerView && entry != null && MainApplication.isDisplayingMapView() && entry.getPos() != null) {
                MainApplication.getMap().mapView.zoomTo(entry.getPos());
            }

            currentEntry = entry;

            for (ImageAction action : Arrays.asList(this.imageFirstAction, this.imagePreviousAction,
                    this.imageNextAction, this.imageLastAction)) {
                action.updateEnabledState();
            }
        }

        if (entries == null || entries.isEmpty() || entries.stream().allMatch(Objects::isNull)) {
            this.tabbedEntries.remove(layer);
        } else {
            this.tabbedEntries.put(layer, entries);
        }
        this.updateLayers();
        if (entry != null) {
            this.updateButtonsNonNullEntry(entry, imageChanged);
        } else if (this.tabbedEntries.isEmpty()) {
            this.updateButtonsNullEntry(entries);
            return;
        } else {
            Map.Entry<Layer, List<IImageEntry<?>>> realEntry =
                    this.tabbedEntries.entrySet().stream().filter(mapEntry -> mapEntry.getValue().size() == 1).findFirst().orElse(null);
            if (realEntry == null) {
                this.updateButtonsNullEntry(entries);
            } else {
                this.displayImages(realEntry.getKey(), realEntry.getValue());
            }
            return;
        }
        if (!isDialogShowing()) {
            setIsDocked(false); // always open a detached window when an image is clicked and dialog is closed
            showDialog();
        } else if (isDocked && isCollapsed) {
            expand();
            dialogsPanel.reconstruct(DialogsPanel.Action.COLLAPSED_TO_DEFAULT, this);
        }
    }

    /**
     * Update buttons for null entry
     * @param entries {@code true} if multiple images are selected
     */
    private void updateButtonsNullEntry(List<IImageEntry<?>> entries) {
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
        btnOpenExternal.setEnabled(false);
        if (hasMultipleImages) {
            imgDisplay.setEmptyText(tr("Multiple images selected"));
            btnFirst.setEnabled(!isFirstImageSelected(entries));
            btnLast.setEnabled(!isLastImageSelected(entries));
        }
        imgDisplay.setImage(null);
        imgDisplay.setOsdText("");
    }

    /**
     * Update the image viewer buttons for the new entry
     * @param entry The new entry
     * @param imageChanged {@code true} if it is not the same image as the previous image.
     */
    private void updateButtonsNonNullEntry(IImageEntry<?> entry, boolean imageChanged) {
        if (imageChanged) {
            cancelLoadingImage();
            // Set only if the image is new to preserve zoom and position if the same image is redisplayed
            // (e.g. to update the OSD).
            imgLoadingFuture = imgDisplay.setImage(entry);
        }

        // Update buttons after setting the new entry
        setNextEnabled(entry.getNextImage() != null);
        setPreviousEnabled(entry.getPreviousImage() != null);
        btnDelete.setEnabled(entry.isRemoveSupported());
        btnDeleteFromDisk.setEnabled(entry.isDeleteSupported() && entry.isRemoveSupported());
        btnCopyPath.setEnabled(true);
        btnOpenExternal.setEnabled(true);

        setTitle(tr("Geotagged Images") + (!entry.getDisplayName().isEmpty() ? " - " + entry.getDisplayName() : ""));
        StringBuilder osd = new StringBuilder(entry.getDisplayName());
        if (entry.getElevation() != null) {
            osd.append(tr("\nAltitude: {0} m", Math.round(entry.getElevation())));
        }
        if (entry.getSpeed() != null) {
            osd.append(tr("\nSpeed: {0} km/h", Math.round(entry.getSpeed())));
        }
        if (entry.getExifImgDir() != null) {
            osd.append(tr("\nDirection {0}\u00b0", Math.round(entry.getExifImgDir())));
        }

        DateTimeFormatter dtf = DateUtils.getDateTimeFormatter(FormatStyle.SHORT, FormatStyle.MEDIUM)
                // Set timezone to UTC since UTC is assumed when parsing the EXIF timestamp,
                // see see org.openstreetmap.josm.tools.ExifReader.readTime(com.drew.metadata.Metadata)
                .withZone(ZoneOffset.UTC);

        if (entry.hasExifTime()) {
            osd.append(tr("\nEXIF time: {0}", dtf.format(entry.getExifInstant())));
        }
        if (entry.hasGpsTime()) {
            osd.append(tr("\nGPS time: {0}", dtf.format(entry.getGpsInstant())));
        }
        Optional.ofNullable(entry.getIptcCaption()).map(s -> tr("\nCaption: {0}", s)).ifPresent(osd::append);
        Optional.ofNullable(entry.getIptcHeadline()).map(s -> tr("\nHeadline: {0}", s)).ifPresent(osd::append);
        Optional.ofNullable(entry.getIptcKeywords()).map(s -> tr("\nKeywords: {0}", s)).ifPresent(osd::append);
        Optional.ofNullable(entry.getIptcObjectName()).map(s -> tr("\nObject name: {0}", s)).ifPresent(osd::append);

        imgDisplay.setOsdText(osd.toString());
    }

    /**
     * Displays images for the given layer.
     * @param ignoredData the image data (unused, may be {@code null})
     * @param entries image entries
     * @since 18246 (signature)
     * @deprecated Use {@link #displayImages(List)} (The data param is no longer used)
     */
    @Deprecated
    public void displayImages(ImageData ignoredData, List<IImageEntry<?>> entries) {
        this.displayImages(entries);
    }

    private static boolean isLastImageSelected(List<IImageEntry<?>> data) {
        return data.stream().anyMatch(image -> data.contains(image.getLastImage()));
    }

    private static boolean isFirstImageSelected(List<IImageEntry<?>> data) {
        return data.stream().anyMatch(image -> data.contains(image.getFirstImage()));
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
        this.updateLayers();
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
     * @since 18246 (signature)
     */
    public static IImageEntry<?> getCurrentImage() {
        return getInstance().currentEntry;
    }

    /**
     * Returns the rotation of the currently displayed image.
     * @param entry The entry to get the rotation for. May be {@code null}.
     * @return the rotation of the currently displayed image, or {@code null}
     * @since 18263
     */
    public Vector3D getRotation(IImageEntry<?> entry) {
        return imgDisplay.getRotation(entry);
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
        if (e.getRemovedLayer() instanceof GeoImageLayer && this.currentEntry instanceof ImageEntry) {
            ImageData removedData = ((GeoImageLayer) e.getRemovedLayer()).getImageData();
            if (removedData == ((ImageEntry) this.currentEntry).getDataSet()) {
                displayImages(null);
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
        if (!MainApplication.worker.isShutdown()) {
            showLayer(e.getSource().getActiveLayer());
        }
    }

    /**
     * Reload the image. Call this if you load a low-resolution image first, and then get a high-resolution image, or
     * if you know that the image has changed on disk.
     * @since 18591
     */
    public void refresh() {
        if (SwingUtilities.isEventDispatchThread()) {
            this.updateButtonsNonNullEntry(currentEntry, true);
        } else {
            GuiHelper.runInEDT(this::refresh);
        }
    }

    private void registerOnLayer(Layer layer) {
        if (layer instanceof GeoImageLayer) {
            ((GeoImageLayer) layer).getImageData().addImageDataUpdateListener(this);
        }
    }

    private void showLayer(Layer newLayer) {
        if (this.currentEntry == null && newLayer instanceof GeoImageLayer) {
            ImageData imageData = ((GeoImageLayer) newLayer).getImageData();
            imageData.setSelectedImage(imageData.getFirstImage());
        }
    }

    private void cancelLoadingImage() {
        if (imgLoadingFuture != null) {
            imgLoadingFuture.cancel(false);
            imgLoadingFuture = null;
        }
    }

    @Override
    public void selectedImageChanged(ImageData data) {
        if (this.currentEntry != data.getSelectedImage() && this.currentEntry instanceof ImageEntry &&
                !data.getSelectedImages().contains(this.currentEntry)) {
            displayImages(data.getLayer(), new ArrayList<>(data.getSelectedImages()));
        }
    }

    @Override
    public void imageDataUpdated(ImageData data) {
        displayImages(data.getLayer(), new ArrayList<>(data.getSelectedImages()));
    }
}
