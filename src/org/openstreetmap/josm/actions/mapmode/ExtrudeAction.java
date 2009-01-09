// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Makes a rectangle from a line, or modifies a rectangle.
 *
 * This class currently contains some "sleeping" code copied from DrawAction (move and rotate)
 * which can eventually be removed, but it may also get activated here and removed in DrawAction.
 */
public class ExtrudeAction extends MapMode implements MapViewPaintable {

    enum Mode { EXTRUDE, rotate, select }
    private Mode mode = null;
    private long mouseDownTime = 0;
    private WaySegment selectedSegment = null;
    private Color selectedColor;

    double xoff;
    double yoff;
    double distance;

    /**
     * The old cursor before the user pressed the mouse button.
     */
    private Cursor oldCursor;
    /**
     * The current position of the mouse
     */
    private Point mousePos;
    /**
     * The position of the mouse cursor when the drag action was initiated.
     */
    private Point initialMousePos;
    /**
     * The time which needs to pass between click and release before something
     * counts as a move, in milliseconds
     */
    private int initialMoveDelay = 200;

    /**
     * Create a new SelectAction
     * @param mapFrame The MapFrame this action belongs to.
     */
    public ExtrudeAction(MapFrame mapFrame) {
        super(tr("Extrude"), "extrude/extrude", tr("Create areas"),
                Shortcut.registerShortcut("mapmode:extrude", tr("Mode: {0}", tr("Extrude")), KeyEvent.VK_X, Shortcut.GROUP_EDIT),
            mapFrame,
            getCursor("normal", "rectangle", Cursor.DEFAULT_CURSOR));
        putValue("help", "Action/Extrude/Extrude");
        initialMoveDelay = Main.pref.getInteger("edit.initial-move-delay",200);
        selectedColor = Main.pref.getColor(marktr("selected"), Color.red);
    }

    private static Cursor getCursor(String name, String mod, int def) {
        try {
            return ImageProvider.getCursor(name, mod);
        } catch (Exception e) {
        }
        return Cursor.getPredefinedCursor(def);
    }

    private void setCursor(Cursor c) {
        if (oldCursor == null) {
            oldCursor = Main.map.mapView.getCursor();
            Main.map.mapView.setCursor(c);
        }
    }

    private void restoreCursor() {
        if (oldCursor != null) {
            Main.map.mapView.setCursor(oldCursor);
            oldCursor = null;
        }
    }

    @Override public void enterMode() {
        super.enterMode();
        Main.map.mapView.addMouseListener(this);
        Main.map.mapView.addMouseMotionListener(this);
    }

    @Override public void exitMode() {
        super.exitMode();
        Main.map.mapView.removeMouseListener(this);
        Main.map.mapView.removeMouseMotionListener(this);
        Main.map.mapView.removeTemporaryLayer(this);

    }

