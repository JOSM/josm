// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImagingOpException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MenuScroller;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProcessor;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;

/**
 * Abstract base class for background imagery layers ({@link WMSLayer}, {@link TMSLayer}, {@link WMTSLayer}).
 *
 * Handles some common tasks, like image filters, image processors, etc.
 */
public abstract class ImageryLayer extends Layer {

    /**
     * The default value for the sharpen filter for each imagery layer.
     */
    public static final IntegerProperty PROP_SHARPEN_LEVEL = new IntegerProperty("imagery.sharpen_level", 0);

    private final List<ImageProcessor> imageProcessors = new ArrayList<>();

    protected final ImageryInfo info;

    protected Icon icon;

    private final ImageryFilterSettings filterSettings = new ImageryFilterSettings();

    /**
     * Constructs a new {@code ImageryLayer}.
     * @param info imagery info
     */
    protected ImageryLayer(ImageryInfo info) {
        super(info.getName());
        this.info = info;
        if (info.getIcon() != null) {
            icon = new ImageProvider(info.getIcon()).setOptional(true).
                    setMaxSize(ImageSizes.LAYER).get();
        }
        if (icon == null) {
            icon = ImageProvider.get("imagery_small");
        }
        for (ImageProcessor processor : filterSettings.getProcessors()) {
            addImageProcessor(processor);
        }
        filterSettings.setSharpenLevel(1 + PROP_SHARPEN_LEVEL.get() / 2f);
    }

    public double getPPD() {
        if (!MainApplication.isDisplayingMapView())
            return ProjectionRegistry.getProjection().getDefaultZoomInPPD();
        MapView mapView = MainApplication.getMap().mapView;
        ProjectionBounds bounds = mapView.getProjectionBounds();
        return mapView.getWidth() / (bounds.maxEast - bounds.minEast);
    }

    /**
     * Returns imagery info.
     * @return imagery info
     */
    public ImageryInfo getInfo() {
        return info;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public void mergeFrom(Layer from) {
    }

    @Override
    public Object getInfoComponent() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(new JLabel(getToolTipText()), GBC.eol());
        if (info != null) {
            List<List<String>> content = new ArrayList<>();
            content.add(Arrays.asList(tr("Name"), info.getName()));
            content.add(Arrays.asList(tr("Type"), info.getImageryType().getTypeString().toUpperCase(Locale.ENGLISH)));
            content.add(Arrays.asList(tr("URL"), info.getUrl()));
            content.add(Arrays.asList(tr("Id"), info.getId() == null ? "-" : info.getId()));
            if (info.getMinZoom() != 0) {
                content.add(Arrays.asList(tr("Min. zoom"), Integer.toString(info.getMinZoom())));
            }
            if (info.getMaxZoom() != 0) {
                content.add(Arrays.asList(tr("Max. zoom"), Integer.toString(info.getMaxZoom())));
            }
            if (info.getDescription() != null) {
                content.add(Arrays.asList(tr("Description"), info.getDescription()));
            }
            for (List<String> entry: content) {
                panel.add(new JLabel(entry.get(0) + ':'), GBC.std());
                panel.add(GBC.glue(5, 0), GBC.std());
                panel.add(createTextField(entry.get(1)), GBC.eol().fill(GBC.HORIZONTAL));
            }
        }
        return panel;
    }

    protected JComponent createTextField(String text) {
        if (text != null && text.matches("https?://.*")) {
            return new UrlLabel(text);
        }
        JTextField ret = new JTextField(text);
        ret.setEditable(false);
        ret.setBorder(BorderFactory.createEmptyBorder());
        return ret;
    }

    /**
     * Create a new imagery layer
     * @param info The imagery info to use as base
     * @return The created layer
     */
    public static ImageryLayer create(ImageryInfo info) {
        switch(info.getImageryType()) {
        case WMS:
        case WMS_ENDPOINT:
            return new WMSLayer(info);
        case WMTS:
            return new WMTSLayer(info);
        case TMS:
        case BING:
        case SCANEX:
            return new TMSLayer(info);
        default:
            throw new AssertionError(tr("Unsupported imagery type: {0}", info.getImageryType()));
        }
    }

    private static class ApplyOffsetAction extends AbstractAction {
        private final transient OffsetMenuEntry menuEntry;

        ApplyOffsetAction(OffsetMenuEntry menuEntry) {
            super(menuEntry.getLabel());
            this.menuEntry = menuEntry;
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            menuEntry.actionPerformed();
            //TODO: Use some form of listeners for this.
            MainApplication.getMenu().imageryMenu.refreshOffsetMenu();
        }
    }

