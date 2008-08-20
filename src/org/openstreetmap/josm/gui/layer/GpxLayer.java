// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui.layer;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Date;
import java.text.DateFormat;
import java.text.DecimalFormat;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.markerlayer.AudioMarker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.GpxWriter;
import org.openstreetmap.josm.io.MultiPartFormOutputStream;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.DontShowAgainInfo;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UrlLabel;

public class GpxLayer extends Layer {
	public GpxData data;
	private final GpxLayer me;
	protected static final double PHI = Math.toRadians(15);
	private boolean computeCacheInSync;
	private int computeCacheMaxLineLengthUsed;
	private Color computeCacheColorUsed;
	private boolean computeCacheColored;

	public GpxLayer(GpxData d) {
		super((String) d.attr.get("name"));
		data = d;
		me = this;
		computeCacheInSync = false;
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
				JColorChooser c = new JColorChooser(Main.pref.getColor(marktr("gps point"), "layer "+name, Color.gray));
				Object[] options = new Object[]{tr("OK"), tr("Cancel"), tr("Default")};
				int answer = JOptionPane.showOptionDialog(Main.parent, c, tr("Choose a color"), JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
				switch (answer) {
				case 0:
					Main.pref.putColor("layer "+name, c.getColor());
					break;
				case 1:
					return;
				case 2:
					Main.pref.putColor("layer "+name, null);
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

		JMenuItem importAudio = new JMenuItem(tr("Import Audio"), ImageProvider.get("importaudio"));
		importAudio.putClientProperty("help", "ImportAudio");
		importAudio.addActionListener(new ActionListener() {
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
				fc.setMultiSelectionEnabled(true);
				if(fc.showOpenDialog(Main.parent) == JFileChooser.APPROVE_OPTION) {
					if (!fc.getCurrentDirectory().getAbsolutePath().equals(dir))
						Main.pref.put("markers.lastaudiodirectory", fc.getCurrentDirectory().getAbsolutePath());

					// FIXME: properly support multi-selection here.
					// Calling importAudio several times just creates N maker layers, which
					// is sub-optimal.
					File sel[] = fc.getSelectedFiles();
					if(sel != null)
						for (int i = 0; i < sel.length; i++)
							importAudio(sel[i]);

					Main.map.repaint();
				}
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
			importAudio,
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
		data.tracks.size(), data.tracks.size())).append(trn("{0} route, ", "{0} routes, ",
		data.routes.size(), data.routes.size())).append(trn("{0} waypoint", "{0} waypoints",
		data.waypoints.size(), data.waypoints.size())).append("<br>");

		if (data.attr.containsKey("name"))
			info.append(tr("Name: {0}", data.attr.get("name"))).append("<br>");

		if (data.attr.containsKey("desc"))
			info.append(tr("Description: {0}", data.attr.get("desc"))).append("<br>");

		if(data.tracks.size() > 0){
			boolean first = true;
			WayPoint earliest = null, latest = null;

			for(GpxTrack trk: data.tracks){
				for(Collection<WayPoint> seg:trk.trackSegs){
					for(WayPoint pnt:seg){
						if(first){
							latest = earliest = pnt;
							first = false;
						}else{
							if(pnt.compareTo(earliest) < 0){
								earliest = pnt;
							}else{
								latest = pnt;
							}
						}
					}
				}
			}
			if(earliest != null && latest != null){
				DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
				info.append(tr("Timespan: ") + df.format(new Date((long)(earliest.time * 1000))) + " - "
				+ df.format(new Date((long)(latest.time * 1000))));
				int diff = (int)(latest.time - earliest.time);
				info.append(" (" + (diff / 3600) + ":" + ((diff % 3600)/60) + ")");
				info.append("<br>");
			}
		}
		info.append(tr("Length: ") + new DecimalFormat("#0.00").format(data.length() / 1000) + "km");
		info.append("<br>");

		return info.append("</html>").toString();
	}

	@Override public boolean isMergable(Layer other) {
		return other instanceof GpxLayer;
	}

	@Override public void mergeFrom(Layer from) {
		data.mergeFrom(((GpxLayer)from).data);
		computeCacheInSync = false;
	}

	private static Color[] colors = new Color[256];
	static {
		for (int i = 0; i < colors.length; i++) {
			colors[i] = Color.getHSBColor(i/300.0f, 1, 1);
		}
	}

	// lookup array to draw arrows without doing any math
	private static int ll0 = 9;
	private static int sl4 = 5;
	private static int sl9 = 3;
	private static int[][] dir = {
		{+sl4,+ll0,+ll0,+sl4},
		{-sl9,+ll0,+sl9,+ll0},
		{-ll0,+sl4,-sl4,+ll0},
		{-ll0,-sl9,-ll0,+sl9},
		{-sl4,-ll0,-ll0,-sl4},
		{+sl9,-ll0,-sl9,-ll0},
		{+ll0,-sl4,+sl4,-ll0},
		{+ll0,+sl9,+ll0,-sl9},
		{+sl4,+ll0,+ll0,+sl4},
		{-sl9,+ll0,+sl9,+ll0},
		{-ll0,+sl4,-sl4,+ll0},
		{-ll0,-sl9,-ll0,+sl9}
	};

	@Override public void paint(Graphics g, MapView mv) {

		/****************************************************************
		 ********** STEP 1 - GET CONFIG VALUES **************************
		 ****************************************************************/
		Long startTime = System.currentTimeMillis();
		Color neutralColor = Main.pref.getColor(marktr("gps point"), "layer "+name, Color.GRAY);
		boolean forceLines = Main.pref.getBoolean("draw.rawgps.lines.force");                     // also draw lines between points belonging to different segments
		boolean direction = Main.pref.getBoolean("draw.rawgps.direction");                        // draw direction arrows on the lines
		int maxLineLength = -1;
		try {
			maxLineLength = Integer.parseInt(Main.pref.get("draw.rawgps.max-line-length", "-1"));   // don't draw lines if longer than x meters
		} catch (java.lang.NumberFormatException e) {
			Main.pref.put("draw.rawgps.max-line-length", "-1");
		}
		boolean lines = Main.pref.getBoolean("draw.rawgps.lines");                                // draw line between points, global setting
		String linesKey = "draw.rawgps.lines.layer "+name;
		if (Main.pref.hasKey(linesKey))
			lines = Main.pref.getBoolean(linesKey);                                                 // draw lines, per-layer setting
		boolean large = Main.pref.getBoolean("draw.rawgps.large");                                // paint large dots for points
		boolean colored = Main.pref.getBoolean("draw.rawgps.colors");                             // color the lines
		boolean alternatedirection = Main.pref.getBoolean("draw.rawgps.alternatedirection");      // paint direction arrow with alternate math. may be faster

		/****************************************************************
		 ********** STEP 2a - CHECK CACHE VALIDITY **********************
		 ****************************************************************/
		if (computeCacheInSync && ((computeCacheMaxLineLengthUsed != maxLineLength) ||
								   (!neutralColor.equals(computeCacheColorUsed)) ||
								   (computeCacheColored != colored))) {
//          System.out.println("(re-)computing gpx line styles, reason: CCIS=" + computeCacheInSync + " CCMLLU=" + (computeCacheMaxLineLengthUsed != maxLineLength) + " CCCU=" +  (!neutralColor.equals(computeCacheColorUsed)) + " CCC=" + (computeCacheColored != colored));
			computeCacheMaxLineLengthUsed = maxLineLength;
			computeCacheInSync = false;
			computeCacheColorUsed = neutralColor;
			computeCacheColored = colored;
		}

		/****************************************************************
		 ********** STEP 2b - RE-COMPUTE CACHE DATA *********************
		 ****************************************************************/
		if (!computeCacheInSync) { // don't compute if the cache is good
		WayPoint oldWp = null;
		for (GpxTrack trk : data.tracks) {
				if (!forceLines) { // don't draw lines between segments, unless forced to
					oldWp = null;
			}
			for (Collection<WayPoint> segment : trk.trackSegs) {
				for (WayPoint trkPnt : segment) {
						if (Double.isNaN(trkPnt.latlon.lat()) || Double.isNaN(trkPnt.latlon.lon())) {
						continue;
						}
						if (oldWp != null) {
						double dist = trkPnt.latlon.greatCircleDistance(oldWp.latlon);
						double dtime = trkPnt.time - oldWp.time;
						double vel = dist/dtime;

							if (!colored) {
								trkPnt.speedLineColor = neutralColor;
							} else if (dtime <= 0 || vel < 0 || vel > 36) { // attn: bad case first
								trkPnt.speedLineColor = colors[255];
							} else {
								trkPnt.speedLineColor = colors[(int) (7*vel)];
							}
							if (maxLineLength == -1 || dist <= maxLineLength) {
								trkPnt.drawLine = true;
								trkPnt.dir = (int)(Math.atan2(-trkPnt.eastNorth.north()+oldWp.eastNorth.north(), trkPnt.eastNorth.east()-oldWp.eastNorth.east()) / Math.PI * 4 + 3.5); // crude but works
							} else {
								trkPnt.drawLine = false;
						}
						} else { // make sure we reset outdated data
							trkPnt.speedLineColor = colors[255];
							trkPnt.drawLine = false;
						}
						oldWp = trkPnt;
					}
				}
			}
			computeCacheInSync = true;
		}

		/****************************************************************
		 ********** STEP 3a - DRAW LINES ********************************
		 ****************************************************************/
		if (lines) {
		Point old = null;
		for (GpxTrack trk : data.tracks) {
			for (Collection<WayPoint> segment : trk.trackSegs) {
				for (WayPoint trkPnt : segment) {
					if (Double.isNaN(trkPnt.latlon.lat()) || Double.isNaN(trkPnt.latlon.lon()))
						continue;
					Point screen = mv.getPoint(trkPnt.eastNorth);
						if (trkPnt.drawLine) {
							// skip points that are on the same screenposition
							if (old != null && ((old.x != screen.x) || (old.y != screen.y))) {
								g.setColor(trkPnt.speedLineColor);
								g.drawLine(old.x, old.y, screen.x, screen.y);
							}
						}
						old = screen;
					} // end for trkpnt
				} // end for segment
			} // end for trk
		} // end if lines

		/****************************************************************
		 ********** STEP 3b - DRAW NICE ARROWS **************************
		 ****************************************************************/
		if (lines && direction && !alternatedirection) {
			Point old = null;
			for (GpxTrack trk : data.tracks) {
				for (Collection<WayPoint> segment : trk.trackSegs) {
					for (WayPoint trkPnt : segment) {
						if (Double.isNaN(trkPnt.latlon.lat()) || Double.isNaN(trkPnt.latlon.lon()))
							continue;
						if (trkPnt.drawLine) {
							Point screen = mv.getPoint(trkPnt.eastNorth);
							// skip points that are on the same screenposition
							if (old != null && ((old.x != screen.x) || (old.y != screen.y))) {
								g.setColor(trkPnt.speedLineColor);
								double t = Math.atan2(screen.y-old.y, screen.x-old.x) + Math.PI;
								g.drawLine(screen.x,screen.y, (int)(screen.x + 10*Math.cos(t-PHI)), (int)(screen.y
								+ 10*Math.sin(t-PHI)));
								g.drawLine(screen.x,screen.y, (int)(screen.x + 10*Math.cos(t+PHI)), (int)(screen.y
								+ 10*Math.sin(t+PHI)));
							}
							old = screen;
						}
					} // end for trkpnt
				} // end for segment
			} // end for trk
		} // end if lines

		/****************************************************************
		 ********** STEP 3c - DRAW FAST ARROWS **************************
		 ****************************************************************/
		if (lines && direction && alternatedirection) {
			Point old = null;
			for (GpxTrack trk : data.tracks) {
				for (Collection<WayPoint> segment : trk.trackSegs) {
					for (WayPoint trkPnt : segment) {
						if (Double.isNaN(trkPnt.latlon.lat()) || Double.isNaN(trkPnt.latlon.lon()))
							continue;
						if (trkPnt.drawLine) {
							Point screen = mv.getPoint(trkPnt.eastNorth);
							// skip points that are on the same screenposition
							if (old != null && ((old.x != screen.x) || (old.y != screen.y))) {
								g.setColor(trkPnt.speedLineColor);
								g.drawLine(screen.x, screen.y, screen.x + dir[trkPnt.dir][0], screen.y + dir[trkPnt.dir][1]);
								g.drawLine(screen.x, screen.y, screen.x + dir[trkPnt.dir][2], screen.y + dir[trkPnt.dir][3]);
							}
							old = screen;
						}
					} // end for trkpnt
				} // end for segment
			} // end for trk
		} // end if lines

		/****************************************************************
		 ********** STEP 3d - DRAW LARGE POINTS *************************
		 ****************************************************************/
		if (large) {
			g.setColor(neutralColor);
			for (GpxTrack trk : data.tracks) {
				for (Collection<WayPoint> segment : trk.trackSegs) {
					for (WayPoint trkPnt : segment) {
						if (Double.isNaN(trkPnt.latlon.lat()) || Double.isNaN(trkPnt.latlon.lon()))
							continue;
						Point screen = mv.getPoint(trkPnt.eastNorth);
							g.fillRect(screen.x-1, screen.y-1, 3, 3);
					} // end for trkpnt
				} // end for segment
			} // end for trk
		} // end if large

		/****************************************************************
		 ********** STEP 3e - DRAW SMALL POINTS FOR LINES ***************
		 ****************************************************************/
		if (!large && lines){
			g.setColor(neutralColor);
			for (GpxTrack trk : data.tracks) {
				for (Collection<WayPoint> segment : trk.trackSegs) {
					for (WayPoint trkPnt : segment) {
						if (Double.isNaN(trkPnt.latlon.lat()) || Double.isNaN(trkPnt.latlon.lon()))
							continue;
						if (!trkPnt.drawLine) {
							Point screen = mv.getPoint(trkPnt.eastNorth);
						g.drawRect(screen.x, screen.y, 0, 0);
					}
					} // end for trkpnt
				} // end for segment
			} // end for trk
		} // end if large

		/****************************************************************
		 ********** STEP 3f - DRAW SMALL POINTS INSTEAD OF LINES ********
		 ****************************************************************/
		if (!large && !lines){
			g.setColor(neutralColor);
			for (GpxTrack trk : data.tracks) {
				for (Collection<WayPoint> segment : trk.trackSegs) {
					for (WayPoint trkPnt : segment) {
						if (Double.isNaN(trkPnt.latlon.lat()) || Double.isNaN(trkPnt.latlon.lon()))
							continue;
						Point screen = mv.getPoint(trkPnt.eastNorth);
						g.drawRect(screen.x, screen.y, 0, 0);
					} // end for trkpnt
				} // end for segment
			} // end for trk
		} // end if large

		//Long duration = System.currentTimeMillis() - startTime;
		//System.out.println(duration);
	} // end paint

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
					URL url = new URL(Main.pref.get("osm-server.url") + "/" + version + "/gpx/create");

					// create a boundary string
					String boundary = MultiPartFormOutputStream.createBoundary();
					URLConnection urlConn = MultiPartFormOutputStream.createConnection(url);
					urlConn.setRequestProperty("Accept", "*/*");
					urlConn.setRequestProperty("Content-Type", MultiPartFormOutputStream.getContentType(boundary));
					// set some other request headers...
					urlConn.setRequestProperty("Connection", "Keep-Alive");
					urlConn.setRequestProperty("Cache-Control", "no-cache");
					// no need to connect cuz getOutputStream() does it
					MultiPartFormOutputStream out = new MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
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
					BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
					String line = "";
					while((line = in.readLine()) != null) {
						System.out.println(line);
					}
					in.close();

					//TODO check response
					/*                  int retCode = urlConn.getResponseCode();
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
					//  return; // assume cancel
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
						String timestr = p.getString("time");
						if(timestr != null)
						{
							timestr = timestr.replace("Z","+00:00");
							n.timestamp = timestr;
						}
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
	 * Makes a new marker layer derived from this GpxLayer containing at least one
	 * audio marker which the given audio file is associated with.
	 * Markers are derived from the following
	 * (a) explict waypoints in the GPX layer, or
	 * (b) named trackpoints in the GPX layer, or
	 * (c) (in future) voice recognised markers in the sound recording
	 * (d) a single marker at the beginning of the track
	 * @param wavFile : the file to be associated with the markers in the new marker layer
	 */
	private void importAudio(File wavFile) {
		String uri = "file:".concat(wavFile.getAbsolutePath());
		MarkerLayer ml = new MarkerLayer(new GpxData(), tr("Audio markers from {0}", name), associatedFile, me);

		Collection<WayPoint> waypoints = new ArrayList<WayPoint>();
		boolean timedMarkersOmitted = false;
		boolean untimedMarkersOmitted = false;
		double snapDistance = Main.pref.getDouble("marker.audiofromuntimedwaypoints.distance", 1.0e-3); /* about 25m */

		// determine time of first point in track
		double firstTime = -1.0;
		if (data.tracks != null && ! data.tracks.isEmpty()) {
			for (GpxTrack track : data.tracks) {
				if (track.trackSegs == null) continue;
				for (Collection<WayPoint> seg : track.trackSegs) {
					for (WayPoint w : seg) {
						firstTime = w.time;
						break;
					}
					if (firstTime >= 0.0) break;
				}
				if (firstTime >= 0.0) break;
			}
		}
		if (firstTime < 0.0) {
			JOptionPane.showMessageDialog(Main.parent, tr("No GPX track available in layer to associate audio with."));
			return;
		}

		// (a) try explicit timestamped waypoints - unless suppressed
		if (Main.pref.getBoolean("marker.audiofromexplicitwaypoints", true) &&
			data.waypoints != null && ! data.waypoints.isEmpty())
		{
			for (WayPoint w : data.waypoints) {
				if (w.time > firstTime) {
					waypoints.add(w);
				} else if (w.time > 0.0) {
					timedMarkersOmitted = true;
				}
			}
		}

		// (b) try explicit waypoints without timestamps - unless suppressed
		if (Main.pref.getBoolean("marker.audiofromuntimedwaypoints", true) &&
			data.waypoints != null && ! data.waypoints.isEmpty())
		{
			for (WayPoint w : data.waypoints) {
				if (waypoints.contains(w)) { continue; }
				WayPoint wNear = nearestPointOnTrack(w.eastNorth, snapDistance);
				if (wNear != null) {
					WayPoint wc = new WayPoint(w.latlon);
					wc.time = wNear.time;
					if (w.attr.containsKey("name")) wc.attr.put("name", w.getString("name"));
					waypoints.add(wc);
				} else {
					untimedMarkersOmitted = true;
				}
			}
		}

		// (c) use explicitly named track points, again unless suppressed
		if ((Main.pref.getBoolean("marker.audiofromnamedtrackpoints", false)) &&
			data.tracks != null && ! data.tracks.isEmpty())
		{
			for (GpxTrack track : data.tracks) {
				if (track.trackSegs == null) continue;
				for (Collection<WayPoint> seg : track.trackSegs) {
					for (WayPoint w : seg) {
						if (w.attr.containsKey("name") || w.attr.containsKey("desc")) {
							waypoints.add(w);
						}
					}
				}
			}
		}

		// (d) analyse audio for spoken markers here, in due course

		// (e) simply add a single marker at the start of the track
		if ((Main.pref.getBoolean("marker.audiofromstart") || waypoints.isEmpty()) &&
			data.tracks != null && ! data.tracks.isEmpty())
		{
			boolean gotOne = false;
			for (GpxTrack track : data.tracks) {
				if (track.trackSegs == null) continue;
				for (Collection<WayPoint> seg : track.trackSegs) {
					for (WayPoint w : seg) {
						WayPoint wStart = new WayPoint(w.latlon);
						wStart.attr.put("name", "start");
						wStart.time = w.time;
						waypoints.add(wStart);
						gotOne = true;
						break;
					}
					if (gotOne) break;
				}
				if (gotOne) break;
			}
		}

		/* we must have got at least one waypoint now */

		Collections.sort((ArrayList<WayPoint>) waypoints, new Comparator<WayPoint>() {
			public int compare(WayPoint a, WayPoint b) {
				return a.time <= b.time ? -1 : 1;
			}
		});

		firstTime = -1.0; /* this time of the first waypoint, not first trackpoint */
		for (WayPoint w : waypoints) {
			if (firstTime < 0.0) firstTime = w.time;
			double offset = w.time - firstTime;
			String name;
			if (w.attr.containsKey("name"))
				name = w.getString("name");
			else if (w.attr.containsKey("desc"))
				name = w.getString("desc");
			else
				name = AudioMarker.inventName(offset);
			AudioMarker am = AudioMarker.create(w.latlon,
					name, uri, ml, w.time, offset);
			ml.data.add(am);
		}
		Main.main.addLayer(ml);

		if (timedMarkersOmitted) {
			JOptionPane.showMessageDialog(Main.parent,
			tr("Some waypoints with timestamps from before the start of the track were omitted."));
		}
		if (untimedMarkersOmitted) {
			JOptionPane.showMessageDialog(Main.parent,
			tr("Some waypoints which were too far from the track to sensibly estimate their time were omitted."));
		}
	}

	/**
	 * Makes a WayPoint at the projection of point P onto the track providing P is
	 * less than tolerance away from the track

	 * @param P : the point to determine the projection for
	 * @param tolerance : must be no further than this from the track
	 * @return the closest point on the track to P, which may be the
	 * first or last point if off the end of a segment, or may be null if
	 * nothing close enough
	 */
	public WayPoint nearestPointOnTrack(EastNorth P, double tolerance) {
		/*
		 * assume the coordinates of P are xp,yp, and those of a section of track
		 * between two trackpoints are R=xr,yr and S=xs,ys. Let N be the projected point.
		 *
		 * The equation of RS is Ax + By + C = 0 where
		 * A = ys - yr
		 * B = xr - xs
		 * C = - Axr - Byr
		 *
		 * Also, note that the distance RS^2 is A^2 + B^2
		 *
		 * If RS^2 == 0.0 ignore the degenerate section of track
		 *
		 * PN^2 = (Axp + Byp + C)^2 / RS^2
		 * that is the distance from P to the line
		 *
		 * so if PN^2 is less than PNmin^2 (initialized to tolerance) we can reject
		 * the line; otherwise...
		 * determine if the projected poijnt lies within the bounds of the line:
		 * PR^2 - PN^2 <= RS^2 and PS^2 - PN^2 <= RS^2
		 *
		 * where PR^2 = (xp - xr)^2 + (yp-yr)^2
		 * and   PS^2 = (xp - xs)^2 + (yp-ys)^2
		 *
		 * If so, calculate N as
		 * xn = xr + (RN/RS) B
		 * yn = y1 + (RN/RS) A
		 *
		 * where RN = sqrt(PR^2 - PN^2)
		 */

		double PNminsq = tolerance * tolerance;
		EastNorth bestEN = null;
		double bestTime = 0.0;
		double px = P.east();
		double py = P.north();
		double rx = 0.0, ry = 0.0, sx, sy, x, y;
		if (data.tracks == null) return null;
		for (GpxTrack track : data.tracks) {
			if (track.trackSegs == null) continue;
			for (Collection<WayPoint> seg : track.trackSegs) {
				WayPoint R = null;
				for (WayPoint S : seg) {
					if (R == null) {
						R = S;
						rx = R.eastNorth.east();
						ry = R.eastNorth.north();
						x = px - rx;
						y = py - ry;
						double PRsq = x * x + y * y;
						if (PRsq < PNminsq) {
							PNminsq = PRsq;
							bestEN = R.eastNorth;
							bestTime = R.time;
						}
					} else {
						sx = S.eastNorth.east();
						sy = S.eastNorth.north();
						double A = sy - ry;
						double B = rx - sx;
						double C = - A * rx - B * ry;
						double RSsq = A * A + B * B;
						if (RSsq == 0.0) continue;
						double PNsq = A * px + B * py + C;
						PNsq = PNsq * PNsq / RSsq;
						if (PNsq < PNminsq) {
							x = px - rx;
							y = py - ry;
							double PRsq = x * x + y * y;
							x = px - sx;
							y = py - sy;
							double PSsq = x * x + y * y;
							if (PRsq - PNsq <= RSsq && PSsq - PNsq <= RSsq) {
								double RNoverRS = Math.sqrt((PRsq - PNsq)/RSsq);
								double nx = rx - RNoverRS * B;
								double ny = ry + RNoverRS * A;
								bestEN = new EastNorth(nx, ny);
								bestTime = R.time + RNoverRS * (S.time - R.time);
								PNminsq = PNsq;
							}
						}
						R = S;
						rx = sx;
						ry = sy;
					}
				}
				if (R != null) {
					/* if there is only one point in the seg, it will do this twice, but no matter */
					rx = R.eastNorth.east();
					ry = R.eastNorth.north();
					x = px - rx;
					y = py - ry;
					double PRsq = x * x + y * y;
					if (PRsq < PNminsq) {
						PNminsq = PRsq;
						bestEN = R.eastNorth;
						bestTime = R.time;
					}
				}
			}
		}
		if (bestEN == null) return null;
		WayPoint best = new WayPoint(Main.proj.eastNorth2latlon(bestEN));
		best.time = bestTime;
		return best;
	}
}
