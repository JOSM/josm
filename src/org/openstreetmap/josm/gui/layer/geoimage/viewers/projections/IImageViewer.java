// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage.viewers.projections;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.Set;

import org.openstreetmap.josm.data.imagery.street_level.Projections;
import org.openstreetmap.josm.gui.layer.geoimage.ImageDisplay;
import org.openstreetmap.josm.gui.util.imagery.Vector3D;

/**
 * An interface for image viewers for specific projections
 * @since 18246
 */
public interface IImageViewer extends ComponentListener {
    /**
     * Get the supported projections for the image viewer
     * @return The projections supported. Typically, only one.
     */
    Set<Projections> getSupportedProjections();

    /**
     * Paint the image
     * @param g The graphics to paint on
     * @param image The image to paint
     * @param target The target area
     * @param visibleRect The visible rectangle
     */
    void paintImage(Graphics g, BufferedImage image, Rectangle target, Rectangle visibleRect);

    /**
     * Get the default visible rectangle for the projection
     * @param component The component the image will be displayed in
     * @param image The image that will be shown
     * @return The default visible rectangle
     */
    ImageDisplay.VisRect getDefaultVisibleRectangle(Component component, Image image);

    /**
     * Get the current rotation in the image viewer
     * @return The rotation
     * @since 18263
     */
    default Vector3D getRotation() {
        return null;
    }

    /**
     * Indicate that the mouse has been dragged to a point
     * @param from The point the mouse was dragged from
     * @param to The point the mouse has been dragged to
     * @param currentVisibleRect The currently visible rectangle (this is updated by the default implementation)
     */
    default void mouseDragged(Point from, Point to, ImageDisplay.VisRect currentVisibleRect) {
        currentVisibleRect.isDragUpdate = true;
        currentVisibleRect.x += from.x - to.x;
        currentVisibleRect.y += from.y - to.y;
    }

    /**
     * Check and modify the visible rect size to appropriate dimensions
     * @param visibleRect the visible rectangle to update
     * @param image The image to use for checking
     */
    default void checkAndModifyVisibleRectSize(Image image, ImageDisplay.VisRect visibleRect) {
        if (visibleRect.width > image.getWidth(null)) {
            visibleRect.width = image.getWidth(null);
        }
        if (visibleRect.height > image.getHeight(null)) {
            visibleRect.height = image.getHeight(null);
        }
    }

    /**
     * Get the maximum image size that can be displayed
     * @param imageDisplay The image display
     * @param image The image
     * @return The maximum image size (may be the original image passed in)
     */
    default Image getMaxImageSize(ImageDisplay imageDisplay, Image image) {
        return image;
    }
}
