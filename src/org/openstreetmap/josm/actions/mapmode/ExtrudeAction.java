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
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Makes a rectangle from a line, or modifies a rectangle.
 */
public class ExtrudeAction extends MapMode implements MapViewPaintable {

    enum Mode { extrude, translate, select }
    private Mode mode = Mode.select;
    private long mouseDownTime = 0;
    private WaySegment selectedSegment = null;
    private Color selectedColor;

    /**
     * The old cursor before the user pressed the mouse button.
     */
    private Cursor oldCursor;
    /**
     * The position of the mouse cursor when the drag action was initiated.
     */
    private Point initialMousePos;
    /**
     * The time which needs to pass between click and release before something
     * counts as a move, in milliseconds
     */
    private static int initialMoveDelay = 200;
    /**
     * The initial EastNorths of node1 and node2
     */
    private EastNorth initialN1en;
    private EastNorth initialN2en;
    /**
     * The new EastNorths of node1 and node2
     */
    private EastNorth newN1en;
    private EastNorth newN2en;
    /**
     * This is to work around some deficiencies in MoveCommand when translating
     */
    private EastNorth lastTranslatedN1en;
    /**
     * Normal unit vector of the selected segment.
     */
    private EastNorth normalUnitVector;
    /**
     * Vector of node2 from node1.
     */
    private EastNorth segmentVector;
    /**
     * Transforms the mouse point (in EastNorth space) to the normal-shifted position
     * of point 1 of the selectedSegment.
     */
    private AffineTransform normalTransform;

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
     * Perform action depending on what mode we're in.
     */
    @Override public void mouseDragged(MouseEvent e) {
        if(!Main.map.mapView.isActiveLayerVisible())
            return;

        // do not count anything as a drag if it lasts less than 100 milliseconds.
        if (System.currentTimeMillis() - mouseDownTime < initialMoveDelay) return;

        if (mode == Mode.select) {
            // Just sit tight and wait for mouse to be released.
        } else {
            // This may be ugly, but I can't see any other way of getting a mapview from here.
            EastNorth mouseen = Main.map.mapView.getEastNorth(e.getPoint().x, e.getPoint().y);

            Point2D newN1point = normalTransform.transform(mouseen, null);

            newN1en = new EastNorth(newN1point.getX(), newN1point.getY());
            newN2en = newN1en.add(segmentVector.getX(), segmentVector.getY());

            // find out the distance, in metres, between the initial position of N1 and the new one.
            Main.map.statusLine.setDist(Main.proj.eastNorth2latlon(initialN1en).greatCircleDistance(Main.proj.eastNorth2latlon(newN1en)));
            updateStatusLine();

            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

            if (mode == Mode.extrude) {

            } else if (mode == Mode.translate) {
                Command c = !Main.main.undoRedo.commands.isEmpty()
                ? Main.main.undoRedo.commands.getLast() : null;
                if (c instanceof SequenceCommand) {
                    c = ((SequenceCommand)c).getLastCommand();
                }

                Node n1 = selectedSegment.way.getNode(selectedSegment.lowerIndex);
                Node n2 = selectedSegment.way.getNode(selectedSegment.lowerIndex+1);

                EastNorth difference = new EastNorth(newN1en.getX()-lastTranslatedN1en.getX(), newN1en.getY()-lastTranslatedN1en.getY());

                // Better way of testing list equality non-order-sensitively?
                if (c instanceof MoveCommand
                        && ((MoveCommand)c).getMovedNodes().contains(n1)
                        && ((MoveCommand)c).getMovedNodes().contains(n2)
                        && ((MoveCommand)c).getMovedNodes().size() == 2) {
                    // MoveCommand doesn't let us know how much it has already moved the selection
                    // so we have to do some ugly record-keeping.
                    ((MoveCommand)c).moveAgain(difference.getX(), difference.getY());
                    lastTranslatedN1en = newN1en;
                } else {
                    Collection<OsmPrimitive> nodelist = new LinkedList<OsmPrimitive>();
                    nodelist.add(n1);
                    nodelist.add(n2);
                    Main.main.undoRedo.add(c = new MoveCommand(nodelist, difference.getX(), difference.getY()));
                    lastTranslatedN1en = newN1en;
                }
            }
            Main.map.mapView.repaint();
        }
    }

