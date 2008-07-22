// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapMover;
import org.openstreetmap.josm.gui.MapScaler;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.SelectionManager;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;


/**
 * A component that let the user select a lat/lon bounding box from zooming
 * into the world as a picture and selecting a region.
 *
 * The component has to be of the aspect ration 2:1 to look good.
 *
 * @author imi
 */
public class WorldChooser extends NavigatableComponent implements DownloadSelection {

	/**
	 * The world as picture.
	 */
	private ImageIcon world;

	/**
	 * Maximum scale level
	 */
	private double scaleMax;

	/**
	 * Mark this rectangle (lat/lon values) when painting.
	 */
	private EastNorth markerMin, markerMax;

	private Projection projection;

	/**
	 * Create the chooser component.
	 */
	public WorldChooser() {
		URL path = Main.class.getResource("/images/world.jpg");
		world = new ImageIcon(path);
		center = new EastNorth(world.getIconWidth()/2, world.getIconHeight()/2);
		setPreferredSize(new Dimension(400, 200));

		projection = new Projection() {
			public EastNorth latlon2eastNorth(LatLon p) {
				return new EastNorth(
						(p.lon()+180) / 360 * world.getIconWidth(),
						(p.lat()+90) / 180 * world.getIconHeight());
			}
			public LatLon eastNorth2latlon(EastNorth p) {
				return new LatLon(
						p.north()*180/world.getIconHeight() - 90,
						p.east()*360/world.getIconWidth() - 180);
			}
			@Override public String toString() {
				return "WorldChooser";
			}
            public String getCacheDirectoryName() {
                throw new UnsupportedOperationException();
            }
			public double scaleFactor() {
	            return 1.0 / world.getIconWidth();
            }

		};

		MapScaler scaler = new MapScaler(this, projection);
		add(scaler);
		scaler.setLocation(10,10);

		setMinimumSize(new Dimension(350, 350/2));
	}

	public void addGui(final DownloadDialog gui) {
		JPanel temp = new JPanel();
		temp.setLayout(new BorderLayout());
		temp.add(this, BorderLayout.CENTER);
		temp.add(new JLabel(tr("You can use the mouse or Ctrl+Arrow keys/./ to zoom and pan.")), BorderLayout.SOUTH);
		gui.tabpane.add(temp, tr("Map"));
		new MapMover(this, temp);
		SelectionEnded selListener = new SelectionEnded(){
			public void selectionEnded(Rectangle r, boolean alt, boolean shift, boolean ctrl) {
				markerMin = getEastNorth(r.x, r.y+r.height);
				markerMax = getEastNorth(r.x+r.width, r.y);
				LatLon min = getProjection().eastNorth2latlon(markerMin);
				LatLon max = getProjection().eastNorth2latlon(markerMax);
				gui.minlat = min.lat();
				gui.minlon = min.lon();
				gui.maxlat = max.lat();
				gui.maxlon = max.lon();
				gui.boundingBoxChanged(WorldChooser.this);
				repaint();
			}
			public void addPropertyChangeListener(PropertyChangeListener listener) {}
			public void removePropertyChangeListener(PropertyChangeListener listener) {}
		};
		SelectionManager sm = new SelectionManager(selListener, false, this);
		sm.register(this);
    }

	public void boundingBoxChanged(DownloadDialog gui) {
		markerMin = getProjection().latlon2eastNorth(new LatLon(gui.minlat, gui.minlon));
		markerMax = getProjection().latlon2eastNorth(new LatLon(gui.maxlat, gui.maxlon));
		repaint();
	}

	/**
	 * Set the scale as well as the preferred size.
	 */
	@Override public void setPreferredSize(Dimension preferredSize) {
		super.setPreferredSize(preferredSize);
		scale = world.getIconWidth()/preferredSize.getWidth();
		scaleMax = scale;
	}

	/**
	 * Draw the current selected region.
	 */
	@Override public void paint(Graphics g) {
		EastNorth tl = getEastNorth(0,0);
		EastNorth br = getEastNorth(getWidth(),getHeight());
		g.drawImage(world.getImage(),0,0,getWidth(),getHeight(),(int)tl.east(),(int)tl.north(),(int)br.east(),(int)br.north(), null);

		// draw marker rect
		if (markerMin != null && markerMax != null) {
			Point p1 = getPoint(markerMin);
			Point p2 = getPoint(markerMax);
			double x = Math.min(p1.x, p2.x);
			double y = Math.min(p1.y, p2.y);
			double w = Math.max(p1.x, p2.x) - x;
			double h = Math.max(p1.y, p2.y) - y;
			if (w < 1)
				w = 1;
			if (h < 1)
				h = 1;
			g.setColor(Color.YELLOW);
			g.drawRect((int)x, (int)y, (int)w, (int)h);
		}
		super.paint(g);
	}

	@Override public void zoomTo(EastNorth newCenter, double scale) {
		if (getWidth() != 0 && scale > scaleMax) {
			scale = scaleMax;
			newCenter = center;
		}
		super.zoomTo(newCenter, scale);
	}

	/**
	 * Always use our image projection mode.
	 */
	@Override protected Projection getProjection() {
		return projection;
	}
}
