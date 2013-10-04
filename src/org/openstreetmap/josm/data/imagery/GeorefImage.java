// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.WMSLayer;

public class GeorefImage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum State { IMAGE, NOT_IN_CACHE, FAILED, PARTLY_IN_CACHE}

    private WMSLayer layer;
    private State state;

    private BufferedImage image;
    private SoftReference<BufferedImage> reImg;
    private int xIndex;
    private int yIndex;

    private static final Color transparentColor = new Color(0,0,0,0);
    private Color fadeColor = transparentColor;

    public EastNorth getMin() {
        return layer.getEastNorth(xIndex, yIndex);
    }

    public EastNorth getMax() {
        return layer.getEastNorth(xIndex+1, yIndex+1);
    }


    public GeorefImage(WMSLayer layer) {
        this.layer = layer;
    }

    public void changePosition(int xIndex, int yIndex) {
        if (!equalPosition(xIndex, yIndex)) {
            this.xIndex = xIndex;
            this.yIndex = yIndex;
            this.image = null;
            flushedResizedCachedInstance();
        }
    }

    public boolean equalPosition(int xIndex, int yIndex) {
        return this.xIndex == xIndex && this.yIndex == yIndex;
    }

    public void changeImage(State state, BufferedImage image) {
        flushedResizedCachedInstance();
        this.image = image;
        this.state = state;

        switch (state) {
        case FAILED:
        {
            BufferedImage img = createImage();
            layer.drawErrorTile(img);
            this.image = img;
            break;
        }
        case NOT_IN_CACHE:
        {
            BufferedImage img = createImage();
            Graphics g = img.getGraphics();
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, img.getWidth(), img.getHeight());
            Font font = g.getFont();
            Font tempFont = font.deriveFont(Font.PLAIN).deriveFont(36.0f);
            g.setFont(tempFont);
            g.setColor(Color.BLACK);
            String text = tr("Not in cache");
            g.drawString(text, (img.getWidth() - g.getFontMetrics().stringWidth(text)) / 2, img.getHeight()/2);
            g.setFont(font);
            this.image = img;
            break;
        }
        default:
            if (this.image != null) {
                this.image = layer.sharpenImage(this.image);
            }
            break;
        }
    }

    private BufferedImage createImage() {
        return new BufferedImage(layer.getImageSize(), layer.getImageSize(), BufferedImage.TYPE_INT_RGB);
    }

    public boolean paint(Graphics g, NavigatableComponent nc, int xIndex, int yIndex, int leftEdge, int bottomEdge) {
        if (image == null)
            return false;

        if(!(this.xIndex == xIndex && this.yIndex == yIndex))
            return false;

        int left = layer.getImageX(xIndex);
        int bottom = layer.getImageY(yIndex);
        int width = layer.getImageWidth(xIndex);
        int height = layer.getImageHeight(yIndex);

        int x = left - leftEdge;
        int y = nc.getHeight() - (bottom - bottomEdge) - height;

        // This happens if you zoom outside the world
        if(width == 0 || height == 0)
            return false;

        // TODO: implement per-layer fade color
        Color newFadeColor;
        if (ImageryLayer.PROP_FADE_AMOUNT.get() == 0) {
            newFadeColor = transparentColor;
        } else {
            newFadeColor = ImageryLayer.getFadeColorWithAlpha();
        }

        BufferedImage img = reImg == null?null:reImg.get();
        if(img != null && img.getWidth() == width && img.getHeight() == height && fadeColor.equals(newFadeColor)) {
            g.drawImage(img, x, y, null);
            return true;
        }

        fadeColor = newFadeColor;

        boolean alphaChannel = WMSLayer.PROP_ALPHA_CHANNEL.get() && getImage().getTransparency() != Transparency.OPAQUE;

        try {
            if(img != null) {
                img.flush();
            }
            long freeMem = Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
            // Notice that this value can get negative due to integer overflows

            int multipl = alphaChannel ? 4 : 3;
            // This happens when requesting images while zoomed out and then zooming in
            // Storing images this large in memory will certainly hang up JOSM. Luckily
            // traditional rendering is as fast at these zoom levels, so it's no loss.
            // Also prevent caching if we're out of memory soon
            if(width > 2000 || height > 2000 || width*height*multipl > freeMem) {
                fallbackDraw(g, getImage(), x, y, width, height, alphaChannel);
            } else {
                // We haven't got a saved resized copy, so resize and cache it
                img = new BufferedImage(width, height, alphaChannel?BufferedImage.TYPE_INT_ARGB:BufferedImage.TYPE_3BYTE_BGR);
                img.getGraphics().drawImage(getImage(),
                        0, 0, width, height, // dest
                        0, 0, getImage().getWidth(null), getImage().getHeight(null), // src
                        null);
                if (!alphaChannel) {
                    drawFadeRect(img.getGraphics(), 0, 0, width, height);
                }
                img.getGraphics().dispose();
                g.drawImage(img, x, y, null);
                reImg = new SoftReference<BufferedImage>(img);
            }
        } catch(Exception e) {
            fallbackDraw(g, getImage(), x, y, width, height, alphaChannel);
        }
        return true;
    }

    private void fallbackDraw(Graphics g, Image img, int x, int y, int width, int height, boolean alphaChannel) {
        flushedResizedCachedInstance();
        g.drawImage(
                img, x, y, x + width, y + height,
                0, 0, img.getWidth(null), img.getHeight(null),
                null);
        if (!alphaChannel) { //FIXME: fading for layers with alpha channel currently is not supported
            drawFadeRect(g, x, y, width, height);
        }
    }

    private void drawFadeRect(Graphics g, int x, int y, int width, int height) {
        if (fadeColor != transparentColor) {
            g.setColor(fadeColor);
            g.fillRect(x, y, width, height);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        state = (State) in.readObject();
        boolean hasImage = in.readBoolean();
        if (hasImage) {
            image = (ImageIO.read(ImageIO.createImageInputStream(in)));
        } else {
            in.readObject(); // read null from input stream
            image = null;
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(state);
        if(getImage() == null) {
            out.writeBoolean(false);
            out.writeObject(null);
        } else {
            out.writeBoolean(true);
            ImageIO.write(getImage(), "png", ImageIO.createImageOutputStream(out));
        }
    }

    public void flushedResizedCachedInstance() {
        if (reImg != null) {
            BufferedImage img = reImg.get();
            if (img != null) {
                img.flush();
            }
        }
        reImg = null;
    }


    public BufferedImage getImage() {
        return image;
    }

    public State getState() {
        return state;
    }

    public int getXIndex() {
        return xIndex;
    }

    public int getYIndex() {
        return yIndex;
    }

    public void setLayer(WMSLayer layer) {
        this.layer = layer;
    }
}