    /**
     * If the left mouse button is pressed, move all currently selected
     * objects (if one of them is under the mouse) or the current one under the
     * mouse (which will become selected).
     */
    @Override public void mouseDragged(MouseEvent e) {
        if (mode == Mode.select) return;

        // do not count anything as a move if it lasts less than 100 milliseconds.
        if ((mode == Mode.EXTRUDE) && (System.currentTimeMillis() - mouseDownTime < initialMoveDelay)) return;

        if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
            return;

        if (mode == Mode.EXTRUDE) {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        if (mousePos == null) {
            mousePos = e.getPoint();
            return;
        }

        Main.map.mapView.repaint();
        mousePos = e.getPoint();

    }

    public void paint(Graphics g, MapView mv) {
        if (selectedSegment != null) {
            Node n1 = selectedSegment.way.nodes.get(selectedSegment.lowerIndex);
            Node n2 = selectedSegment.way.nodes.get(selectedSegment.lowerIndex + 1);

            EastNorth en1 = n1.eastNorth;
            EastNorth en2 = n2.eastNorth;
            EastNorth en3 = mv.getEastNorth(mousePos.x, mousePos.y);

            double u = ((en3.east() - en1.east()) * (en2.east() - en1.east()) +
                        (en3.north() - en1.north()) * (en2.north() - en1.north())) /
                       en2.distanceSq(en1);
            // the point on the segment from which the distance to mouse pos is shortest
            EastNorth base = new EastNorth(en1.east() + u * (en2.east() - en1.east()),
                                           en1.north() + u * (en2.north() - en1.north()));

            // find out the distance, in metres, between the base point and the mouse cursor
            distance = Main.proj.eastNorth2latlon(base).greatCircleDistance(Main.proj.eastNorth2latlon(en3));
            Main.map.statusLine.setDist(distance);
            updateStatusLine();

            // compute vertical and horizontal components.
            xoff = en3.east() - base.east();
            yoff = en3.north() - base.north();

            Graphics2D g2 = (Graphics2D)g;
            g2.setColor(selectedColor);
            g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            GeneralPath b = new GeneralPath();
            Point p1 = mv.getPoint(en1);
            Point p2 = mv.getPoint(en2);
            Point p3 = mv.getPoint(en1.add(xoff, yoff));
            Point p4 = mv.getPoint(en2.add(xoff, yoff));

            b.moveTo(p1.x, p1.y); b.lineTo(p3.x, p3.y);
            b.lineTo(p4.x, p4.y); b.lineTo(p2.x, p2.y);
            b.lineTo(p1.x, p1.y);
            g2.draw(b);
            g2.setStroke(new BasicStroke(1));
        }
    }

    /**
     */
    @Override public void mousePressed(MouseEvent e) {
        if (!(Boolean)this.getValue("active")) return;
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        // boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        // boolean alt = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;
        // boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;

        mouseDownTime = System.currentTimeMillis();

        selectedSegment =
            Main.map.mapView.getNearestWaySegment(e.getPoint());

        mode = (selectedSegment == null) ? Mode.select : Mode.EXTRUDE;
        oldCursor = Main.map.mapView.getCursor();

        updateStatusLine();
        Main.map.mapView.addTemporaryLayer(this);
        Main.map.mapView.repaint();

        mousePos = e.getPoint();
        initialMousePos = e.getPoint();
    }

    /**
     * Restore the old mouse cursor.
     */
    @Override public void mouseReleased(MouseEvent e) {
        restoreCursor();
        if (selectedSegment == null) return;
        if (mousePos.distance(initialMousePos) > 10) {
            Node n1 = selectedSegment.way.nodes.get(selectedSegment.lowerIndex);
            Node n2 = selectedSegment.way.nodes.get(selectedSegment.lowerIndex+1);
            EastNorth en3 = n2.eastNorth.add(xoff, yoff);
            Node n3 = new Node(Main.proj.eastNorth2latlon(en3));
            EastNorth en4 = n1.eastNorth.add(xoff, yoff);
            Node n4 = new Node(Main.proj.eastNorth2latlon(en4));
            Way wnew = new Way(selectedSegment.way);
            wnew.nodes.add(selectedSegment.lowerIndex+1, n3);
            wnew.nodes.add(selectedSegment.lowerIndex+1, n4);
            if (wnew.nodes.size() == 4) wnew.nodes.add(n1);
            Collection<Command> cmds = new LinkedList<Command>();
            cmds.add(new AddCommand(n4));
            cmds.add(new AddCommand(n3));
            cmds.add(new ChangeCommand(selectedSegment.way, wnew));
            Command c = new SequenceCommand(tr("Extrude Way"), cmds);
            Main.main.undoRedo.add(c);
        }

        Main.map.mapView.removeTemporaryLayer(this);
        selectedSegment = null;
        mode = null;
        updateStatusLine();
        Main.map.mapView.repaint();
    }

    @Override public String getModeHelpText() {
        if (mode == Mode.select) {
            return tr("Release the mouse button to select the objects in the rectangle.");
        } else if (mode == Mode.EXTRUDE) {
            return tr("Draw a rectangle of the desired size, then release the mouse button.");
        } else if (mode == Mode.rotate) {
            return tr("Release the mouse button to stop rotating.");
        } else {
            return tr("Drag a way segment to make a rectangle.");
        }
    }
}