    /**
     * Create a new Line that extends off the edge of the viewport in one direction
     * @param start The start point of the line
     * @param unitvector A unit vector denoting the direction of the line
     * @param g the Graphics2D object  it will be used on
     */
    static private Line2D createSemiInfiniteLine(Point2D start, Point2D unitvector, Graphics2D g) {
        Rectangle bounds = g.getDeviceConfiguration().getBounds();
        try {
            AffineTransform invtrans = g.getTransform().createInverse();
            Point2D widthpoint = invtrans.deltaTransform(new Point2D.Double(bounds.width,0), null);
            Point2D heightpoint = invtrans.deltaTransform(new Point2D.Double(0,bounds.height), null);

            // Here we should end up with a gross overestimate of the maximum viewport diagonal in what
            // Graphics2D calls 'user space'. Essentially a manhattan distance of manhattan distances.
            // This can be used as a safe length of line to generate which will always go off-viewport.
            double linelength = Math.abs(widthpoint.getX()) + Math.abs(widthpoint.getY()) + Math.abs(heightpoint.getX()) + Math.abs(heightpoint.getY());

            return new Line2D.Double(start, new Point2D.Double(start.getX() + (unitvector.getX() * linelength) , start.getY() + (unitvector.getY() * linelength)));
        }
        catch (NoninvertibleTransformException e) {
            return new Line2D.Double(start, new Point2D.Double(start.getX() + (unitvector.getX() * 10) , start.getY() + (unitvector.getY() * 10)));
        }
    }

    public void paint(Graphics g, MapView mv) {
        if (mode == Mode.select) {
            // Nothing to do
        } else {
            if (newN1en != null) {
                Graphics2D g2 = (Graphics2D)g;
                g2.setColor(selectedColor);
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                Point p1 = mv.getPoint(initialN1en);
                Point p2 = mv.getPoint(initialN2en);
                Point p3 = mv.getPoint(newN1en);
                Point p4 = mv.getPoint(newN2en);

                if (mode == Mode.extrude) {
                    // Draw rectangle around new area.
                    GeneralPath b = new GeneralPath();
                    b.moveTo(p1.x, p1.y); b.lineTo(p3.x, p3.y);
                    b.lineTo(p4.x, p4.y); b.lineTo(p2.x, p2.y);
                    b.lineTo(p1.x, p1.y);
                    g2.draw(b);
                    g2.setStroke(new BasicStroke(1));
                } else if (mode == Mode.translate) {
                    // Highlight the new and old segments.
                    Line2D newline = new Line2D.Double(p3, p4);
                    g2.draw(newline);
                    g2.setStroke(new BasicStroke(1));
                    Line2D oldline = new Line2D.Double(p1, p2);
                    g2.draw(oldline);

                    // Draw a guideline along the normal.
                    Line2D normline;
                    Point2D centerpoint = new Point2D.Double((p1.getX()+p2.getX())*0.5, (p1.getY()+p2.getY())*0.5);
                    EastNorth drawnorm;
                    // Check to see if our new N1 is in a positive direction with respect to the normalUnitVector.
                    // Even if the x component is zero, we should still be able to discern using +0.0 and -0.0
                    if (newN1en == null || (newN1en.getX() > initialN1en.getX() == normalUnitVector.getX() > -0.0)) {
                        drawnorm = normalUnitVector;
                    } else {
                        // If not, use a sign-flipped version of the normalUnitVector.
                        drawnorm = new EastNorth(-normalUnitVector.getX(), -normalUnitVector.getY());
                    }
                    normline = createSemiInfiniteLine(centerpoint, drawnorm, g2);
                    g2.draw(normline);

                    // EastNorth units per pixel
                    double factor = 1.0/g2.getTransform().getScaleX();

                    // Draw right angle marker on initial position.
                    double raoffsetx = 8.0*factor*drawnorm.getX();
                    double raoffsety = 8.0*factor*drawnorm.getY();
                    Point2D ra1 = new Point2D.Double(centerpoint.getX()+raoffsetx, centerpoint.getY()+raoffsety);
                    Point2D ra3 = new Point2D.Double(centerpoint.getX()-raoffsety, centerpoint.getY()+raoffsetx);
                    Point2D ra2 = new Point2D.Double(ra1.getX()-raoffsety, ra1.getY()+raoffsetx);
                    GeneralPath ra = new GeneralPath();
                    ra.moveTo((float)ra1.getX(), (float)ra1.getY());
                    ra.lineTo((float)ra2.getX(), (float)ra2.getY());
                    ra.lineTo((float)ra3.getX(), (float)ra3.getY());
                    g2.draw(ra);
                }
            }
        }
    }

