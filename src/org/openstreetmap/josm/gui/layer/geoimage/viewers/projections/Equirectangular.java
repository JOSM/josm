// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage.viewers.projections;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openstreetmap.josm.data.imagery.street_level.Projections;
import org.openstreetmap.josm.gui.layer.geoimage.ImageDisplay;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.imagery.CameraPlane;
import org.openstreetmap.josm.gui.util.imagery.Vector3D;

/**
 * A class for showing 360 images that use the equirectangular projection
 * @author Taylor Smock
 * @since 18246
 */
public class Equirectangular extends ComponentAdapter implements IImageViewer {
    @Nullable
    private volatile CameraPlane cameraPlane;
    private volatile BufferedImage offscreenImage;

    @Override
    public Set<Projections> getSupportedProjections() {
        return Collections.singleton(Projections.EQUIRECTANGULAR);
    }

    @Override
    public void paintImage(Graphics g, BufferedImage image, Rectangle target, Rectangle visibleRect) {
        final CameraPlane currentCameraPlane;
        final BufferedImage currentOffscreenImage;
        synchronized (this) {
            final CameraPlane currentPlane = this.cameraPlane;
            currentCameraPlane = currentPlane != null ? currentPlane : this.updateCameraPlane(target.width, target.height);
            currentOffscreenImage = this.offscreenImage;
        }
        currentCameraPlane.mapping(image, currentOffscreenImage, visibleRect);
        if (target == null) {
            target = new Rectangle(0, 0, currentOffscreenImage.getWidth(null), currentOffscreenImage.getHeight(null));
        }
        g.drawImage(currentOffscreenImage, target.x, target.y, target.x + target.width, target.y + target.height,
                visibleRect.x, visibleRect.y, visibleRect.x + visibleRect.width, visibleRect.y + visibleRect.height,
                null);
    }

    @Override
    public ImageDisplay.VisRect getDefaultVisibleRectangle(Component component, Image image) {
        return new ImageDisplay.VisRect(0, 0, component.getSize().width, component.getSize().height);
    }

    @Override
    public Vector3D getRotation() {
        final CameraPlane currentPlane = this.cameraPlane;
        if (currentPlane != null) {
            return currentPlane.getRotation();
        }
        return IImageViewer.super.getRotation();
    }

    @Override
    public void componentResized(ComponentEvent e) {
        final Component imgDisplay = e.getComponent();
        if (e.getComponent().getWidth() > 0
                && e.getComponent().getHeight() > 0) {
            updateCameraPlane(imgDisplay.getWidth(), imgDisplay.getHeight());
            if (imgDisplay instanceof ImageDisplay) {
                ((ImageDisplay) imgDisplay).updateVisibleRectangle();
            }
            GuiHelper.runInEDT(imgDisplay::revalidate);
        }
    }

    /**
     * Update the current camera plane
     * @param width The width to use
     * @param height The height to use
     */
    @Nonnull
    private CameraPlane updateCameraPlane(int width, int height) {
        // FIXME: Do something so that the types of the images are the same between the offscreenImage and
        // the image entry
        final CameraPlane currentCameraPlane;
        synchronized (this) {
            currentCameraPlane = this.cameraPlane;
        }
        final BufferedImage temporaryOffscreenImage = new BufferedImage(width, height,
                BufferedImage.TYPE_4BYTE_ABGR);

        Vector3D currentRotation = null;
        if (currentCameraPlane != null) {
            currentRotation = currentCameraPlane.getRotation();
        }
        final CameraPlane temporaryCameraPlane = new CameraPlane(width, height);
        if (currentRotation != null) {
            temporaryCameraPlane.setRotation(currentRotation);
        }
        synchronized (this) {
            this.cameraPlane = temporaryCameraPlane;
            this.offscreenImage = temporaryOffscreenImage;
        }
        return temporaryCameraPlane;
    }

    @Override
    public void mouseDragged(final Point from, final Point to, ImageDisplay.VisRect currentVisibleRect) {
        final CameraPlane currentPlane = this.cameraPlane;
        if (from != null && to != null && currentPlane != null) {
            currentPlane.setRotationFromDelta(from, to);
        }
    }

    @Override
    public void checkAndModifyVisibleRectSize(Image image, ImageDisplay.VisRect visibleRect) {
        IImageViewer.super.checkAndModifyVisibleRectSize(this.offscreenImage, visibleRect);
    }

    @Override
    public Image getMaxImageSize(ImageDisplay imageDisplay, Image image) {
        return this.offscreenImage;
    }
}
