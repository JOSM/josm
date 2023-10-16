// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Future;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.imagery.street_level.IImageEntry;
import org.openstreetmap.josm.data.imagery.street_level.Projections;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.DoubleProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.AbstractMapViewPaintable;
import org.openstreetmap.josm.gui.layer.geoimage.viewers.projections.IImageViewer;
import org.openstreetmap.josm.gui.layer.geoimage.viewers.projections.ImageProjectionRegistry;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings;
import org.openstreetmap.josm.gui.layer.imagery.ImageryFilterSettings.FilterChangeListener;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.imagery.Vector3D;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProcessor;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * GUI component to display an image (photograph).
 *
 * Offers basic mouse interaction (zoom, drag) and on-screen text.
 * @since 2566
 */
public class ImageDisplay extends JComponent implements Destroyable, PreferenceChangedListener, FilterChangeListener {

    /** The current image viewer */
    private IImageViewer iImageViewer;

    /** The file that is currently displayed */
    private IImageEntry<?> entry;

    /** The previous file that is currently displayed. Cleared on paint. Only used to help improve UI error information. */
    private IImageEntry<?> oldEntry;

    /** The image currently displayed */
    private transient BufferedImage image;

    /** The image currently displayed after applying {@link #imageProcessor} */
    private transient BufferedImage processedImage;

    /**
     * Process the image before it is being displayed
     */
    private final ImageProcessor imageProcessor;

    /** The image currently displayed */
    private boolean errorLoading;

    /** The rectangle (in image coordinates) of the image that is visible. This rectangle is calculated
     * each time the zoom is modified */
    private VisRect visibleRect;

    /** When a selection is done, the rectangle of the selection (in image coordinates) */
    private VisRect selectedRect;

    private final ImgDisplayMouseListener imgMouseListener = new ImgDisplayMouseListener();

    private String emptyText;
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

    /** Maximum width (in pixels) for loading images **/
    private static final IntegerProperty MAX_WIDTH =
        new IntegerProperty("geoimage.maximum-width", 6000);

    /** Show a background for the error text (may be hard on eyes) */
    private static final BooleanProperty ERROR_MESSAGE_BACKGROUND = new BooleanProperty("geoimage.message.error.background", false);

    private UpdateImageThread updateImageThreadInstance;

    private class UpdateImageThread extends Thread {
        private boolean restart;

        @SuppressWarnings("DoNotCall") // we are calling `run` from the thread we want it to be running on (aka recursive)
        @Override
        public void run() {
            updateProcessedImage();
            if (restart) {
                restart = false;
                run();
            }
        }

