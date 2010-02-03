// License: GPL. See LICENSE file for details.
// Copyright 2007 by Christian Gallioz (aka khris78)
// Parts of code from Geotagged plugin (by Rob Neild)
// and the core JOSM source code (by Immanuel Scholz and others)

package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.MapFrame.MapModeChangeListener;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.ImageProvider;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.lang.CompoundException;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;

public class GeoImageLayer extends Layer implements PropertyChangeListener {

    List<ImageEntry> data;
    GpxLayer gpxLayer;

    private Icon icon = ImageProvider.get("dialogs/geoimage/photo-marker");
    private Icon selectedIcon = ImageProvider.get("dialogs/geoimage/photo-marker-selected");

    private int currentPhoto = -1;

    boolean useThumbs = false;
    ThumbsLoader thumbsloader;
    boolean thumbsLoaded = false;
    private BufferedImage offscreenBuffer;
    boolean updateOffscreenBuffer = true;

    /** Loads a set of images, while displaying a dialog that indicates what the plugin is currently doing.
     * In facts, this object is instantiated with a list of files. These files may be JPEG files or
     * directories. In case of directories, they are scanned to find all the images they contain.
     * Then all the images that have be found are loaded as ImageEntry instances.
     */
    private static final class Loader extends PleaseWaitRunnable {

        private boolean cancelled = false;
        private GeoImageLayer layer;
        private Collection<File> selection;
        private HashSet<String> loadedDirectories = new HashSet<String>();
        private LinkedHashSet<String> errorMessages;
        private GpxLayer gpxLayer;

        protected void rememberError(String message) {
            this.errorMessages.add(message);
        }

        public Loader(Collection<File> selection, GpxLayer gpxLayer) {
            super(tr("Extracting GPS locations from EXIF"));
            this.selection = selection;
            this.gpxLayer = gpxLayer;
            errorMessages = new LinkedHashSet<String>();
        }

        @Override protected void realRun() throws IOException {

            progressMonitor.subTask(tr("Starting directory scan"));
            Collection<File> files = new ArrayList<File>();
            try {
                addRecursiveFiles(files, selection);
            } catch(NullPointerException npe) {
                rememberError(tr("One of the selected files was null"));
            }

            if (cancelled)
                return;
            progressMonitor.subTask(tr("Read photos..."));
            progressMonitor.setTicksCount(files.size());

            progressMonitor.subTask(tr("Read photos..."));
            progressMonitor.setTicksCount(files.size());

            // read the image files
            List<ImageEntry> data = new ArrayList<ImageEntry>(files.size());

            for (File f : files) {

                if (cancelled) {
                    break;
                }

                progressMonitor.subTask(tr("Reading {0}...", f.getName()));
                progressMonitor.worked(1);

                ImageEntry e = new ImageEntry();

                // Changed to silently cope with no time info in exif. One case
                // of person having time that couldn't be parsed, but valid GPS info

                try {
                    e.setExifTime(ExifReader.readTime(f));
                } catch (ParseException e1) {
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

                if(cancelled) {
                    break;
                }

                if (f == null) {
                    nullFile = true;

                } else if (f.isDirectory()) {
                    String canonical = null;
                    try {
                        canonical = f.getCanonicalPath();
                    } catch (IOException e) {
                        e.printStackTrace();
                        rememberError(tr("Unable to get canonical path for directory {0}\n",
                                f.getAbsolutePath()));
                    }

                    if (canonical == null || loadedDirectories.contains(canonical)) {
                        continue;
                    } else {
                        loadedDirectories.add(canonical);
                    }

                    Collection<File> children = Arrays.asList(f.listFiles(JpegFileFilter.getInstance()));
                    if (children != null) {
                        progressMonitor.subTask(tr("Scanning directory {0}", f.getPath()));
                        try {
                            addRecursiveFiles(files, children);
                        } catch(NullPointerException npe) {
                            npe.printStackTrace();
                            rememberError(tr("Found null file in directory {0}\n", f.getPath()));
                        }
                    } else {
                        rememberError(tr("Error while getting files from directory {0}\n", f.getPath()));
                    }

                } else {
                    files.add(f);
                }
            }

            if (nullFile)
                throw new NullPointerException();
        }

        protected String formatErrorMessages() {
            StringBuffer sb = new StringBuffer();
            sb.append("<html>");
            if (errorMessages.size() == 1) {
                sb.append(errorMessages.iterator().next());
            } else {
                sb.append("<ul>");
                for (String msg: errorMessages) {
                    sb.append("<li>").append(msg).append("</li>");
                }
                sb.append("/ul>");
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
                layer.hook_up_mouse_events(); // Main.map.mapView should exist
                // now. Can add mouse listener
                Main.map.mapView.addPropertyChangeListener(layer);
                if (!addedToggleDialog) {
                    // TODO Workaround for bug in DialogsPanel
                    // When GeoImageLayer is added as a first layer, division by zero exception is thrown
                    // This is caused by DialogsPanel.reconstruct method which use height of other dialogs
                    // to calculate height of newly added ImageViewerDialog. But height of other dialogs is
                    // zero because it's calculated by layout manager later
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Main.map.addToggleDialog(ImageViewerDialog.getInstance());
                        }
                    });
                    addedToggleDialog = true;
                }

