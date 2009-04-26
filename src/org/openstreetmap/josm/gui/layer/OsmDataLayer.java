// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.osm.visitor.MapPaintVisitor;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer holding data from a specific dataset.
 * The data can be fully edited.
 *
 * @author imi
 */
public class OsmDataLayer extends Layer {

    public final static class DataCountVisitor extends AbstractVisitor {
        public final int[] normal = new int[3];
        public final int[] deleted = new int[3];
        public final String[] names = {"node", "way", "relation"};

        private void inc(final OsmPrimitive osm, final int i) {
            normal[i]++;
            if (osm.deleted)
                deleted[i]++;
        }

        public void visit(final Node n) {
            inc(n, 0);
        }

        public void visit(final Way w) {
            inc(w, 1);
        }
        public void visit(final Relation w) {
            inc(w, 2);
        }
    }

    public interface ModifiedChangedListener {
        void modifiedChanged(boolean value, OsmDataLayer source);
    }
    public interface CommandQueueListener {
        void commandChanged(int queueSize, int redoSize);
    }

    /**
     * The data behind this layer.
     */
    public final DataSet data;

    /**
     * Whether the data of this layer was modified during the session.
     */
    private boolean modified = false;
    /**
     * Whether the data was modified due an upload of the data to the server.
     */
    public boolean uploadedModified = false;

    public final LinkedList<ModifiedChangedListener> listenerModified = new LinkedList<ModifiedChangedListener>();
    public final LinkedList<DataChangeListener> listenerDataChanged = new LinkedList<DataChangeListener>();

    /**
     * a paint texture for non-downloaded area
     */
    private static TexturePaint hatched;

    static {
        createHatchTexture();
    }

    /**
     * Initialize the hatch pattern used to paint the non-downloaded area
     */
    public static void createHatchTexture() {
        BufferedImage bi = new BufferedImage(15, 15, BufferedImage.TYPE_INT_ARGB);
        Graphics2D big = bi.createGraphics();
        big.setColor(Main.pref.getColor(marktr("background"), Color.BLACK));
        Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
        big.setComposite(comp);
        big.fillRect(0,0,15,15);
        big.setColor(Main.pref.getColor(marktr("outside downloaded area"), Color.YELLOW));
        big.drawLine(0,15,15,0);
        Rectangle r = new Rectangle(0, 0, 15,15);
        hatched = new TexturePaint(bi, r);
    }

    /**
     * Construct a OsmDataLayer.
     */
    public OsmDataLayer(final DataSet data, final String name, final File associatedFile) {
        super(name);
        this.data = data;
        this.associatedFile = associatedFile;
    }

    /**
     * TODO: @return Return a dynamic drawn icon of the map data. The icon is
     *         updated by a background thread to not disturb the running programm.
     */
    @Override public Icon getIcon() {
        return ImageProvider.get("layer", "osmdata_small");
    }

    /**
     * Draw all primitives in this layer but do not draw modified ones (they
     * are drawn by the edit layer).
     * Draw nodes last to overlap the ways they belong to.
     */
    @Override public void paint(final Graphics g, final MapView mv) {
        boolean active = Main.map.mapView.getActiveLayer() == this;
        boolean inactive = !active && Main.pref.getBoolean("draw.data.inactive_color", true);
        boolean virtual = !inactive && Main.map.mapView.useVirtualNodes();

        // draw the hatched area for non-downloaded region. only draw if we're the active
        // and bounds are defined; don't draw for inactive layers or loaded GPX files etc
        if (active && Main.pref.getBoolean("draw.data.downloaded_area", true) && !data.dataSources.isEmpty()) {
            // initialize area with current viewport
            Rectangle b = Main.map.mapView.getBounds();
            // on some platforms viewport bounds seem to be offset from the left,
            // over-grow it just to be sure
            b.grow(100, 100);
            Area a = new Area(b);

            // now succesively subtract downloaded areas
            for (DataSource src : data.dataSources) {
                if (src.bounds != null && !src.bounds.min.equals(src.bounds.max)) {
                    EastNorth en1 = Main.proj.latlon2eastNorth(src.bounds.min);
                    EastNorth en2 = Main.proj.latlon2eastNorth(src.bounds.max);
                    Point p1 = mv.getPoint(en1);
                    Point p2 = mv.getPoint(en2);
                    Rectangle r = new Rectangle(Math.min(p1.x, p2.x),Math.min(p1.y, p2.y),Math.abs(p2.x-p1.x),Math.abs(p2.y-p1.y));
                    a.subtract(new Area(r));
                }
            }

            // paint remainder
            ((Graphics2D)g).setPaint(hatched);
            ((Graphics2D)g).fill(a);
        }

        SimplePaintVisitor painter;
        if (Main.pref.getBoolean("draw.wireframe"))
            painter = new SimplePaintVisitor();
        else
            painter = new MapPaintVisitor();
        painter.setGraphics(g);
        painter.setNavigatableComponent(mv);
        painter.inactive = inactive;
        painter.visitAll(data, virtual);
        Main.map.conflictDialog.paintConflicts(g, mv);
    }

