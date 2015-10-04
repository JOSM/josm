// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.LassoModeAction;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.SelectAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrame.MapModeChangeListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToMarkerLayer;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToNextMarker;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToPreviousMarker;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.JpgImporter;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.lang.CompoundException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;

/**
 * Layer displaying geottaged pictures.
 */
public class GeoImageLayer extends Layer implements PropertyChangeListener, JumpToMarkerLayer {

    List<ImageEntry> data;
    GpxLayer gpxLayer;

    private Icon icon = ImageProvider.get("dialogs/geoimage/photo-marker");
    private Icon selectedIcon = ImageProvider.get("dialogs/geoimage/photo-marker-selected");

    private int currentPhoto = -1;

    boolean useThumbs = false;
    private ExecutorService thumbsLoaderExecutor =
            Executors.newSingleThreadExecutor(Utils.newThreadFactory("thumbnail-loader-%d", Thread.MIN_PRIORITY));
    private ThumbsLoader thumbsloader;
    private boolean thumbsLoaderRunning = false;
    volatile boolean thumbsLoaded = false;
    private BufferedImage offscreenBuffer;
    boolean updateOffscreenBuffer = true;

    /** Loads a set of images, while displaying a dialog that indicates what the plugin is currently doing.
     * In facts, this object is instantiated with a list of files. These files may be JPEG files or
     * directories. In case of directories, they are scanned to find all the images they contain.
     * Then all the images that have be found are loaded as ImageEntry instances.
     */
    private static final class Loader extends PleaseWaitRunnable {

        private boolean canceled = false;
        private GeoImageLayer layer;
        private Collection<File> selection;
        private Set<String> loadedDirectories = new HashSet<>();
        private Set<String> errorMessages;
        private GpxLayer gpxLayer;

        protected void rememberError(String message) {
            this.errorMessages.add(message);
        }

        public Loader(Collection<File> selection, GpxLayer gpxLayer) {
            super(tr("Extracting GPS locations from EXIF"));
            this.selection = selection;
            this.gpxLayer = gpxLayer;
            errorMessages = new LinkedHashSet<>();
        }

        @Override protected void realRun() throws IOException {

            progressMonitor.subTask(tr("Starting directory scan"));
            Collection<File> files = new ArrayList<>();
            try {
                addRecursiveFiles(files, selection);
            } catch (IllegalStateException e) {
                rememberError(e.getMessage());
            }

            if (canceled)
                return;
            progressMonitor.subTask(tr("Read photos..."));
            progressMonitor.setTicksCount(files.size());

            progressMonitor.subTask(tr("Read photos..."));
            progressMonitor.setTicksCount(files.size());

            // read the image files
            List<ImageEntry> data = new ArrayList<>(files.size());

            for (File f : files) {

                if (canceled) {
                    break;
                }

                progressMonitor.subTask(tr("Reading {0}...", f.getName()));
                progressMonitor.worked(1);

                ImageEntry e = new ImageEntry();

                // Changed to silently cope with no time info in exif. One case
                // of person having time that couldn't be parsed, but valid GPS info

                try {
                    e.setExifTime(ExifReader.readTime(f));
                } catch (ParseException ex) {
                    e.setExifTime(null);
                }
                e.setFile(f);
                extractExif(e);
                data.add(e);
            }
            layer = new GeoImageLayer(data, gpxLayer);
            files.clear();
        }

        private void addRecursiveFiles(Collection<File> files, Collection<File> sel) {
            boolean nullFile = false;

            for (File f : sel) {

                if (canceled) {
                    break;
                }

                if (f == null) {
                    nullFile = true;

                } else if (f.isDirectory()) {
                    String canonical = null;
                    try {
                        canonical = f.getCanonicalPath();
                    } catch (IOException e) {
                        Main.error(e);
                        rememberError(tr("Unable to get canonical path for directory {0}\n",
                                f.getAbsolutePath()));
                    }

                    if (canonical == null || loadedDirectories.contains(canonical)) {
                        continue;
                    } else {
                        loadedDirectories.add(canonical);
                    }

                    File[] children = f.listFiles(JpgImporter.FILE_FILTER_WITH_FOLDERS);
                    if (children != null) {
                        progressMonitor.subTask(tr("Scanning directory {0}", f.getPath()));
                        addRecursiveFiles(files, Arrays.asList(children));
                    } else {
                        rememberError(tr("Error while getting files from directory {0}\n", f.getPath()));
                    }

                } else {
                    files.add(f);
                }
            }

            if (nullFile) {
                throw new IllegalStateException(tr("One of the selected files was null"));
            }
        }

