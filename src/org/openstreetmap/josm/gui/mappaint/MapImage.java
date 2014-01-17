// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.mappaint.BoxTextElemStyle.BoxProvider;
import org.openstreetmap.josm.gui.mappaint.BoxTextElemStyle.BoxProviderResult;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageCallback;
import org.openstreetmap.josm.tools.Utils;

public class MapImage {
    
    private static final int MAX_SIZE = 48;
    
    /**
     * ImageIcon can change while the image is loading.
     */
    private BufferedImage img;

    /**
     * The 5 following fields are only used to check for equality.
     */
    public int alpha = 255;
    public String name;
    public StyleSource source;
    public int width = -1;
    public int height = -1;

    private boolean temporary;
    private Image disabledImgCache;

    public MapImage(String name, StyleSource source) {
        this.name = name;
        this.source = source;
    }

    public Image getDisabled() {
        if (disabledImgCache != null)
                return disabledImgCache;
        if (img == null)
            getImage(); // fix #7498 ?
        disabledImgCache = GuiHelper.getDisabledImage(img);
        return disabledImgCache;
    }

    public BufferedImage getImage() {
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
                .getInBackground(new ImageCallback() {
                    @Override
                    public void finished(ImageIcon result) {
                        synchronized (MapImage.this) {
                            if (result == null) {
                                ImageIcon noIcon = MapPaintStyles.getNoIcon_Icon(source);
                                img = noIcon == null ? null : (BufferedImage) noIcon.getImage();
                            } else {
                                img = (BufferedImage) result.getImage();
                            }
                            if (temporary) {
                                disabledImgCache = null;
                                Main.map.mapView.preferenceChanged(null); // otherwise repaint is ignored, because layer hasn't changed
                                Main.map.mapView.repaint();
                            }
                            temporary = false;
                        }
                    }
                }
        );
        synchronized (this) {
            if (img == null) {
                img = (BufferedImage) ImageProvider.get("clock").getImage();
                temporary = true;
            }
        }
        return img;
    }

    public int getWidth() {
        return getImage().getWidth(null);
    }

    public int getHeight() {
        return getImage().getHeight(null);
    }

    public float getAlphaFloat() {
        return Utils.color_int2float(alpha);
    }

    /**
     * Returns true, if image is not completely loaded and getImage() returns a temporary image.
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

    public BoxProvider getBoxProvider() {
        return new MapImageBoxProvider();
    }
    
    /**
     * Returns the really displayed node icon for this {@code MapImage}.
     * @param disabled {@code} true to request disabled version, {@code false} for the standard version
     * @return The scaled down version to 16x16 pixels if the image height and width exceeds 48 pixels and no size has been explicitely specified
     * @since 6174
     */
    public Image getDisplayedNodeIcon(boolean disabled) {
        final Image image = disabled ? getDisabled() : getImage();
        // Scale down large (.svg) images to 16x16 pixels if no size is explicitely specified
        if (mustRescale(image)) {
            return ImageProvider.createBoundedImage(image, 16);
        } else {
            return image;
        }
    }
    
    private boolean mustRescale(Image image) {
        return ((width  == -1 && image.getWidth(null) > MAX_SIZE) 
             && (height == -1 && image.getHeight(null) > MAX_SIZE));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        final MapImage other = (MapImage) obj;
        // img changes when image is fully loaded and can't be used for equality check.
        return  alpha == other.alpha &&
                equal(name, other.name) &&
                equal(source, other.source) &&
                width == other.width &&
                height == other.height;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + alpha;
        hash = 67 * hash + name.hashCode();
        hash = 67 * hash + source.hashCode();
        hash = 67 * hash + width;
        hash = 67 * hash + height;
        return hash;
    }

    @Override
    public String toString() {
        return name;
    }
}