    @Override public String getToolTipText() {
        String tool = "";
        tool += undeletedSize(data.nodes)+" "+trn("node", "nodes", undeletedSize(data.nodes))+", ";
        tool += undeletedSize(data.ways)+" "+trn("way", "ways", undeletedSize(data.ways));
        if (data.version != null) tool += ", " + tr("version {0}", data.version);
        if (associatedFile != null)
            tool = "<html>"+tool+"<br>"+associatedFile.getPath()+"</html>";
        return tool;
    }

    @Override public void mergeFrom(final Layer from) {
        final MergeVisitor visitor = new MergeVisitor(data,((OsmDataLayer)from).data);
        for (final OsmPrimitive osm : ((OsmDataLayer)from).data.allPrimitives()) {
//            i++;
//            if(i%100 == 0) {
//                double perc = (((double)i) / ((double)max) * 100.0);
//                System.out.format(" " + (int)perc + "%%");
//            }
            osm.visit(visitor);
        }
        visitor.fixReferences();
//        System.out.println("");

        Area a = data.getDataSourceArea();
        
        // copy the merged layer's data source info; 
        // only add source rectangles if they are not contained in the
        // layer already.
        for (DataSource src : ((OsmDataLayer)from).data.dataSources) {
            if (a == null || !a.contains(src.bounds.asRect()))
                data.dataSources.add(src);
        }
        
        // copy the merged layer's API version, downgrade if required
        if (data.version == null) {
            data.version = ((OsmDataLayer)from).data.version;
        } else {
            if ("0.5".equals(data.version) ^ "0.5".equals(((OsmDataLayer)from).data.version)) {
                System.err.println("Warning: mixing 0.6 and 0.5 data results in version 0.5");
                data.version = "0.5";
            }
        }
        fireDataChange();
        // repaint to make sure new data is displayed properly.
        Main.map.mapView.repaint();

        if (visitor.conflicts.isEmpty())
            return;
        final ConflictDialog dlg = Main.map.conflictDialog;
        dlg.add(visitor.conflicts);
        JOptionPane.showMessageDialog(Main.parent,tr("There were conflicts during import."));
        if (!dlg.isVisible())
            dlg.action.actionPerformed(new ActionEvent(this, 0, ""));
    }

    @Override public boolean isMergable(final Layer other) {
        return other instanceof OsmDataLayer;
    }

    @Override public void visitBoundingBox(final BoundingXYVisitor v) {
        for (final Node n : data.nodes)
            if (!n.deleted && !n.incomplete)
                v.visit(n);
    }

    /**
     * Clean out the data behind the layer. This means clearing the redo/undo lists,
     * really deleting all deleted objects and reset the modified flags. This is done
     * after a successfull upload.
     *
     * @param processed A list of all objects that were actually uploaded.
     *         May be <code>null</code>, which means nothing has been uploaded but
     *         saved to disk instead. Note that an empty collection for "processed"
     *      means that an upload has been attempted but failed.
     */
    public void cleanData(final Collection<OsmPrimitive> processed, boolean dataAdded) {

        // return immediately if an upload attempt failed
        if (processed != null && processed.isEmpty() && !dataAdded)
            return;

        Main.main.undoRedo.clean();

        // if uploaded, clean the modified flags as well
        if (processed != null) {
            final Set<OsmPrimitive> processedSet = new HashSet<OsmPrimitive>(processed);
            for (final Iterator<Node> it = data.nodes.iterator(); it.hasNext();)
                cleanIterator(it, processedSet);
            for (final Iterator<Way> it = data.ways.iterator(); it.hasNext();)
                cleanIterator(it, processedSet);
            for (final Iterator<Relation> it = data.relations.iterator(); it.hasNext();)
                cleanIterator(it, processedSet);
        }

        // update the modified flag
        if (associatedFile != null && processed != null && !dataAdded)
            return; // do nothing when uploading non-harmful changes.

        // modified if server changed the data (esp. the id).
        uploadedModified = associatedFile != null && processed != null && dataAdded;
        setModified(uploadedModified);
    }

