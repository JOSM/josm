// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.DoubleProperty;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * GUI component to display an image (photograph).
 *
 * Offers basic mouse interaction (zoom, drag) and on-screen text.
 */
public class ImageDisplay extends JComponent implements Destroyable, PreferenceChangedListener {

    /** The file that is currently displayed */
    private ImageEntry entry;

    /** The image currently displayed */
    private transient Image image;

    /** The image currently displayed */
    private boolean errorLoading;

    /** The rectangle (in image coordinates) of the image that is visible. This rectangle is calculated
     * each time the zoom is modified */
    private VisRect visibleRect;

    /** When a selection is done, the rectangle of the selection (in image coordinates) */
    private VisRect selectedRect;

    /** The tracker to load the images */
    private final MediaTracker tracker = new MediaTracker(this);

    private final ImgDisplayMouseListener imgMouseListener = new ImgDisplayMouseListener();

    private String osdText;

    private static final BooleanProperty AGPIFO_STYLE =
        new BooleanProperty("geoimage.agpifo-style-drag-and-zoom", false);
    private static int dragButton;
    private static int zoomButton;

    /** Alternative to mouse wheel zoom; esp. handy if no mouse wheel is present **/
    private static final BooleanProperty ZOOM_ON_CLICK =
        new BooleanProperty("geoimage.use-mouse-clicks-to-zoom", true);

    /** Zoom factor when click or wheel zooming **/
    private static final DoubleProperty ZOOM_STEP =
        new DoubleProperty("geoimage.zoom-step-factor", 3 / 2.0);

    /** Maximum zoom allowed **/
    private static final DoubleProperty MAX_ZOOM =
        new DoubleProperty("geoimage.maximum-zoom-scale", 2.0);

