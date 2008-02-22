// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.AudioMarker;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.AudioPlayer;

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
	public GpxLayer fromLayer = null;
	private Rectangle audioTracer = null;
	private Icon audioTracerIcon = null;
	private EastNorth playheadPosition = null;
	private static Timer timer = null;
	private static double audioAnimationInterval = 0.0; // seconds
	private static double playheadTime = -1.0;

	public MarkerLayer(GpxData indata, String name, File associatedFile, GpxLayer fromLayer) {
		
		super(name);
		this.associatedFile = associatedFile;
		this.data = new ArrayList<Marker>();
		this.fromLayer = fromLayer;
		double firstTime = -1.0;

		for (WayPoint wpt : indata.waypoints) {
			/* calculate time differences in waypoints */
			double time = wpt.time;
			if (firstTime < 0)
				firstTime = time;
            Marker m = Marker.createMarker(wpt, indata.storageFile, this, time, time - firstTime);
            if (m != null)
            	data.add(m);
		}
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				Main.map.mapView.addMouseListener(new MouseAdapter() {
					@Override public void mousePressed(MouseEvent e) {
						if (e.getButton() != MouseEvent.BUTTON1)
							return;
						boolean mousePressedInButton = false;
						if (e.getPoint() != null) {
							for (Marker mkr : data) {
								if (mkr.containsPoint(e.getPoint())) {
									mousePressedInButton = true;
									break;
								}
							}
						}
						if (! mousePressedInButton)
							return;
						mousePressed  = true;
						if (visible)
							Main.map.mapView.repaint();
					}
					@Override public void mouseReleased(MouseEvent ev) {
						if (ev.getButton() != MouseEvent.BUTTON1 || ! mousePressed)
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

		if (audioTracer != null) {
			Point screen = Main.map.mapView.getPoint(playheadPosition);
			audioTracer.setLocation(screen.x, screen.y);
			audioTracerIcon.paintIcon(Main.map.mapView, g, screen.x, screen.y);
		}
	}

	protected void traceAudio() {
		if (timer == null) {
			audioAnimationInterval = Double.parseDouble(Main.pref.get("marker.audioanimationinterval", "1")); //milliseconds
			timer = new Timer((int)(audioAnimationInterval * 1000.0), new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					timerAction();
				}
			});
			timer.start();
		}
	}
	
	/**
	 * callback for AudioPlayer when position changes 
	 * @param position seconds into the audio stream
	 */
	public void timerAction() {
		AudioMarker recentlyPlayedMarker = AudioMarker.recentlyPlayedMarker();
		if (recentlyPlayedMarker == null)
			return;
		double audioTime = recentlyPlayedMarker.time + 
			AudioPlayer.position() - 
			recentlyPlayedMarker.offset -
			recentlyPlayedMarker.syncOffset;
		if (Math.abs(audioTime- playheadTime) < audioAnimationInterval)
			return;
		if (fromLayer == null)
			return;
		/* find the pair of track points for this position (adjusted by the syncOffset)
		 * and interpolate between them 
		 */
		WayPoint w1 = null;
		WayPoint w2 = null;

		for (GpxTrack track : fromLayer.data.tracks) {
			for (Collection<WayPoint> trackseg : track.trackSegs) {
				for (Iterator<WayPoint> it = trackseg.iterator(); it.hasNext();) {
					WayPoint w = it.next();
					if (audioTime < w.time) {
						w2 = w;
						break;
					}
					w1 = w;
				}
				if (w2 != null) break;
			}
			if (w2 != null) break;
		}
		
		if (w1 == null)
			return;
		playheadPosition = w2 == null ? 
			w1.eastNorth : 
			w1.eastNorth.interpolate(w2.eastNorth, 
					(audioTime - w1.time)/(w2.time - w1.time));
		
		if (audioTracer == null) {
			audioTracerIcon = ImageProvider.getIfAvailable("markers",Main.pref.get("marker.audiotracericon", "audio-tracer"));
			audioTracer = new Rectangle(0, 0, audioTracerIcon.getIconWidth(), audioTracerIcon.getIconHeight());			
		}
		playheadTime = audioTime;
		Main.map.mapView.repaint();
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

	public void applyAudio(File wavFile) {
		String uri = "file:".concat(wavFile.getAbsolutePath());
		Collection<Marker> markers = new ArrayList<Marker>();
	    for (Marker mkr : data) {
	    	AudioMarker audioMarker = mkr.audioMarkerFromMarker(uri);
	    	if (audioMarker == null) {
	    		markers.add(mkr);
	    	} else { 
	            markers.add(audioMarker);
	    	}
	    }
	    data.clear();
	    data.addAll(markers);
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

		JMenuItem applyaudio = new JMenuItem(tr("Apply Audio"), ImageProvider.get("applyaudio"));
		applyaudio.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JFileChooser fc = new JFileChooser(Main.pref.get("tagimages.lastdirectory"));
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
				if (sel == null)
					return;
				applyAudio(sel);
				Main.map.repaint();
			}
		});

		JMenuItem syncaudio = new JMenuItem(tr("Synchronize Audio"), ImageProvider.get("audio-sync"));
		syncaudio.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				adjustOffsetsOnAudioMarkers();
			}
		});

		JMenuItem moveaudio = new JMenuItem(tr("Make Audio Marker At Play Head"), ImageProvider.get("addmarkers"));
		moveaudio.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				makeAudioMarkerAtPlayHead();
			}
		});

		Collection<Component> components = new ArrayList<Component>();
		components.add(new JMenuItem(new LayerListDialog.ShowHideLayerAction(this)));
		components.add(new JMenuItem(new LayerListDialog.ShowHideMarkerText(this)));
		components.add(new JMenuItem(new LayerListDialog.DeleteLayerAction(this)));
		components.add(new JSeparator());
		components.add(color);
		components.add(new JSeparator());
		components.add(syncaudio);
		components.add(applyaudio);
		if (Main.pref.getBoolean("marker.traceaudio", true)) {
			components.add (moveaudio);
		}		
		components.add(new JMenuItem(new RenameLayerAction(associatedFile, this)));
		components.add(new JSeparator());
		components.add(new JMenuItem(new LayerListPopup.InfoAction(this)));
		return components.toArray(new Component[0]);
	}

	private void adjustOffsetsOnAudioMarkers() {
		if (! AudioPlayer.paused()) {
			JOptionPane.showMessageDialog(Main.parent,tr("You need to pause audio at the moment when you hear your synchronization cue."));
			return;
		}
		Marker startMarker = AudioMarker.recentlyPlayedMarker();
		boolean explicitMarker = true;
		if (startMarker != null && ! data.contains(startMarker)) {
			explicitMarker = false;
			startMarker = null;
		}
		if (startMarker == null) {
			// find the first audioMarker in this layer
			for (Marker m : data) {
				if (m.getClass() == AudioMarker.class) {
					startMarker = m;
					break;
				}
			}
		}
		if (startMarker == null) {
			// still no marker to work from - message?
			JOptionPane.showMessageDialog(Main.parent,tr("No audio marker found in the layer to synchronize with."));
			return;
		}
		// apply adjustment to all subsequent audio markers in the layer
		double adjustment = AudioPlayer.position() - startMarker.offset; // in seconds
		boolean seenStart = false;
		URL url = ((AudioMarker)startMarker).url();
		for (Marker m : data) {
			if (m == startMarker)
				seenStart = true;
			if (seenStart) {
				AudioMarker ma = (AudioMarker) m; // it must be an AudioMarker
				if (ma.url().equals(url))
					ma.adjustOffset(adjustment);
			}
		}
		
		JOptionPane.showMessageDialog(Main.parent, explicitMarker ? 
			tr("Audio synchronized with most recently played marker and subsequent ones (that have the same sound track).") :
			tr("Audio synchronized with audio markers in the layer (that have the same sound track as the first one)."));
	}
	
	private void makeAudioMarkerAtPlayHead() {
		if (! AudioPlayer.paused()) {
			JOptionPane.showMessageDialog(Main.parent,tr("You need to pause audio at the point on the track where you want the marker."));
			return;
		}
		// find first audio marker to get absolute start time
		double offset = 0.0;
		AudioMarker am = null; 
		for (Marker m : data) {
			if (m.getClass() == AudioMarker.class) {
				am = (AudioMarker)m;
				offset = playheadTime - am.time;
				break;
			}
		}
		if (am == null) {
			JOptionPane.showMessageDialog(Main.parent,tr("No existing audio markers in this layer to offset from."));
			return;
		}

		// make our new marker
		AudioMarker newAudioMarker = AudioMarker.create(Main.proj.eastNorth2latlon(playheadPosition), 
			AudioMarker.inventName(offset), AudioPlayer.url().toString(), this, playheadTime, offset);
		
		// insert it at the right place in a copy the collection
		Collection<Marker> newData = new ArrayList<Marker>();
		am = null; 
		for (Marker m : data) {
			if (m.getClass() == AudioMarker.class) {
				am = (AudioMarker) m;
				if (newAudioMarker != null && offset < am.offset) {
					newAudioMarker.adjustOffset(am.syncOffset()); // i.e. same as predecessor
					newData.add(newAudioMarker);
					newAudioMarker = null;
				}
			}
			newData.add(m);
		}
		if (newAudioMarker != null) {
			if (am != null)
				newAudioMarker.adjustOffset(am.syncOffset()); // i.e. same as predecessor				
			newData.add(newAudioMarker); // insert at end
			newAudioMarker = null;
		}
		
		// replace the collection
		data.clear();
		data.addAll(newData);
		Main.map.mapView.repaint();
	}
	
	public static void playAudio() {
		if (Main.map == null || Main.map.mapView == null)
			return;
		for (Layer layer : Main.map.mapView.getAllLayers()) {
			if (layer.getClass() == MarkerLayer.class) {
				MarkerLayer markerLayer = (MarkerLayer) layer;
				for (Marker marker : markerLayer.data) {
					if (marker.getClass() == AudioMarker.class) {
						((AudioMarker)marker).play();
						break;
					}
				}
			}
		}
	}

	public static void playNextMarker() {
		playAdjacentMarker(true);
	}
	
	public static void playPreviousMarker() {
		playAdjacentMarker(false);
	}
	
	private static void playAdjacentMarker(boolean next) {
		Marker startMarker = AudioMarker.recentlyPlayedMarker();
		if (startMarker == null) {
			// message?
			return;
		}
		Marker previousMarker = null;
		Marker targetMarker = null;
		boolean nextTime = false;
		if (Main.map == null || Main.map.mapView == null)
			return;
		for (Layer layer : Main.map.mapView.getAllLayers()) {
			if (layer.getClass() == MarkerLayer.class) {
				MarkerLayer markerLayer = (MarkerLayer) layer;
				for (Marker marker : markerLayer.data) {
					if (marker == startMarker) {
						if (next) {
							nextTime = true;
						} else {
							if (previousMarker == null)
								previousMarker = startMarker; // if no previous one, play the first one again
							((AudioMarker)previousMarker).play();
							break;
						}
					} else if (nextTime && marker.getClass() == AudioMarker.class) {
						((AudioMarker)marker).play();
						return;
					}
					if (marker.getClass() == AudioMarker.class)
						previousMarker = marker;
				}
				if (nextTime) {
					// there was no next marker in that layer, so play the last one again
					((AudioMarker)startMarker).play();
					return;
				}
			}
		}
	}
	
}
