// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.HelpAction.Helpful;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.tools.GBC;

/**
 * A component that manages some status information display about the map.
 * It keeps a status line below the map up to date and displays some tooltip
 * information if the user hold the mouse long enough at some point.
 *
 * All this is done in background to not disturb other processes.
 *
 * The background thread does not alter any data of the map (read only thread).
 * Also it is rather fail safe. In case of some error in the data, it just do
 * nothing instead of whining and complaining.
 *
 * @author imi
 */
public class MapStatus extends JPanel implements Helpful {

	/**
	 * The MapView this status belongs.
	 */
	final MapView mv;
	/**
	 * The position of the mouse cursor.
	 */
	DecimalFormat latlon = new DecimalFormat("###0.0000000");
	JTextField positionText = new JTextField(25);
	
	/**
	 * The field holding the name of the object under the mouse.
	 */
	JTextField nameText = new JTextField(30);

	/**
	 * The field holding information about what the user can do.
	 */
	JTextField helpText = new JTextField();
	
	/**
	 * The collector class that waits for notification and then update
	 * the display objects.
	 *
	 * @author imi
	 */
	private final class Collector implements Runnable {
		/**
		 * The last object displayed in status line.
		 */
		Collection<OsmPrimitive> osmStatus;
		/**
		 * The old modifiers, that was pressed the last time this collector ran.
		 */
		private int oldModifiers;
		/**
		 * The popup displayed to show additional information
		 */
		private Popup popup;

		private MapFrame parent;

		public Collector(MapFrame parent) {
			this.parent = parent;
		}

		/**
		 * Execution function for the Collector.
		 */
		public void run() {
			for (;;) {
				MouseState ms = new MouseState();
				synchronized (this) {
					try {wait();} catch (InterruptedException e) {}
					ms.modifiers = mouseState.modifiers;
					ms.mousePos = mouseState.mousePos;
				}
				if (parent != Main.map)
					return; // exit, if new parent.
				if ((ms.modifiers & MouseEvent.CTRL_DOWN_MASK) != 0 || ms.mousePos == null)
					continue; // freeze display when holding down ctrl

				if (mv.center == null)
					continue;

				// This try/catch is a hack to stop the flooding bug reports about this.
				// The exception needed to handle with in the first place, means that this
				// access to the data need to be restarted, if the main thread modifies
				// the data.
				try {
					// Popup Information
					if ((ms.modifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0 ) {
						Collection<OsmPrimitive> osms = mv.getAllNearest(ms.mousePos);

						if (osms == null)
							continue;
						if (osms != null && osms.equals(osmStatus) && ms.modifiers == oldModifiers)
							continue;
					/*
					osmStatus = osms;
					oldModifiers = ms.modifiers;

					OsmPrimitive osmNearest = null;
					// Set the text label in the bottom status bar
					osmNearest = mv.getNearest(ms.mousePos);
					if (osmNearest != null) {
						NameVisitor visitor = new NameVisitor();
						osmNearest.visit(visitor);
						nameText.setText(visitor.name);
					} else
						nameText.setText("");
					*/
					

						if (popup != null) {
							try {
	                            EventQueue.invokeAndWait(new Runnable() {
	                                public void run() {
	                                	popup.hide();
	                                }
	                            });
                            } catch (InterruptedException e) {
                            } catch (InvocationTargetException e) {
                            	throw new RuntimeException(e);
                            }
						}

						JPanel c = new JPanel(new GridBagLayout());
						for (final OsmPrimitive osm : osms) {
							NameVisitor visitor = new NameVisitor();
							osm.visit(visitor);
							final StringBuilder text = new StringBuilder();
							if (osm.id == 0 || osm.modified)
								visitor.name = "<i><b>"+visitor.name+"*</b></i>";
							text.append(visitor.name);
							if (osm.id != 0)
								text.append("<br>id="+osm.id);
							for (Entry<String, String> e : osm.entrySet())
								text.append("<br>"+e.getKey()+"="+e.getValue());
							final JLabel l = new JLabel("<html>"+text.toString()+"</html>", visitor.icon, JLabel.HORIZONTAL);
							l.setFont(l.getFont().deriveFont(Font.PLAIN));
							l.setVerticalTextPosition(JLabel.TOP);
							l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
							l.addMouseListener(new MouseAdapter(){
								@Override public void mouseEntered(MouseEvent e) {
									l.setText("<html><u color='blue'>"+text.toString()+"</u></html>");
								}
								@Override public void mouseExited(MouseEvent e) {
									l.setText("<html>"+text.toString()+"</html>");
								}
								@Override public void mouseClicked(MouseEvent e) {
									Main.ds.setSelected(osm);
									mv.repaint();
								}
							});
							c.add(l, GBC.eol());
						}

						Point p = mv.getLocationOnScreen();
						popup = PopupFactory.getSharedInstance().getPopup(mv, c, p.x+ms.mousePos.x+16, p.y+ms.mousePos.y+16);
						final Popup staticPopup = popup;
						EventQueue.invokeLater(new Runnable(){
							public void run() {
								staticPopup.show();
                            }
						});
					} else if (popup != null) {
						final Popup staticPopup = popup;
						popup = null;
						EventQueue.invokeLater(new Runnable(){
							public void run() {
								staticPopup.hide();
                            }
						});
					}
				} catch (ConcurrentModificationException x) {
				} catch (NullPointerException x) {
				}
			}
		}
	}

