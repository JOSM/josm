//License: GPL. Copyright 2007 by Immanuel Scholz, Raphael Mack and others

package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.io.MultiPartFormOutputStream;
import org.openstreetmap.josm.io.GpxWriter;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.markerlayer.AudioMarker;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.DontShowAgainInfo;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UrlLabel;

public class GpxLayer extends Layer {
	public GpxData data;
	private final GpxLayer me;
	
	public GpxLayer(GpxData d) {
		super((String) d.attr.get("name"));
		data = d;
		me = this;
	}

	public GpxLayer(GpxData d, String name) {
		this(d);
		this.name = name;
	}

	@Override public Icon getIcon() {
		return ImageProvider.get("layer", "gpx_small");
	}

	@Override public Object getInfoComponent() {
		return getToolTipText();
	}

	@Override public Component[] getMenuEntries() {
		JMenuItem line = new JMenuItem(tr("Customize line drawing"), ImageProvider.get("mapmode/addsegment"));
		line.addActionListener(new ActionListener() {
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
				String propName = "draw.rawgps.lines.layer "+name;
				if (Main.pref.hasKey(propName))
					group.setSelected(r[Main.pref.getBoolean(propName) ? 1:2].getModel(), true);
				else
					group.setSelected(r[0].getModel(), true);
				int answer = JOptionPane.showConfirmDialog(Main.parent, panel, tr("Select line drawing options"), JOptionPane.OK_CANCEL_OPTION);
				if (answer == JOptionPane.CANCEL_OPTION)
					return;
				if (group.getSelection() == r[0].getModel())
					Main.pref.put(propName, null);
				else
					Main.pref.put(propName, group.getSelection() == r[1].getModel());
				Main.map.repaint();
			}
		});

		JMenuItem color = new JMenuItem(tr("Customize Color"), ImageProvider.get("colorchooser"));
		color.putClientProperty("help", "Action/LayerCustomizeColor");
		color.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String col = Main.pref.get("color.layer "+name, Main.pref.get("color.gps point", ColorHelper.color2html(Color.gray)));
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

		JMenuItem markersFromNamedTrackpoints = new JMenuItem(tr("Markers From Named Points"), ImageProvider.get("addmarkers"));
		markersFromNamedTrackpoints.putClientProperty("help", "Action/MarkersFromNamedPoints");
		markersFromNamedTrackpoints.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GpxData namedTrackPoints = new GpxData();
				for (GpxTrack track : data.tracks)
					for (Collection<WayPoint> seg : track.trackSegs)
						for (WayPoint point : seg)
							if (point.attr.containsKey("name") || point.attr.containsKey("desc")) 
								namedTrackPoints.waypoints.add(point);