        public void restart() {
            restart = true;
            if (!isAlive()) {
                restart = false;
                updateImageThreadInstance = new UpdateImageThread();
                updateImageThreadInstance.start();
            }
        }
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e == null ||
            e.getKey().equals(AGPIFO_STYLE.getKey())) {
            dragButton = AGPIFO_STYLE.get() ? 1 : 3;
            zoomButton = dragButton == 1 ? 3 : 1;
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

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + Objects.hash(init);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj) || getClass() != obj.getClass())
                return false;
            VisRect other = (VisRect) obj;
            return Objects.equals(init, other.init);
        }
    }

    /** The thread that reads the images. */
    protected class LoadImageRunnable implements Runnable {

        private final IImageEntry<?> entry;

        LoadImageRunnable(IImageEntry<?> entry) {
            this.entry = entry;
        }

        @Override
        public void run() {
            try {
                Dimension target = new Dimension(MAX_WIDTH.get(), MAX_WIDTH.get());
                BufferedImage img = entry.read(target);
                if (img == null) {
                    synchronized (ImageDisplay.this) {
                        errorLoading = true;
                        ImageDisplay.this.repaint();
                        return;
                    }
                }

                int width = img.getWidth();
                int height = img.getHeight();
                entry.setWidth(width);
                entry.setHeight(height);

                synchronized (ImageDisplay.this) {
                    if (this.entry != ImageDisplay.this.entry) {
                        // The file has changed
                        return;
                    }

                    ImageDisplay.this.image = img;
                    updateProcessedImage();
                    // This will clear the loading info box
                    ImageDisplay.this.oldEntry = ImageDisplay.this.entry;
                    visibleRect = getIImageViewer(entry).getDefaultVisibleRectangle(ImageDisplay.this, image);

                    selectedRect = null;
                    errorLoading = false;
                }
                ImageDisplay.this.repaint();
            } catch (IOException ex) {
                Logging.error(ex);
            }
        }
    }

    private class ImgDisplayMouseListener extends MouseAdapter {

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
            IImageEntry<?> currentEntry;
            IImageViewer imageViewer;
            Image currentImage;
            VisRect currentVisibleRect;

            synchronized (ImageDisplay.this) {
                currentEntry = ImageDisplay.this.entry;
                currentImage = ImageDisplay.this.image;
                currentVisibleRect = ImageDisplay.this.visibleRect;
                imageViewer = ImageDisplay.this.iImageViewer;
            }

            selectedRect = null;

            if (currentImage == null)
                return;

            // Calculate the mouse cursor position in image coordinates to center the zoom.
            if (refreshMousePointInImg)
                mousePointInImg = comp2imgCoord(currentVisibleRect, x, y, getSize());

            // Apply the zoom to the visible rectangle in image coordinates
            if (rotation > 0) {
                currentVisibleRect.width = (int) (currentVisibleRect.width * ZOOM_STEP.get());
                currentVisibleRect.height = (int) (currentVisibleRect.height * ZOOM_STEP.get());
            } else if (rotation < 0) {
                currentVisibleRect.width = (int) (currentVisibleRect.width / ZOOM_STEP.get());
                currentVisibleRect.height = (int) (currentVisibleRect.height / ZOOM_STEP.get());
            } // else rotation == 0, which can happen with some modern trackpads (see #22770)

            // Check that the zoom doesn't exceed MAX_ZOOM:1
            ensureMaxZoom(currentVisibleRect);

            // The size of the visible rectangle is limited by the image size or the viewer implementation.
            if (imageViewer != null) {
                imageViewer.checkAndModifyVisibleRectSize(currentImage, currentVisibleRect);
            } else {
                currentVisibleRect.checkRectSize();
            }

            // Set the position of the visible rectangle, so that the mouse cursor doesn't move on the image.
            Rectangle drawRect = calculateDrawImageRectangle(currentVisibleRect, getSize());
            currentVisibleRect.x = mousePointInImg.x + ((drawRect.x - x) * currentVisibleRect.width) / drawRect.width;
            currentVisibleRect.y = mousePointInImg.y + ((drawRect.y - y) * currentVisibleRect.height) / drawRect.height;

            // The position is also limited by the image size
            currentVisibleRect.checkRectPos();

            synchronized (ImageDisplay.this) {
                if (ImageDisplay.this.entry == currentEntry) {
                    ImageDisplay.this.visibleRect = currentVisibleRect;
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
            IImageEntry<?> currentEntry;
            Image currentImage;
            VisRect currentVisibleRect;

            synchronized (ImageDisplay.this) {
                currentEntry = ImageDisplay.this.entry;
                currentImage = ImageDisplay.this.image;
                currentVisibleRect = ImageDisplay.this.visibleRect;
            }

            if (currentImage == null)
                return;

            if (ZOOM_ON_CLICK.get()) {
                // click notions are less coherent than wheel, refresh mousePointInImg on each click
                lastMouseEvent = null;

                if (mouseIsZoomSelecting(e) && !isAtMaxZoom(currentVisibleRect)) {
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
            Point click = comp2imgCoord(currentVisibleRect, e.getX(), e.getY(), getSize());
            Point center = getCenterImgCoord(currentVisibleRect);

            currentVisibleRect.x += click.x - center.x;
            currentVisibleRect.y += click.y - center.y;

            currentVisibleRect.checkRectPos();

            synchronized (ImageDisplay.this) {
                if (ImageDisplay.this.entry == currentEntry) {
                    ImageDisplay.this.visibleRect = currentVisibleRect;
                }
            }
            ImageDisplay.this.repaint();
        }

        /** Initialize the dragging, either with button 1 (simple dragging) or button 3 (selection of
         * a picture part) */
        @Override
        public void mousePressed(MouseEvent e) {
            Image currentImage;
            VisRect currentVisibleRect;

            synchronized (ImageDisplay.this) {
                currentImage = ImageDisplay.this.image;
                currentVisibleRect = ImageDisplay.this.visibleRect;
            }

            if (currentImage == null)
                return;

            selectedRect = null;

            if (mouseIsDragging(e) || mouseIsZoomSelecting(e))
                mousePointInImg = comp2imgCoord(currentVisibleRect, e.getX(), e.getY(), getSize());
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!mouseIsDragging(e) && !mouseIsZoomSelecting(e))
                return;

            IImageEntry<?> imageEntry;
            Image currentImage;
            VisRect currentVisibleRect;

            synchronized (ImageDisplay.this) {
                imageEntry = ImageDisplay.this.entry;
                currentImage = ImageDisplay.this.image;
                currentVisibleRect = ImageDisplay.this.visibleRect;
            }

            if (currentImage == null)
                return;

            if (mouseIsDragging(e) && mousePointInImg != null) {
                Point p = comp2imgCoord(currentVisibleRect, e.getX(), e.getY(), getSize());
                getIImageViewer(entry).mouseDragged(this.mousePointInImg, p, currentVisibleRect);
                currentVisibleRect.checkRectPos();
                synchronized (ImageDisplay.this) {
                    if (ImageDisplay.this.entry == imageEntry) {
                        ImageDisplay.this.visibleRect = currentVisibleRect;
                    }
                }
                // We have to update the mousePointInImg for 360 image panning, as otherwise the panning never stops.
                // This does not work well with the perspective viewer at this time (2021-08-26).
                boolean is360panning = entry != null && Projections.EQUIRECTANGULAR == entry.getProjectionType();
                if (is360panning) {
                    this.mousePointInImg = p;
                }
                ImageDisplay.this.repaint();
                if (is360panning) {
                    // repaint direction arrow
                    MainApplication.getLayerManager().getLayersOfType(GeoImageLayer.class).forEach(AbstractMapViewPaintable::invalidate);
                }
            }

            if (mouseIsZoomSelecting(e) && mousePointInImg != null) {
                Point p = comp2imgCoord(currentVisibleRect, e.getX(), e.getY(), getSize());
                currentVisibleRect.checkPointInside(p);
                VisRect selectedRectTemp = new VisRect(
                        Math.min(p.x, mousePointInImg.x),
                        Math.min(p.y, mousePointInImg.y),
                        p.x < mousePointInImg.x ? mousePointInImg.x - p.x : p.x - mousePointInImg.x,
                        p.y < mousePointInImg.y ? mousePointInImg.y - p.y : p.y - mousePointInImg.y,
                        currentVisibleRect);
                selectedRectTemp.checkRectSize();
                selectedRectTemp.checkRectPos();
                ImageDisplay.this.selectedRect = selectedRectTemp;
                ImageDisplay.this.repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            IImageEntry<?> currentEntry;
            Image currentImage;
            VisRect currentVisibleRect;

            synchronized (ImageDisplay.this) {
                currentEntry = ImageDisplay.this.entry;
                currentImage = ImageDisplay.this.image;
                currentVisibleRect = ImageDisplay.this.visibleRect;
            }

            if (currentImage == null)
                return;

            if (mouseIsDragging(e)) {
                currentVisibleRect.isDragUpdate = false;
            }

            if (mouseIsZoomSelecting(e) && selectedRect != null) {
                int oldWidth = selectedRect.width;
                int oldHeight = selectedRect.height;

                // Check that the zoom doesn't exceed MAX_ZOOM:1
                ensureMaxZoom(selectedRect);

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
                if (currentEntry == ImageDisplay.this.entry) {
                    if (selectedRect == null) {
                        ImageDisplay.this.visibleRect = currentVisibleRect;
                    } else {
                        ImageDisplay.this.visibleRect.setBounds(selectedRect);
                        selectedRect = null;
                    }
                }
            }
            ImageDisplay.this.repaint();
        }
    }

    /**
     * Constructs a new {@code ImageDisplay} with no image processor.
     */
    public ImageDisplay() {
        this(imageObject -> imageObject);
    }

    /**
     * Constructs a new {@code ImageDisplay} with a given image processor.
     * @param imageProcessor image processor
     * @since 17740
     */
    public ImageDisplay(ImageProcessor imageProcessor) {
        addMouseListener(imgMouseListener);
        addMouseWheelListener(imgMouseListener);
        addMouseMotionListener(imgMouseListener);
        Config.getPref().addPreferenceChangeListener(this);
        preferenceChanged(null);
        this.imageProcessor = imageProcessor;
        if (imageProcessor instanceof ImageryFilterSettings) {
            ((ImageryFilterSettings) imageProcessor).addFilterChangeListener(this);
        }
    }

    @Override
    public void destroy() {
        removeMouseListener(imgMouseListener);
        removeMouseWheelListener(imgMouseListener);
        removeMouseMotionListener(imgMouseListener);
        Config.getPref().removePreferenceChangeListener(this);
        if (imageProcessor instanceof ImageryFilterSettings) {
            ((ImageryFilterSettings) imageProcessor).removeFilterChangeListener(this);
        }
    }

    /**
     * Sets a new source image to be displayed by this {@code ImageDisplay}.
     * @param entry new source image
     * @return a {@link Future} representing pending completion of the image loading task
     * @since 18246 (signature)
     */
    public Future<?> setImage(IImageEntry<?> entry) {
        LoadImageRunnable runnable = setImage0(entry);
        return runnable != null && !MainApplication.worker.isShutdown() ? MainApplication.worker.submit(runnable) : null;
    }

    protected LoadImageRunnable setImage0(IImageEntry<?> entry) {
        synchronized (this) {
            this.oldEntry = this.entry;
            this.entry = entry;
            if (entry == null) {
                image = null;
                updateProcessedImage();
                this.oldEntry = null;
            }
            errorLoading = false;
        }
        repaint();
        return entry != null ? new LoadImageRunnable(entry) : null;
    }

    /**
     * Set the message displayed when there is no image to display.
     * By default it display a simple No image
     * @param emptyText the string to display
     * @since 15333
     */
    public void setEmptyText(String emptyText) {
        this.emptyText = emptyText;
    }

    /**
     * Sets the On-Screen-Display text.
     * @param text text to display on top of the image
     */
    public void setOsdText(String text) {
        if (!text.equals(this.osdText)) {
            this.osdText = text;
            repaint();
        }
    }

    @Override
    public void filterChanged() {
        if (updateImageThreadInstance != null) {
            updateImageThreadInstance.restart();
        } else {
            updateImageThreadInstance = new UpdateImageThread();
            updateImageThreadInstance.start();
        }
    }

    private void updateProcessedImage() {
        processedImage = image == null ? null : imageProcessor.process(image);
        GuiHelper.runInEDT(this::repaint);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        IImageEntry<?> currentEntry;
        IImageEntry<?> currentOldEntry;
        IImageViewer currentImageViewer;
        BufferedImage currentImage;
        boolean currentErrorLoading;

        synchronized (this) {
            currentImage = this.processedImage;
            currentEntry = this.entry;
            currentOldEntry = this.oldEntry;
            currentErrorLoading = this.errorLoading;
        }

        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        Dimension size = getSize();
        // Draw the image first, then draw error information
        if (currentImage != null && (currentEntry != null || currentOldEntry != null)) {
            currentImageViewer = this.getIImageViewer(currentEntry);
            // This must be after the getIImageViewer call, since we may be switching image viewers. This is important,
            // since an image viewer on switch may change the visible rectangle.
            VisRect currentVisibleRect;
            synchronized (this) {
                currentVisibleRect = this.visibleRect;
            }
            Rectangle r = new Rectangle(currentVisibleRect);
            Rectangle target = calculateDrawImageRectangle(currentVisibleRect, size);

            currentImageViewer.paintImage(g, currentImage, target, r);
            paintSelectedRect(g, target, currentVisibleRect, size);
            if (currentErrorLoading && currentEntry != null) {
                String loadingStr = tr("Error on file {0}", currentEntry.getDisplayName());
                Rectangle2D noImageSize = g.getFontMetrics(g.getFont()).getStringBounds(loadingStr, g);
                g.drawString(loadingStr, (int) ((size.width - noImageSize.getWidth()) / 2),
                        (int) ((size.height - noImageSize.getHeight()) / 2));
            }
            paintOsdText(g);
        }
        paintErrorMessage(g, currentEntry, currentOldEntry, currentImage, currentErrorLoading, size);
    }

    /**
     * Paint an error message
     * @param g The graphics to paint on
     * @param imageEntry The current image entry
     * @param oldImageEntry The old image entry
     * @param bufferedImage The image being painted
     * @param currentErrorLoading If there was an error loading the image
     * @param size The size of the component
     */
    private void paintErrorMessage(Graphics g, IImageEntry<?> imageEntry, IImageEntry<?> oldImageEntry,
            BufferedImage bufferedImage, boolean currentErrorLoading, Dimension size) {
        final String errorMessage;
        // If the new entry is null, then there is no image.
        if (imageEntry == null) {
            if (emptyText == null) {
                emptyText = tr("No image");
            }
            errorMessage = emptyText;
        } else if (bufferedImage == null || !Objects.equals(imageEntry, oldImageEntry)) {
            // The image is not necessarily null when loading anymore. If the oldEntry is not the same as the new entry,
            // we are probably still loading the image. (oldEntry gets set to entry when the image finishes loading).
            if (!currentErrorLoading) {
                errorMessage = tr("Loading {0}", imageEntry.getDisplayName());
            } else {
                errorMessage = tr("Error on file {0}", imageEntry.getDisplayName());
            }
        } else {
            errorMessage = null;
        }
        if (!Utils.isBlank(errorMessage)) {
            Rectangle2D errorStringSize = g.getFontMetrics(g.getFont()).getStringBounds(errorMessage, g);
            if (Boolean.TRUE.equals(ERROR_MESSAGE_BACKGROUND.get())) {
                int height = g.getFontMetrics().getHeight();
                int descender = g.getFontMetrics().getDescent();
                g.setColor(getBackground());
                int width = (int) (errorStringSize.getWidth() * 1);
                // top-left of text
                int tlx = (int) ((size.getWidth() - errorStringSize.getWidth()) / 2);
                int tly = (int) ((size.getHeight() - 3 * errorStringSize.getHeight()) / 2 + descender);
                g.fillRect(tlx, tly, width, height);
            }

            // lower-left of text
            int llx = (int) ((size.width - errorStringSize.getWidth()) / 2);
            int lly = (int) ((size.height - errorStringSize.getHeight()) / 2);
            g.setColor(getForeground());
            g.drawString(errorMessage, llx, lly);
        }
    }

    /**
     * Paint OSD text
     * @param g The graphics to paint on
     */
    private void paintOsdText(Graphics g) {
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

    /**
     * Paint the selected rectangle
     * @param g The graphics to paint on
     * @param target The target area (i.e., the selection)
     * @param visibleRectTemp The current visible rect
     * @param size The size of the component
     */
    private void paintSelectedRect(Graphics g, Rectangle target, VisRect visibleRectTemp, Dimension size) {
        if (selectedRect != null) {
            Point topLeft = img2compCoord(visibleRectTemp, selectedRect.x, selectedRect.y, size);
            Point bottomRight = img2compCoord(visibleRectTemp,
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

    /**
     * calculateDrawImageRectangle
     *
     * @param visibleRect the part of the image that should be drawn (in image coordinates)
     * @param compSize the part of the component where the image should be drawn (in component coordinates)
     * @return the part of compRect with the same width/height ratio as the image
     */
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
        IImageEntry<?> currentEntry;
        Image currentImage;
        VisRect currentVisibleRect;

        synchronized (this) {
            currentEntry = this.entry;
            currentImage = this.image;
            currentVisibleRect = this.visibleRect;
        }

        if (currentImage == null)
            return;

        if (currentVisibleRect.width != currentImage.getWidth(null) || currentVisibleRect.height != currentImage.getHeight(null)) {
            // The display is not at best fit. => Zoom to best fit
            currentVisibleRect.reset();
        } else {
            // The display is at best fit => zoom to 1:1
            Point center = getCenterImgCoord(currentVisibleRect);
            currentVisibleRect.setBounds(center.x - getWidth() / 2, center.y - getHeight() / 2,
                    getWidth(), getHeight());
            currentVisibleRect.checkRectSize();
            currentVisibleRect.checkRectPos();
        }

        synchronized (this) {
            if (this.entry == currentEntry) {
                this.visibleRect = currentVisibleRect;
            }
        }
        repaint();
    }

    /**
     * Get the image viewer for an entry
     * @param entry The entry to get the viewer for. May be {@code null}.
     * @return The new image viewer, may be {@code null}
     */
    private IImageViewer getIImageViewer(IImageEntry<?> entry) {
        IImageViewer imageViewer;
        IImageEntry<?> imageEntry;
        synchronized (this) {
            imageViewer = this.iImageViewer;
            imageEntry = entry == null ? this.entry : entry;
        }
        if (imageEntry == null || (imageViewer != null && imageViewer.getSupportedProjections().contains(imageEntry.getProjectionType()))) {
            return imageViewer;
        }
        try {
            imageViewer = ImageProjectionRegistry.getViewer(imageEntry.getProjectionType()).getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new JosmRuntimeException(e);
        }
        synchronized (this) {
            if (imageEntry.equals(this.entry)) {
                this.removeComponentListener(this.iImageViewer);
                this.iImageViewer = imageViewer;
                imageViewer.componentResized(new ComponentEvent(this, ComponentEvent.COMPONENT_RESIZED));
                this.addComponentListener(this.iImageViewer);
            }
        }
        return imageViewer;
    }

    /**
     * Get the rotation in the image viewer for an entry
     * @param entry The entry to get the rotation for. May be {@code null}.
     * @return the current rotation in the image viewer, or {@code null}
     * @since 18263
     */
    public Vector3D getRotation(IImageEntry<?> entry) {
        return entry != null ? getIImageViewer(entry).getRotation() : null;
    }

    /**
     * Ensure that a rectangle isn't zoomed in too much
     * @param rectangle The rectangle to get (typically the visible area)
     */
    private void ensureMaxZoom(final Rectangle rectangle) {
        if (rectangle.width < getSize().width / MAX_ZOOM.get()) {
            rectangle.width = (int) (getSize().width / MAX_ZOOM.get());
        }
        if (rectangle.height < getSize().height / MAX_ZOOM.get()) {
            rectangle.height = (int) (getSize().height / MAX_ZOOM.get());
        }

        // Set the same ratio for the visible rectangle and the display area
        int hFact = rectangle.height * getSize().width;
        int wFact = rectangle.width * getSize().height;
        if (hFact > wFact) {
            rectangle.width = hFact / getSize().height;
        } else {
            rectangle.height = wFact / getSize().width;
        }
    }

    /**
     * Update the visible rectangle (ensure zoom does not exceed specified values).
     * Specifically only visible for {@link IImageViewer} implementations.
     * @since 18246
     */
    public void updateVisibleRectangle() {
        final VisRect currentVisibleRect;
        final Image mouseImage;
        final IImageViewer iImageViewer;
        synchronized (this) {
            currentVisibleRect = this.visibleRect;
            mouseImage = this.image;
            iImageViewer = this.getIImageViewer(this.entry);
        }
        if (mouseImage != null && currentVisibleRect != null && iImageViewer != null) {
            final Image maxImageSize = iImageViewer.getMaxImageSize(this, mouseImage);
            final VisRect maxVisibleRect = new VisRect(0, 0, maxImageSize.getWidth(null), maxImageSize.getHeight(null));
            maxVisibleRect.setRect(currentVisibleRect);
            ensureMaxZoom(maxVisibleRect);

            maxVisibleRect.checkRectSize();
            synchronized (this) {
                this.visibleRect = maxVisibleRect;
            }
        }
    }
}
