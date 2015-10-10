// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.awt.image.ShortLookupTable;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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
                    setMaxHeight(ICON_SIZE).setMaxWidth(ICON_SIZE).get();
        }
        if (icon == null) {
            icon = ImageProvider.get("imagery_small");
        }
        addImageProcessor(createSharpener(PROP_SHARPEN_LEVEL.get()));
        addImageProcessor(gammaImageProcessor);
    }

    public double getPPD() {
        if (!Main.isDisplayingMapView()) return Main.getProjection().getDefaultZoomInPPD();
        ProjectionBounds bounds = Main.map.mapView.getProjectionBounds();
        return Main.map.mapView.getWidth() / (bounds.maxEast - bounds.minEast);
    }

    public double getDx() {
        return dx;
    }

    public double getDy() {
        return dy;
    }

    public void setOffset(double dx, double dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public void displace(double dx, double dy) {
        setOffset(this.dx += dx, this.dy += dy);
    }

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
        private transient OffsetBookmark b;

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

    public ImageProcessor createSharpener(int sharpenLevel) {
        final Kernel kernel;
        if (sharpenLevel == 1) {
            kernel = new Kernel(3, 3, new float[]{-0.25f, -0.5f, -0.25f, -0.5f, 4, -0.5f, -0.25f, -0.5f, -0.25f});
        } else if (sharpenLevel == 2) {
            kernel = new Kernel(3, 3, new float[]{-0.5f, -1, -0.5f, -1, 7, -1, -0.5f, -1, -0.5f});
        } else {
            return null;
        }
        BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return createImageProcessor(op, false);
    }

    /**
     * An image processor which adjusts the gamma value of an image.
     */
    public static class GammaImageProcessor implements ImageProcessor {
        private double gamma = 1;
        final short[] gammaChange = new short[256];
        private LookupOp op3 = new LookupOp(new ShortLookupTable(0, new short[][]{gammaChange, gammaChange, gammaChange}), null);
        private LookupOp op4 = new LookupOp(new ShortLookupTable(0, new short[][]{gammaChange, gammaChange, gammaChange, gammaChange}), null);

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
                if (Main.isTraceEnabled()) {
                    Main.trace(ignore.getMessage());
                }
            }
            final int type = image.getTransparency() == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
            final BufferedImage to = new BufferedImage(image.getWidth(), image.getHeight(), type);
            to.getGraphics().drawImage(image, 0, 0, null);
            return process(to);
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

    /**
     * Draws a red error tile when imagery tile cannot be fetched.
     * @param img The buffered image
     * @param message Additional error message to display
     */
    public void drawErrorTile(BufferedImage img, String message) {
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setFont(g.getFont().deriveFont(Font.PLAIN).deriveFont(24.0f));
        g.setColor(Color.BLACK);

        String text = tr("ERROR");
        g.drawString(text, (img.getWidth() - g.getFontMetrics().stringWidth(text)) / 2, g.getFontMetrics().getHeight()+5);
        if (message != null) {
            float drawPosY = 2.5f*g.getFontMetrics().getHeight()+10;
            if (!message.contains(" ")) {
                g.setFont(g.getFont().deriveFont(Font.PLAIN).deriveFont(18.0f));
                g.drawString(message, 5, (int) drawPosY);
            } else {
                // Draw message on several lines
                Map<TextAttribute, Object> map = new Hashtable<TextAttribute, Object>();
                map.put(TextAttribute.FAMILY, "Serif");
                map.put(TextAttribute.SIZE, new Float(18.0));
                AttributedString vanGogh = new AttributedString(message, map);
                // Create a new LineBreakMeasurer from the text
                AttributedCharacterIterator paragraph = vanGogh.getIterator();
                int paragraphStart = paragraph.getBeginIndex();
                int paragraphEnd = paragraph.getEndIndex();
                FontRenderContext frc = g.getFontRenderContext();
                LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(paragraph, frc);
                // Set break width to width of image with some margin
                float breakWidth = img.getWidth()-10;
                // Set position to the index of the first character in the text
                lineMeasurer.setPosition(paragraphStart);
                // Get lines until the entire paragraph has been displayed
                while (lineMeasurer.getPosition() < paragraphEnd) {
                    // Retrieve next layout
                    TextLayout layout = lineMeasurer.nextLayout(breakWidth);

                    // Compute pen x position
                    float drawPosX = layout.isLeftToRight() ? 0 : breakWidth - layout.getAdvance();

                    // Move y-coordinate by the ascent of the layout
                    drawPosY += layout.getAscent();

                    // Draw the TextLayout at (drawPosX, drawPosY)
                    layout.draw(g, drawPosX, drawPosY);

                    // Move y-coordinate in preparation for next layout
                    drawPosY += layout.getDescent() + layout.getLeading();
                }
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        adjustAction.destroy();
    }
}
