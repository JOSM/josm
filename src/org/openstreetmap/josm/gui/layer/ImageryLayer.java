// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
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
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MenuScroller;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public abstract class ImageryLayer extends Layer {

    public static final ColorProperty PROP_FADE_COLOR = new ColorProperty(marktr("Imagery fade"), Color.white);
    public static final IntegerProperty PROP_FADE_AMOUNT = new IntegerProperty("imagery.fade_amount", 0);
    public static final IntegerProperty PROP_SHARPEN_LEVEL = new IntegerProperty("imagery.sharpen_level", 0);

    public static Color getFadeColor() {
        return PROP_FADE_COLOR.get();
    }

    public static Color getFadeColorWithAlpha() {
        Color c = PROP_FADE_COLOR.get();
        return new Color(c.getRed(),c.getGreen(),c.getBlue(),PROP_FADE_AMOUNT.get()*255/100);
    }

    protected final ImageryInfo info;

    protected Icon icon;

    protected double dx = 0.0;
    protected double dy = 0.0;

    protected int sharpenLevel;

    private final ImageryAdjustAction adjustAction = new ImageryAdjustAction(this);

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
        this.sharpenLevel = PROP_SHARPEN_LEVEL.get();
    }

    public double getPPD(){
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
            if (dx != 0.0 || dy != 0.0) {
                panel.add(new JLabel(tr("Offset: ") + dx + ";" + dy), GBC.eol().insets(0, 5, 10, 0));
            }
        }
        return panel;
    }

    public static ImageryLayer create(ImageryInfo info) {
        if (info.getImageryType() == ImageryType.WMS || info.getImageryType() == ImageryType.HTML)
            return new WMSLayer(info);
        else if (info.getImageryType() == ImageryType.TMS || info.getImageryType() == ImageryType.BING || info.getImageryType() == ImageryType.SCANEX)
            return new TMSLayer(info);
        else throw new AssertionError();
    }

    class ApplyOffsetAction extends AbstractAction {
        private OffsetBookmark b;
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
        return (JMenuItem)getOffsetMenuItem(subMenu);
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
            if (b.dx == dx && b.dy == dy) {
                item.setSelected(true);
            }
            subMenu.add(item);
            menuItemHeight = item.getPreferredSize().height;
            hasBookmarks = true;
        }
        if (menuItemHeight > 0) {
            int scrollcount = (Toolkit.getDefaultToolkit().getScreenSize().height / menuItemHeight) - 1;
            if (subMenu instanceof JMenu) {
                MenuScroller.setScrollerFor((JMenu) subMenu, scrollcount);
            } else if (subMenu instanceof JPopupMenu) {
                MenuScroller.setScrollerFor((JPopupMenu)subMenu, scrollcount);
            }
        }
        return hasBookmarks ? subMenu : adjustMenuItem;
    }

    public BufferedImage sharpenImage(BufferedImage img) {
        if (sharpenLevel <= 0) return img;
        int width = img.getWidth(null);
        int height = img.getHeight(null);
        BufferedImage tmp = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        tmp.getGraphics().drawImage(img, 0, 0, null);
        Kernel kernel;
        if (sharpenLevel == 1) {
            kernel = new Kernel(3, 3, new float[] { -0.25f, -0.5f, -0.25f, -0.5f, 4, -0.5f, -0.25f, -0.5f, -0.25f});
        } else {
            kernel = new Kernel(3, 3, new float[] { -0.5f, -1, -0.5f, -1, 7, -1, -0.5f, -1, -0.5f});
        }
        BufferedImageOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(tmp, null);
    }

    public void drawErrorTile(BufferedImage img) {
        Graphics g = img.getGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setFont(g.getFont().deriveFont(Font.PLAIN).deriveFont(36.0f));
        g.setColor(Color.BLACK);

        String text = tr("ERROR");
        g.drawString(text, (img.getWidth() + g.getFontMetrics().stringWidth(text)) / 2, img.getHeight()/2);
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.josm.gui.layer.Layer#destroy()
     */
    @Override
    public void destroy() {
        super.destroy();
        adjustAction.destroy();
    }
}
