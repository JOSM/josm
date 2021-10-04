// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage.viewers.projections;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.Set;

import org.openstreetmap.josm.data.imagery.street_level.Projections;
import org.openstreetmap.josm.gui.layer.geoimage.ImageDisplay;

/**
 * The default perspective image viewer class.
 * This also handles (by default) unknown projections.
 * @since 18246
 */
public class Perspective extends ComponentAdapter implements IImageViewer {

    @Override
    public Set<Projections> getSupportedProjections() {
        return EnumSet.of(Projections.PERSPECTIVE);
    }

    @Override
    public void paintImage(Graphics g, BufferedImage image, Rectangle target, Rectangle r) {
        g.drawImage(image,
                target.x, target.y, target.x + target.width, target.y + target.height,
                r.x, r.y, r.x + r.width, r.y + r.height, null);
    }

    @Override
    public ImageDisplay.VisRect getDefaultVisibleRectangle(Component component, Image image) {
        return new ImageDisplay.VisRect(0, 0, image.getWidth(null), image.getHeight(null));
    }
}
