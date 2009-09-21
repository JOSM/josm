// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;
import static org.openstreetmap.josm.tools.I18n.trnc;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UrlLabel;

/**
 * A layer holding data from a gps source.
 * The data is read only.
 *
 * @author imi
 */
public class RawGpsLayer extends Layer implements PreferenceChangedListener {

    public class ConvertToDataLayerAction extends AbstractAction {
        public ConvertToDataLayerAction() {
            super(tr("Convert to data layer"), ImageProvider.get("converttoosm"));
        }
        public void actionPerformed(ActionEvent e) {
            JPanel msg = new JPanel(new GridBagLayout());
            msg.add(new JLabel(tr("<html>Upload of unprocessed GPS data as map data is considered harmful.<br>If you want to upload traces, look here:</html>")), GBC.eol());
            msg.add(new UrlLabel(tr("http://www.openstreetmap.org/traces")), GBC.eop());
            if (!ConditionalOptionPaneUtil.showConfirmationDialog(
                    "convert_to_data",
                    Main.parent,
                    msg,
                    tr("Warning"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.OK_OPTION))
                return;
            DataSet ds = new DataSet();
            for (Collection<GpsPoint> c : data) {
                List<Node> nodes = new ArrayList<Node>();
                for (GpsPoint p : c) {
                    Node n = new Node(p.latlon);
                    ds.nodes.add(n);
                    nodes.add(n);
                }
                Way w = new Way();
                w.setNodes(nodes);
                ds.ways.add(w);
            }
            Main.main.addLayer(new OsmDataLayer(ds, tr("Converted from: {0}", RawGpsLayer.this.getName()), null));
            Main.main.removeLayer(RawGpsLayer.this);
        }
    }

    public static class GpsPoint {
        public final LatLon latlon;
        public final EastNorth eastNorth;
        public final String time;
        public GpsPoint(LatLon ll, String t) {
            latlon = ll;
            eastNorth = Main.proj.latlon2eastNorth(ll);
            time = t;
        }
    }

    /**
     * A list of ways which containing a list of points.
     */
    public final Collection<Collection<GpsPoint>> data;
    public final boolean fromServer;

    public RawGpsLayer(boolean fromServer, Collection<Collection<GpsPoint>> data, String name, File associatedFile) {
        super(name);
        this.fromServer = fromServer;
        setAssociatedFile(associatedFile);
        this.data = data;
        Main.pref.listener.add(this);
    }

    /**
     * Return a static icon.
     */
    @Override public Icon getIcon() {
        return ImageProvider.get("layer", "rawgps_small");
    }

    @Override public void paint(Graphics g, MapView mv) {
        g.setColor(Main.pref.getColor(marktr("gps point"), "layer "+getName(), Color.gray));
        Point old = null;

        boolean force = Main.pref.getBoolean("draw.rawgps.lines.force");
        boolean lines = Main.pref.getBoolean("draw.rawgps.lines", true);
        String linesKey = "draw.rawgps.lines.layer "+getName();
        if (Main.pref.hasKey(linesKey)) {
            lines = Main.pref.getBoolean(linesKey);
        }
        boolean large = Main.pref.getBoolean("draw.rawgps.large");
        for (Collection<GpsPoint> c : data) {
            if (!force) {
                old = null;
            }
            for (GpsPoint p : c) {
                Point screen = mv.getPoint(p.eastNorth);
                if (lines && old != null) {
                    g.drawLine(old.x, old.y, screen.x, screen.y);
                } else if (!large) {
                    g.drawRect(screen.x, screen.y, 0, 0);
                }
                if (large) {
                    g.fillRect(screen.x-1, screen.y-1, 3, 3);
                }
                old = screen;
            }
        }
    }

    @Override public String getToolTipText() {
        int points = 0;
        for (Collection<GpsPoint> c : data) {
            points += c.size();
        }
        String tool = data.size()+" "+trnc("gps", "track", "tracks", data.size())
        +" "+points+" "+trn("point", "points", points);
        File f = getAssociatedFile();
        if (f != null) {
            tool = "<html>"+tool+"<br>"+f.getPath()+"</html>";
        }
        return tool;
    }

    @Override public void mergeFrom(Layer from) {
        RawGpsLayer layer = (RawGpsLayer)from;
        data.addAll(layer.data);
    }

    @Override public boolean isMergable(Layer other) {
        return other instanceof RawGpsLayer;
    }

