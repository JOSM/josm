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
            currentCameraPlane = this.cameraPlane;
            currentOffscreenImage = this.offscreenImage;
        }
        currentCameraPlane.mapping(image, currentOffscreenImage);
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
    public double getRotation() {
        return this.cameraPlane.getRotation().getAzimuthalAngle();
    }

    @Override
    public void componentResized(ComponentEvent e) {
        final Component imgDisplay = e.getComponent();
        if (e.getComponent().getWidth() > 0
                && e.getComponent().getHeight() > 0) {
            // FIXME: Do something so that the types of the images are the same between the offscreenImage and
            // the image entry
            final CameraPlane currentCameraPlane;
            synchronized (this) {
                currentCameraPlane = this.cameraPlane;
            }
            final BufferedImage temporaryOffscreenImage = new BufferedImage(imgDisplay.getWidth(), imgDisplay.getHeight(),
                    BufferedImage.TYPE_4BYTE_ABGR);

            Vector3D currentRotation = null;
            if (currentCameraPlane != null) {
                currentRotation = currentCameraPlane.getRotation();
            }
            final CameraPlane temporaryCameraPlane = new CameraPlane(imgDisplay.getWidth(), imgDisplay.getHeight());
            if (currentRotation != null) {
                temporaryCameraPlane.setRotation(currentRotation);
            }
            synchronized (this) {
                this.cameraPlane = temporaryCameraPlane;
                this.offscreenImage = temporaryOffscreenImage;
            }
            if (imgDisplay instanceof ImageDisplay) {
                ((ImageDisplay) imgDisplay).updateVisibleRectangle();
            }
            GuiHelper.runInEDT(imgDisplay::revalidate);
        }
    }

    @Override
    public void mouseDragged(final Point from, final Point to, ImageDisplay.VisRect currentVisibleRect) {
        if (from != null && to != null) {
            this.cameraPlane.setRotationFromDelta(from, to);
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