	/**
	 * Everything, the collector is interested of. Access must be synchronized.
	 * @author imi
	 */
	class MouseState {
		Point mousePos;
		int modifiers;
	}
	/**
	 * The last sent mouse movement event.
	 */
	MouseState mouseState = new MouseState();

	/**
	 * Construct a new MapStatus and attach it to the map view.
	 * @param mv The MapView the status line is part of.
	 */
	public MapStatus(final MapFrame mapFrame) {
		this.mv = mapFrame.mapView;

		// Listen for mouse movements and set the position text field
		mv.addMouseMotionListener(new MouseMotionListener(){
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
			public void mouseMoved(MouseEvent e) {
				if (mv.center == null)
					return;
				// Do not update the view, if ctrl is pressed.
				if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == 0) {
					LatLon p = mv.getLatLon(e.getX(),e.getY());
					positionText.setText(latlon.format(p.lat())+" "+latlon.format(p.lon()));
				}
			}
		});

		positionText.setEditable(false);
		nameText.setEditable(false);
		helpText.setEditable(false);
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		add(new JLabel(tr("Lat/Lon")+" "), GBC.std());
		add(positionText, GBC.std());
		//add(new JLabel(" "+tr("Object")+" "));
		//add(nameText);
		add(helpText, GBC.eol().fill(GBC.HORIZONTAL));
		positionText.setMinimumSize(new Dimension(positionText.getMinimumSize().height, 200));
		
		// The background thread
		final Collector collector = new Collector(mapFrame);
		new Thread(collector).start();

		// Listen to keyboard/mouse events for pressing/releasing alt key and
		// inform the collector.
		try {
			Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener(){
				public void eventDispatched(AWTEvent event) {
					synchronized (collector) {
						mouseState.modifiers = ((InputEvent)event).getModifiersEx();
						if (event instanceof MouseEvent)
							mouseState.mousePos = ((MouseEvent)event).getPoint();
						collector.notify();
					}
				}
			}, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
		} catch (SecurityException ex) {
			mapFrame.mapView.addMouseMotionListener(new MouseMotionListener() {
				public void mouseMoved(MouseEvent e) {
					synchronized (collector) {
						mouseState.modifiers = e.getModifiersEx();
						mouseState.mousePos = e.getPoint();
						collector.notify();
					}
				}

				public void mouseDragged(MouseEvent e) {
					mouseMoved(e);
				}
			});

			mapFrame.mapView.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					synchronized (collector) {
						mouseState.modifiers = e.getModifiersEx();
						collector.notify();
					}
				}

				public void keyReleased(KeyEvent e) {
					keyReleased(e);
				}
			});
		}
	}

	public String helpTopic() {
	    return "Statusline";
    }
	
	public void setHelpText(String t) {
		helpText.setText(t);
	}
}