	            MarkerLayer ml = new MarkerLayer(namedTrackPoints, tr("Named Trackpoints from {0}", name), associatedFile, me);
	            if (ml.data.size() > 0) {
	            	Main.main.addLayer(ml);
	            }
			}
		});

		JMenuItem applyAudio = new JMenuItem(tr("Make Sampled Audio Layer"), ImageProvider.get("applyaudio"));
		applyAudio.putClientProperty("help", "Action/MakeSampledAudioLayer");
		applyAudio.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String dir = Main.pref.get("markers.lastaudiodirectory");
				JFileChooser fc = new JFileChooser(dir);
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(new FileFilter(){
					@Override public boolean accept(File f) {
						return f.isDirectory() || f.getName().toLowerCase().endsWith(".wav");
					}
					@Override public String getDescription() {
						return tr("Wave Audio files (*.wav)");
					}
				});
				fc.showOpenDialog(Main.parent);
				File sel = fc.getSelectedFile();
				if (!fc.getCurrentDirectory().getAbsolutePath().equals(dir))
					Main.pref.put("markers.lastaudiodirectory", fc.getCurrentDirectory().getAbsolutePath());
				if (sel == null)
					return;
				applyAudio(sel);
				Main.map.repaint();
			}
		});

		JMenuItem tagimage = new JMenuItem(tr("Import images"), ImageProvider.get("tagimages"));
		tagimage.putClientProperty("help", "Action/ImportImages");
		tagimage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser(Main.pref.get("tagimages.lastdirectory"));
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fc.setMultiSelectionEnabled(true);
				fc.setAcceptAllFileFilterUsed(false);
				fc.setFileFilter(new FileFilter() {
					@Override public boolean accept(File f) {
						return f.isDirectory() || f.getName().toLowerCase().endsWith(".jpg");
					}
					@Override public String getDescription() {
						return tr("JPEG images (*.jpg)");
					}
				});
				fc.showOpenDialog(Main.parent);
				File[] sel = fc.getSelectedFiles();
				if (sel == null || sel.length == 0)
					return;
				LinkedList<File> files = new LinkedList<File>();
				addRecursiveFiles(files, sel);
				Main.pref.put("tagimages.lastdirectory", fc.getCurrentDirectory().getPath());
				GeoImageLayer.create(files, GpxLayer.this);
			}

			private void addRecursiveFiles(LinkedList<File> files, File[] sel) {
				for (File f : sel) {
					if (f.isDirectory())
						addRecursiveFiles(files, f.listFiles());
					else if (f.getName().toLowerCase().endsWith(".jpg"))
						files.add(f);
				}
			}
		});

		if (Main.applet)
			return new Component[] {
				new JMenuItem(new LayerListDialog.ShowHideLayerAction(this)),
				new JMenuItem(new LayerListDialog.DeleteLayerAction(this)),
				new JSeparator(),
				color,
				line,
				new JMenuItem(new ConvertToDataLayerAction()),
				new JSeparator(),
				new JMenuItem(new RenameLayerAction(associatedFile, this)),
				new JSeparator(),
				new JMenuItem(new LayerListPopup.InfoAction(this))};
		return new Component[] {
				new JMenuItem(new LayerListDialog.ShowHideLayerAction(this)),
				new JMenuItem(new LayerListDialog.DeleteLayerAction(this)),
				new JSeparator(),
				new JMenuItem(new SaveAction(this)),
				new JMenuItem(new SaveAsAction(this)),
				// new JMenuItem(new UploadTraceAction()),
				color,
				line,
				tagimage,
				applyAudio,
				markersFromNamedTrackpoints,
				new JMenuItem(new ConvertToDataLayerAction()),
				new JSeparator(),
				new JMenuItem(new RenameLayerAction(associatedFile, this)),
				new JSeparator(),
				new JMenuItem(new LayerListPopup.InfoAction(this))};
	}

	@Override public String getToolTipText() {
		StringBuilder info = new StringBuilder().append("<html>");

		info.append(trn("{0} track, ", "{0} tracks, ",
				data.tracks.size(), data.tracks.size()))
			.append(trn("{0} route, ", "{0} routes, ",
				data.routes.size(), data.routes.size()))
			.append(trn("{0} waypoint", "{0} waypoints",
				data.waypoints.size(), data.waypoints.size()))
			.append("<br />");

		if (data.attr.containsKey("name"))
			info.append(tr("Name: {0}", data.attr.get("name")))
				.append("<br />");

		if (data.attr.containsKey("desc"))
			info.append(tr("Description: {0}", data.attr.get("desc")))
				.append("<br />");

		return info.append("</html>").toString();
	}

	@Override public boolean isMergable(Layer other) {
		return other instanceof GpxLayer;
	}

	@Override public void mergeFrom(Layer from) {
		data.mergeFrom(((GpxLayer)from).data);
	}

	@Override public void paint(Graphics g, MapView mv) {
		String gpsCol = Main.pref.get("color.gps point");
		String gpsColSpecial = Main.pref.get("color.layer "+name);
		if (!gpsColSpecial.equals("")) {
			g.setColor(ColorHelper.html2color(gpsColSpecial));
		} else if (!gpsCol.equals("")) {
			g.setColor(ColorHelper.html2color(gpsCol));
		} else{
			g.setColor(Color.GRAY);
		}
		
		boolean forceLines = Main.pref.getBoolean("draw.rawgps.lines.force");
		boolean lines = Main.pref.getBoolean("draw.rawgps.lines");
		String linesKey = "draw.rawgps.lines.layer "+name;
		if (Main.pref.hasKey(linesKey))
			lines = Main.pref.getBoolean(linesKey);
		boolean large = Main.pref.getBoolean("draw.rawgps.large");

		Point old = null;
		for (GpxTrack trk : data.tracks) {
			if (!forceLines) {
				old = null;
			}
			for (Collection<WayPoint> segment : trk.trackSegs) {
				for (WayPoint trkPnt : segment) {
					if (Double.isNaN(trkPnt.latlon.lat()) || Double.isNaN(trkPnt.latlon.lon()))
						continue;
					Point screen = mv.getPoint(trkPnt.eastNorth);
					if (lines && old != null) {
						g.drawLine(old.x, old.y, screen.x, screen.y);
					} else if (!large) {
						g.drawRect(screen.x, screen.y, 0, 0);
					}
					if (large)
						g.fillRect(screen.x-1, screen.y-1, 3, 3);
					old = screen;
				}
			}
		}
	}

	@Override public void visitBoundingBox(BoundingXYVisitor v) {
		for (WayPoint p : data.waypoints)
			v.visit(p.eastNorth);

		for (GpxRoute rte : data.routes) {
			Collection<WayPoint> r = rte.routePoints;
			for (WayPoint p : r) {
				v.visit(p.eastNorth);
			}
		}

		for (GpxTrack trk : data.tracks) {
			for (Collection<WayPoint> seg : trk.trackSegs) {
				for (WayPoint p : seg) {
					v.visit(p.eastNorth);
				}
			}
		}
	}

	public class UploadTraceAction extends AbstractAction {
		public UploadTraceAction() {
			super(tr("Upload this trace..."), ImageProvider.get("uploadtrace"));
		}
		public void actionPerformed(ActionEvent e) {
			JPanel msg = new JPanel(new GridBagLayout());
			msg.add(new JLabel(tr("<html>This functionality has been added only recently. Please<br>"+
			"use with care and check if it works as expected.</html>")), GBC.eop());
			ButtonGroup bg = new ButtonGroup();
			JRadioButton c1 = null;
			JRadioButton c2 = null;

			//TODO
			//check whether data comes from server
			//check whether data changed sind last save/open

			c1 = new JRadioButton(tr("Upload track filtered by JOSM"), true);
			c2 = new JRadioButton(tr("Upload raw file: "), false);
			c2.setEnabled(false);
			c1.setEnabled(false);
			bg.add(c1);
			bg.add(c2);

			msg.add(c1, GBC.eol());
			msg.add(c2, GBC.eop());


			JLabel description = new JLabel((String) data.attr.get("desc"));
			JTextField tags = new JTextField();
			tags.setText((String) data.attr.get("keywords"));
			msg.add(new JLabel(tr("Description:")), GBC.std());
			msg.add(description, GBC.eol().fill(GBC.HORIZONTAL));
			msg.add(new JLabel(tr("Tags (keywords in GPX):")), GBC.std());
			msg.add(tags, GBC.eol().fill(GBC.HORIZONTAL));
			JCheckBox c3 = new JCheckBox("public");
			msg.add(c3, GBC.eop());
			msg.add(new JLabel("Please ensure that you don't upload your traces twice."), GBC.eop());

			int answer = JOptionPane.showConfirmDialog(Main.parent, msg, tr("GPX-Upload"), JOptionPane.OK_CANCEL_OPTION);
			if (answer == JOptionPane.OK_OPTION)
			{
				try {
					String version = Main.pref.get("osm-server.version", "0.5");
					URL url = new URL(Main.pref.get("osm-server.url") +
							"/" + version + "/gpx/create");

					// create a boundary string
					String boundary = MultiPartFormOutputStream.createBoundary();
					URLConnection urlConn = MultiPartFormOutputStream.createConnection(url);
					urlConn.setRequestProperty("Accept", "*/*");
					urlConn.setRequestProperty("Content-Type", 
							MultiPartFormOutputStream.getContentType(boundary));
					// set some other request headers...
					urlConn.setRequestProperty("Connection", "Keep-Alive");
					urlConn.setRequestProperty("Cache-Control", "no-cache");
					// no need to connect cuz getOutputStream() does it
					MultiPartFormOutputStream out = 
						new MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
					out.writeField("description", description.getText());
					out.writeField("tags", tags.getText());
					out.writeField("public", (c3.getSelectedObjects() != null) ? "1" : "0");
					// upload a file
					// out.writeFile("gpx_file", "text/xml", associatedFile);
					// can also write bytes directly
					// out.writeFile("myFile", "text/plain", "C:\\test.txt", 
					// "This is some file text.".getBytes("ASCII"));
					File tmp = File.createTempFile("josm", "tmp.gpx");
					FileOutputStream outs = new FileOutputStream(tmp);
					new GpxWriter(outs).write(data);
					outs.close();
					FileInputStream ins = new FileInputStream(tmp);
					new GpxWriter(System.out).write(data);
					out.writeFile("gpx_file", "text/xml", data.storageFile.getName(), ins);
					out.close();
					tmp.delete();
					// read response from server
					BufferedReader in = new BufferedReader(
							new InputStreamReader(urlConn.getInputStream()));
					String line = "";
					while((line = in.readLine()) != null) {
						System.out.println(line);
					}
					in.close();

					//TODO check response
					/*					int retCode = urlConn.getResponseCode();
					System.out.println("got return: " + retCode);
					String retMsg = urlConn.getResponseMessage();
					urlConn.disconnect();
					if (retCode != 200) {
						// Look for a detailed error message from the server
						if (urlConn.getHeaderField("Error") != null)
							retMsg += "\n" + urlConn.getHeaderField("Error");

						// Report our error
						ByteArrayOutputStream o = new ByteArrayOutputStream();
						System.out.println(new String(o.toByteArray(), "UTF-8").toString());
						throw new RuntimeException(retCode+" "+retMsg);
					}
					 */
				} catch (UnknownHostException ex) {
					throw new RuntimeException(tr("Unknown host")+": "+ex.getMessage(), ex);
				} catch (Exception ex) {
					//if (cancel)
					//	return; // assume cancel
					if (ex instanceof RuntimeException)
						throw (RuntimeException)ex;
					throw new RuntimeException(ex.getMessage(), ex);
				}	
			}
		}
	}


	public class ConvertToDataLayerAction extends AbstractAction {
		public ConvertToDataLayerAction() {
			super(tr("Convert to data layer"), ImageProvider.get("converttoosm"));
		}
		public void actionPerformed(ActionEvent e) {
			JPanel msg = new JPanel(new GridBagLayout());
			msg.add(new JLabel(tr("<html>Upload of unprocessed GPS data as map data is considered harmful.<br>If you want to upload traces, look here:")), GBC.eol());
			msg.add(new UrlLabel(tr("http://www.openstreetmap.org/traces")), GBC.eop());
			if (!DontShowAgainInfo.show("convert_to_data", msg))
				return;
			DataSet ds = new DataSet();
			for (GpxTrack trk : data.tracks) {
				for (Collection<WayPoint> segment : trk.trackSegs) {
					Way w = new Way();
					for (WayPoint p : segment) {
						Node n = new Node(p.latlon);
						ds.nodes.add(n);
						w.nodes.add(n);
					}
					ds.ways.add(w);
				}
			}
			Main.main.addLayer(new OsmDataLayer(ds, tr("Converted from: {0}", GpxLayer.this.name), null));
			Main.main.removeLayer(GpxLayer.this);
		}
	}

	/**
	 * 
	 *
	 */
	private void applyAudio(File wavFile) {
		String uri = "file:".concat(wavFile.getAbsolutePath());
	    double audioGapSecs = 15.0; 
		try {
			audioGapSecs = Double.parseDouble(Main.pref.get("marker.audiosampleminsecs", Double.toString(audioGapSecs)));
		} catch (NumberFormatException e) {
		}
	    double audioGapMetres = 75.0; 
		try {
			audioGapMetres = Double.parseDouble(Main.pref.get("marker.audiosampleminmetres", Double.toString(audioGapMetres)));
		} catch (NumberFormatException e) {
		}
		double audioGapRadians = (audioGapMetres / 40041455.0 /* circumference of Earth in metres */) * 2.0 * Math.PI;
		double audioGapRadiansSquared = audioGapRadians * audioGapRadians;
		double firstTime = -1.0;
	    double prevOffset = - (audioGapSecs + 1.0); // first point always exceeds time difference
	    WayPoint prevPoint = null;

	    MarkerLayer ml = new MarkerLayer(new GpxData(), tr("Sampled audio markers from {0}", name), associatedFile, me);
	    
		for (GpxTrack track : data.tracks) {
			for (Collection<WayPoint> seg : track.trackSegs) {
				for (WayPoint point : seg) {
					double time = point.time;
					if (firstTime < 0.0)
						firstTime = time;
					double offset = time - firstTime;
					if (prevPoint == null ||
						(offset - prevOffset > audioGapSecs &&
						/* note: distance is misleading: it actually gives distance _squared_ */
						point.eastNorth.distance(prevPoint.eastNorth) > audioGapRadiansSquared))
					{
						
						AudioMarker am = AudioMarker.create(point.latlon, 
								AudioMarker.inventName(offset), uri, ml, time, offset);
						ml.data.add(am);
						prevPoint = point;
						prevOffset = offset;
					}
				}
			}
		}

        if (ml.data.size() > 0) {
        	Main.main.addLayer(ml);
        }
	}
}