                if (! cancelled && layer.data.size() > 0) {
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
            cancelled = true;
        }
    }

    private static boolean addedToggleDialog = false;

    public static void create(Collection<File> files, GpxLayer gpxLayer) {
        Loader loader = new Loader(files, gpxLayer);
        Main.worker.execute(loader);
    }

    private GeoImageLayer(final List<ImageEntry> data, GpxLayer gpxLayer) {

        super(tr("Geotagged Images"));

        Collections.sort(data);
        this.data = data;
        this.gpxLayer = gpxLayer;
    }

    @Override
    public Icon getIcon() {
        return ImageProvider.get("dialogs/geoimage");
    }
    
    public static interface LayerMenuAddition {
        public Component getComponent(Layer layer);
    }

    private static List<LayerMenuAddition> menuAdditions = new LinkedList<LayerMenuAddition>();
    public static void registerMenuAddition(LayerMenuAddition addition) {
        menuAdditions.add(addition);
    }

    @Override
    public Component[] getMenuEntries() {

        JMenuItem correlateItem = new JMenuItem(tr("Correlate to GPX"), ImageProvider.get("dialogs/geoimage/gpx2img"));
        correlateItem.addActionListener(new CorrelateGpxWithImages(this));
        
        List<Component> entries = new ArrayList<Component>();
        entries.add(new JMenuItem(LayerListDialog.getInstance().createShowHideLayerAction(this)));
        entries.add(new JMenuItem(LayerListDialog.getInstance().createDeleteLayerAction(this)));
        entries.add(new JMenuItem(new RenameLayerAction(null, this)));
        entries.add(new JSeparator());
        entries.add(correlateItem);
        if (!menuAdditions.isEmpty()) {
            entries.add(new JSeparator());
        }
        for (LayerMenuAddition addition : menuAdditions) {
            entries.add(addition.getComponent(this));
        }
        entries.add(new JSeparator());
        entries.add(new JMenuItem(new LayerListPopup.InfoAction(this)));

        return entries.toArray(new Component[0]);
        
    }

