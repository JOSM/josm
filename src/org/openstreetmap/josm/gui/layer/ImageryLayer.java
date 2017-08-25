// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
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

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MenuScroller;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.gui.layer.imagery.TileSourceDisplaySettings;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Utils;

/**
 * Abstract base class for background imagery layers ({@link WMSLayer}, {@link TMSLayer}, {@link WMTSLayer}).
 *
 * Handles some common tasks, like image filters, image processors, etc.
 */
public abstract class ImageryLayer extends Layer {

    public static final IntegerProperty PROP_SHARPEN_LEVEL = new IntegerProperty("imagery.sharpen_level", 0);

    private final List<ImageProcessor> imageProcessors = new ArrayList<>();

    protected final ImageryInfo info;

    protected Icon icon;

    private final ImageryFilterSettings filterSettings = new ImageryFilterSettings();

    /**
     * Constructs a new {@code ImageryLayer}.
     * @param info imagery info
     */
    public ImageryLayer(ImageryInfo info) {
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
            return Main.getProjection().getDefaultZoomInPPD();
        MapView mapView = MainApplication.getMap().mapView;
        ProjectionBounds bounds = mapView.getProjectionBounds();
        return mapView.getWidth() / (bounds.maxEast - bounds.minEast);
    }

    /**
     * Gets the x displacement of this layer.
     * To be removed end of 2016
     * @return The x displacement.
     * @deprecated Use {@link TileSourceDisplaySettings#getDx()}
     */
    @Deprecated
    public double getDx() {
        // moved to AbstractTileSourceLayer/TileSourceDisplaySettings. Remains until all actions migrate.
        return 0;
    }

    /**
     * Gets the y displacement of this layer.
     * To be removed end of 2016
     * @return The y displacement.
     * @deprecated Use {@link TileSourceDisplaySettings#getDy()}
     */
    @Deprecated
    public double getDy() {
        // moved to AbstractTileSourceLayer/TileSourceDisplaySettings. Remains until all actions migrate.
        return 0;
    }

    /**
     * Sets the displacement offset of this layer. The layer is automatically invalidated.
     * To be removed end of 2016
     * @param offset the offset bookmark
     * @deprecated Use {@link TileSourceDisplaySettings}
     */
    @Deprecated
    public void setOffset(OffsetBookmark offset) {
        // moved to AbstractTileSourceLayer/TileSourceDisplaySettings. Remains until all actions migrate.
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

    protected JTextField createTextField(String text) {
        JTextField ret = new JTextField(text);
        ret.setEditable(false);
        ret.setBorder(BorderFactory.createEmptyBorder());
        return ret;
    }

    public static ImageryLayer create(ImageryInfo info) {
        switch(info.getImageryType()) {
        case WMS:
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

    class ApplyOffsetAction extends AbstractAction {
        private final transient OffsetBookmark b;

        ApplyOffsetAction(OffsetBookmark b) {
            super(b.getName());
            this.b = b;
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            setOffset(b);
            MainApplication.getMenu().imageryMenu.refreshOffsetMenu();
            MainApplication.getMap().repaint();
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

    public JMenuItem getOffsetMenuItem() {
        JMenu subMenu = new JMenu(trc("layer", "Offset"));
        subMenu.setIcon(ImageProvider.get("mapmode", "adjustimg"));
        return (JMenuItem) getOffsetMenuItem(subMenu);
    }

    public JComponent getOffsetMenuItem(JComponent subMenu) {
        JMenuItem adjustMenuItem = new JMenuItem(getAdjustAction());
        List<OffsetBookmark> allBookmarks = OffsetBookmark.getBookmarks();
        if (allBookmarks.isEmpty()) return adjustMenuItem;

        subMenu.add(adjustMenuItem);
        subMenu.add(new JSeparator());
        boolean hasBookmarks = false;
        int menuItemHeight = 0;
        for (OffsetBookmark b : allBookmarks) {
            if (!b.isUsable(this)) {
                continue;
            }
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(new ApplyOffsetAction(b));
            EastNorth offset = b.getDisplacement(Main.getProjection());
            if (Utils.equalsEpsilon(offset.east(), getDx()) && Utils.equalsEpsilon(offset.north(), getDy())) {
                item.setSelected(true);
            }
            subMenu.add(item);
            menuItemHeight = item.getPreferredSize().height;
            hasBookmarks = true;
        }
        if (menuItemHeight > 0) {
            if (subMenu instanceof JMenu) {
                MenuScroller.setScrollerFor((JMenu) subMenu);
            } else if (subMenu instanceof JPopupMenu) {
                MenuScroller.setScrollerFor((JPopupMenu) subMenu);
            }
        }
        return hasBookmarks ? subMenu : adjustMenuItem;
    }

    protected abstract Action getAdjustAction();

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
            img = processor.process(img);
        }
        return img;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [info=" + info + ']';
    }
}
