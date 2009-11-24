//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.GeoImageLayer.ImageLoader.ImageLoadedListener;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.DateParser;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer which imports several photos from disk and read EXIF time information from them.
 *
 * @author Imi
 */
public class GeoImageLayer extends Layer {

    /**
     * Allows to load and scale images. Loaded images are kept in cache (using soft reference). Both
     * synchronous and asynchronous loading is supported
     *
     */
    public static class ImageLoader implements ImageObserver {

        public static class Entry {
            final File file;
            final int width;
            final int height;
            final int maxSize;
            private final ImageLoadedListener listener;

            volatile Image scaledImage;

            public Entry(File file, int width, int height, int maxSize, ImageLoadedListener listener) {
                this.file = file;
                this.height = height;
                this.width = width;
                this.maxSize = maxSize;
                this.listener = listener;
            }
        }

        public interface ImageLoadedListener {
            void imageLoaded();
        }

        private final List<ImageLoader.Entry> queue = new ArrayList<ImageLoader.Entry>();
        private final WeakHashMap<File, SoftReference<Image>> loadedImageCache = new WeakHashMap<File, SoftReference<Image>>();
        private ImageLoader.Entry currentEntry;

        private Image getOrLoadImage(File file) {
            SoftReference<Image> cachedImageRef = loadedImageCache.get(file);
            if (cachedImageRef != null) {
                Image cachedImage = cachedImageRef.get();
                if (cachedImage != null)
                    return cachedImage;
            }
            return Toolkit.getDefaultToolkit().createImage(currentEntry.file.getAbsolutePath());
        }

        private BufferedImage createResizedCopy(Image originalImage,
                int scaledWidth, int scaledHeight)
        {
            BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaledBI.createGraphics();
            while (!g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null))
            {
                try {
                    Thread.sleep(10);
                } catch(InterruptedException ie) {}
            }
            g.dispose();
            return scaledBI;
        }
        private void loadImage() {
            if (currentEntry != null)
                return;
            while (!queue.isEmpty()) {
                currentEntry = queue.get(0);
                queue.remove(0);

                Image newImage = getOrLoadImage(currentEntry.file);
                if (newImage.getWidth(this) == -1) {
                    break;
                } else {
                    finishImage(newImage, currentEntry);
                    currentEntry = null;
                }
            }
        }

        private void finishImage(Image img, ImageLoader.Entry entry) {
            loadedImageCache.put(entry.file, new SoftReference<Image>(img));
            if (entry.maxSize != -1) {
                int w = img.getWidth(null);
                int h = img.getHeight(null);
                if (w>h) {
                    h = Math.round(entry.maxSize*((float)h/w));
                    w = entry.maxSize;
                } else {
                    w = Math.round(entry.maxSize*((float)w/h));
                    h = entry.maxSize;
                }
                entry.scaledImage = createResizedCopy(img, w, h);
            } else if (entry.width != -1 && entry.height != -1) {
                entry.scaledImage = createResizedCopy(img, entry.width, entry.height);
            } else {
                entry.scaledImage = img;
            }
            if (entry.listener != null) {
                entry.listener.imageLoaded();
            }
        }

        public synchronized ImageLoader.Entry loadImage(File file, int width, int height, int maxSize, ImageLoadedListener listener) {
            ImageLoader.Entry e = new Entry(file, width, height, maxSize, listener);
            queue.add(e);
            loadImage();
            return e;
        }

        public Image waitForImage(File file, int width, int height) {
            return waitForImage(file, width, height, -1);
        }

        public Image waitForImage(File file, int maxSize) {
            return waitForImage(file, -1, -1, maxSize);
        }

        public Image waitForImage(File file) {
            return waitForImage(file, -1, -1, -1);
        }

        private synchronized Image waitForImage(File file, int width, int height, int maxSize) {
            ImageLoader.Entry entry;
            if (currentEntry != null && currentEntry.file.equals(file)) {
                entry = currentEntry;
            } else {
                entry = new Entry(file, width, height, maxSize, null);
                queue.add(0, entry);
            }
            loadImage();

            while (true) {
                if (entry.scaledImage != null)
                    return entry.scaledImage;
                try {
                    wait();
                } catch (InterruptedException e) {}
            }
        }

