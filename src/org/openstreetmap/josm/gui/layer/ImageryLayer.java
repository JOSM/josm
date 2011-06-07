// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
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
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ImageryAdjustAction;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.io.imagery.OffsetServer;
import org.openstreetmap.josm.io.imagery.OsmosnimkiOffsetServer;
import org.openstreetmap.josm.tools.ImageProvider;

public abstract class ImageryLayer extends Layer {
    protected static final Icon icon = ImageProvider.get("imagery_small");

    public static final IntegerProperty PROP_FADE_AMOUNT = new IntegerProperty("imagery.fade_amount", 0);
    public static final IntegerProperty PROP_SHARPEN_LEVEL = new IntegerProperty("imagery.sharpen_level", 0);

    public static Color getFadeColor() {
        return Main.pref.getColor("imagery.fade", Color.white);
    }

    public static Color getFadeColorWithAlpha() {
        Color c = getFadeColor();
        return new Color(c.getRed(),c.getGreen(),c.getBlue(),PROP_FADE_AMOUNT.get()*255/100);
    }

    public static void setFadeColor(Color color) {
        Main.pref.putColor("imagery.fade", color);
    }

    protected final ImageryInfo info;

    protected double dx = 0.0;
    protected double dy = 0.0;

    protected int sharpenLevel;

    protected boolean offsetServerSupported;
    protected boolean offsetServerUsed;
    protected OffsetServerThread offsetServerThread;

    protected OffsetServerThread createoffsetServerThread() {
        return new OffsetServerThread(new OsmosnimkiOffsetServer(
                OsmosnimkiOffsetServer.PROP_SERVER_URL.get()));
    }

    public ImageryLayer(ImageryInfo info) {
        super(info.getName());
        this.info = info;
        this.sharpenLevel = PROP_SHARPEN_LEVEL.get();
        if (OffsetServer.PROP_SERVER_ENABLED.get()) {
            offsetServerThread = createoffsetServerThread();
            offsetServerThread.start();
        }
    }

    public double getPPD(){
        if (Main.map == null || Main.map.mapView == null) return Main.getProjection().getDefaultZoomInPPD();
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
        return getToolTipText();
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
            enableOffsetServer(false);
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

    ImageryAdjustAction adjustAction = new ImageryAdjustAction(this);
    AbstractAction useServerOffsetAction = new AbstractAction(tr("(use server offset)")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            enableOffsetServer(true);
        }
    };

    public void enableOffsetServer(boolean enable) {
        offsetServerUsed = enable;
        if (offsetServerUsed && !offsetServerThread.isAlive()) {
            offsetServerThread = createoffsetServerThread();
            offsetServerThread.start();
        }
    }

    public JMenuItem getOffsetMenuItem() {
        JMenu subMenu = new JMenu(trc("layer", "Offset"));
        subMenu.setIcon(ImageProvider.get("mapmode", "adjustimg"));
        return (JMenuItem)getOffsetMenuItem(subMenu);
    }

    public JComponent getOffsetMenuItem(JComponent subMenu) {
        JMenuItem adjustMenuItem = new JMenuItem(adjustAction);
        if (OffsetBookmark.allBookmarks.isEmpty() && !offsetServerSupported) return adjustMenuItem;

        subMenu.add(adjustMenuItem);
        if (offsetServerSupported) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(useServerOffsetAction);
            if (offsetServerUsed) {
                item.setSelected(true);
            }
            subMenu.add(item);
        }
        subMenu.add(new JSeparator());
        boolean hasBookmarks = false;
        for (OffsetBookmark b : OffsetBookmark.allBookmarks) {
            if (!b.isUsable(this)) {
                continue;
            }
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(new ApplyOffsetAction(b));
            if (b.dx == dx && b.dy == dy && !offsetServerUsed) {
                item.setSelected(true);
            }
            subMenu.add(item);
            hasBookmarks = true;
        }
        return (hasBookmarks || offsetServerSupported) ? subMenu : adjustMenuItem;
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

    protected class OffsetServerThread extends Thread {
        OffsetServer offsetServer;
        EastNorth oldCenter = new EastNorth(Double.NaN, Double.NaN);

        public OffsetServerThread(OffsetServer offsetServer) {
            this.offsetServer = offsetServer;
            setDaemon(true);
        }

        private void updateOffset() {
            if (Main.map == null || Main.map.mapView == null) return;
            EastNorth center = Main.map.mapView.getCenter();
            if (center.equals(oldCenter)) return;
            oldCenter = center;

            EastNorth offset = offsetServer.getOffset(getInfo(), center);
            if (offset != null) {
                setOffset(offset.east(),offset.north());
            }
        }

        @Override
        public void run() {
            if (!offsetServerSupported) {
                if (!offsetServer.isLayerSupported(info)) return;
                offsetServerSupported = true;
            }
            offsetServerUsed = true;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Main.main.menu.imageryMenu.refreshOffsetMenu();
                }
            });
            try {
                while (offsetServerUsed) {
                    updateOffset();
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
            }
            offsetServerUsed = false;
        }
    }
}
