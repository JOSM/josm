// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.GBC;
/**
 * Bounding box selector.
 * 
 * Provides max/min lat/lon input fields as well as the "URL from www.openstreetmap.org" text field.
 * 
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class BoundingBoxSelection implements DownloadSelection {

	private JTextField[] latlon = new JTextField[] {
			new JTextField(11),
			new JTextField(11),
			new JTextField(11),
			new JTextField(11) };
	final JTextArea osmUrl = new JTextArea();
	final JLabel showUrl = new JLabel();
	String noteUrl = tr("You can paste an URL here to download the area.");
	
	final JLabel sizeCheck = new JLabel();
	
	public void addGui(final DownloadDialog gui) {

		JPanel dlg = new JPanel(new GridBagLayout());
		osmUrl.setText(noteUrl);

		final FocusListener dialogUpdater = new FocusAdapter() {
			@Override public void focusLost(FocusEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							double minlat = Double.parseDouble(latlon[0].getText());
							double minlon = Double.parseDouble(latlon[1].getText());
							double maxlat = Double.parseDouble(latlon[2].getText());
							double maxlon = Double.parseDouble(latlon[3].getText());
							if (minlat != gui.minlat || minlon != gui.minlon || maxlat != gui.maxlat || maxlon != gui.maxlon) {
								gui.minlat = minlat; gui.minlon = minlon; 
								gui.maxlat = maxlat; gui.maxlon = maxlon;
								gui.boundingBoxChanged(BoundingBoxSelection.this);
							}
						} catch (NumberFormatException x) {
							// ignore
						}
						updateUrl(gui);
						updateSizeCheck(gui);
					}
				});
			}
		};
		
		for (JTextField f : latlon) {
			f.setMinimumSize(new Dimension(100,new JTextField().getMinimumSize().height));
			f.addFocusListener(dialogUpdater);
		}
		class osmUrlRefresher implements DocumentListener {
			public void changedUpdate(DocumentEvent e) { dowork(); }
			public void insertUpdate(DocumentEvent e) { dowork(); }
			public void removeUpdate(DocumentEvent e) { dowork(); }
			private void dowork() {
				Bounds b = osmurl2bounds(osmUrl.getText());
				if (b != null) {
					gui.minlon = b.min.lon();
					gui.minlat = b.min.lat();
					gui.maxlon = b.max.lon();
					gui.maxlat = b.max.lat();
					gui.boundingBoxChanged(BoundingBoxSelection.this);
					updateBboxFields(gui);
					updateSizeCheck(gui);
				}
			}
		}
		
		osmUrl.getDocument().addDocumentListener(new osmUrlRefresher());
		
		// select content on receiving focus. this seems to be the default in the
		// windows look+feel but not for others. needs invokeLater to avoid strange
		// side effects that will cancel out the newly made selection otherwise.
		osmUrl.addFocusListener(new FocusAdapter() {
			@Override public void focusGained(FocusEvent e) {
		        SwingUtilities.invokeLater(new Runnable() {
		                public void run() {
		                	osmUrl.selectAll();
		        		}
				});
			}
		});
		osmUrl.setLineWrap(true);
		osmUrl.setBorder(latlon[0].getBorder());
		
		Font labelFont = sizeCheck.getFont();
		sizeCheck.setFont(labelFont.deriveFont(Font.PLAIN, labelFont.getSize()));
		
		dlg.add(new JLabel(tr("min lat")), GBC.std().insets(10,20,5,0));
		dlg.add(latlon[0], GBC.std().insets(0,20,0,0));
		dlg.add(new JLabel(tr("min lon")), GBC.std().insets(10,20,5,0));
		dlg.add(latlon[1], GBC.eol().insets(0,20,0,0));
		dlg.add(new JLabel(tr("max lat")), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[2], GBC.std());
		dlg.add(new JLabel(tr("max lon")), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[3], GBC.eol());
		
		dlg.add(new JLabel(tr("URL from www.openstreetmap.org")), GBC.eol().insets(10,20,5,0));
		dlg.add(osmUrl, GBC.eop().insets(10,0,5,0).fill());
		dlg.add(showUrl, GBC.eop().insets(10,0,5,20));
		dlg.add(sizeCheck, GBC.eop().insets(10,0,5,20));

		gui.tabpane.addTab(tr("Bounding Box"), dlg);
	}
	
	/**
	 * Called when bounding box is changed by one of the other download dialog tabs.
	 */
	public void boundingBoxChanged(DownloadDialog gui) {
		updateBboxFields(gui);
		updateUrl(gui);
		updateSizeCheck(gui);
	}
	
	private void updateBboxFields(DownloadDialog gui) {
		latlon[0].setText(Double.toString(gui.minlat));
		latlon[1].setText(Double.toString(gui.minlon));
		latlon[2].setText(Double.toString(gui.maxlat));
		latlon[3].setText(Double.toString(gui.maxlon));
		for (JTextField f : latlon) 
			f.setCaretPosition(0);
	}
	
	private void updateUrl(DownloadDialog gui) {	
		double lat = (gui.minlat + gui.maxlat)/2;
		double lon = (gui.minlon + gui.maxlon)/2;
		// convert to mercator (for calculation of zoom only)
		double latMin = Math.log(Math.tan(Math.PI/4.0+gui.minlat/180.0*Math.PI/2.0))*180.0/Math.PI;
		double latMax = Math.log(Math.tan(Math.PI/4.0+gui.maxlat/180.0*Math.PI/2.0))*180.0/Math.PI;
		double size = Math.max(Math.abs(latMax-latMin), Math.abs(gui.maxlon-gui.minlon));
		int zoom = 0;
		while (zoom <= 20) {
			if (size >= 180)
				break;
			size *= 2;
			zoom++;
		}
		showUrl.setText("http://www.openstreetmap.org/index.html?mlat="+lat+"&mlon="+lon+"&zoom="+zoom);
	}
	
	private void updateSizeCheck(DownloadDialog gui) {
		double squareDegrees = (gui.maxlon-gui.minlon)*(gui.maxlat-gui.minlat);
		double maxBboxSize = 0.25;
		try {
			Double.parseDouble(Main.pref.get("osm-server.max-request-area", "0.25"));
		} catch (NumberFormatException nfe) {
			maxBboxSize = 0.25;
		}
		if (squareDegrees > maxBboxSize) {
			sizeCheck.setText(tr("Download area too large; will probably be rejected by server"));
			sizeCheck.setForeground(Color.red);
		} else {
			sizeCheck.setText(tr("Download area ok, size probably acceptable to server"));
			sizeCheck.setForeground(Color.darkGray);
		}
	}
	
	public static Bounds osmurl2bounds(String url) {
		int i = url.indexOf('?');
		if (i == -1)
			return null;
		String[] args = url.substring(i+1).split("&");
		HashMap<String, String> map = new HashMap<String, String>();
		for (String arg : args) {
			int eq = arg.indexOf('=');
			if (eq != -1) {
				map.put(arg.substring(0, eq), arg.substring(eq + 1));
			}
		}

		Bounds b = null;
		try {
			if (map.containsKey("bbox")) {
				String bbox[] = map.get("bbox").split(",");
				b = new Bounds(
					new LatLon(Double.parseDouble(bbox[1]), Double.parseDouble(bbox[0])),
					new LatLon(Double.parseDouble(bbox[3]), Double.parseDouble(bbox[2])));
			
			} else {
				double size = 180.0 / Math.pow(2, Integer.parseInt(map.get("zoom")));
				b = new Bounds(
	            	new LatLon(parseDouble(map, "lat") - size/2, parseDouble(map, "lon") - size),
	            	new LatLon(parseDouble(map, "lat") + size/2, parseDouble(map, "lon") + size));
			}
		} catch (NumberFormatException x) {
		} catch (NullPointerException x) {
		}
		return b;
	}

	private static double parseDouble(HashMap<String, String> map, String key) {
		if (map.containsKey(key))
			return Double.parseDouble(map.get(key));
		return Double.parseDouble(map.get("m"+key));
    }
}
