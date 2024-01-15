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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.ImageData;
import org.openstreetmap.josm.data.imagery.street_level.IImageEntry;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
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
import org.openstreetmap.josm.gui.widgets.HideableTabbedPane;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Dialog to view and manipulate geo-tagged images from a {@link GeoImageLayer}.
 */
public final class ImageViewerDialog extends ToggleDialog implements LayerChangeListener, ActiveLayerChangeListener {
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
        MapFrame map = MainApplication.getMap();
        synchronized (ImageViewerDialog.class) {
            if (dialog == null)
                createInstance();
            if (map != null && map.getToggleDialog(ImageViewerDialog.class) == null) {
                map.addToggleDialog(dialog);
            }
        }
        return dialog;
    }

    /**
     * Check if there is an instance for the {@link ImageViewerDialog}
     * @return {@code true} if there is a static singleton instance of {@link ImageViewerDialog}
     * @since 18613
     */
    public static boolean hasInstance() {
        return dialog != null;
    }

    /**
     * Destroy the current dialog
     */
    private static void destroyInstance() {
        MapFrame map = MainApplication.getMap();
        synchronized (ImageViewerDialog.class) {
            if (dialog != null) {
                if (map != null && map.getToggleDialog(ImageViewerDialog.class) != null) {
                    map.removeToggleDialog(dialog);
                }
                if (!dialog.isDestroyed()) {
                    dialog.destroy();
                }
            }
        }
        dialog = null;
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
    private final HideableTabbedPane layers = new HideableTabbedPane();

    private ImageViewerDialog() {
        super(tr("Geotagged Images"), "geoimage", tr("Display geotagged images"), Shortcut.registerShortcut("tools:geotagged",
        tr("Windows: {0}", tr("Geotagged Images")), KeyEvent.VK_Y, Shortcut.DIRECT), 200);
        build();
        MainApplication.getLayerManager().addActiveLayerChangeListener(this);
        MainApplication.getLayerManager().addLayerChangeListener(this);
        for (Layer l: MainApplication.getLayerManager().getLayers()) {
            registerOnLayer(l);
        }
        // This listener gets called _prior to_ the reorder event. If we do not delay the execution of the
        // model update, then the image will change instead of remaining the same.
        this.layers.getModel().addChangeListener(l -> {
            // We need to check to see whether or not the worker is shut down. See #22922 for details.
            if (!MainApplication.worker.isShutdown() && this.isDialogShowing()) {
                MainApplication.worker.execute(() -> GuiHelper.runInEDT(this::showNotify));
            }
        });
    }

    @Override
    public void showNotify() {
        super.showNotify();
        Component selected = this.layers.getSelectedComponent();
        if (selected instanceof MoveImgDisplayPanel) {
            ((MoveImgDisplayPanel<?>) selected).fireModelUpdate();
        }
    }

    @Override
    public void hideNotify() {
        super.hideNotify();
        this.currentEntry = null;
        this.imgDisplay.setImage(null);
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
        content.add(this.layers, BorderLayout.CENTER);

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
        tbCentre.setSelected(Config.getPref().getBoolean("geoimage.viewer.centre.on.image", false));
        tbCentre.setPreferredSize(buttonDim);

        JButton btnZoomBestFit = new JButton(imageZoomAction);
        btnZoomBestFit.setPreferredSize(buttonDim);

        btnCollapse = createButton(imageCollapseAction, new Dimension(20, 20));
        btnCollapse.setAlignmentY(Component.TOP_ALIGNMENT);

        JPanel buttons = new JPanel();
        addButtonGroup(buttons, this.btnFirst, this.btnPrevious, this.btnNext, this.btnLast);
        addButtonGroup(buttons, this.tbCentre, btnZoomBestFit);
        addButtonGroup(buttons, this.btnDelete, this.btnDeleteFromDisk);
        addButtonGroup(buttons, this.btnCopyPath, this.btnOpenExternal);
        addButtonGroup(buttons, createButton(visibilityAction, buttonDim));

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

    /**
     * Add a button group to a panel
     * @param buttonPanel The panel holding the buttons
     * @param buttons The button group to add
     */
    private static void addButtonGroup(JPanel buttonPanel, AbstractButton... buttons) {
        if (buttonPanel.getComponentCount() != 0) {
            buttonPanel.add(Box.createRigidArea(new Dimension(7, 0)));
        }

        for (AbstractButton jButton : buttons) {
            buttonPanel.add(jButton);
        }
    }

    /**
     * Update the tabs for the different image layers
     * @param changed {@code true} if the tabs changed
     */
    private void updateLayers(boolean changed) {
        MainLayerManager layerManager = MainApplication.getLayerManager();
        List<IGeoImageLayer> geoImageLayers = layerManager.getLayers().stream()
                .filter(IGeoImageLayer.class::isInstance).map(IGeoImageLayer.class::cast).collect(Collectors.toList());
        if (geoImageLayers.isEmpty()) {
            this.layers.setVisible(false);
            hideNotify();
            if (hasInstance())
                destroyInstance();
        } else {
            this.layers.setVisible(true);
            if (changed) {
                addButtonsForImageLayers();
            }
            MoveImgDisplayPanel<?> selected = (MoveImgDisplayPanel<?>) this.layers.getSelectedComponent();
            if ((this.imgDisplay.getParent() == null || this.imgDisplay.getParent().getParent() == null)
                && selected != null && selected.layer.containsImage(this.currentEntry)) {
                selected.setVisible(selected.isVisible());
            } else if (selected != null && !selected.layer.containsImage(this.currentEntry)) {
                this.getImageTabs().filter(m -> m.layer.containsImage(this.currentEntry)).mapToInt(this.layers::indexOfComponent).findFirst()
                        .ifPresent(this.layers::setSelectedIndex);
            } else if (selected == null) {
                updateTitle();
            }
            this.layers.invalidate();
        }
        this.layers.getParent().invalidate();
        this.revalidate();
    }

    /**
     * Add the buttons for image layers
     */
    private void addButtonsForImageLayers() {
        List<MoveImgDisplayPanel<?>> alreadyAdded = this.getImageTabs().collect(Collectors.toList());
        List<Layer> availableLayers = MainApplication.getLayerManager().getLayers();
        List<IGeoImageLayer> geoImageLayers = availableLayers.stream()
                .sorted(Comparator.comparingInt(entry -> /*reverse*/-availableLayers.indexOf(entry)))
                .filter(IGeoImageLayer.class::isInstance).map(IGeoImageLayer.class::cast).collect(Collectors.toList());
        List<IGeoImageLayer> tabLayers = geoImageLayers.stream()
                .filter(l -> alreadyAdded.stream().anyMatch(m -> Objects.equals(l, m.layer)) || l.containsImage(this.currentEntry))
                .collect(Collectors.toList());
        for (IGeoImageLayer layer : tabLayers) {
            final MoveImgDisplayPanel<?> panel = alreadyAdded.stream()
                    .filter(m -> Objects.equals(m.layer, layer)).findFirst()
                    .orElseGet(() -> new MoveImgDisplayPanel<>(this.imgDisplay, (Layer & IGeoImageLayer) layer));
            int componentIndex = this.layers.indexOfComponent(panel);
            if (componentIndex == geoImageLayers.indexOf(layer)) {
                this.layers.setTitleAt(componentIndex, panel.getLabel(availableLayers));
            } else {
                this.removeImageTab((Layer) layer);
                this.layers.insertTab(panel.getLabel(availableLayers), null, panel, null, tabLayers.indexOf(layer));
                int idx = this.layers.indexOfComponent(panel);
                CloseableTab closeableTab = new CloseableTab(this.layers, l -> {
                    Component source = (Component) l.getSource();
                    do {
                        int index = layers.indexOfTabComponent(source);
                        if (index >= 0) {
                            removeImageTab(((MoveImgDisplayPanel<?>) layers.getComponentAt(index)).layer);
                            getImageTabs().forEach(m -> m.setVisible(m.isVisible()));
                            showNotify();
                            return;
                        }
                        source = source.getParent();
                    } while (source != null);
                });
                this.layers.setTabComponentAt(idx, closeableTab);
            }
            if (layer.containsImage(this.currentEntry)) {
                this.layers.setSelectedComponent(panel);
            }
        }
        this.getImageTabs().map(p -> p.layer).filter(layer -> !availableLayers.contains(layer))
                // We have to collect to a list prior to removal -- if we don't, then the stream may get a layer at index 0,
                // remove that layer, and then get a layer at index 1, which was previously at index 2.
                .collect(Collectors.toList()).forEach(this::removeImageTab);

        // After that, trigger the visibility set code
        this.getImageTabs().forEach(m -> m.setVisible(m.isVisible()));
    }

    /**
     * Remove a tab for a layer from the {@link #layers} tab pane
     * @param layer The layer to remove
     */
    private void removeImageTab(Layer layer) {
        // This must be reversed to avoid removing the wrong tab
        for (int i = this.layers.getTabCount() - 1; i >= 0; i--) {
            Component component = this.layers.getComponentAt(i);
            if (component instanceof MoveImgDisplayPanel) {
                MoveImgDisplayPanel<?> moveImgDisplayPanel = (MoveImgDisplayPanel<?>) component;
                if (Objects.equals(layer, moveImgDisplayPanel.layer)) {
                    this.layers.removeTabAt(i);
                    this.layers.remove(moveImgDisplayPanel);
                }
            }
        }
    }

    /**
     * Get the {@link MoveImgDisplayPanel} objects in {@link #layers}.
     * @return The individual panels
     */
    private Stream<MoveImgDisplayPanel<?>> getImageTabs() {
        return IntStream.range(0, this.layers.getTabCount())
                .mapToObj(this.layers::getComponentAt)
                .filter(MoveImgDisplayPanel.class::isInstance)
                .map(m -> (MoveImgDisplayPanel<?>) m);
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
        toggleAction.destroy();
        cancelLoadingImage();
        super.destroy();
        // Ensure that this dialog is removed from memory
        destroyInstance();
    }

    private boolean isDestroyed() {
        return dialogsPanel == null;
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
            Config.getPref().putBoolean("geoimage.viewer.centre.on.image", centerView);
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
                selectNextImageAfterDeletion(imageEntry);
            }
        }

        /**
         * Select the logical next entry after deleting the currently viewed image
         * @param oldEntry The image entry that was just deleted
         */
        private void selectNextImageAfterDeletion(IImageEntry<?> oldEntry) {
            final IImageEntry<?> currentImageEntry = ImageViewerDialog.this.currentEntry;
            // This is mostly just in case something changes the displayed entry (aka avoid race condition) or an image provider
            // sets the next image itself.
            if (Objects.equals(currentImageEntry, oldEntry)) {
                final IImageEntry<?> nextImage;
                if (oldEntry instanceof ImageEntry) {
                    nextImage = ((ImageEntry) oldEntry).getDataSet().getSelectedImage();
                } else if (oldEntry.getNextImage() != null) {
                    nextImage = oldEntry.getNextImage();
                } else if (oldEntry.getPreviousImage() != null) {
                    nextImage = oldEntry.getPreviousImage();
                } else {
                    nextImage = null;
                }
                ImageViewerDialog.getInstance().displayImages(nextImage == null ? null : Collections.singletonList(nextImage));
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
                IImageEntry<?> oldEntry = ImageViewerDialog.this.currentEntry;
                List<IImageEntry<?>> toDelete = oldEntry instanceof ImageEntry ?
                        new ArrayList<>(((ImageEntry) oldEntry).getDataSet().getSelectedImages())
                        : Collections.singletonList(oldEntry);
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
                    ImageViewerDialog.this.imageRemoveAction.selectNextImageAfterDeletion(oldEntry);
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
            if (currentEntry != null && currentEntry.getImageURI() != null) {
                try {
                    PlatformManager.getPlatform().openUrl(currentEntry.getImageURI().toURL().toExternalForm());
                } catch (IOException ex) {
                    Logging.error(ex);
                }
            }
        }
    }

    /**
     * A tab title renderer for {@link HideableTabbedPane} that allows us to close tabs.
     */
    private static class CloseableTab extends JPanel implements PropertyChangeListener {
        private final JLabel title;
        private final JButton close;

        /**
         * Create a new {@link CloseableTab}.
         * @param parent The parent to add property change listeners to. It should be a {@link HideableTabbedPane} in most cases.
         * @param closeAction The action to run to close the tab. You probably want to call {@link JTabbedPane#removeTabAt(int)}
         *                    at the very least.
         */
        CloseableTab(Component parent, ActionListener closeAction) {
            this.title = new JLabel();
            this.add(this.title);
            close = new JButton(ImageProvider.get("misc", "close"));
            close.setBorder(BorderFactory.createEmptyBorder());
            this.add(close);
            close.addActionListener(closeAction);
            close.addActionListener(l -> parent.removePropertyChangeListener("indexForTitle", this));
            parent.addPropertyChangeListener("indexForTitle", this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() instanceof JTabbedPane) {
                JTabbedPane source = (JTabbedPane) evt.getSource();
                if (this.getParent() == null) {
                    source.removePropertyChangeListener(evt.getPropertyName(), this);
                }
                if ("indexForTitle".equals(evt.getPropertyName())) {
                    int idx = source.indexOfTabComponent(this);
                    if (idx >= 0) {
                        this.title.setText(source.getTitleAt(idx));
                    }
                }
                // Used to hack around UI staying visible. This assumes that the parent component is a HideableTabbedPane.
                this.title.setVisible(source.getTabCount() != 1);
                this.close.setVisible(source.getTabCount() != 1);
            }
        }
    }

    /**
     * A JPanel whose entire purpose is to display an image by (a) moving the imgDisplay around and (b) setting the imgDisplay as a child
     * for this panel.
     */
    private static class MoveImgDisplayPanel<T extends Layer & IGeoImageLayer> extends JPanel {
        private final T layer;
        private final ImageDisplay imgDisplay;

        MoveImgDisplayPanel(ImageDisplay imgDisplay, T layer) {
            super(new BorderLayout());
            this.layer = layer;
            this.imgDisplay = imgDisplay;
        }

        /**
         * Call when the selection model updates
         */
        void fireModelUpdate() {
            JTabbedPane layers = ImageViewerDialog.getInstance().layers;
            int index = layers.indexOfComponent(this);
            if (this == layers.getSelectedComponent()) {
                if (!this.layer.getSelection().isEmpty() && !this.layer.getSelection().contains(ImageViewerDialog.getCurrentImage())) {
                    ImageViewerDialog.getInstance().displayImages(this.layer.getSelection());
                    this.layer.invalidate(); // This will force the geoimage layers to update properly.
                }
                if (this.imgDisplay.getParent() != this) {
                    this.add(this.imgDisplay, BorderLayout.CENTER);
                    this.imgDisplay.invalidate();
                    this.revalidate();
                }
                if (index >= 0) {
                    layers.setTitleAt(index, "* " + getLabel(MainApplication.getLayerManager().getLayers()));
                }
            } else if (index >= 0) {
                layers.setTitleAt(index, getLabel(MainApplication.getLayerManager().getLayers()));
            }
        }

        /**
         * Get the label for this panel
         * @param availableLayers The layers to use to get the index
         * @return The label for this layer
         */
        String getLabel(List<Layer> availableLayers) {
            final int index = availableLayers.size() - availableLayers.indexOf(layer);
            return (ExpertToggleAction.isExpert() ? "[" + index + "] " : "") + layer.getLabel();
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
    public void displayImages(List<? extends IImageEntry<?>> entries) {
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


        final boolean updateRequired;
        final List<IGeoImageLayer> imageLayers = MainApplication.getLayerManager().getLayers().stream()
                    .filter(IGeoImageLayer.class::isInstance).map(IGeoImageLayer.class::cast).collect(Collectors.toList());
        if (!Config.getPref().getBoolean("geoimage.viewer.show.tabs", true)) {
            updateRequired = true;
            // Clear the selected images in other geoimage layers
            this.getImageTabs().map(m -> m.layer).filter(IGeoImageLayer.class::isInstance).map(IGeoImageLayer.class::cast)
                    .filter(l -> !Objects.equals(entries, l.getSelection()))
                    .forEach(IGeoImageLayer::clearSelection);
        } else {
            updateRequired = imageLayers.stream().anyMatch(l -> this.getImageTabs().map(m -> m.layer).noneMatch(l::equals));
        }
        this.updateLayers(updateRequired);
        if (entry != null) {
            this.updateButtonsNonNullEntry(entry, imageChanged);
        } else if (imageLayers.isEmpty()) {
            this.updateButtonsNullEntry(entries);
            return;
        } else {
            IGeoImageLayer layer = this.getImageTabs().map(m -> m.layer).filter(l -> l.getSelection().size() == 1).findFirst().orElse(null);
            if (layer == null) {
                this.updateButtonsNullEntry(entries);
            } else {
                this.displayImages(layer.getSelection());
            }
            return;
        }
        if (!isDialogShowing()) {
            setIsDocked(false); // always open a detached window when an image is clicked and dialog is closed
            unfurlDialog();
        } else if (isDocked && isCollapsed) {
            expand();
            dialogsPanel.reconstruct(DialogsPanel.Action.COLLAPSED_TO_DEFAULT, this);
        }
    }

    /**
     * Update buttons for null entry
     * @param entries {@code true} if multiple images are selected
     */
    private void updateButtonsNullEntry(List<? extends IImageEntry<?>> entries) {
        boolean hasMultipleImages = entries != null && entries.size() > 1;
        // if this method is called to reinitialize dialog content with a blank image,
        // do not actually show the dialog again with a blank image if currently hidden (fix #10672)
        this.updateTitle();
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

        this.updateTitle();
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

    private void updateTitle() {
        final IImageEntry<?> entry;
        synchronized (this) {
            entry = this.currentEntry;
        }
        String baseTitle = Optional.ofNullable(this.layers.getSelectedComponent())
                .filter(MoveImgDisplayPanel.class::isInstance).map(MoveImgDisplayPanel.class::cast)
                .map(m -> m.layer).map(Layer::getLabel).orElse(tr("Geotagged Images"));
        if (entry == null) {
            this.setTitle(baseTitle);
        } else {
            this.setTitle(baseTitle + (!entry.getDisplayName().isEmpty() ? " - " + entry.getDisplayName() : ""));
        }
    }

    private static boolean isLastImageSelected(List<? extends IImageEntry<?>> data) {
        return data.stream().anyMatch(image -> data.contains(image.getLastImage()));
    }

    private static boolean isFirstImageSelected(List<? extends IImageEntry<?>> data) {
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
        if (e.getRemovedLayer() instanceof IGeoImageLayer && ((IGeoImageLayer) e.getRemovedLayer()).containsImage(this.currentEntry)) {
            displayImages(null);
        }
        this.updateLayers(true);
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        this.updateLayers(true);
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
        if (layer instanceof IGeoImageLayer) {
            layer.addPropertyChangeListener(l -> {
                final List<?> currentTabLayers = this.getImageTabs().map(m -> m.layer).collect(Collectors.toList());
                if (Layer.NAME_PROP.equals(l.getPropertyName()) && currentTabLayers.contains(layer)) {
                    this.updateLayers(true);
                        if (((IGeoImageLayer) layer).containsImage(this.currentEntry)) {
                            this.updateTitle();
                        }
                } // Use Layer.VISIBLE_PROP here if we decide to do something when layer visibility changes
            });
        }
    }

    private void showLayer(Layer newLayer) {
        if (this.currentEntry == null && newLayer instanceof GeoImageLayer) {
            ImageData imageData = ((GeoImageLayer) newLayer).getImageData();
            imageData.setSelectedImage(imageData.getFirstImage());
        }
        if (newLayer instanceof IGeoImageLayer) {
            this.updateLayers(true);
        }
    }

    private void cancelLoadingImage() {
        if (imgLoadingFuture != null) {
            imgLoadingFuture.cancel(false);
            imgLoadingFuture = null;
        }
    }
}