        protected String formatErrorMessages() {
            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            if (errorMessages.size() == 1) {
                sb.append(errorMessages.iterator().next());
            } else {
                sb.append(Utils.joinAsHtmlUnorderedList(errorMessages));
            }
            sb.append("</html>");
            return sb.toString();
        }

        @Override protected void finish() {
            if (!errorMessages.isEmpty()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        formatErrorMessages(),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                        );
            }
            if (layer != null) {
                Main.main.addLayer(layer);

                if (!canceled && layer.data != null && !layer.data.isEmpty()) {
                    boolean noGeotagFound = true;
                    for (ImageEntry e : layer.data) {
                        if (e.getPos() != null) {
                            noGeotagFound = false;
                        }
                    }
                    if (noGeotagFound) {
                        new CorrelateGpxWithImages(layer).actionPerformed(null);
                    }
                }
            }
        }

        @Override protected void cancel() {
            canceled = true;
        }
    }

    public static void create(Collection<File> files, GpxLayer gpxLayer) {
        Loader loader = new Loader(files, gpxLayer);
        Main.worker.execute(loader);
    }

    /**
     * Constructs a new {@code GeoImageLayer}.
     * @param data The list of images to display
     * @param gpxLayer The associated GPX layer
     */
    public GeoImageLayer(final List<ImageEntry> data, GpxLayer gpxLayer) {
        this(data, gpxLayer, null, false);
    }

    /**
     * Constructs a new {@code GeoImageLayer}.
     * @param data The list of images to display
     * @param gpxLayer The associated GPX layer
     * @param name Layer name
     * @since 6392
     */
    public GeoImageLayer(final List<ImageEntry> data, GpxLayer gpxLayer, final String name) {
        this(data, gpxLayer, name, false);
    }

    /**
     * Constructs a new {@code GeoImageLayer}.
     * @param data The list of images to display
     * @param gpxLayer The associated GPX layer
     * @param useThumbs Thumbnail display flag
     * @since 6392
     */
    public GeoImageLayer(final List<ImageEntry> data, GpxLayer gpxLayer, boolean useThumbs) {
        this(data, gpxLayer, null, useThumbs);
    }

    /**
     * Constructs a new {@code GeoImageLayer}.
     * @param data The list of images to display
     * @param gpxLayer The associated GPX layer
     * @param name Layer name
     * @param useThumbs Thumbnail display flag
     * @since 6392
     */
    public GeoImageLayer(final List<ImageEntry> data, GpxLayer gpxLayer, final String name, boolean useThumbs) {
        super(name != null ? name : tr("Geotagged Images"));
        if (data != null) {
            Collections.sort(data);
        }
        this.data = data;
        this.gpxLayer = gpxLayer;
        this.useThumbs = useThumbs;
    }

    @Override
    public Icon getIcon() {
        return ImageProvider.get("dialogs/geoimage");
    }

    private static List<Action> menuAdditions = new LinkedList<>();

    public static void registerMenuAddition(Action addition) {
        menuAdditions.add(addition);
    }

    @Override
    public Action[] getMenuEntries() {

        List<Action> entries = new ArrayList<>();
        entries.add(LayerListDialog.getInstance().createShowHideLayerAction());
        entries.add(LayerListDialog.getInstance().createDeleteLayerAction());
        entries.add(LayerListDialog.getInstance().createMergeLayerAction(this));
        entries.add(new RenameLayerAction(null, this));
        entries.add(SeparatorLayerAction.INSTANCE);
        entries.add(new CorrelateGpxWithImages(this));
        entries.add(new ShowThumbnailAction(this));
        if (!menuAdditions.isEmpty()) {
            entries.add(SeparatorLayerAction.INSTANCE);
            entries.addAll(menuAdditions);
        }
        entries.add(SeparatorLayerAction.INSTANCE);
        entries.add(new JumpToNextMarker(this));
        entries.add(new JumpToPreviousMarker(this));
        entries.add(SeparatorLayerAction.INSTANCE);
        entries.add(new LayerListPopup.InfoAction(this));

        return entries.toArray(new Action[entries.size()]);

    }

    /**
     * Prepare the string that is displayed if layer information is requested.
     * @return String with layer information
     */
    private String infoText() {
        int tagged = 0;
        int newdata = 0;
        int n = 0;
        if (data != null) {
            n = data.size();
            for (ImageEntry e : data) {
                if (e.getPos() != null) {
                    tagged++;
                }
                if (e.hasNewGpsData()) {
                    newdata++;
                }
            }
        }
        return "<html>"
                + trn("{0} image loaded.", "{0} images loaded.", n, n)
                + " " + trn("{0} was found to be GPS tagged.", "{0} were found to be GPS tagged.", tagged, tagged)
                + (newdata > 0 ? "<br>" + trn("{0} has updated GPS data.", "{0} have updated GPS data.", newdata, newdata) : "")
                + "</html>";
    }

    @Override public Object getInfoComponent() {
        return infoText();
    }

    @Override
    public String getToolTipText() {
        return infoText();
    }

    @Override
    public boolean isMergable(Layer other) {
        return other instanceof GeoImageLayer;
    }

    @Override
    public void mergeFrom(Layer from) {
        GeoImageLayer l = (GeoImageLayer) from;

        // Stop to load thumbnails on both layers.  Thumbnail loading will continue the next time
        // the layer is painted.
        stopLoadThumbs();
        l.stopLoadThumbs();

        final ImageEntry selected = l.data != null && l.currentPhoto >= 0 ? l.data.get(l.currentPhoto) : null;

        if (l.data != null) {
            data.addAll(l.data);
        }
        Collections.sort(data);

        // Supress the double photos.
        if (data.size() > 1) {
            ImageEntry cur;
            ImageEntry prev = data.get(data.size() - 1);
            for (int i = data.size() - 2; i >= 0; i--) {
                cur = data.get(i);
                if (cur.getFile().equals(prev.getFile())) {
                    data.remove(i);
                } else {
                    prev = cur;
                }
            }
        }

        if (selected != null && !data.isEmpty()) {
            GuiHelper.runInEDTAndWait(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < data.size(); i++) {
                        if (selected.equals(data.get(i))) {
                            currentPhoto = i;
                            ImageViewerDialog.showImage(GeoImageLayer.this, data.get(i));
                            break;
                        }
                    }
                }
            });
        }

        setName(l.getName());
        thumbsLoaded &= l.thumbsLoaded;
    }

    private Dimension scaledDimension(Image thumb) {
        final double d = Main.map.mapView.getDist100Pixel();
        final double size = 10 /*meter*/;     /* size of the photo on the map */
        double s = size * 100 /*px*/ / d;

        final double sMin = ThumbsLoader.minSize;
        final double sMax = ThumbsLoader.maxSize;

        if (s < sMin) {
            s = sMin;
        }
        if (s > sMax) {
            s = sMax;
        }
        final double f = s / sMax;  /* scale factor */

        if (thumb == null)
            return null;

        return new Dimension(
                (int) Math.round(f * thumb.getWidth(null)),
                (int) Math.round(f * thumb.getHeight(null)));
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bounds) {
        int width = mv.getWidth();
        int height = mv.getHeight();
        Rectangle clip = g.getClipBounds();
        if (useThumbs) {
            if (!thumbsLoaded) {
                startLoadThumbs();
            }

            if (null == offscreenBuffer || offscreenBuffer.getWidth() != width  // reuse the old buffer if possible
                    || offscreenBuffer.getHeight() != height) {
                offscreenBuffer = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_ARGB);
                updateOffscreenBuffer = true;
            }

            if (updateOffscreenBuffer) {
                Graphics2D tempG = offscreenBuffer.createGraphics();
                tempG.setColor(new Color(0, 0, 0, 0));
                Composite saveComp = tempG.getComposite();
                tempG.setComposite(AlphaComposite.Clear);   // remove the old images
                tempG.fillRect(0, 0, width, height);
                tempG.setComposite(saveComp);

                if (data != null) {
                    for (ImageEntry e : data) {
                        if (e.getPos() == null) {
                            continue;
                        }
                        Point p = mv.getPoint(e.getPos());
                        if (e.thumbnail != null) {
                            Dimension d = scaledDimension(e.thumbnail);
                            Rectangle target = new Rectangle(p.x - d.width / 2, p.y - d.height / 2, d.width, d.height);
                            if (clip.intersects(target)) {
                                tempG.drawImage(e.thumbnail, target.x, target.y, target.width, target.height, null);
                            }
                        } else { // thumbnail not loaded yet
                            icon.paintIcon(mv, tempG,
                                    p.x - icon.getIconWidth() / 2,
                                    p.y - icon.getIconHeight() / 2);
                        }
                    }
                }
                updateOffscreenBuffer = false;
            }
            g.drawImage(offscreenBuffer, 0, 0, null);
        } else if (data != null) {
            for (ImageEntry e : data) {
                if (e.getPos() == null) {
                    continue;
                }
                Point p = mv.getPoint(e.getPos());
                icon.paintIcon(mv, g,
                        p.x - icon.getIconWidth() / 2,
                        p.y - icon.getIconHeight() / 2);
            }
        }

        if (currentPhoto >= 0 && currentPhoto < data.size()) {
            ImageEntry e = data.get(currentPhoto);

            if (e.getPos() != null) {
                Point p = mv.getPoint(e.getPos());

                int imgWidth = 100;
                int imgHeight = 100;
                if (useThumbs && e.thumbnail != null) {
                    Dimension d = scaledDimension(e.thumbnail);
                    imgWidth = d.width;
                    imgHeight = d.height;
                } else {
                    imgWidth = selectedIcon.getIconWidth();
                    imgHeight = selectedIcon.getIconHeight();
                }

                if (e.getExifImgDir() != null) {
                    // Multiplier must be larger than sqrt(2)/2=0.71.
                    double arrowlength = Math.max(25, Math.max(imgWidth, imgHeight) * 0.85);
                    double arrowwidth = arrowlength / 1.4;

                    double dir = e.getExifImgDir();
                    // Rotate 90 degrees CCW
                    double headdir = (dir < 90) ? dir + 270 : dir - 90;
                    double leftdir = (headdir < 90) ? headdir + 270 : headdir - 90;
                    double rightdir = (headdir > 270) ? headdir - 270 : headdir + 90;

                    double ptx = p.x + Math.cos(Math.toRadians(headdir)) * arrowlength;
                    double pty = p.y + Math.sin(Math.toRadians(headdir)) * arrowlength;

                    double ltx = p.x + Math.cos(Math.toRadians(leftdir)) * arrowwidth/2;
                    double lty = p.y + Math.sin(Math.toRadians(leftdir)) * arrowwidth/2;

                    double rtx = p.x + Math.cos(Math.toRadians(rightdir)) * arrowwidth/2;
                    double rty = p.y + Math.sin(Math.toRadians(rightdir)) * arrowwidth/2;

                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(new Color(255, 255, 255, 192));
                    int[] xar = {(int) ltx, (int) ptx, (int) rtx, (int) ltx};
                    int[] yar = {(int) lty, (int) pty, (int) rty, (int) lty};
                    g.fillPolygon(xar, yar, 4);
                    g.setColor(Color.black);
                    g.setStroke(new BasicStroke(1.2f));
                    g.drawPolyline(xar, yar, 3);
                }

                if (useThumbs && e.thumbnail != null) {
                    g.setColor(new Color(128, 0, 0, 122));
                    g.fillRect(p.x - imgWidth / 2, p.y - imgHeight / 2, imgWidth, imgHeight);
                } else {
                    selectedIcon.paintIcon(mv, g,
                            p.x - imgWidth / 2,
                            p.y - imgHeight / 2);

                }
            }
        }
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        for (ImageEntry e : data) {
            v.visit(e.getPos());
        }
    }

    /**
     * Extract GPS metadata from image EXIF
     *
     * If successful, fills in the LatLon and EastNorth attributes of passed in image
     */
    private static void extractExif(ImageEntry e) {

        Metadata metadata;
        Directory dirExif;
        GpsDirectory dirGps;

        try {
            metadata = JpegMetadataReader.readMetadata(e.getFile());
            dirExif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            dirGps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        } catch (CompoundException | IOException p) {
            e.setExifCoor(null);
            e.setPos(null);
            return;
        }

        try {
            if (dirExif != null) {
                int orientation = dirExif.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                e.setExifOrientation(orientation);
            }
        } catch (MetadataException ex) {
            Main.debug(ex.getMessage());
        }

        if (dirGps == null) {
            e.setExifCoor(null);
            e.setPos(null);
            return;
        }

        try {
            double speed = dirGps.getDouble(GpsDirectory.TAG_SPEED);
            String speedRef = dirGps.getString(GpsDirectory.TAG_SPEED_REF);
            if ("M".equalsIgnoreCase(speedRef)) {
                // miles per hour
                speed *= 1.609344;
            } else if ("N".equalsIgnoreCase(speedRef)) {
                // knots == nautical miles per hour
                speed *= 1.852;
            }
            // default is K (km/h)
            e.setSpeed(speed);
        } catch (Exception ex) {
            Main.debug(ex.getMessage());
        }

        try {
            double ele = dirGps.getDouble(GpsDirectory.TAG_ALTITUDE);
            int d = dirGps.getInt(GpsDirectory.TAG_ALTITUDE_REF);
            if (d == 1) {
                ele *= -1;
            }
            e.setElevation(ele);
        } catch (MetadataException ex) {
            Main.debug(ex.getMessage());
        }

        try {
            LatLon latlon = ExifReader.readLatLon(dirGps);
            e.setExifCoor(latlon);
            e.setPos(e.getExifCoor());

        } catch (Exception ex) { // (other exceptions, e.g. #5271)
            Main.error("Error reading EXIF from file: "+ex);
            e.setExifCoor(null);
            e.setPos(null);
        }

        try {
            Double direction = ExifReader.readDirection(dirGps);
            if (direction != null) {
                e.setExifImgDir(direction.doubleValue());
            }
        } catch (Exception ex) { // (CompoundException and other exceptions, e.g. #5271)
            Main.debug(ex.getMessage());
        }

        // Time and date. We can have these cases:
        // 1) GPS_TIME_STAMP not set -> date/time will be null
        // 2) GPS_DATE_STAMP not set -> use EXIF date or set to default
        // 3) GPS_TIME_STAMP and GPS_DATE_STAMP are set
        int[] timeStampComps = dirGps.getIntArray(GpsDirectory.TAG_TIME_STAMP);
        if (timeStampComps != null) {
            int gpsHour = timeStampComps[0];
            int gpsMin = timeStampComps[1];
            int gpsSec = timeStampComps[2];
            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));

            // We have the time. Next step is to check if the GPS date stamp is set.
            // dirGps.getString() always succeeds, but the return value might be null.
            String dateStampStr = dirGps.getString(GpsDirectory.TAG_DATE_STAMP);
            if (dateStampStr != null && dateStampStr.matches("^\\d+:\\d+:\\d+$")) {
                String[] dateStampComps = dateStampStr.split(":");
                cal.set(Calendar.YEAR, Integer.parseInt(dateStampComps[0]));
                cal.set(Calendar.MONTH, Integer.parseInt(dateStampComps[1]) - 1);
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateStampComps[2]));
            } else {
                // No GPS date stamp in EXIF data. Copy it from EXIF time.
                // Date is not set if EXIF time is not available.
                if (e.hasExifTime()) {
                    // Time not set yet, so we can copy everything, not just date.
                    cal.setTime(e.getExifTime());
                }
            }

            cal.set(Calendar.HOUR_OF_DAY, gpsHour);
            cal.set(Calendar.MINUTE, gpsMin);
            cal.set(Calendar.SECOND, gpsSec);

            e.setExifGpsTime(cal.getTime());
        }
    }

    public void showNextPhoto() {
        if (data != null && !data.isEmpty()) {
            currentPhoto++;
            if (currentPhoto >= data.size()) {
                currentPhoto = data.size() - 1;
            }
            ImageViewerDialog.showImage(this, data.get(currentPhoto));
        } else {
            currentPhoto = -1;
        }
        Main.map.repaint();
    }

    public void showPreviousPhoto() {
        if (data != null && !data.isEmpty()) {
            currentPhoto--;
            if (currentPhoto < 0) {
                currentPhoto = 0;
            }
            ImageViewerDialog.showImage(this, data.get(currentPhoto));
        } else {
            currentPhoto = -1;
        }
        Main.map.repaint();
    }

    public void showFirstPhoto() {
        if (data != null && !data.isEmpty()) {
            currentPhoto = 0;
            ImageViewerDialog.showImage(this, data.get(currentPhoto));
        } else {
            currentPhoto = -1;
        }
        Main.map.repaint();
    }

    public void showLastPhoto() {
        if (data != null && !data.isEmpty()) {
            currentPhoto = data.size() - 1;
            ImageViewerDialog.showImage(this, data.get(currentPhoto));
        } else {
            currentPhoto = -1;
        }
        Main.map.repaint();
    }

    public void checkPreviousNextButtons() {
        ImageViewerDialog.setNextEnabled(data != null && currentPhoto < data.size() - 1);
        ImageViewerDialog.setPreviousEnabled(currentPhoto > 0);
    }

    public void removeCurrentPhoto() {
        if (data != null && !data.isEmpty() && currentPhoto >= 0 && currentPhoto < data.size()) {
            data.remove(currentPhoto);
            if (currentPhoto >= data.size()) {
                currentPhoto = data.size() - 1;
            }
            if (currentPhoto >= 0) {
                ImageViewerDialog.showImage(this, data.get(currentPhoto));
            } else {
                ImageViewerDialog.showImage(this, null);
            }
            updateOffscreenBuffer = true;
            Main.map.repaint();
        }
    }

    public void removeCurrentPhotoFromDisk() {
        ImageEntry toDelete = null;
        if (data != null && !data.isEmpty() && currentPhoto >= 0 && currentPhoto < data.size()) {
            toDelete = data.get(currentPhoto);

            int result = new ExtendedDialog(
                    Main.parent,
                    tr("Delete image file from disk"),
                    new String[] {tr("Cancel"), tr("Delete")})
            .setButtonIcons(new String[] {"cancel", "dialogs/delete"})
            .setContent(new JLabel(tr("<html><h3>Delete the file {0} from disk?<p>The image file will be permanently lost!</h3></html>",
                    toDelete.getFile().getName()), ImageProvider.get("dialogs/geoimage/deletefromdisk"), SwingConstants.LEFT))
                    .toggleEnable("geoimage.deleteimagefromdisk")
                    .setCancelButton(1)
                    .setDefaultButton(2)
                    .showDialog()
                    .getValue();

            if (result == 2) {
                data.remove(currentPhoto);
                if (currentPhoto >= data.size()) {
                    currentPhoto = data.size() - 1;
                }
                if (currentPhoto >= 0) {
                    ImageViewerDialog.showImage(this, data.get(currentPhoto));
                } else {
                    ImageViewerDialog.showImage(this, null);
                }

                if (toDelete.getFile().delete()) {
                    Main.info("File "+toDelete.getFile()+" deleted. ");
                } else {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Image file could not be deleted."),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                            );
                }

                updateOffscreenBuffer = true;
                Main.map.repaint();
            }
        }
    }

    public void copyCurrentPhotoPath() {
        ImageEntry toCopy = null;
        if (data != null && !data.isEmpty() && currentPhoto >= 0 && currentPhoto < data.size()) {
            toCopy = data.get(currentPhoto);
            String copyString = toCopy.getFile().toString();
            Utils.copyToClipboard(copyString);
        }
    }

    /**
     * Removes a photo from the list of images by index.
     * @param idx Image index
     * @since 6392
     */
    public void removePhotoByIdx(int idx) {
        if (idx >= 0 && data != null && idx < data.size()) {
            data.remove(idx);
        }
    }

    /**
     * Returns the image that matches the position of the mouse event.
     * @param evt Mouse event
     * @return Image at mouse position, or {@code null} if there is no image at the mouse position
     * @since 6392
     */
    public ImageEntry getPhotoUnderMouse(MouseEvent evt) {
        if (data != null) {
            for (int idx = data.size() - 1; idx >= 0; --idx) {
                ImageEntry img = data.get(idx);
                if (img.getPos() == null) {
                    continue;
                }
                Point p = Main.map.mapView.getPoint(img.getPos());
                Rectangle r;
                if (useThumbs && img.thumbnail != null) {
                    Dimension d = scaledDimension(img.thumbnail);
                    r = new Rectangle(p.x - d.width / 2, p.y - d.height / 2, d.width, d.height);
                } else {
                    r = new Rectangle(p.x - icon.getIconWidth() / 2,
                                      p.y - icon.getIconHeight() / 2,
                                      icon.getIconWidth(),
                                      icon.getIconHeight());
                }
                if (r.contains(evt.getPoint())) {
                    return img;
                }
            }
        }
        return null;
    }

    /**
     * Clears the currentPhoto, i.e. remove select marker, and optionally repaint.
     * @param repaint Repaint flag
     * @since 6392
     */
    public void clearCurrentPhoto(boolean repaint) {
        currentPhoto = -1;
        if (repaint) {
            updateBufferAndRepaint();
        }
    }

    /**
     * Clears the currentPhoto of the other GeoImageLayer's. Otherwise there could be multiple selected photos.
     */
    private void clearOtherCurrentPhotos() {
        for (GeoImageLayer layer:
                 Main.map.mapView.getLayersOfType(GeoImageLayer.class)) {
            if (layer != this) {
                layer.clearCurrentPhoto(false);
            }
        }
    }

    private static volatile List<MapMode> supportedMapModes = null;

    /**
     * Registers a map mode for which the functionality of this layer should be available.
     * @param mapMode Map mode to be registered
     * @since 6392
     */
    public static void registerSupportedMapMode(MapMode mapMode) {
        if (supportedMapModes == null) {
            supportedMapModes = new ArrayList<>();
        }
        supportedMapModes.add(mapMode);
    }

    /**
     * Determines if the functionality of this layer is available in
     * the specified map mode. {@link SelectAction} and {@link LassoModeAction} are supported by default,
     * other map modes can be registered.
     * @param mapMode Map mode to be checked
     * @return {@code true} if the map mode is supported,
     *         {@code false} otherwise
     */
    private static boolean isSupportedMapMode(MapMode mapMode) {
        if (mapMode instanceof SelectAction || mapMode instanceof LassoModeAction) {
            return true;
        }
        if (supportedMapModes != null) {
            for (MapMode supmmode: supportedMapModes) {
                if (mapMode == supmmode) {
                    return true;
                }
            }
        }
        return false;
    }

    private MouseAdapter mouseAdapter = null;
    private MapModeChangeListener mapModeListener = null;

    @Override
    public void hookUpMapView() {
        mouseAdapter = new MouseAdapter() {
            private boolean isMapModeOk() {
                return Main.map.mapMode == null || isSupportedMapMode(Main.map.mapMode);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1)
                    return;
                if (isVisible() && isMapModeOk()) {
                    Main.map.mapView.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent ev) {
                if (ev.getButton() != MouseEvent.BUTTON1)
                    return;
                if (data == null || !isVisible() || !isMapModeOk())
                    return;

                for (int i = data.size() - 1; i >= 0; --i) {
                    ImageEntry e = data.get(i);
                    if (e.getPos() == null) {
                        continue;
                    }
                    Point p = Main.map.mapView.getPoint(e.getPos());
                    Rectangle r;
                    if (useThumbs && e.thumbnail != null) {
                        Dimension d = scaledDimension(e.thumbnail);
                        r = new Rectangle(p.x - d.width / 2, p.y - d.height / 2, d.width, d.height);
                    } else {
                        r = new Rectangle(p.x - icon.getIconWidth() / 2,
                                p.y - icon.getIconHeight() / 2,
                                icon.getIconWidth(),
                                icon.getIconHeight());
                    }
                    if (r.contains(ev.getPoint())) {
                        clearOtherCurrentPhotos();
                        currentPhoto = i;
                        ImageViewerDialog.showImage(GeoImageLayer.this, e);
                        Main.map.repaint();
                        break;
                    }
                }
            }
        };

        mapModeListener = new MapModeChangeListener() {
            @Override
            public void mapModeChange(MapMode oldMapMode, MapMode newMapMode) {
                if (newMapMode == null || isSupportedMapMode(newMapMode)) {
                    Main.map.mapView.addMouseListener(mouseAdapter);
                } else {
                    Main.map.mapView.removeMouseListener(mouseAdapter);
                }
            }
        };

        MapFrame.addMapModeChangeListener(mapModeListener);
        mapModeListener.mapModeChange(null, Main.map.mapMode);

        MapView.addLayerChangeListener(new LayerChangeListener() {
            @Override
            public void activeLayerChange(Layer oldLayer, Layer newLayer) {
                if (newLayer == GeoImageLayer.this) {
                    // only in select mode it is possible to click the images
                    Main.map.selectSelectTool(false);
                }
            }

            @Override
            public void layerAdded(Layer newLayer) {
            }

            @Override
            public void layerRemoved(Layer oldLayer) {
                if (oldLayer == GeoImageLayer.this) {
                    stopLoadThumbs();
                    Main.map.mapView.removeMouseListener(mouseAdapter);
                    MapFrame.removeMapModeChangeListener(mapModeListener);
                    currentPhoto = -1;
                    if (data != null) {
                        data.clear();
                    }
                    data = null;
                    // stop listening to layer change events
                    MapView.removeLayerChangeListener(this);
                }
            }
        });

        Main.map.mapView.addPropertyChangeListener(this);
        if (Main.map.getToggleDialog(ImageViewerDialog.class) == null) {
            ImageViewerDialog.newInstance();
            Main.map.addToggleDialog(ImageViewerDialog.getInstance());
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (NavigatableComponent.PROPNAME_CENTER.equals(evt.getPropertyName()) ||
                NavigatableComponent.PROPNAME_SCALE.equals(evt.getPropertyName())) {
            updateOffscreenBuffer = true;
        }
    }

    /**
     * Start to load thumbnails.
     */
    public synchronized void startLoadThumbs() {
        if (useThumbs && !thumbsLoaded && !thumbsLoaderRunning) {
            stopLoadThumbs();
            thumbsloader = new ThumbsLoader(this);
            thumbsLoaderExecutor.submit(thumbsloader);
            thumbsLoaderRunning = true;
        }
    }

    /**
     * Stop to load thumbnails.
     *
     * Can be called at any time to make sure that the
     * thumbnail loader is stopped.
     */
    public synchronized void stopLoadThumbs() {
        if (thumbsloader != null) {
            thumbsloader.stop = true;
        }
        thumbsLoaderRunning = false;
    }

    /**
     * Called to signal that the loading of thumbnails has finished.
     *
     * Usually called from {@link ThumbsLoader} in another thread.
     */
    public void thumbsLoaded() {
        thumbsLoaded = true;
    }

    public void updateBufferAndRepaint() {
        updateOffscreenBuffer = true;
        Main.map.mapView.repaint();
    }

    /**
     * Get list of images in layer.
     * @return List of images in layer
     */
    public List<ImageEntry> getImages() {
        if (data == null) {
            return Collections.emptyList();
        }
        List<ImageEntry> copy = new ArrayList<>(data.size());
        for (ImageEntry ie : data) {
            copy.add(ie);
        }
        return copy;
    }

    /**
     * Returns the associated GPX layer.
     * @return The associated GPX layer
     */
    public GpxLayer getGpxLayer() {
        return gpxLayer;
    }

    @Override
    public void jumpToNextMarker() {
        showNextPhoto();
    }

    @Override
    public void jumpToPreviousMarker() {
        showPreviousPhoto();
    }

    /**
     * Returns the current thumbnail display status.
     * {@code true}: thumbnails are displayed, {@code false}: an icon is displayed instead of thumbnails.
     * @return Current thumbnail display status
     * @since 6392
     */
    public boolean isUseThumbs() {
        return useThumbs;
    }

    /**
     * Enables or disables the display of thumbnails.  Does not update the display.
     * @param useThumbs New thumbnail display status
     * @since 6392
     */
    public void setUseThumbs(boolean useThumbs) {
        this.useThumbs = useThumbs;
        if (useThumbs && !thumbsLoaded) {
            startLoadThumbs();
        } else if (!useThumbs) {
            stopLoadThumbs();
        }
    }
}