    public class OffsetAction extends AbstractAction implements LayerAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Do nothing
        }

        @Override
        public Component createMenuComponent() {
            return getOffsetMenuItem();
        }

        @Override
        public boolean supportLayers(List<Layer> layers) {
            return false;
        }
    }

    /**
     * Create the menu item that should be added to the offset menu.
     * It may have a sub menu of e.g. bookmarks added to it.
     * @return The menu item to add to the imagery menu.
     */
    public JMenuItem getOffsetMenuItem() {
        JMenu subMenu = new JMenu(trc("layer", "Offset"));
        subMenu.setIcon(ImageProvider.get("mapmode", "adjustimg"));
        return (JMenuItem) getOffsetMenuItem(subMenu);
    }

    /**
     * Create the submenu or the menu item to set the offset of the layer.
     *
     * If only one menu item for this layer exists, it is returned by this method.
     *
     * If there are multiple, this method appends them to the subMenu and then returns the reference to the subMenu.
     * @param subMenu The subMenu to use
     * @return A single menu item to adjust the layer or the passed subMenu to which the menu items were appended.
     */
    public JComponent getOffsetMenuItem(JComponent subMenu) {
        JMenuItem adjustMenuItem = new JMenuItem(getAdjustAction());
        List<OffsetMenuEntry> usableBookmarks = getOffsetMenuEntries();
        if (usableBookmarks.isEmpty()) {
            return adjustMenuItem;
        }

        subMenu.add(adjustMenuItem);
        subMenu.add(new JSeparator());
        int menuItemHeight = 0;
        for (OffsetMenuEntry b : usableBookmarks) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(new ApplyOffsetAction(b));
            item.setSelected(b.isActive());
            subMenu.add(item);
            menuItemHeight = item.getPreferredSize().height;
        }
        if (menuItemHeight > 0) {
            if (subMenu instanceof JMenu) {
                MenuScroller.setScrollerFor((JMenu) subMenu);
            } else if (subMenu instanceof JPopupMenu) {
                MenuScroller.setScrollerFor((JPopupMenu) subMenu);
            }
        }
        return subMenu;
    }

    protected abstract Action getAdjustAction();

    protected abstract List<OffsetMenuEntry> getOffsetMenuEntries();

    /**
     * Gets the settings for the filter that is applied to this layer.
     * @return The filter settings.
     * @since 10547
     */
    public ImageryFilterSettings getFilterSettings() {
        return filterSettings;
    }

    /**
     * This method adds the {@link ImageProcessor} to this Layer if it is not {@code null}.
     *
     * @param processor that processes the image
     *
     * @return true if processor was added, false otherwise
     */
    public boolean addImageProcessor(ImageProcessor processor) {
        return processor != null && imageProcessors.add(processor);
    }

    /**
     * This method removes given {@link ImageProcessor} from this layer
     *
     * @param processor which is needed to be removed
     *
     * @return true if processor was removed
     */
    public boolean removeImageProcessor(ImageProcessor processor) {
        return imageProcessors.remove(processor);
    }

    /**
     * Wraps a {@link BufferedImageOp} to be used as {@link ImageProcessor}.
     * @param op the {@link BufferedImageOp}
     * @param inPlace true to apply filter in place, i.e., not create a new {@link BufferedImage} for the result
     *                (the {@code op} needs to support this!)
     * @return the {@link ImageProcessor} wrapper
     */
    public static ImageProcessor createImageProcessor(final BufferedImageOp op, final boolean inPlace) {
        return image -> op.filter(image, inPlace ? image : null);
    }

    /**
     * This method gets all {@link ImageProcessor}s of the layer
     *
     * @return list of image processors without removed one
     */
    public List<ImageProcessor> getImageProcessors() {
        return imageProcessors;
    }

    /**
     * Applies all the chosen {@link ImageProcessor}s to the image
     *
     * @param img - image which should be changed
     *
     * @return the new changed image
     */
    public BufferedImage applyImageProcessors(BufferedImage img) {
        for (ImageProcessor processor : imageProcessors) {
            try {
                img = processor.process(img);
            } catch (ImagingOpException e) {
                Logging.error(e);
            }
        }
        return img;
    }

    /**
     * An additional menu entry in the imagery offset menu.
     * @author Michael Zangl
     * @see ImageryLayer#getOffsetMenuEntries()
     * @since 13243
     */
    public interface OffsetMenuEntry {
        /**
         * Get the label to use for this menu item
         * @return The label to display in the menu.
         */
        String getLabel();

        /**
         * Test whether this bookmark is currently active
         * @return <code>true</code> if it is active
         */
        boolean isActive();

        /**
         * Load this bookmark
         */
        void actionPerformed();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [info=" + info + ']';
    }

    @Override
    public String getChangesetSourceTag() {
        return getInfo().getSourceName();
    }
}
