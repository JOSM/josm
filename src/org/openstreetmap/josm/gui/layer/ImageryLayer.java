// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.awt.image.ShortLookupTable;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ImageryAdjustAction;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MenuScroller;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Utils;

public abstract class ImageryLayer extends Layer {

    public static final ColorProperty PROP_FADE_COLOR = new ColorProperty(marktr("Imagery fade"), Color.white);
    public static final IntegerProperty PROP_FADE_AMOUNT = new IntegerProperty("imagery.fade_amount", 0);
    public static final IntegerProperty PROP_SHARPEN_LEVEL = new IntegerProperty("imagery.sharpen_level", 0);

    private final List<ImageProcessor> imageProcessors = new ArrayList<>();

    public static Color getFadeColor() {
        return PROP_FADE_COLOR.get();
    }

    public static Color getFadeColorWithAlpha() {
        Color c = PROP_FADE_COLOR.get();
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), PROP_FADE_AMOUNT.get()*255/100);
    }

    protected final ImageryInfo info;

    protected Icon icon;

    protected double dx;
    protected double dy;

    protected GammaImageProcessor gammaImageProcessor = new GammaImageProcessor();
    protected SharpenImageProcessor sharpenImageProcessor = new SharpenImageProcessor();
    protected ColorfulImageProcessor collorfulnessImageProcessor = new ColorfulImageProcessor();

    private final ImageryAdjustAction adjustAction = new ImageryAdjustAction(this);

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
        addImageProcessor(collorfulnessImageProcessor);
        addImageProcessor(gammaImageProcessor);
        addImageProcessor(sharpenImageProcessor);
        sharpenImageProcessor.setSharpenLevel(1 + PROP_SHARPEN_LEVEL.get() / 2f);
    }

    public double getPPD() {
        if (!Main.isDisplayingMapView())
            return Main.getProjection().getDefaultZoomInPPD();
        ProjectionBounds bounds = Main.map.mapView.getProjectionBounds();
        return Main.map.mapView.getWidth() / (bounds.maxEast - bounds.minEast);
    }

    public double getDx() {
        return dx;
    }

    public double getDy() {
        return dy;
    }

    /**
     * Sets the displacement offset of this layer. The layer is automatically invalidated.
     * @param dx The x offset
     * @param dy The y offset
     */
    public void setOffset(double dx, double dy) {
        this.dx = dx;
        this.dy = dy;
        invalidate();
    }

    public void displace(double dx, double dy) {
        this.dx += dx;
        this.dy += dy;
        setOffset(this.dx, this.dy);
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
            String url = info.getUrl();
            if (url != null) {
                panel.add(new JLabel(tr("URL: ")), GBC.std().insets(0, 5, 2, 0));
                panel.add(new UrlLabel(url), GBC.eol().insets(2, 5, 10, 0));
            }
            if (dx != 0 || dy != 0) {
                panel.add(new JLabel(tr("Offset: ") + dx + ';' + dy), GBC.eol().insets(0, 5, 10, 0));
            }
        }
        return panel;
    }

    public static ImageryLayer create(ImageryInfo info) {
        switch(info.getImageryType()) {
        case WMS:
        case HTML:
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
            super(b.name);
            this.b = b;
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            setOffset(b.dx, b.dy);
            Main.main.menu.imageryMenu.refreshOffsetMenu();
            Main.map.repaint();
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
        JMenuItem adjustMenuItem = new JMenuItem(adjustAction);
        if (OffsetBookmark.allBookmarks.isEmpty()) return adjustMenuItem;

        subMenu.add(adjustMenuItem);
        subMenu.add(new JSeparator());
        boolean hasBookmarks = false;
        int menuItemHeight = 0;
        for (OffsetBookmark b : OffsetBookmark.allBookmarks) {
            if (!b.isUsable(this)) {
                continue;
            }
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(new ApplyOffsetAction(b));
            if (Utils.equalsEpsilon(b.dx, dx) && Utils.equalsEpsilon(b.dy, dy)) {
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

    /**
     * An image processor which adjusts the gamma value of an image.
     */
    public static class GammaImageProcessor implements ImageProcessor {
        private double gamma = 1;
        final short[] gammaChange = new short[256];
        private final LookupOp op3 = new LookupOp(
                new ShortLookupTable(0, new short[][]{gammaChange, gammaChange, gammaChange}), null);
        private final LookupOp op4 = new LookupOp(
                new ShortLookupTable(0, new short[][]{gammaChange, gammaChange, gammaChange, gammaChange}), null);

        /**
         * Returns the currently set gamma value.
         * @return the currently set gamma value
         */
        public double getGamma() {
            return gamma;
        }

        /**
         * Sets a new gamma value, {@code 1} stands for no correction.
         * @param gamma new gamma value
         */
        public void setGamma(double gamma) {
            this.gamma = gamma;
            for (int i = 0; i < 256; i++) {
                gammaChange[i] = (short) (255 * Math.pow(i / 255., gamma));
            }
        }

        @Override
        public BufferedImage process(BufferedImage image) {
            if (gamma == 1) {
                return image;
            }
            try {
                final int bands = image.getRaster().getNumBands();
                if (image.getType() != BufferedImage.TYPE_CUSTOM && bands == 3) {
                    return op3.filter(image, null);
                } else if (image.getType() != BufferedImage.TYPE_CUSTOM && bands == 4) {
                    return op4.filter(image, null);
                }
            } catch (IllegalArgumentException ignore) {
                Main.trace(ignore);
            }
            final int type = image.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
            final BufferedImage to = new BufferedImage(image.getWidth(), image.getHeight(), type);
            to.getGraphics().drawImage(image, 0, 0, null);
            return process(to);
        }

        @Override
        public String toString() {
            return "GammaImageProcessor [gamma=" + gamma + ']';
        }
    }

    /**
     * Sharpens or blurs the image, depending on the sharpen value.
     * <p>
     * A positive sharpen level means that we sharpen the image.
     * <p>
     * A negative sharpen level let's us blur the image. -1 is the most useful value there.
     *
     * @author Michael Zangl
     */
    public static class SharpenImageProcessor implements ImageProcessor {
        private float sharpenLevel;
        private ConvolveOp op;

        private static float[] KERNEL_IDENTITY = new float[] {
            0, 0, 0,
            0, 1, 0,
            0, 0, 0
        };

        private static float[] KERNEL_BLUR = new float[] {
            1f / 16, 2f / 16, 1f / 16,
            2f / 16, 4f / 16, 2f / 16,
            1f / 16, 2f / 16, 1f / 16
        };

        private static float[] KERNEL_SHARPEN = new float[] {
            -.5f, -1f, -.5f,
             -1f, 7, -1f,
            -.5f, -1f, -.5f
        };

        /**
         * Gets the current sharpen level.
         * @return The level.
         */
        public float getSharpenLevel() {
            return sharpenLevel;
        }

        /**
         * Sets the sharpening level.
         * @param sharpenLevel The level. Clamped to be positive or 0.
         */
        public void setSharpenLevel(float sharpenLevel) {
            if (sharpenLevel < 0) {
                this.sharpenLevel = 0;
            } else {
                this.sharpenLevel = sharpenLevel;
            }

            if (this.sharpenLevel < 0.95) {
                op = generateMixed(this.sharpenLevel, KERNEL_IDENTITY, KERNEL_BLUR);
            } else if (this.sharpenLevel > 1.05) {
                op = generateMixed(this.sharpenLevel - 1, KERNEL_SHARPEN, KERNEL_IDENTITY);
            } else {
                op = null;
            }
        }

        private ConvolveOp generateMixed(float aFactor, float[] a, float[] b) {
            if (a.length != 9 || b.length != 9) {
                throw new IllegalArgumentException("Illegal kernel array length.");
            }
            float[] values = new float[9];
            for (int i = 0; i < values.length; i++) {
                values[i] = aFactor * a[i] + (1 - aFactor) * b[i];
            }
            return new ConvolveOp(new Kernel(3, 3, values), ConvolveOp.EDGE_NO_OP, null);
        }

        @Override
        public BufferedImage process(BufferedImage image) {
            if (op != null) {
                return op.filter(image, null);
            } else {
                return image;
            }
        }

        @Override
        public String toString() {
            return "SharpenImageProcessor [sharpenLevel=" + sharpenLevel + ']';
        }
    }

    /**
     * Adds or removes the colorfulness of the image.
     *
     * @author Michael Zangl
     */
    public static class ColorfulImageProcessor implements ImageProcessor {
        private ColorfulFilter op;
        private double colorfulness = 1;

        /**
         * Gets the colorfulness value.
         * @return The value
         */
        public double getColorfulness() {
            return colorfulness;
        }

        /**
         * Sets the colorfulness value. Clamps it to 0+
         * @param colorfulness The value
         */
        public void setColorfulness(double colorfulness) {
            if (colorfulness < 0) {
                this.colorfulness = 0;
            } else {
                this.colorfulness = colorfulness;
            }

            if (this.colorfulness < .95 || this.colorfulness > 1.05) {
                op = new ColorfulFilter(this.colorfulness);
            } else {
                op = null;
            }
        }

        @Override
        public BufferedImage process(BufferedImage image) {
            if (op != null) {
                return op.filter(image, null);
            } else {
                return image;
            }
        }

        @Override
        public String toString() {
            return "ColorfulImageProcessor [colorfulness=" + colorfulness + ']';
        }
    }

    private static class ColorfulFilter implements BufferedImageOp {
        private final double colorfulness;

        /**
         * Create a new colorful filter.
         * @param colorfulness The colorfulness as defined in the {@link ColorfulImageProcessor} class.
         */
        ColorfulFilter(double colorfulness) {
            this.colorfulness = colorfulness;
        }

        @Override
        public BufferedImage filter(BufferedImage src, BufferedImage dest) {
            if (src.getWidth() == 0 || src.getHeight() == 0) {
                return src;
            }

            if (dest == null) {
                dest = createCompatibleDestImage(src, null);
            }
            DataBuffer srcBuffer = src.getRaster().getDataBuffer();
            DataBuffer destBuffer = dest.getRaster().getDataBuffer();
            if (!(srcBuffer instanceof DataBufferByte) || !(destBuffer instanceof DataBufferByte)) {
                Main.trace("Cannot apply color filter: Images do not use DataBufferByte.");
                return src;
            }

            int type = src.getType();
            if (type != dest.getType()) {
                Main.trace("Cannot apply color filter: Src / Dest differ in type (" + type + '/' + dest.getType() + ')');
                return src;
            }
            int redOffset, greenOffset, blueOffset, alphaOffset = 0;
            switch (type) {
            case BufferedImage.TYPE_3BYTE_BGR:
                blueOffset = 0;
                greenOffset = 1;
                redOffset = 2;
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                blueOffset = 1;
                greenOffset = 2;
                redOffset = 3;
                break;
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                redOffset = 0;
                greenOffset = 1;
                blueOffset = 2;
                alphaOffset = 3;
                break;
            default:
                Main.trace("Cannot apply color filter: Source image is of wrong type (" + type + ").");
                return src;
            }
            doFilter((DataBufferByte) srcBuffer, (DataBufferByte) destBuffer, redOffset, greenOffset, blueOffset,
                    alphaOffset, src.getAlphaRaster() != null);
            return dest;
        }

        private void doFilter(DataBufferByte src, DataBufferByte dest, int redOffset, int greenOffset, int blueOffset,
                int alphaOffset, boolean hasAlpha) {
            byte[] srcPixels = src.getData();
            byte[] destPixels = dest.getData();
            if (srcPixels.length != destPixels.length) {
                Main.trace("Cannot apply color filter: Source/Dest lengths differ.");
                return;
            }
            int entries = hasAlpha ? 4 : 3;
            for (int i = 0; i < srcPixels.length; i += entries) {
                int r = srcPixels[i + redOffset] & 0xff;
                int g = srcPixels[i + greenOffset] & 0xff;
                int b = srcPixels[i + blueOffset] & 0xff;
                double luminosity = r * .21d + g * .72d + b * .07d;
                destPixels[i + redOffset] = mix(r, luminosity);
                destPixels[i + greenOffset] = mix(g, luminosity);
                destPixels[i + blueOffset] = mix(b, luminosity);
                if (hasAlpha) {
                    destPixels[i + alphaOffset] = srcPixels[i + alphaOffset];
                }
            }
        }

        private byte mix(int color, double luminosity) {
            int val = (int) (colorfulness * color + (1 - colorfulness) * luminosity);
            if (val < 0) {
                return 0;
            } else if (val > 0xff) {
                return (byte) 0xff;
            } else {
                return (byte) val;
            }
        }

        @Override
        public Rectangle2D getBounds2D(BufferedImage src) {
            return new Rectangle(src.getWidth(), src.getHeight());
        }

        @Override
        public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
            return new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        }

        @Override
        public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
            return (Point2D) srcPt.clone();
        }

        @Override
        public RenderingHints getRenderingHints() {
            return null;
        }

    }

    /**
     * Returns the currently set gamma value.
     * @return the currently set gamma value
     */
    public double getGamma() {
        return gammaImageProcessor.getGamma();
    }

    /**
     * Sets a new gamma value, {@code 1} stands for no correction.
     * @param gamma new gamma value
     */
    public void setGamma(double gamma) {
        gammaImageProcessor.setGamma(gamma);
    }

    /**
     * Gets the current sharpen level.
     * @return The sharpen level.
     */
    public double getSharpenLevel() {
        return sharpenImageProcessor.getSharpenLevel();
    }

    /**
     * Sets the sharpen level for the layer.
     * <code>1</code> means no change in sharpness.
     * Values in range 0..1 blur the image.
     * Values above 1 are used to sharpen the image.
     * @param sharpenLevel The sharpen level.
     */
    public void setSharpenLevel(double sharpenLevel) {
        sharpenImageProcessor.setSharpenLevel((float) sharpenLevel);
    }

    /**
     * Gets the colorfulness of this image.
     * @return The colorfulness
     */
    public double getColorfulness() {
        return collorfulnessImageProcessor.getColorfulness();
    }

    /**
     * Sets the colorfulness of this image.
     * 0 means grayscale.
     * 1 means normal colorfulness.
     * Values greater than 1 are allowed.
     * @param colorfulness The colorfulness.
     */
    public void setColorfulness(double colorfulness) {
        collorfulnessImageProcessor.setColorfulness(colorfulness);
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
        return new ImageProcessor() {
            @Override
            public BufferedImage process(BufferedImage image) {
                return op.filter(image, inPlace ? image : null);
            }
        };
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
    public void destroy() {
        super.destroy();
        adjustAction.destroy();
    }
}
