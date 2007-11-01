// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer holding markers.
 * 
 * Markers are GPS points with a name and, optionally, a symbol code attached;
 * marker layers can be created from waypoints when importing raw GPS data,
 * but they may also come from other sources.
 * 
 * The symbol code is for future use.
 * 
 * The data is read only.
 */
public class MarkerLayer extends Layer {

	/**
	 * A list of markers.
	 */
	public final Collection<Marker> data;
	private boolean mousePressed = false;
	
	public MarkerLayer(GpxData indata, String name, File associatedFile) {
		
		super(name);
		this.associatedFile = associatedFile;
		this.data = new ArrayList<Marker>();
		
		for (WayPoint wpt : indata.waypoints) {
            Marker m = Marker.createMarker(wpt, indata.storageFile);
            if (m != null)
            	data.add(m);
		}
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				Main.map.mapView.addMouseListener(new MouseAdapter() {
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
						if (ev.getPoint() != null) {
							for (Marker mkr : data) {
								if (mkr.containsPoint(ev.getPoint()))
									mkr.actionPerformed(new ActionEvent(this, 0, null));
							}
						}
						Main.map.mapView.repaint();
					}
				});
			}
		});
	}

	/**
	 * Return a static icon.
	 */
	@Override public Icon getIcon() {
		return ImageProvider.get("layer", "marker_small");
	}

	@Override public void paint(Graphics g, MapView mv) {
		boolean mousePressedTmp = mousePressed;
		Point mousePos = mv.getMousePosition();
		String mkrCol = Main.pref.get("color.gps marker");
		String mkrColSpecial = Main.pref.get("color.layer "+name);
        String mkrTextShow = Main.pref.get("marker.show "+name, "show");

		if (!mkrColSpecial.equals(""))
			g.setColor(ColorHelper.html2color(mkrColSpecial));
		else if (!mkrCol.equals(""))
			g.setColor(ColorHelper.html2color(mkrCol));
		else
			g.setColor(Color.GRAY);

		for (Marker mkr : data) {
			if (mousePos != null && mkr.containsPoint(mousePos)) {
				mkr.paint(g, mv, mousePressedTmp, mkrTextShow);
				mousePressedTmp = false;
			} else {
				mkr.paint(g, mv, false, mkrTextShow);
			}
		}
	}

	@Override public String getToolTipText() {
		return data.size()+" "+trn("marker", "markers", data.size());
	}

	@Override public void mergeFrom(Layer from) {
		MarkerLayer layer = (MarkerLayer)from;
		data.addAll(layer.data);
	}

	@Override public boolean isMergable(Layer other) {
		return other instanceof MarkerLayer;
	}

	@Override public void visitBoundingBox(BoundingXYVisitor v) {
		for (Marker mkr : data)
			v.visit(mkr.eastNorth);
	}

	@Override public Object getInfoComponent() {
		return "<html>"+trn("{0} consists of {1} marker", "{0} consists of {1} markers", data.size(), name, data.size()) + "</html>";
	}

	@Override public Component[] getMenuEntries() {
		JMenuItem color = new JMenuItem(tr("Customize Color"), ImageProvider.get("colorchooser"));
		color.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				String col = Main.pref.get("color.layer "+name, Main.pref.get("color.gps marker", ColorHelper.color2html(Color.gray)));
				JColorChooser c = new JColorChooser(ColorHelper.html2color(col));
				Object[] options = new Object[]{tr("OK"), tr("Cancel"), tr("Default")};
				int answer = JOptionPane.showOptionDialog(Main.parent, c, tr("Choose a color"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
				switch (answer) {
				case 0:
					Main.pref.put("color.layer "+name, ColorHelper.color2html(c.getColor()));
					break;
				case 1:
					return;
				case 2:
					Main.pref.put("color.layer "+name, null);
					break;
				}
				Main.map.repaint();
			}
		});

		return new Component[] {
			new JMenuItem(new LayerListDialog.ShowHideLayerAction(this)),
            new JMenuItem(new LayerListDialog.ShowHideMarkerText(this)),
			new JMenuItem(new LayerListDialog.DeleteLayerAction(this)),
			new JSeparator(),
			color,
			new JMenuItem(new RenameLayerAction(associatedFile, this)),
			new JSeparator(),
			new JMenuItem(new LayerListPopup.InfoAction(this))
		};
	}
}