    /**
     * If the left mouse button is pressed over a segment, switch
     * to either extrude or translate mode depending on whether ctrl is held.
     */
    @Override public void mousePressed(MouseEvent e) {
        if(!Main.map.mapView.isActiveLayerVisible())
            return;
        if (!(Boolean)this.getValue("active")) return;
        if (e.getButton() != MouseEvent.BUTTON1)
            return;
        // boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
        // boolean alt = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;
        // boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;

        selectedSegment = Main.map.mapView.getNearestWaySegment(e.getPoint());

        if (selectedSegment == null) {
            // If nothing gets caught, stay in select mode
        } else {
            // Otherwise switch to another mode

            // For extrusion, these positions are actually never changed,
            // but keeping note of this anyway allows us to not continually
            // look it up and also allows us to unify code with the translate mode
            initialN1en = selectedSegment.way.getNode(selectedSegment.lowerIndex).getEastNorth();
            initialN2en = selectedSegment.way.getNode(selectedSegment.lowerIndex + 1).getEastNorth();

            // Signifies that nothing has happened yet
            newN1en = null;
            newN2en = null;

            Main.map.mapView.addTemporaryLayer(this);

            updateStatusLine();
            Main.map.mapView.repaint();

            // Make note of time pressed
            mouseDownTime = System.currentTimeMillis();

            // Make note of mouse position
            initialMousePos = e.getPoint();

            segmentVector = new EastNorth(initialN2en.getX()-initialN1en.getX(), initialN2en.getY()-initialN1en.getY());
            double factor = 1.0 / Math.hypot(segmentVector.getX(), segmentVector.getY());
            // swap coords to get normal, mult by factor to get unit vector.
            normalUnitVector = new EastNorth(segmentVector.getY() * factor, segmentVector.getX() * factor);

            // The calculation of points along the normal of the segment from mouse
            // points is actually a purely affine mapping. So the majority of the maths
            // can be done once, on mousePress, by building an AffineTransform which
            // we can use in the other functions.
            double r = 1.0 / ( (normalUnitVector.getX()*normalUnitVector.getX()) + (normalUnitVector.getY()*normalUnitVector.getY()) );
            double s = (normalUnitVector.getX()*initialN1en.getX()) - (normalUnitVector.getY()*initialN1en.getY());
            double compcoordcoeff = -r*normalUnitVector.getX()*normalUnitVector.getY();

            // Build the matrix. Takes a mouse position in EastNorth-space and returns the new position of node1
            // based on that.
            normalTransform = new AffineTransform(
                    r*normalUnitVector.getX()*normalUnitVector.getX(), compcoordcoeff,
                    compcoordcoeff, r*normalUnitVector.getY()*normalUnitVector.getY(),
                    initialN1en.getX()-(s*r*normalUnitVector.getX()), initialN1en.getY()+(s*r*normalUnitVector.getY()));

            // Switch mode.
            if ( (e.getModifiers() & ActionEvent.CTRL_MASK) != 0 ) {
                mode = Mode.translate;
                lastTranslatedN1en = initialN1en;
            } else {
                mode = Mode.extrude;
                getCurrentDataSet().setSelected(selectedSegment.way);
            }
        }
    }

    /**
     * Do anything that needs to be done, then switch back to select mode
     */
    @Override public void mouseReleased(MouseEvent e) {

        if(!Main.map.mapView.isActiveLayerVisible())
            return;

        if (mode == mode.select) {
            // Nothing to be done
        } else {
            if (mode == mode.extrude) {
                if (e.getPoint().distance(initialMousePos) > 10 && newN1en != null) {
                    // Commit extrusion

                    Node n1 = selectedSegment.way.getNode(selectedSegment.lowerIndex);
                    Node n2 = selectedSegment.way.getNode(selectedSegment.lowerIndex+1);
                    Node n3 = new Node(Main.proj.eastNorth2latlon(newN2en));
                    Node n4 = new Node(Main.proj.eastNorth2latlon(newN1en));
                    Way wnew = new Way(selectedSegment.way);
                    wnew.addNode(selectedSegment.lowerIndex+1, n3);
                    wnew.addNode(selectedSegment.lowerIndex+1, n4);
                    if (wnew.getNodesCount() == 4) {
                        wnew.addNode(n1);
                    }
                    Collection<Command> cmds = new LinkedList<Command>();
                    cmds.add(new AddCommand(n4));
                    cmds.add(new AddCommand(n3));
                    cmds.add(new ChangeCommand(selectedSegment.way, wnew));
                    Command c = new SequenceCommand(tr("Extrude Way"), cmds);
                    Main.main.undoRedo.add(c);
                }
            } else if (mode == mode.translate) {
                // I don't think there's anything to do
            }

            // Switch back into select mode
            restoreCursor();
            Main.map.mapView.removeTemporaryLayer(this);
            selectedSegment = null;
            mode = Mode.select;

            updateStatusLine();
            Main.map.mapView.repaint();
        }
    }

    @Override public String getModeHelpText() {
        if (mode == Mode.translate)
            return tr("Move a segment along its normal, then release the mouse button.");
        else if (mode == Mode.extrude)
            return tr("Draw a rectangle of the desired size, then release the mouse button.");
        else
            return tr("Drag a way segment to make a rectangle. Ctrl-drag to move a segment along its normal.");
    }

    @Override public boolean layerIsSupported(Layer l) {
        return l instanceof OsmDataLayer;
    }
}