        public synchronized boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
            if ((infoflags & ImageObserver.ALLBITS) != 0) {
                finishImage(img, currentEntry);
                currentEntry = null;
                loadImage();
                notifyAll();
            } else if ((infoflags & ImageObserver.ERROR) != 0) {
                currentEntry.scaledImage = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
                currentEntry = null;
            }
            return true;
        }
    }

    private static final int ICON_SIZE = 16;
    private static ImageLoader imageLoader = new ImageLoader();

    private static final class ImageEntry implements Comparable<ImageEntry>, ImageLoadedListener {

        private static final Image EMPTY_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);

        final File image;
        ImageLoader.Entry icon;
        Date time;
        CachedLatLon pos;

        public ImageEntry(File image) {
            this.image = image;
            icon = imageLoader.loadImage(image, ICON_SIZE, ICON_SIZE, -1, this);
        }

        public int compareTo(ImageEntry image) {
            return time.compareTo(image.time);
        }

        public Image getIcon() {
            if (icon.scaledImage == null)
                return EMPTY_IMAGE;
            else
                return icon.scaledImage;
        }

        public void imageLoaded() {
            MapFrame frame = Main.map;
            if (frame != null) {
                frame.mapView.repaint();
            }
        }
    }

    private static final class Loader extends PleaseWaitRunnable {
        private GeoImageLayer layer;
        private final Collection<File> files;
        private final GpxLayer gpxLayer;
        private LinkedList<TimedPoint> gps;

        public Loader(Collection<File> files, GpxLayer gpxLayer) {
            super(tr("Images for {0}", gpxLayer.getName()));
            this.files = files;
            this.gpxLayer = gpxLayer;
        }
        @Override protected void realRun() throws IOException {
            progressMonitor.subTask(tr("Read GPX..."));
            progressMonitor.setTicksCount(10 + files.size());
            gps = new LinkedList<TimedPoint>();

            // Extract dates and locations from GPX input

            ProgressMonitor gpxSubTask = progressMonitor.createSubTaskMonitor(10, true);
            int size = 0;
            for (GpxTrack trk:gpxLayer.data.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    size += segment.size();
                }
            }
            gpxSubTask.beginTask(null, size);

            try {
                for (GpxTrack trk : gpxLayer.data.tracks) {
                    for (Collection<WayPoint> segment : trk.trackSegs) {
                        for (WayPoint p : segment) {
                            LatLon c = p.getCoor();
                            if (!p.attr.containsKey("time"))
                                throw new IOException(tr("No time for point {0} x {1}",c.lat(),c.lon()));
                            Date d = null;
                            try {
                                d = DateParser.parse((String) p.attr.get("time"));
                            } catch (ParseException e) {
                                throw new IOException(tr("Cannot read time \"{0}\" from point {1} x {2}",p.attr.get("time"),c.lat(),c.lon()));
                            }
                            gps.add(new TimedPoint(d, c));
                            gpxSubTask.worked(1);
                        }
                    }
                }
            } finally {
                gpxSubTask.finishTask();
            }

            if (gps.isEmpty())
                return;

            // read the image files
            ArrayList<ImageEntry> data = new ArrayList<ImageEntry>(files.size());
            for (File f : files) {
                if (progressMonitor.isCancelled()) {
                    break;
                }
                progressMonitor.subTask(tr("Reading {0}...",f.getName()));

                ImageEntry e = new ImageEntry(f);
                try {
                    e.time = ExifReader.readTime(f);
                    progressMonitor.worked(1);
                } catch (ParseException e1) {
                    continue;
                }
                if (e.time == null) {
                    continue;
                }

                data.add(e);
            }
            layer = new GeoImageLayer(data, gps);
            layer.calculatePosition();
        }
        @Override protected void finish() {
            if (gps.isEmpty()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("No images with readable timestamps found."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            if (layer != null) {
                Main.main.addLayer(layer);
            }
        }

        @Override
        protected void cancel() {

        }
    }

    public ArrayList<ImageEntry> data;
    private LinkedList<TimedPoint> gps = new LinkedList<TimedPoint>();

    /**
     * The delta added to all timestamps in files from the camera
     * to match to the timestamp from the gps receivers tracklog.
     */
    private long delta = Long.parseLong(Main.pref.get("tagimages.delta", "0"));
    private long gpstimezone = Long.parseLong(Main.pref.get("tagimages.gpstimezone", "0"))*60*60*1000;
    private boolean mousePressed = false;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    private MouseAdapter mouseAdapter;
    private ImageViewerDialog imageViewerDialog;

    public static final class GpsTimeIncorrect extends Exception {
        public GpsTimeIncorrect(String message, Throwable cause) {
            super(message, cause);
        }
        public GpsTimeIncorrect(String message) {
            super(message);
        }
    }

    private static final class TimedPoint implements Comparable<TimedPoint> {
        Date time;
        CachedLatLon pos;

        public TimedPoint(Date time, LatLon pos) {
            this.time = time;
            this.pos = new CachedLatLon(pos);
        }
        public int compareTo(TimedPoint point) {
            return time.compareTo(point.time);
        }
    }

    public static void create(Collection<File> files, GpxLayer gpxLayer) {
        Loader loader = new Loader(files, gpxLayer);
        Main.worker.execute(loader);
    }

    private GeoImageLayer(final ArrayList<ImageEntry> data, LinkedList<TimedPoint> gps) {
        super(tr("Geotagged Images"));
        Collections.sort(data);
        Collections.sort(gps);
        this.data = data;
        this.gps = gps;
        final Layer self = this;
        mouseAdapter = new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1)
                    return;
                mousePressed  = true;
                if (isVisible()) {
                    Main.map.mapView.repaint();
                }
            }
            @Override public void mouseReleased(MouseEvent ev) {
                if (ev.getButton() != MouseEvent.BUTTON1)
                    return;
                mousePressed = false;
                if (!isVisible())
                    return;
                for (int i = data.size(); i > 0; --i) {
                    ImageEntry e = data.get(i-1);
                    if (e.pos == null) {
                        continue;
                    }
                    Point p = Main.map.mapView.getPoint(e.pos);
                    Rectangle r = new Rectangle(p.x-ICON_SIZE/2, p.y-ICON_SIZE/2, ICON_SIZE, ICON_SIZE);
                    if (r.contains(ev.getPoint())) {
                        showImage(i-1);
                        break;
                    }
                }
                Main.map.mapView.repaint();
            }
        };
        Main.map.mapView.addMouseListener(mouseAdapter);
        Layer.listeners.add(new LayerChangeListener(){
            public void activeLayerChange(Layer oldLayer, Layer newLayer) {}
            public void layerAdded(Layer newLayer) {}
            public void layerRemoved(Layer oldLayer) {
                if (oldLayer == self) {
                    Main.map.mapView.removeMouseListener(mouseAdapter);
                }
            }
        });
    }

    private class ImageViewerDialog {

        private int currentImage;
        private ImageEntry currentImageEntry;

        private final JDialog dlg;
        private final JButton nextButton;
        private final JButton prevButton;
        private final JToggleButton scaleToggle;
        private final JToggleButton centerToggle;
        private final JViewport imageViewport;
        private final JLabel imageLabel;

        private class ImageAction implements ActionListener {

            private final int offset;

            public ImageAction(int offset) {
                this.offset = offset;
            }

            public void actionPerformed(ActionEvent e) {
                showImage(currentImage + offset);
            }

        }

        public ImageViewerDialog(ImageEntry firstImage) {
            final JPanel p = new JPanel(new BorderLayout());
            imageLabel = new JLabel(new ImageIcon(imageLoader.waitForImage(firstImage.image, 580)));
            final JScrollPane scroll = new JScrollPane(imageLabel);
            scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            imageViewport = scroll.getViewport();
            p.add(scroll, BorderLayout.CENTER);

            scaleToggle = new JToggleButton(ImageProvider.get("dialogs", "zoom-best-fit"));
            nextButton  = new JButton(ImageProvider.get("dialogs", "next"));
            prevButton = new JButton(ImageProvider.get("dialogs", "previous"));
            centerToggle = new JToggleButton(ImageProvider.get("dialogs", "centreview"));

            JPanel p2 = new JPanel();
            p2.add(prevButton);
            p2.add(scaleToggle);
            p2.add(centerToggle);
            p2.add(nextButton);
            p.add(p2, BorderLayout.SOUTH);
            final JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE);
            dlg = pane.createDialog(Main.parent, "");
            scaleToggle.addActionListener(new ImageAction(0));
            scaleToggle.setSelected(true);
            centerToggle.addActionListener(new ImageAction(0));

            nextButton.setActionCommand("Next");
            prevButton.setActionCommand("Previous");
            nextButton.setMnemonic(KeyEvent.VK_RIGHT);
            prevButton.setMnemonic(KeyEvent.VK_LEFT);
            scaleToggle.setMnemonic(KeyEvent.VK_F);
            centerToggle.setMnemonic(KeyEvent.VK_C);
            nextButton.setToolTipText("Show next image");
            prevButton.setToolTipText("Show previous image");
            centerToggle.setToolTipText("Centre image location in main display");
            scaleToggle.setToolTipText("Scale image to fit");

            prevButton.addActionListener(new ImageAction(-1));
            nextButton.addActionListener(new ImageAction(1));
            centerToggle.setSelected(false);

            dlg.addComponentListener(new ComponentListener() {
                boolean ignoreEvent = true;
                public void componentHidden(ComponentEvent e) {}
                public void componentMoved(ComponentEvent e) {}
                public void componentResized(ComponentEvent ev) {
                    // we ignore the first resize event, as the picture is scaled already on load:
                    if (scaleToggle.getModel().isSelected() && !ignoreEvent) {
                        imageLabel.setIcon(new ImageIcon(imageLoader.waitForImage(currentImageEntry.image,
                                Math.max(imageViewport.getWidth(), imageViewport.getHeight()))));
                    }
                    ignoreEvent = false;
                }
                public void componentShown(ComponentEvent e) {}

            });
            dlg.setModal(false);
            dlg.setResizable(true);
            dlg.pack();
        }

        public void showImage(int index) {
            dlg.setVisible(true);
            dlg.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            if (index < 0) {
                index = 0;
            } else if (index >= data.size() - 1) {
                index = data.size() - 1;
            }

            currentImage = index;
            currentImageEntry = data.get(currentImage);

            prevButton.setEnabled(currentImage > 0);
            nextButton.setEnabled(currentImage < data.size() - 1);

            if (scaleToggle.getModel().isSelected()) {
                imageLabel.setIcon(new ImageIcon(imageLoader.waitForImage(currentImageEntry.image,
                        Math.max(imageViewport.getWidth(), imageViewport.getHeight()))));
            } else {
                imageLabel.setIcon(new ImageIcon(imageLoader.waitForImage(currentImageEntry.image)));
            }

            if (centerToggle.getModel().isSelected()) {
                Main.map.mapView.zoomTo(currentImageEntry.pos);
            }

            dlg.setTitle(currentImageEntry.image +
                    " (" + currentImageEntry.pos.toDisplayString() + ")");
            dlg.setCursor(Cursor.getDefaultCursor());
        }

    }

    private void showImage(int i) {
        if (imageViewerDialog == null) {
            imageViewerDialog = new ImageViewerDialog(data.get(i));
        }
        imageViewerDialog.showImage(i);
    }

    @Override public Icon getIcon() {
        return ImageProvider.get("layer", "tagimages_small");
    }

    @Override public Object getInfoComponent() {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(getToolTipText()), GBC.eop());

        p.add(new JLabel(tr("GPS start: {0}",dateFormat.format(gps.getFirst().time))), GBC.eol());
        p.add(new JLabel(tr("GPS end: {0}",dateFormat.format(gps.getLast().time))), GBC.eop());

        p.add(new JLabel(tr("current delta: {0}s",(delta/1000.0))), GBC.eol());
        p.add(new JLabel(tr("timezone difference: ")+(gpstimezone>0?"+":"")+(gpstimezone/1000/60/60)), GBC.eop());

        JList img = new JList(data.toArray());
        img.setCellRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                ImageEntry e = (ImageEntry)value;
                setIcon(new ImageIcon(e.getIcon()));
                setText(e.image.getName()+" ("+dateFormat.format(new Date(e.time.getTime()+(delta+gpstimezone)))+")");
                if (e.pos == null) {
                    setForeground(Color.red);
                }
                return this;
            }
        });
        img.setVisibleRowCount(5);
        p.add(new JScrollPane(img), GBC.eop().fill(GBC.BOTH));
        return p;
    }

    @Override public String getToolTipText() {
        int i = 0;
        for (ImageEntry e : data)
            if (e.pos != null) {
                i++;
            }
        return data.size()+" "+trn("image","images",data.size())+". "+tr("{0} within the track.",i);
    }

    @Override public boolean isMergable(Layer other) {
        return other instanceof GeoImageLayer;
    }

    @Override public void mergeFrom(Layer from) {
        GeoImageLayer l = (GeoImageLayer)from;
        data.addAll(l.data);
    }

    @Override public void paint(Graphics2D g, MapView mv, Bounds box) {
        int clickedIndex = -1;

        // First select beveled icon (for cases where are more icons on the same spot)
        Point mousePosition = mv.getMousePosition();
        if (mousePosition != null  && mousePressed) {
            for (int i = data.size() - 1; i >= 0; i--) {
                ImageEntry e = data.get(i);
                if (e.pos == null) {
                    continue;
                }

                Point p = mv.getPoint(e.pos);
                Rectangle r = new Rectangle(p.x-ICON_SIZE / 2, p.y-ICON_SIZE / 2, ICON_SIZE, ICON_SIZE);
                if (r.contains(mousePosition)) {
                    clickedIndex = i;
                    break;
                }
            }
        }

        for (int i = 0; i < data.size(); i++) {
            ImageEntry e = data.get(i);
            if (e.pos != null) {
                Point p = mv.getPoint(e.pos);
                Rectangle r = new Rectangle(p.x-ICON_SIZE / 2, p.y-ICON_SIZE / 2, ICON_SIZE, ICON_SIZE);
                g.drawImage(e.getIcon(), r.x, r.y, null);
                Border b = null;
                if (i == clickedIndex) {
                    b = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
                } else {
                    b = BorderFactory.createBevelBorder(BevelBorder.RAISED);
                }
                Insets inset = b.getBorderInsets(mv);
                r.grow((inset.top+inset.bottom)/2, (inset.left+inset.right)/2);
                b.paintBorder(mv, g, r.x, r.y, r.width, r.height);
            }
        }
    }

    @Override public void visitBoundingBox(BoundingXYVisitor v) {
        for (ImageEntry e : data) {
            v.visit(e.pos);
        }
    }

    @Override public Component[] getMenuEntries() {
        JMenuItem sync = new JMenuItem(tr("Sync clock"), ImageProvider.get("clock"));
        sync.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(Main.pref.get("tagimages.lastdirectory"));
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                fc.setFileFilter(new FileFilter(){
                    @Override public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".jpg");
                    }
                    @Override public String getDescription() {
                        return tr("JPEG images (*.jpg)");
                    }
                });
                fc.showOpenDialog(Main.parent);
                File sel = fc.getSelectedFile();
                if (sel == null)
                    return;
                Main.pref.put("tagimages.lastdirectory", sel.getPath());
                sync(sel);
                Main.map.repaint();
            }
        });
        return new Component[]{
                new JMenuItem(LayerListDialog.getInstance().createShowHideLayerAction(this)),
                new JMenuItem(LayerListDialog.getInstance().createDeleteLayerAction(this)),
                new JSeparator(),
                sync,
                new JSeparator(),
                new JMenuItem(new RenameLayerAction(null, this)),
                new JSeparator(),
                new JMenuItem(new LayerListPopup.InfoAction(this))};
    }

    private void calculatePosition() {
        for (ImageEntry e : data) {
            TimedPoint lastTP = null;
            for (TimedPoint tp : gps) {
                Date time = new Date(tp.time.getTime() - (delta+gpstimezone));
                if (time.after(e.time) && lastTP != null) {
                    e.pos = new CachedLatLon(lastTP.pos.getCenter(tp.pos));
                    break;
                }
                lastTP = tp;
            }
            if (e.pos == null) {
                e.pos = gps.getLast().pos;
            }
        }
    }

    private void sync(File f) {
        Date exifDate;
        try {
            exifDate = ExifReader.readTime(f);
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("The date in file \"{0}\" could not be parsed.", f.getName()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        if (exifDate == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("There is no EXIF time within the file \"{0}\".", f.getName()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Image")), GBC.eol());
        p.add(new JLabel(new ImageIcon(imageLoader.waitForImage(f, 300))), GBC.eop());
        p.add(new JLabel(tr("Enter shown date (mm/dd/yyyy HH:MM:SS)")), GBC.eol());
        JTextField gpsText = new JTextField(dateFormat.format(new Date(exifDate.getTime()+delta)));
        p.add(gpsText, GBC.eol().fill(GBC.HORIZONTAL));
        p.add(new JLabel(tr("GPS unit timezone (difference to photo)")), GBC.eol());
        String t = Main.pref.get("tagimages.gpstimezone", "0");
        if (t.charAt(0) != '-') {
            t = "+"+t;
        }
        JTextField gpsTimezone = new JTextField(t);
        p.add(gpsTimezone, GBC.eol().fill(GBC.HORIZONTAL));

        while (true) {
            int answer = JOptionPane.showConfirmDialog(
                    Main.parent,
                    p,
                    tr("Synchronize Time with GPS Unit"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (answer != JOptionPane.OK_OPTION || gpsText.getText().equals(""))
                return;
            try {
                delta = DateParser.parse(gpsText.getText()).getTime() - exifDate.getTime();
                String time = gpsTimezone.getText();
                if (!time.equals("") && time.charAt(0) == '+') {
                    time = time.substring(1);
                }
                if (time.equals("")) {
                    time = "0";
                }
                gpstimezone = Long.valueOf(time)*60*60*1000;
                Main.pref.put("tagimages.delta", ""+delta);
                Main.pref.put("tagimages.gpstimezone", time);
                calculatePosition();
                return;
            } catch (NumberFormatException x) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Time entered could not be parsed."),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            } catch (ParseException x) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Time entered could not be parsed."),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

}
