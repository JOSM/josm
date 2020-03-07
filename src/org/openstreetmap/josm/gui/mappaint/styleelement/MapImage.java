// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.mappaint.styleelement.BoxTextElement.BoxProvider;
import org.openstreetmap.josm.gui.mappaint.styleelement.BoxTextElement.BoxProviderResult;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageResource;
import org.openstreetmap.josm.tools.Utils;

/**
 * An image that will be displayed on the map.
 */
public class MapImage {

    private static final int MAX_SIZE = 48;

    /**
     * ImageIcon can change while the image is loading.
     */
    private Image img;
    private ImageResource imageResource;

    /**
     * The alpha (opacity) value of the image. It is multiplied to the image alpha channel.
     * Range: 0...255
     */
    public int alpha = 255;
    /**
     * The name of the image that should be displayed. It is given to the {@link ImageProvider}
     */
    public String name;
    /**
     * The StyleSource that registered the image
     */
    public StyleSource source;
    /**
     * A flag indicating that the image should automatically be scaled to the right size.
     */
    public boolean autoRescale;
    /**
     * The width of the image, as set by MapCSS
     */
    public int width = -1;
    /**
     * The height of the image, as set by MapCSS
     */
    public int height = -1;
    /**
     * The x offset of the anchor of this image
     */
    public int offsetX;
    /**
     * The y offset of the anchor of this image
     */
    public int offsetY;

    private boolean temporary;

    /**
     * A cache that holds a disabled (gray) version of this image
     */
    private BufferedImage disabledImgCache;

    /**
     * Creates a new {@link MapImage}
     * @param name The image name
     * @param source The style source that requests this image
     */
    public MapImage(String name, StyleSource source) {
        this(name, source, true);
    }

    /**
     * Creates a new {@link MapImage}
     * @param name The image name
     * @param source The style source that requests this image
     * @param autoRescale A flag indicating to automatically adjust the width/height of the image
     */
    public MapImage(String name, StyleSource source, boolean autoRescale) {
        this.name = name;
        this.source = source;
        this.autoRescale = autoRescale;
    }

    /**
     * Get the image associated with this MapImage object.
     *
     * @param disabled {@code} true to request disabled version, {@code false} for the standard version
     * @return the image
     */
    public Image getImage(boolean disabled) {
        if (disabled) {
            return getDisabled();
        } else {
            return getImage();
        }
    }

    /**
     * Get the image resource associated with this MapImage object.
     * @return the image resource
     */
    public ImageResource getImageResource() {
        return imageResource;
    }

    private Image getDisabled() {
        if (disabledImgCache != null)
            return disabledImgCache;
        if (img == null)
            getImage(); // fix #7498 ?
        Image disImg = GuiHelper.getDisabledImage(img);
        if (disImg instanceof BufferedImage) {
            disabledImgCache = (BufferedImage) disImg;
        } else {
            disabledImgCache = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = disabledImgCache.getGraphics();
            g.drawImage(disImg, 0, 0, null);
            g.dispose();
        }
        return disabledImgCache;
    }

    private Image getImage() {
        if (img != null)
            return img;
        temporary = false;
        new ImageProvider(name)
                .setDirs(MapPaintStyles.getIconSourceDirs(source))
                .setId("mappaint."+source.getPrefName())
                .setArchive(source.zipIcons)
                .setInArchiveDir(source.getZipEntryDirName())
                .setWidth(width)
                .setHeight(height)
                .setOptional(true)
                .getResourceAsync(result -> {
                    synchronized (this) {
                        imageResource = result;
                        if (result == null) {
                            source.logWarning(tr("Failed to locate image ''{0}''", name));
                            ImageIcon noIcon = MapPaintStyles.getNoIconIcon(source);
                            img = noIcon == null ? null : noIcon.getImage();
                        } else {
                            img = rescale(result.getImageIcon().getImage());
                        }
                        if (temporary) {
                            disabledImgCache = null;
                            MapView mapView = MainApplication.getMap().mapView;
                            mapView.preferenceChanged(null); // otherwise repaint is ignored, because layer hasn't changed
                            mapView.repaint();
                        }
                        temporary = false;
                    }
                }
        );
        synchronized (this) {
            if (img == null) {
                img = ImageProvider.get("clock").getImage();
                temporary = true;
            }
        }
        return img;
    }

    /**
     * Gets the image width
     * @return The real image width
     */
    public int getWidth() {
        return getImage().getWidth(null);
    }

    /**
     * Gets the image height
     * @return The real image height
     */
    public int getHeight() {
        return getImage().getHeight(null);
    }

    /**
     * Gets the alpha value the image should be multiplied with
     * @return The value in range 0..1
     */
    public float getAlphaFloat() {
        return Utils.colorInt2float(alpha);
    }

    /**
     * Determines if image is not completely loaded and {@code getImage()} returns a temporary image.
     * @return {@code true} if image is not completely loaded and getImage() returns a temporary image
     */
    public boolean isTemporary() {
        return temporary;
    }

    protected class MapImageBoxProvider implements BoxProvider {
        @Override
        public BoxProviderResult get() {
            return new BoxProviderResult(box(), temporary);
        }

        private Rectangle box() {
            int w = getWidth(), h = getHeight();
            if (mustRescale(getImage())) {
                w = 16;
                h = 16;
            }
            return new Rectangle(-w/2, -h/2, w, h);
        }

        private MapImage getParent() {
            return MapImage.this;
        }

        @Override
        public int hashCode() {
            return MapImage.this.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BoxProvider))
                return false;
            if (obj instanceof MapImageBoxProvider) {
                MapImageBoxProvider other = (MapImageBoxProvider) obj;
                return MapImage.this.equals(other.getParent());
            } else if (temporary) {
                return false;
            } else {
                final BoxProvider other = (BoxProvider) obj;
                BoxProviderResult resultOther = other.get();
                if (resultOther.isTemporary()) return false;
                return box().equals(resultOther.getBox());
            }
        }
    }

    /**
     * Gets a box provider that provides a box that covers the size of this image
     * @return The box provider
     */
    public BoxProvider getBoxProvider() {
        return new MapImageBoxProvider();
    }

    /**
     * Rescale excessively large images.
     * @param image the unscaled image
     * @return The scaled down version to 16x16 pixels if the image height and width exceeds 48 pixels and no size has been explicitly specified
     */
    private Image rescale(Image image) {
        if (image == null) return null;
        // Scale down large (.svg) images to 16x16 pixels if no size is explicitly specified
        if (mustRescale(image)) {
            return ImageProvider.createBoundedImage(image, 16);
        } else {
            return image;
        }
    }

    private boolean mustRescale(Image image) {
        return autoRescale && width == -1 && image.getWidth(null) > MAX_SIZE
             && height == -1 && image.getHeight(null) > MAX_SIZE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MapImage mapImage = (MapImage) obj;
        return alpha == mapImage.alpha &&
                autoRescale == mapImage.autoRescale &&
                width == mapImage.width &&
                height == mapImage.height &&
                Objects.equals(name, mapImage.name) &&
                Objects.equals(source, mapImage.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alpha, name, source, autoRescale, width, height);
    }

    @Override
    public String toString() {
        return name;
    }
}
