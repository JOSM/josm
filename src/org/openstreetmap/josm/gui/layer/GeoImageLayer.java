//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
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
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

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
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
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

    private static final class ImageEntry implements Comparable<ImageEntry> {
        File image;
        Date time;
        LatLon coor;
        EastNorth pos;
        Icon icon;
        public int compareTo(ImageEntry image) {
            return time.compareTo(image.time);
        }
    }

    private static final class Loader extends PleaseWaitRunnable {
        boolean cancelled = false;
        private GeoImageLayer layer;
        private final Collection<File> files;
        private final GpxLayer gpxLayer;
        public Loader(Collection<File> files, GpxLayer gpxLayer) {
            super(tr("Images for {0}", gpxLayer.name));
            this.files = files;
            this.gpxLayer = gpxLayer;
        }
        @Override protected void realRun() throws IOException {
            Main.pleaseWaitDlg.currentAction.setText(tr("Read GPX..."));
            LinkedList<TimedPoint> gps = new LinkedList<TimedPoint>();

            // Extract dates and locations from GPX input

            for (GpxTrack trk : gpxLayer.data.tracks) {
                for (Collection<WayPoint> segment : trk.trackSegs) {
                    for (WayPoint p : segment) {
                    if (!p.attr.containsKey("time"))
                        throw new IOException(tr("No time for point {0} x {1}",p.latlon.lat(),p.latlon.lon()));
                    Date d = null;
                    try {
                        d = DateParser.parse((String) p.attr.get("time"));
                    } catch (ParseException e) {
                        throw new IOException(tr("Cannot read time \"{0}\" from point {1} x {2}",p.attr.get("time"),p.latlon.lat(),p.latlon.lon()));
                    }
                    gps.add(new TimedPoint(d, p.eastNorth));
                }
            }
            }

            if (gps.isEmpty()) {
                errorMessage = tr("No images with readable timestamps found.");
                return;
            }

            // read the image files
            ArrayList<ImageEntry> data = new ArrayList<ImageEntry>(files.size());
            int i = 0;
            Main.pleaseWaitDlg.progress.setMaximum(files.size());
            for (File f : files) {
                if (cancelled)
                    break;
                Main.pleaseWaitDlg.currentAction.setText(tr("Reading {0}...",f.getName()));
                Main.pleaseWaitDlg.progress.setValue(i++);

                ImageEntry e = new ImageEntry();
                try {
                    e.time = ExifReader.readTime(f);
                } catch (ParseException e1) {
                    continue;
                }
                if (e.time == null)
                    continue;
                e.image = f;
                e.icon = loadScaledImage(f, 16);

                data.add(e);
            }
            layer = new GeoImageLayer(data, gps);
            layer.calculatePosition();
        }
        @Override protected void finish() {
            if (layer != null)
                Main.main.addLayer(layer);
        }
        @Override protected void cancel() {cancelled = true;}
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
    private int currentImage;

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
        EastNorth pos;
        public TimedPoint(Date time, EastNorth pos) {
            this.time = time;
            this.pos = pos;
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
                if (visible)
                    Main.map.mapView.repaint();
            }
            @Override public void mouseReleased(MouseEvent ev) {
                if (ev.getButton() != MouseEvent.BUTTON1)
                    return;
                mousePressed = false;
                if (!visible)
                    return;
                for (int i = data.size(); i > 0; --i) {
                    ImageEntry e = data.get(i-1);
                    if (e.pos == null)
                        continue;
                    Point p = Main.map.mapView.getPoint(e.pos);
                    Rectangle r = new Rectangle(p.x-e.icon.getIconWidth()/2, p.y-e.icon.getIconHeight()/2, e.icon.getIconWidth(), e.icon.getIconHeight());
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
                if (oldLayer == self)
                    Main.map.mapView.removeMouseListener(mouseAdapter);
            }
        });
    }

    private void showImage(int i) {
        currentImage = i;
        final JPanel p = new JPanel(new BorderLayout());
        final ImageEntry e = data.get(currentImage);
        if (!(e.image.exists() && e.image.canRead()))
        {
            JOptionPane.showMessageDialog(Main.parent,
            tr("Image with path {0} does not exist or is not readable.", e.image),
            tr("Warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        final JScrollPane scroll = new JScrollPane(new JLabel(loadScaledImage(e.image, 580)));
        final JViewport vp = scroll.getViewport();
        p.add(scroll, BorderLayout.CENTER);

        final JToggleButton scale = new JToggleButton(ImageProvider.get("dialogs", "zoom-best-fit"));
        final JButton next  = new JButton(ImageProvider.get("dialogs", "next"));
        final JButton prev = new JButton(ImageProvider.get("dialogs", "previous"));
        final JToggleButton cent = new JToggleButton(ImageProvider.get("dialogs", "centreview"));

        JPanel p2 = new JPanel();
        p2.add(prev);
        p2.add(scale);
        p2.add(cent);
        p2.add(next);
        prev.setEnabled(currentImage>0?true:false);
        next.setEnabled(currentImage<data.size()-1?true:false);
        p.add(p2, BorderLayout.SOUTH);
        final JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE);
        final JDialog dlg = pane.createDialog(Main.parent, e.image+" ("+e.coor.toDisplayString()+")");
        scale.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev) {
                p.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (scale.getModel().isSelected())
                    ((JLabel)vp.getView()).setIcon(loadScaledImage(e.image, Math.max(vp.getWidth(), vp.getHeight())));
                else
                    ((JLabel)vp.getView()).setIcon(new ImageIcon(e.image.getPath()));
                p.setCursor(Cursor.getDefaultCursor());
            }
        });
        scale.setSelected(true);
        cent.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev) {
                final ImageEntry e = data.get(currentImage);
                if (cent.getModel().isSelected())
                    Main.map.mapView.zoomTo(e.pos, Main.map.mapView.getScale());
            }
        });

        ActionListener nextprevAction = new ActionListener(){
            public void actionPerformed(ActionEvent ev) {
                p.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                if (ev.getActionCommand().equals("Next")) {
                    currentImage++;
                    if(currentImage>=data.size()-1) next.setEnabled(false);
                    prev.setEnabled(true);
                } else {
                    currentImage--;
                    if(currentImage<=0) prev.setEnabled(false);
                    next.setEnabled(true);
                }

                final ImageEntry e = data.get(currentImage);
                if (scale.getModel().isSelected())
                    ((JLabel)vp.getView()).setIcon(loadScaledImage(e.image, Math.max(vp.getWidth(), vp.getHeight())));
                else
                    ((JLabel)vp.getView()).setIcon(new ImageIcon(e.image.getPath()));
                dlg.setTitle(e.image+" ("+e.coor.toDisplayString()+")");
                if (cent.getModel().isSelected())
                    Main.map.mapView.zoomTo(e.pos, Main.map.mapView.getScale());
                p.setCursor(Cursor.getDefaultCursor());
            }
        };
        next.setActionCommand("Next");
        prev.setActionCommand("Previous");
        next.setMnemonic(KeyEvent.VK_RIGHT);
        prev.setMnemonic(KeyEvent.VK_LEFT);
        scale.setMnemonic(KeyEvent.VK_F);
        cent.setMnemonic(KeyEvent.VK_C);
        next.setToolTipText("Show next image");
        prev.setToolTipText("Show previous image");
        cent.setToolTipText("Centre image location in main display");
        scale.setToolTipText("Scale image to fit");

        prev.addActionListener(nextprevAction);
        next.addActionListener(nextprevAction);
        cent.setSelected(false);

        dlg.addComponentListener(new ComponentListener() {
            boolean ignoreEvent = true;
            public void componentHidden(ComponentEvent e) {}
            public void componentMoved(ComponentEvent e) {}
            public void componentResized(ComponentEvent ev) {
                // we ignore the first resize event, as the picture is scaled already on load:
                if (scale.getModel().isSelected() && !ignoreEvent) {
                    ((JLabel)vp.getView()).setIcon(loadScaledImage(e.image, Math.max(vp.getWidth(), vp.getHeight())));
                }
                ignoreEvent = false;
            }
            public void componentShown(ComponentEvent e) {}

        });
        dlg.setModal(false);
        dlg.setVisible(true);
        dlg.setResizable(true);
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
                setIcon(e.icon);
                setText(e.image.getName()+" ("+dateFormat.format(new Date(e.time.getTime()+(delta+gpstimezone)))+")");
                if (e.pos == null)
                    setForeground(Color.red);
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
            if (e.pos != null)
                i++;
        return data.size()+" "+trn("image","images",data.size())+". "+tr("{0} within the track.",i);
    }

    @Override public boolean isMergable(Layer other) {
        return other instanceof GeoImageLayer;
    }

    @Override public void mergeFrom(Layer from) {
        GeoImageLayer l = (GeoImageLayer)from;
        data.addAll(l.data);
    }

    @Override public void paint(Graphics g, MapView mv) {
        boolean clickedFound = false;
        for (ImageEntry e : data) {
            if (e.pos != null) {
                Point p = mv.getPoint(e.pos);
                Rectangle r = new Rectangle(p.x-e.icon.getIconWidth()/2, p.y-e.icon.getIconHeight()/2, e.icon.getIconWidth(), e.icon.getIconHeight());
                e.icon.paintIcon(mv, g, r.x, r.y);
                Border b = null;
                Point mousePosition = mv.getMousePosition();
                if (mousePosition == null)
                    continue; // mouse outside the whole window
                if (!clickedFound && mousePressed && r.contains(mousePosition)) {
                    b = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
                    clickedFound = true;
                } else
                    b = BorderFactory.createBevelBorder(BevelBorder.RAISED);
                Insets inset = b.getBorderInsets(mv);
                r.grow((inset.top+inset.bottom)/2, (inset.left+inset.right)/2);
                b.paintBorder(mv, g, r.x, r.y, r.width, r.height);
            }
        }
    }

    @Override public void visitBoundingBox(BoundingXYVisitor v) {
        for (ImageEntry e : data)
            v.visit(e.pos);
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
                new JMenuItem(new LayerListDialog.ShowHideLayerAction(this)),
                new JMenuItem(new LayerListDialog.DeleteLayerAction(this)),
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
                    double x = (lastTP.pos.east()+tp.pos.east())/2;
                    double y = (lastTP.pos.north()+tp.pos.north())/2;
                    e.pos = new EastNorth(x,y);
                    break;
                }
                lastTP = tp;
            }
            if (e.pos != null)
                e.coor = Main.proj.eastNorth2latlon(e.pos);
        }
    }

    private void sync(File f) {
        Date exifDate;
        try {
            exifDate = ExifReader.readTime(f);
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(Main.parent, tr("The date in file \"{0}\" could not be parsed.", f.getName()));
            return;
        }
        if (exifDate == null) {
            JOptionPane.showMessageDialog(Main.parent, tr("There is no EXIF time within the file \"{0}\".", f.getName()));
            return;
        }
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Image")), GBC.eol());
        p.add(new JLabel(loadScaledImage(f, 300)), GBC.eop());
        p.add(new JLabel(tr("Enter shown date (mm/dd/yyyy HH:MM:SS)")), GBC.eol());
        JTextField gpsText = new JTextField(dateFormat.format(new Date(exifDate.getTime()+delta)));
        p.add(gpsText, GBC.eol().fill(GBC.HORIZONTAL));
        p.add(new JLabel(tr("GPS unit timezone (difference to photo)")), GBC.eol());
        String t = Main.pref.get("tagimages.gpstimezone", "0");
        if (t.charAt(0) != '-')
            t = "+"+t;
        JTextField gpsTimezone = new JTextField(t);
        p.add(gpsTimezone, GBC.eol().fill(GBC.HORIZONTAL));

        while (true) {
            int answer = JOptionPane.showConfirmDialog(Main.parent, p, tr("Synchronize Time with GPS Unit"), JOptionPane.OK_CANCEL_OPTION);
            if (answer != JOptionPane.OK_OPTION || gpsText.getText().equals(""))
                return;
            try {
                delta = DateParser.parse(gpsText.getText()).getTime() - exifDate.getTime();
                String time = gpsTimezone.getText();
                if (!time.equals("") && time.charAt(0) == '+')
                    time = time.substring(1);
                if (time.equals(""))
                    time = "0";
                gpstimezone = Long.valueOf(time)*60*60*1000;
                Main.pref.put("tagimages.delta", ""+delta);
                Main.pref.put("tagimages.gpstimezone", time);
                calculatePosition();
                return;
            } catch (NumberFormatException x) {
                JOptionPane.showMessageDialog(Main.parent, tr("Time entered could not be parsed."));
            } catch (ParseException x) {
                JOptionPane.showMessageDialog(Main.parent, tr("Time entered could not be parsed."));
            }
        }
    }

    private static Icon loadScaledImage(File f, int maxSize) {
        Image img = Toolkit.getDefaultToolkit().createImage(f.getPath());
        while (img.getWidth(null) < 0 || img.getHeight(null) < 0) {
          try {
            Thread.sleep(10);
          } catch(InterruptedException ie) {}
        }
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        if (w>h) {
            h = Math.round(maxSize*((float)h/w));
            w = maxSize;
        } else {
            w = Math.round(maxSize*((float)w/h));
            h = maxSize;
        }
        return new ImageIcon(createResizedCopy(img, w, h));
    }

    private static BufferedImage createResizedCopy(Image originalImage,
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
}