    /** Use bilinear filtering **/
    private static final BooleanProperty BILIN_DOWNSAMP =
        new BooleanProperty("geoimage.bilinear-downsampling-progressive", true);
    private static final BooleanProperty BILIN_UPSAMP =
        new BooleanProperty("geoimage.bilinear-upsampling", false);
    private static double bilinUpper;
    private static double bilinLower;

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e == null ||
            e.getKey().equals(AGPIFO_STYLE.getKey())) {
            dragButton = AGPIFO_STYLE.get() ? 1 : 3;
            zoomButton = dragButton == 1 ? 3 : 1;
        }
        if (e == null ||
            e.getKey().equals(MAX_ZOOM.getKey()) ||
            e.getKey().equals(BILIN_DOWNSAMP.getKey()) ||
            e.getKey().equals(BILIN_UPSAMP.getKey())) {
            bilinUpper = (BILIN_UPSAMP.get() ? 2*MAX_ZOOM.get() : (BILIN_DOWNSAMP.get() ? 0.5 : 0));
            bilinLower = (BILIN_DOWNSAMP.get() ? 0 : 1);
        }
    }

    /**
     * Manage the visible rectangle of an image with full bounds stored in init.
     * @since 13127
     */
    public static class VisRect extends Rectangle {
        private final Rectangle init;

        /** set when this {@code VisRect} is updated by a mouse drag operation and
         * unset on mouse release **/
        public boolean isDragUpdate;

        /**
         * Constructs a new {@code VisRect}.
         * @param     x the specified X coordinate
         * @param     y the specified Y coordinate
         * @param     width  the width of the rectangle
         * @param     height the height of the rectangle
         */
        public VisRect(int x, int y, int width, int height) {
            super(x, y, width, height);
            init = new Rectangle(this);
        }

        /**
         * Constructs a new {@code VisRect}.
         * @param     x the specified X coordinate
         * @param     y the specified Y coordinate
         * @param     width  the width of the rectangle
         * @param     height the height of the rectangle
         * @param     peer share full bounds with this peer {@code VisRect}
         */
        public VisRect(int x, int y, int width, int height, VisRect peer) {
            super(x, y, width, height);
            init = peer.init;
        }

        /**
         * Constructs a new {@code VisRect} from another one.
         * @param v rectangle to copy
         */
        public VisRect(VisRect v) {
            super(v);
            init = v.init;
        }

        /**
         * Constructs a new empty {@code VisRect}.
         */
        public VisRect() {
            this(0, 0, 0, 0);
        }

        public boolean isFullView() {
            return init.equals(this);
        }

        public boolean isFullView1D() {
            return (init.x == x && init.width == width)
                || (init.y == y && init.height == height);
        }

        public void reset() {
            setBounds(init);
        }

        public void checkRectPos() {
            if (x < 0) {
                x = 0;
            }
            if (y < 0) {
                y = 0;
            }
            if (x + width > init.width) {
                x = init.width - width;
            }
            if (y + height > init.height) {
                y = init.height - height;
            }
        }

        public void checkRectSize() {
            if (width > init.width) {
                width = init.width;
            }
            if (height > init.height) {
                height = init.height;
            }
        }

        public void checkPointInside(Point p) {
            if (p.x < x) {
                p.x = x;
            }
            if (p.x > x + width) {
                p.x = x + width;
            }
            if (p.y < y) {
                p.y = y;
            }
            if (p.y > y + height) {
                p.y = y + height;
            }
        }
    }

    /** The thread that reads the images. */
    private class LoadImageRunnable implements Runnable, ImageObserver {

        private final ImageEntry entry;
        private final File file;

        LoadImageRunnable(ImageEntry entry) {
            this.entry = entry;
            this.file = entry.getFile();
        }

        @Override
        public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
            if (((infoflags & ImageObserver.WIDTH) == ImageObserver.WIDTH) &&
                ((infoflags & ImageObserver.HEIGHT) == ImageObserver.HEIGHT)) {
                synchronized (entry) {
                    entry.setWidth(width);
                    entry.setHeight(height);
                    entry.notifyAll();
                    return false;
                }
            }
            return true;
        }

        private boolean updateImageEntry(Image img) {
            if (!(entry.getWidth() > 0 && entry.getHeight() > 0)) {
                synchronized (entry) {
                    img.getWidth(this);
                    img.getHeight(this);

                    long now = System.currentTimeMillis();
                    while (!(entry.getWidth() > 0 && entry.getHeight() > 0)) {
                        try {
                            entry.wait(1000);
                            if (this.entry != ImageDisplay.this.entry)
                                return false;
                            if (System.currentTimeMillis() - now > 10000)
                                synchronized (ImageDisplay.this) {
                                    errorLoading = true;
                                    ImageDisplay.this.repaint();
                                    return false;
                                }
                        } catch (InterruptedException e) {
                            Logging.trace(e);
                            Logging.warn("InterruptedException in {0} while getting properties of image {1}",
                                    getClass().getSimpleName(), file.getPath());
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
            return true;
        }

        private boolean mayFitMemory(long amountWanted) {
            return amountWanted < (
                   Runtime.getRuntime().maxMemory() -
                   Runtime.getRuntime().totalMemory() +
                   Runtime.getRuntime().freeMemory());
        }

        @Override
        public void run() {
            Image img = Toolkit.getDefaultToolkit().createImage(file.getPath());
            if (!updateImageEntry(img))
                return;

            int width = entry.getWidth();
            int height = entry.getHeight();

            if (mayFitMemory(((long) width)*height*4*2)) {
                Logging.info("Loading {0} using default toolkit", file.getPath());
                tracker.addImage(img, 1);

                // Wait for the end of loading
                while (!tracker.checkID(1, true)) {
                    if (this.entry != ImageDisplay.this.entry) {
                        // The file has changed
                        tracker.removeImage(img);
                        return;
                    }
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Logging.trace(e);
                        Logging.warn("InterruptedException in {0} while loading image {1}",
                                getClass().getSimpleName(), file.getPath());
                        Thread.currentThread().interrupt();
                    }
                }
                if (tracker.isErrorID(1)) {
                    // the tracker catches OutOfMemory conditions
                    tracker.removeImage(img);
                    img = null;
                } else {
                    tracker.removeImage(img);
                }
            } else {
                img = null;
            }

            synchronized (ImageDisplay.this) {
                if (this.entry != ImageDisplay.this.entry) {
                    // The file has changed
                    return;
                }

                if (img != null) {
                    boolean switchedDim = false;
                    if (ExifReader.orientationNeedsCorrection(entry.getExifOrientation())) {
                        if (ExifReader.orientationSwitchesDimensions(entry.getExifOrientation())) {
                            width = img.getHeight(null);
                            height = img.getWidth(null);
                            switchedDim = true;
                        }
                        final BufferedImage rot = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                        final AffineTransform xform = ExifReader.getRestoreOrientationTransform(
                                entry.getExifOrientation(),
                                img.getWidth(null),
                                img.getHeight(null));
                        final Graphics2D g = rot.createGraphics();
                        g.drawImage(img, xform, null);
                        g.dispose();
                        img = rot;
                    }

                    ImageDisplay.this.image = img;
                    visibleRect = new VisRect(0, 0, width, height);

                    Logging.info("Loaded {0} with dimensions {1}x{2} memoryTaken={3}m exifOrientationSwitchedDimension={4}",
                            file.getPath(), width, height, width*height*4/1024/1024, switchedDim);
                }

                selectedRect = null;
                errorLoading = (img == null);
            }
            ImageDisplay.this.repaint();
        }
    }

    private class ImgDisplayMouseListener implements MouseListener, MouseWheelListener, MouseMotionListener {

        private MouseEvent lastMouseEvent;
        private Point mousePointInImg;

        private boolean mouseIsDragging(MouseEvent e) {
            return (dragButton == 1 && SwingUtilities.isLeftMouseButton(e)) ||
                   (dragButton == 2 && SwingUtilities.isMiddleMouseButton(e)) ||
                   (dragButton == 3 && SwingUtilities.isRightMouseButton(e));
        }

        private boolean mouseIsZoomSelecting(MouseEvent e) {
            return (zoomButton == 1 && SwingUtilities.isLeftMouseButton(e)) ||
                   (zoomButton == 2 && SwingUtilities.isMiddleMouseButton(e)) ||
                   (zoomButton == 3 && SwingUtilities.isRightMouseButton(e));
        }

        private boolean isAtMaxZoom(Rectangle visibleRect) {
            return (visibleRect.width == (int) (getSize().width / MAX_ZOOM.get()) ||
                    visibleRect.height == (int) (getSize().height / MAX_ZOOM.get()));
        }

        private void mouseWheelMovedImpl(int x, int y, int rotation, boolean refreshMousePointInImg) {
            ImageEntry entry;
            Image image;
            VisRect visibleRect;

            synchronized (ImageDisplay.this) {
                entry = ImageDisplay.this.entry;
                image = ImageDisplay.this.image;
                visibleRect = ImageDisplay.this.visibleRect;
            }

            selectedRect = null;

            if (image == null)
                return;

            // Calculate the mouse cursor position in image coordinates to center the zoom.
            if (refreshMousePointInImg)
                mousePointInImg = comp2imgCoord(visibleRect, x, y, getSize());

            // Apply the zoom to the visible rectangle in image coordinates
            if (rotation > 0) {
                visibleRect.width = (int) (visibleRect.width * ZOOM_STEP.get());
                visibleRect.height = (int) (visibleRect.height * ZOOM_STEP.get());
            } else {
                visibleRect.width = (int) (visibleRect.width / ZOOM_STEP.get());
                visibleRect.height = (int) (visibleRect.height / ZOOM_STEP.get());
            }

            // Check that the zoom doesn't exceed MAX_ZOOM:1
            if (visibleRect.width < getSize().width / MAX_ZOOM.get()) {
                visibleRect.width = (int) (getSize().width / MAX_ZOOM.get());
            }
            if (visibleRect.height < getSize().height / MAX_ZOOM.get()) {
                visibleRect.height = (int) (getSize().height / MAX_ZOOM.get());
            }

            // Set the same ratio for the visible rectangle and the display area
            int hFact = visibleRect.height * getSize().width;
            int wFact = visibleRect.width * getSize().height;
            if (hFact > wFact) {
                visibleRect.width = hFact / getSize().height;
            } else {
                visibleRect.height = wFact / getSize().width;
            }

            // The size of the visible rectangle is limited by the image size.
            visibleRect.checkRectSize();

            // Set the position of the visible rectangle, so that the mouse cursor doesn't move on the image.
            Rectangle drawRect = calculateDrawImageRectangle(visibleRect, getSize());
            visibleRect.x = mousePointInImg.x + ((drawRect.x - x) * visibleRect.width) / drawRect.width;
            visibleRect.y = mousePointInImg.y + ((drawRect.y - y) * visibleRect.height) / drawRect.height;

            // The position is also limited by the image size
            visibleRect.checkRectPos();

            synchronized (ImageDisplay.this) {
                if (ImageDisplay.this.entry == entry) {
                    ImageDisplay.this.visibleRect = visibleRect;
                }
            }
            ImageDisplay.this.repaint();
        }

        /** Zoom in and out, trying to preserve the point of the image that was under the mouse cursor
         * at the same place */
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            boolean refreshMousePointInImg = false;

            // To avoid issues when the user tries to zoom in on the image borders, this
            // point is not recalculated as long as e occurs at roughly the same position.
            if (lastMouseEvent == null || mousePointInImg == null ||
                ((lastMouseEvent.getX()-e.getX())*(lastMouseEvent.getX()-e.getX())
                +(lastMouseEvent.getY()-e.getY())*(lastMouseEvent.getY()-e.getY()) > 4*4)) {
                lastMouseEvent = e;
                refreshMousePointInImg = true;
            }

            mouseWheelMovedImpl(e.getX(), e.getY(), e.getWheelRotation(), refreshMousePointInImg);
        }

        /** Center the display on the point that has been clicked */
        @Override
        public void mouseClicked(MouseEvent e) {
            // Move the center to the clicked point.
            ImageEntry entry;
            Image image;
            VisRect visibleRect;

            synchronized (ImageDisplay.this) {
                entry = ImageDisplay.this.entry;
                image = ImageDisplay.this.image;
                visibleRect = ImageDisplay.this.visibleRect;
            }

            if (image == null)
                return;

            if (ZOOM_ON_CLICK.get()) {
                // click notions are less coherent than wheel, refresh mousePointInImg on each click
                lastMouseEvent = null;

                if (mouseIsZoomSelecting(e) && !isAtMaxZoom(visibleRect)) {
                    // zoom in if clicked with the zoom button
                    mouseWheelMovedImpl(e.getX(), e.getY(), -1, true);
                    return;
                }
                if (mouseIsDragging(e)) {
                    // zoom out if clicked with the drag button
                    mouseWheelMovedImpl(e.getX(), e.getY(), 1, true);
                    return;
                }
            }

            // Calculate the translation to set the clicked point the center of the view.
            Point click = comp2imgCoord(visibleRect, e.getX(), e.getY(), getSize());
            Point center = getCenterImgCoord(visibleRect);

            visibleRect.x += click.x - center.x;
            visibleRect.y += click.y - center.y;

            visibleRect.checkRectPos();

            synchronized (ImageDisplay.this) {
                if (ImageDisplay.this.entry == entry) {
                    ImageDisplay.this.visibleRect = visibleRect;
                }
            }
            ImageDisplay.this.repaint();
        }

        /** Initialize the dragging, either with button 1 (simple dragging) or button 3 (selection of
         * a picture part) */
        @Override
        public void mousePressed(MouseEvent e) {
            Image image;
            VisRect visibleRect;

            synchronized (ImageDisplay.this) {
                image = ImageDisplay.this.image;
                visibleRect = ImageDisplay.this.visibleRect;
            }

            if (image == null)
                return;

            selectedRect = null;

            if (mouseIsDragging(e) || mouseIsZoomSelecting(e))
                mousePointInImg = comp2imgCoord(visibleRect, e.getX(), e.getY(), getSize());
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!mouseIsDragging(e) && !mouseIsZoomSelecting(e))
                return;

            ImageEntry entry;
            Image image;
            VisRect visibleRect;

            synchronized (ImageDisplay.this) {
                entry = ImageDisplay.this.entry;
                image = ImageDisplay.this.image;
                visibleRect = ImageDisplay.this.visibleRect;
            }

            if (image == null)
                return;

            if (mouseIsDragging(e)) {
                Point p = comp2imgCoord(visibleRect, e.getX(), e.getY(), getSize());
                visibleRect.isDragUpdate = true;
                visibleRect.x += mousePointInImg.x - p.x;
                visibleRect.y += mousePointInImg.y - p.y;
                visibleRect.checkRectPos();
                synchronized (ImageDisplay.this) {
                    if (ImageDisplay.this.entry == entry) {
                        ImageDisplay.this.visibleRect = visibleRect;
                    }
                }
                ImageDisplay.this.repaint();
            }

            if (mouseIsZoomSelecting(e)) {
                Point p = comp2imgCoord(visibleRect, e.getX(), e.getY(), getSize());
                visibleRect.checkPointInside(p);
                VisRect selectedRect = new VisRect(
                        p.x < mousePointInImg.x ? p.x : mousePointInImg.x,
                        p.y < mousePointInImg.y ? p.y : mousePointInImg.y,
                        p.x < mousePointInImg.x ? mousePointInImg.x - p.x : p.x - mousePointInImg.x,
                        p.y < mousePointInImg.y ? mousePointInImg.y - p.y : p.y - mousePointInImg.y,
                        visibleRect);
                selectedRect.checkRectSize();
                selectedRect.checkRectPos();
                ImageDisplay.this.selectedRect = selectedRect;
                ImageDisplay.this.repaint();
            }

        }

        @Override
        public void mouseReleased(MouseEvent e) {
            ImageEntry entry;
            Image image;
            VisRect visibleRect;

            synchronized (ImageDisplay.this) {
                entry = ImageDisplay.this.entry;
                image = ImageDisplay.this.image;
                visibleRect = ImageDisplay.this.visibleRect;
            }

            if (image == null)
                return;

            if (mouseIsDragging(e)) {
                visibleRect.isDragUpdate = false;
            }

            if (mouseIsZoomSelecting(e) && selectedRect != null) {
                int oldWidth = selectedRect.width;
                int oldHeight = selectedRect.height;

                // Check that the zoom doesn't exceed MAX_ZOOM:1
                if (selectedRect.width < getSize().width / MAX_ZOOM.get()) {
                    selectedRect.width = (int) (getSize().width / MAX_ZOOM.get());
                }
                if (selectedRect.height < getSize().height / MAX_ZOOM.get()) {
                    selectedRect.height = (int) (getSize().height / MAX_ZOOM.get());
                }

                // Set the same ratio for the visible rectangle and the display area
                int hFact = selectedRect.height * getSize().width;
                int wFact = selectedRect.width * getSize().height;
                if (hFact > wFact) {
                    selectedRect.width = hFact / getSize().height;
                } else {
                    selectedRect.height = wFact / getSize().width;
                }

                // Keep the center of the selection
                if (selectedRect.width != oldWidth) {
                    selectedRect.x -= (selectedRect.width - oldWidth) / 2;
                }
                if (selectedRect.height != oldHeight) {
                    selectedRect.y -= (selectedRect.height - oldHeight) / 2;
                }

                selectedRect.checkRectSize();
                selectedRect.checkRectPos();
            }

            synchronized (ImageDisplay.this) {
                if (entry == ImageDisplay.this.entry) {
                    if (selectedRect == null) {
                        ImageDisplay.this.visibleRect = visibleRect;
                    } else {
                        ImageDisplay.this.visibleRect.setBounds(selectedRect);
                        selectedRect = null;
                    }
                }
            }
            ImageDisplay.this.repaint();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // Do nothing
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // Do nothing
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // Do nothing
        }
    }

    /**
     * Constructs a new {@code ImageDisplay}.
     */
    public ImageDisplay() {
        addMouseListener(imgMouseListener);
        addMouseWheelListener(imgMouseListener);
        addMouseMotionListener(imgMouseListener);
        Config.getPref().addPreferenceChangeListener(this);
        preferenceChanged(null);
    }

    @Override
    public void destroy() {
        removeMouseListener(imgMouseListener);
        removeMouseWheelListener(imgMouseListener);
        removeMouseMotionListener(imgMouseListener);
        Config.getPref().removePreferenceChangeListener(this);
    }

    /**
     * Sets a new source image to be displayed by this {@code ImageDisplay}.
     * @param entry new source image
     * @since 13220
     */
    public void setImage(ImageEntry entry) {
        synchronized (this) {
            this.entry = entry;
            image = null;
            errorLoading = false;
        }
        repaint();
        if (entry != null) {
            new Thread(new LoadImageRunnable(entry), LoadImageRunnable.class.getName()).start();
        }
    }

    /**
     * Sets the On-Screen-Display text.
     * @param text text to display on top of the image
     */
    public void setOsdText(String text) {
        this.osdText = text;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        ImageEntry entry;
        Image image;
        VisRect visibleRect;
        boolean errorLoading;

        synchronized (this) {
            image = this.image;
            entry = this.entry;
            visibleRect = this.visibleRect;
            errorLoading = this.errorLoading;
        }

        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        Dimension size = getSize();
        if (entry == null) {
            g.setColor(Color.black);
            String noImageStr = tr("No image");
            Rectangle2D noImageSize = g.getFontMetrics(g.getFont()).getStringBounds(noImageStr, g);
            g.drawString(noImageStr,
                    (int) ((size.width - noImageSize.getWidth()) / 2),
                    (int) ((size.height - noImageSize.getHeight()) / 2));
        } else if (image == null) {
            g.setColor(Color.black);
            String loadingStr;
            if (!errorLoading) {
                loadingStr = tr("Loading {0}", entry.getFile().getName());
            } else {
                loadingStr = tr("Error on file {0}", entry.getFile().getName());
            }
            Rectangle2D noImageSize = g.getFontMetrics(g.getFont()).getStringBounds(loadingStr, g);
            g.drawString(loadingStr,
                    (int) ((size.width - noImageSize.getWidth()) / 2),
                    (int) ((size.height - noImageSize.getHeight()) / 2));
        } else {
            Rectangle r = new Rectangle(visibleRect);
            Rectangle target = calculateDrawImageRectangle(visibleRect, size);
            double scale = target.width / (double) r.width; // pixel ratio is 1:1

            if (selectedRect == null && !visibleRect.isDragUpdate &&
                bilinLower < scale && scale < bilinUpper) {
                try {
                    BufferedImage bi = ImageProvider.toBufferedImage(image, r);
                    if (bi != null) {
                        r.x = r.y = 0;

                        // See https://community.oracle.com/docs/DOC-983611 - The Perils of Image.getScaledInstance()
                        // Pre-scale image when downscaling by more than two times to avoid aliasing from default algorithm
                        bi = ImageProvider.createScaledImage(bi, target.width, target.height,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        r.width = target.width;
                        r.height = target.height;
                        image = bi;
                    }
                } catch (OutOfMemoryError oom) {
                    // fall-back to the non-bilinear scaler
                    r.x = visibleRect.x;
                    r.y = visibleRect.y;
                }
            } else {
                // if target and r cause drawImage to scale image region to a tmp buffer exceeding
                // its bounds, it will silently fail; crop with r first in such cases
                // (might be impl. dependent, exhibited by openjdk 1.8.0_151)
                if (scale*(r.x+r.width) > Short.MAX_VALUE || scale*(r.y+r.height) > Short.MAX_VALUE) {
                    image = ImageProvider.toBufferedImage(image, r);
                    r.x = r.y = 0;
                }
            }

            g.drawImage(image,
                    target.x, target.y, target.x + target.width, target.y + target.height,
                    r.x, r.y, r.x + r.width, r.y + r.height, null);

            if (selectedRect != null) {
                Point topLeft = img2compCoord(visibleRect, selectedRect.x, selectedRect.y, size);
                Point bottomRight = img2compCoord(visibleRect,
                        selectedRect.x + selectedRect.width,
                        selectedRect.y + selectedRect.height, size);
                g.setColor(new Color(128, 128, 128, 180));
                g.fillRect(target.x, target.y, target.width, topLeft.y - target.y);
                g.fillRect(target.x, target.y, topLeft.x - target.x, target.height);
                g.fillRect(bottomRight.x, target.y, target.x + target.width - bottomRight.x, target.height);
                g.fillRect(target.x, bottomRight.y, target.width, target.y + target.height - bottomRight.y);
                g.setColor(Color.black);
                g.drawRect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
            }
            if (errorLoading) {
                String loadingStr = tr("Error on file {0}", entry.getFile().getName());
                Rectangle2D noImageSize = g.getFontMetrics(g.getFont()).getStringBounds(loadingStr, g);
                g.drawString(loadingStr,
                        (int) ((size.width - noImageSize.getWidth()) / 2),
                        (int) ((size.height - noImageSize.getHeight()) / 2));
            }
            if (osdText != null) {
                FontMetrics metrics = g.getFontMetrics(g.getFont());
                int ascent = metrics.getAscent();
                Color bkground = new Color(255, 255, 255, 128);
                int lastPos = 0;
                int pos = osdText.indexOf('\n');
                int x = 3;
                int y = 3;
                String line;
                while (pos > 0) {
                    line = osdText.substring(lastPos, pos);
                    Rectangle2D lineSize = metrics.getStringBounds(line, g);
                    g.setColor(bkground);
                    g.fillRect(x, y, (int) lineSize.getWidth(), (int) lineSize.getHeight());
                    g.setColor(Color.black);
                    g.drawString(line, x, y + ascent);
                    y += (int) lineSize.getHeight();
                    lastPos = pos + 1;
                    pos = osdText.indexOf('\n', lastPos);
                }

                line = osdText.substring(lastPos);
                Rectangle2D lineSize = g.getFontMetrics(g.getFont()).getStringBounds(line, g);
                g.setColor(bkground);
                g.fillRect(x, y, (int) lineSize.getWidth(), (int) lineSize.getHeight());
                g.setColor(Color.black);
                g.drawString(line, x, y + ascent);
            }
        }
    }

    static Point img2compCoord(VisRect visibleRect, int xImg, int yImg, Dimension compSize) {
        Rectangle drawRect = calculateDrawImageRectangle(visibleRect, compSize);
        return new Point(drawRect.x + ((xImg - visibleRect.x) * drawRect.width) / visibleRect.width,
                drawRect.y + ((yImg - visibleRect.y) * drawRect.height) / visibleRect.height);
    }

    static Point comp2imgCoord(VisRect visibleRect, int xComp, int yComp, Dimension compSize) {
        Rectangle drawRect = calculateDrawImageRectangle(visibleRect, compSize);
        Point p = new Point(
                        ((xComp - drawRect.x) * visibleRect.width),
                        ((yComp - drawRect.y) * visibleRect.height));
        p.x += (((p.x % drawRect.width) << 1) >= drawRect.width) ? drawRect.width : 0;
        p.y += (((p.y % drawRect.height) << 1) >= drawRect.height) ? drawRect.height : 0;
        p.x = visibleRect.x + p.x / drawRect.width;
        p.y = visibleRect.y + p.y / drawRect.height;
        return p;
    }

    static Point getCenterImgCoord(Rectangle visibleRect) {
        return new Point(visibleRect.x + visibleRect.width / 2,
                         visibleRect.y + visibleRect.height / 2);
    }

    static VisRect calculateDrawImageRectangle(VisRect visibleRect, Dimension compSize) {
        return calculateDrawImageRectangle(visibleRect, new Rectangle(0, 0, compSize.width, compSize.height));
    }

    /**
     * calculateDrawImageRectangle
     *
     * @param imgRect the part of the image that should be drawn (in image coordinates)
     * @param compRect the part of the component where the image should be drawn (in component coordinates)
     * @return the part of compRect with the same width/height ratio as the image
     */
    static VisRect calculateDrawImageRectangle(VisRect imgRect, Rectangle compRect) {
        int x = 0;
        int y = 0;
        int w = compRect.width;
        int h = compRect.height;

        int wFact = w * imgRect.height;
        int hFact = h * imgRect.width;
        if (wFact != hFact) {
            if (wFact > hFact) {
                w = hFact / imgRect.height;
                x = (compRect.width - w) / 2;
            } else {
                h = wFact / imgRect.width;
                y = (compRect.height - h) / 2;
            }
        }

        // overscan to prevent empty edges when zooming in to zoom scales > 2:1
        if (w > imgRect.width && h > imgRect.height && !imgRect.isFullView1D() && wFact != hFact) {
            if (wFact > hFact) {
                w = compRect.width;
                x = 0;
                h = wFact / imgRect.width;
                y = (compRect.height - h) / 2;
            } else {
                h = compRect.height;
                y = 0;
                w = hFact / imgRect.height;
                x = (compRect.width - w) / 2;
            }
        }

        return new VisRect(x + compRect.x, y + compRect.y, w, h, imgRect);
    }

    /**
     * Make the current image either scale to fit inside this component,
     * or show a portion of image (1:1), if the image size is larger than
     * the component size.
     */
    public void zoomBestFitOrOne() {
        ImageEntry entry;
        Image image;
        VisRect visibleRect;

        synchronized (this) {
            entry = this.entry;
            image = this.image;
            visibleRect = this.visibleRect;
        }

        if (image == null)
            return;

        if (visibleRect.width != image.getWidth(null) || visibleRect.height != image.getHeight(null)) {
            // The display is not at best fit. => Zoom to best fit
            visibleRect.reset();
        } else {
            // The display is at best fit => zoom to 1:1
            Point center = getCenterImgCoord(visibleRect);
            visibleRect.setBounds(center.x - getWidth() / 2, center.y - getHeight() / 2,
                    getWidth(), getHeight());
            visibleRect.checkRectSize();
            visibleRect.checkRectPos();
        }

        synchronized (this) {
            if (this.entry == entry) {
                this.visibleRect = visibleRect;
            }
        }
        repaint();
    }
}