    /**
     * Clean the modified flag for the given iterator over a collection if it is in the
     * list of processed entries.
     *
     * @param it The iterator to change the modified and remove the items if deleted.
     * @param processed A list of all objects that have been successfully progressed.
     *         If the object in the iterator is not in the list, nothing will be changed on it.
     */
    private void cleanIterator(final Iterator<? extends OsmPrimitive> it, final Collection<OsmPrimitive> processed) {
        final OsmPrimitive osm = it.next();
        if (!processed.remove(osm))
            return;
        osm.modified = false;
        if (osm.deleted)
            it.remove();
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(final boolean modified) {
        if (modified == this.modified)
            return;
        this.modified = modified;
        for (final ModifiedChangedListener l : listenerModified)
            l.modifiedChanged(modified, this);
    }

    /**
     * @return The number of not-deleted primitives in the list.
     */
    private int undeletedSize(final Collection<? extends OsmPrimitive> list) {
        int size = 0;
        for (final OsmPrimitive osm : list)
            if (!osm.deleted)
                size++;
        return size;
    }

    @Override public Object getInfoComponent() {
        final DataCountVisitor counter = new DataCountVisitor();
        for (final OsmPrimitive osm : data.allPrimitives())
            osm.visit(counter);
        final JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("{0} consists of:", name)), GBC.eol());
        for (int i = 0; i < counter.normal.length; ++i) {
            String s = counter.normal[i]+" "+trn(counter.names[i],counter.names[i]+"s",counter.normal[i]);
            if (counter.deleted[i] > 0)
                s += tr(" ({0} deleted.)",counter.deleted[i]);
            p.add(new JLabel(s, ImageProvider.get("data", counter.names[i]), JLabel.HORIZONTAL), GBC.eop().insets(15,0,0,0));
        }
        p.add(new JLabel(tr("API version: {0}", (data.version != null) ? data.version : tr("unset"))));

        return p;
    }

    @Override public Component[] getMenuEntries() {
        if (Main.applet) {
            return new Component[]{
                    new JMenuItem(new LayerListDialog.ShowHideLayerAction(this)),
                    new JMenuItem(new LayerListDialog.DeleteLayerAction(this)),
                    new JSeparator(),
                    new JMenuItem(new RenameLayerAction(associatedFile, this)),
                    new JSeparator(),
                    new JMenuItem(new LayerListPopup.InfoAction(this))};
        }
        return new Component[]{
                new JMenuItem(new LayerListDialog.ShowHideLayerAction(this)),
                new JMenuItem(new LayerListDialog.DeleteLayerAction(this)),
                new JSeparator(),
                new JMenuItem(new SaveAction(this)),
                new JMenuItem(new SaveAsAction(this)),
                new JMenuItem(new GpxExportAction(this)),
                new JMenuItem(new ConvertToGpxLayerAction()),
                new JSeparator(),
                new JMenuItem(new RenameLayerAction(associatedFile, this)),
                new JSeparator(),
                new JMenuItem(new LayerListPopup.InfoAction(this))};
    }

    public void fireDataChange() {
        for (DataChangeListener dcl : listenerDataChanged) {
            dcl.dataChanged(this);
        }
    }

    public static GpxData toGpxData(DataSet data, File file) {
        GpxData gpxData = new GpxData();
        gpxData.storageFile = file;
        HashSet<Node> doneNodes = new HashSet<Node>();
        for (Way w : data.ways) {
            if (w.incomplete || w.deleted) continue;
            GpxTrack trk = new GpxTrack();
            gpxData.tracks.add(trk);

            if (w.get("name") != null)
                trk.attr.put("name", w.get("name"));

            ArrayList<WayPoint> trkseg = null;
            for (Node n : w.nodes) {
                if (n.incomplete || n.deleted) {
                    trkseg = null;
                    continue;
                }
                if (trkseg == null) {
                    trkseg = new ArrayList<WayPoint>();
                    trk.trackSegs.add(trkseg);
                }
                if (!n.isTagged()) {
                    doneNodes.add(n);
                }
                WayPoint wpt = new WayPoint(n.coor);                
                if (!n.isTimestampEmpty())
                {
                    wpt.attr.put("time", DateUtils.fromDate(n.getTimestamp()));
                    wpt.setTime();
                }
                trkseg.add(wpt);
            }
        }

        // what is this loop meant to do? it creates waypoints but never
        // records them?
        for (Node n : data.nodes) {
            if (n.incomplete || n.deleted || doneNodes.contains(n)) continue;
            WayPoint wpt = new WayPoint(n.coor);
            if (!n.isTimestampEmpty()) {
                wpt.attr.put("time", DateUtils.fromDate(n.getTimestamp()));
                wpt.setTime();
            }
            if (n.keys != null && n.keys.containsKey("name")) {
                wpt.attr.put("name", n.keys.get("name"));
            }
        }
        return gpxData;
    }

    public GpxData toGpxData() {
        return toGpxData(data, associatedFile);
    }

    public class ConvertToGpxLayerAction extends AbstractAction {
        public ConvertToGpxLayerAction() {
            super(tr("Convert to GPX layer"), ImageProvider.get("converttogpx"));
        }
        public void actionPerformed(ActionEvent e) {
            Main.main.addLayer(new GpxLayer(toGpxData(), tr("Converted from: {0}", name)));
            Main.main.removeLayer(OsmDataLayer.this);
        }
    }

    public boolean containsPoint(LatLon coor) {
        // we'll assume that if this has no data sources
        // that it also has no borders
        if (this.data.dataSources.isEmpty())
            return true;

        boolean layer_bounds_point = false;
        for (DataSource src : this.data.dataSources) {
            if (src.bounds.contains(coor)) {
                layer_bounds_point = true;
                break;
            }
        }
        return layer_bounds_point;
    }
}