    private String infoText() {
        int i = 0;
        for (ImageEntry e : data)
            if (e.getPos() != null) {
                i++;
            }
        return trn("{0} image loaded.", "{0} images loaded.", data.size(), data.size())
        + " " + trn("{0} was found to be GPS tagged.", "{0} were found to be GPS tagged.", i, i);
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

        ImageEntry selected = null;
        if (l.currentPhoto >= 0) {
            selected = l.data.get(l.currentPhoto);
        }

        data.addAll(l.data);
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

        if (selected != null) {
            for (int i = 0; i < data.size() ; i++) {
                if (data.get(i) == selected) {
                    currentPhoto = i;
                    ImageViewerDialog.showImage(GeoImageLayer.this, data.get(i));
                    break;
                }
            }
        }

        setName(l.getName());
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
        int width = Main.map.mapView.getWidth();
        int height = Main.map.mapView.getHeight();
        Rectangle clip = g.getClipBounds();
        if (useThumbs) {
            if (null == offscreenBuffer || offscreenBuffer.getWidth() != width  // reuse the old buffer if possible
                    || offscreenBuffer.getHeight() != height) {
                offscreenBuffer = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_ARGB);
                updateOffscreenBuffer = true;
            }

            if (updateOffscreenBuffer) {
                Graphics2D tempG = offscreenBuffer.createGraphics();
                tempG.setColor(new Color(0,0,0,0));
                Composite saveComp = tempG.getComposite();
                tempG.setComposite(AlphaComposite.Clear);   // remove the old images
                tempG.fillRect(0, 0, width, height);
                tempG.setComposite(saveComp);

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
                    }
                    else { // thumbnail not loaded yet
                        icon.paintIcon(mv, tempG,
                                p.x - icon.getIconWidth() / 2,
                                p.y - icon.getIconHeight() / 2);
                    }
                }
                updateOffscreenBuffer = false;
            }
            g.drawImage(offscreenBuffer, 0, 0, null);
        }
        else {
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

                if (e.thumbnail != null) {
                    Dimension d = scaledDimension(e.thumbnail);
                    g.setColor(new Color(128, 0, 0, 122));
                    g.fillRect(p.x - d.width / 2, p.y - d.height / 2, d.width, d.height);
                } else {
                    selectedIcon.paintIcon(mv, g,
                            p.x - selectedIcon.getIconWidth() / 2,
                            p.y - selectedIcon.getIconHeight() / 2);
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

    /*
     * Extract gps from image exif
     *
     * If successful, fills in the LatLon and EastNorth attributes of passed in
     * image;
     */

    private static void extractExif(ImageEntry e) {

        try {
            int deg;
            double min, sec;
            double lon, lat;

            Metadata metadata = JpegMetadataReader.readMetadata(e.getFile());
            Directory dir = metadata.getDirectory(GpsDirectory.class);

            // longitude

            Rational[] components = dir.getRationalArray(GpsDirectory.TAG_GPS_LONGITUDE);

            deg = components[0].intValue();
            min = components[1].floatValue();
            sec = components[2].floatValue();

            lon = (deg + (min / 60) + (sec / 3600));

            if (dir.getString(GpsDirectory.TAG_GPS_LONGITUDE_REF).charAt(0) == 'W') {
                lon = -lon;
            }

            // latitude

            components = dir.getRationalArray(GpsDirectory.TAG_GPS_LATITUDE);

            deg = components[0].intValue();
            min = components[1].floatValue();
            sec = components[2].floatValue();

            lat = (deg + (min / 60) + (sec / 3600));

            if (dir.getString(GpsDirectory.TAG_GPS_LATITUDE_REF).charAt(0) == 'S') {
                lat = -lat;
            }

            // Store values

            e.setExifCoor(new LatLon(lat, lon));
            e.setPos(e.getExifCoor());

        } catch (CompoundException p) {
            e.setExifCoor(null);
            e.setPos(null);
        }
    }

    public void showNextPhoto() {
        if (data != null && data.size() > 0) {
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
        if (data != null && data.size() > 0) {
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

    public void checkPreviousNextButtons() {
        //        System.err.println("showing image " + currentPhoto);
        ImageViewerDialog.setNextEnabled(currentPhoto < data.size() - 1);
        ImageViewerDialog.setPreviousEnabled(currentPhoto > 0);
    }

    public void removeCurrentPhoto() {
        if (data != null && data.size() > 0 && currentPhoto >= 0 && currentPhoto < data.size()) {
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
        if (data != null && data.size() > 0 && currentPhoto >= 0 && currentPhoto < data.size()) {
            toDelete = data.get(currentPhoto);

            int result = new ExtendedDialog(
                    Main.parent,
                    tr("Delete image file from disk"),
                    new String[] {tr("Cancel"), tr("Delete")})
            .setButtonIcons(new String[] {"cancel.png", "dialogs/delete.png"})
            .setContent(new JLabel(tr("<html><h3>Delete the file {0} from disk?<p>The image file will be permanently lost!</h3></html>"
                    ,toDelete.getFile().getName()), ImageProvider.get("dialogs/geoimage/deletefromdisk"),SwingConstants.LEFT))
                    .toggleEnable("geoimage.deleteimagefromdisk")
                    .setCancelButton(1)
                    .setDefaultButton(2)
                    .showDialog()
                    .getValue();

            if(result == 2)
            {
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
                    System.out.println("File "+toDelete.getFile().toString()+" deleted. ");
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

    private MouseAdapter mouseAdapter = null;
    private MapModeChangeListener mapModeListener = null;

    private void hook_up_mouse_events() {
        mouseAdapter = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {

                if (e.getButton() != MouseEvent.BUTTON1)
                    return;
                if (isVisible()) {
                    Main.map.mapView.repaint();
                }
            }

            @Override public void mouseReleased(MouseEvent ev) {
                if (ev.getButton() != MouseEvent.BUTTON1)
                    return;
                if (!isVisible())
                    return;

                for (int i = data.size() - 1; i >= 0; --i) {
                    ImageEntry e = data.get(i);
                    if (e.getPos() == null) {
                        continue;
                    }
                    Point p = Main.map.mapView.getPoint(e.getPos());
                    Rectangle r;
                    if (e.thumbnail != null) {
                        Dimension d = scaledDimension(e.thumbnail);
                        r = new Rectangle(p.x - d.width / 2, p.y - d.height / 2, d.width, d.height);
                    } else {
                        r = new Rectangle(p.x - icon.getIconWidth() / 2,
                                p.y - icon.getIconHeight() / 2,
                                icon.getIconWidth(),
                                icon.getIconHeight());
                    }
                    if (r.contains(ev.getPoint())) {
                        currentPhoto = i;
                        ImageViewerDialog.showImage(GeoImageLayer.this, e);
                        Main.map.repaint();
                        break;
                    }
                }
            }
        };

        mapModeListener = new MapModeChangeListener() {
            public void mapModeChange(MapMode oldMapMode, MapMode newMapMode) {
                if (newMapMode instanceof org.openstreetmap.josm.actions.mapmode.SelectAction) {
                    Main.map.mapView.addMouseListener(mouseAdapter);
                } else {
                    Main.map.mapView.removeMouseListener(mouseAdapter);
                }
            }
        };

        MapFrame.addMapModeChangeListener(mapModeListener);
        mapModeListener.mapModeChange(null, Main.map.mapMode);

        MapView.addLayerChangeListener(new LayerChangeListener() {
            public void activeLayerChange(Layer oldLayer, Layer newLayer) {
                if (newLayer == GeoImageLayer.this) {
                    // only in select mode it is possible to click the images
                    Main.map.selectSelectTool(false);
                }
            }

            public void layerAdded(Layer newLayer) {
            }

            public void layerRemoved(Layer oldLayer) {
                if (oldLayer == GeoImageLayer.this) {
                    if (thumbsloader != null) {
                        thumbsloader.stop = true;
                    }
                    Main.map.mapView.removeMouseListener(mouseAdapter);
                    MapFrame.removeMapModeChangeListener(mapModeListener);
                    currentPhoto = -1;
                    data.clear();
                    data = null;
                    // stop listening to layer change events
                    MapView.removeLayerChangeListener(this);
                }
            }
        });
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("center".equals(evt.getPropertyName()) || "scale".equals(evt.getPropertyName())) {
            updateOffscreenBuffer = true;
        }
    }

    public void loadThumbs() {
        if (useThumbs && !thumbsLoaded) {
            thumbsLoaded = true;
            thumbsloader = new ThumbsLoader(this);
            Thread t = new Thread(thumbsloader);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }
    }

    public void updateBufferAndRepaint() {
        updateOffscreenBuffer = true;
        Main.map.mapView.repaint();
    }
    
    public List<ImageEntry> getImages() {
        List<ImageEntry> copy = new ArrayList<ImageEntry>();
        for (ImageEntry ie : data) {
            copy.add(ie.clone());
        }
        return copy;
    }
}