    @Override public void visitBoundingBox(BoundingXYVisitor v) {
        for (Collection<GpsPoint> c : data) {
            for (GpsPoint p : c) {
                v.visit(p.eastNorth);
            }
        }
    }

    @Override public Object getInfoComponent() {
        StringBuilder b = new StringBuilder();
        int points = 0;
        for (Collection<GpsPoint> c : data) {
            b.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"+trn("a track with {0} point","a track with {0} points", c.size(), c.size())+"<br>");
            points += c.size();
        }
        b.append("</html>");
        return "<html>"+trn("{0} consists of {1} track", "{0} consists of {1} tracks", data.size(), getName(), data.size())+" ("+trn("{0} point", "{0} points", points, points)+")<br>"+b.toString();
    }

    @Override public Component[] getMenuEntries() {
        JMenuItem line = new JMenuItem(tr("Customize line drawing"), ImageProvider.get("mapmode/addsegment"));
        line.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                JRadioButton[] r = new JRadioButton[3];
                r[0] = new JRadioButton(tr("Use global settings."));
                r[1] = new JRadioButton(tr("Draw lines between points for this layer."));
                r[2] = new JRadioButton(tr("Do not draw lines between points for this layer."));
                ButtonGroup group = new ButtonGroup();
                Box panel = Box.createVerticalBox();
                for (JRadioButton b : r) {
                    group.add(b);
                    panel.add(b);
                }
                String propName = "draw.rawgps.lines.layer "+getName();
                if (Main.pref.hasKey(propName)) {
                    group.setSelected(r[Main.pref.getBoolean(propName) ? 1:2].getModel(), true);
                } else {
                    group.setSelected(r[0].getModel(), true);
                }
                int answer = JOptionPane.showConfirmDialog(
                        Main.parent,
                        panel,
                        tr("Select line drawing options"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );
                if (answer == JOptionPane.CANCEL_OPTION)
                    return;
                if (group.getSelection() == r[0].getModel()) {
                    Main.pref.put(propName, null);
                } else {
                    Main.pref.put(propName, group.getSelection() == r[1].getModel());
                }
                Main.map.repaint();
            }
        });

        JMenuItem color = new JMenuItem(tr("Customize Color"), ImageProvider.get("colorchooser"));
        color.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                JColorChooser c = new JColorChooser(Main.pref.getColor(marktr("gps point"), "layer "+getName(), Color.gray));
                Object[] options = new Object[]{tr("OK"), tr("Cancel"), tr("Default")};
                int answer = JOptionPane.showOptionDialog(
                        Main.parent,
                        c,
                        tr("Choose a color"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE, null,options, options[0]);
                switch (answer) {
                    case 0:
                        Main.pref.putColor("layer "+getName(), c.getColor());
                        break;
                    case 1:
                        return;
                    case 2:
                        Main.pref.putColor("layer "+getName(), null);
                        break;
                }
                Main.map.repaint();
            }
        });

        if (Main.applet)
            return new Component[]{
                new JMenuItem(LayerListDialog.getInstance().createShowHideLayerAction(this)),
                new JMenuItem(LayerListDialog.getInstance().createDeleteLayerAction(this)),
                new JSeparator(),
                color,
                line,
                new JMenuItem(new ConvertToDataLayerAction()),
                new JSeparator(),
                new JMenuItem(new RenameLayerAction(getAssociatedFile(), this)),
                new JSeparator(),
                new JMenuItem(new LayerListPopup.InfoAction(this))};
        return new Component[]{
                new JMenuItem(LayerListDialog.getInstance().createShowHideLayerAction(this)),
                new JMenuItem(LayerListDialog.getInstance().createDeleteLayerAction(this)),
                new JSeparator(),
                new JMenuItem(new LayerGpxExportAction(this)),
                color,
                line,
                new JMenuItem(new ConvertToDataLayerAction()),
                new JSeparator(),
                new JMenuItem(new RenameLayerAction(getAssociatedFile(), this)),
                new JSeparator(),
                new JMenuItem(new LayerListPopup.InfoAction(this))};
    }

    public void preferenceChanged(String key, String newValue) {
        if (Main.map != null && (key.equals("draw.rawgps.lines") || key.equals("draw.rawgps.lines.force"))) {
            Main.map.repaint();
        }
    }

    @Override public void destroy() {
        Main.pref.listener.remove(RawGpsLayer.this);
    }
}
