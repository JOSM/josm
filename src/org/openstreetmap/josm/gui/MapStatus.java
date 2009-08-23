// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.HelpAction.Helpful;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A component that manages some status information display about the map.
 * It keeps a status line below the map up to date and displays some tooltip
 * information if the user hold the mouse long enough at some point.
 *
 * All this is done in background to not disturb other processes.
 *
 * The background thread does not alter any data of the map (read only thread).
 * Also it is rather fail safe. In case of some error in the data, it just does
 * nothing instead of whining and complaining.
 *
 * @author imi
 */
public class MapStatus extends JPanel implements Helpful {

    /**
     * The MapView this status belongs to.
     */
    final MapView mv;

    /**
     * A small user interface component that consists of an image label and
     * a fixed text content to the right of the image.
     */
    class ImageLabel extends JPanel {
        private JLabel tf;
        private int chars;
        public ImageLabel(String img, String tooltip, int chars) {
            super();
            setLayout(new GridBagLayout());
            setBackground(Color.decode("#b8cfe5"));
            add(new JLabel(ImageProvider.get("statusline/"+img+".png")), GBC.std().anchor(GBC.WEST).insets(0,1,1,0));
            add(tf = new JLabel(), GBC.std().fill(GBC.BOTH).anchor(GBC.WEST).insets(2,1,1,0));
            setToolTipText(tooltip);
            this.chars = chars;
        }
        public void setText(String t) {
            tf.setText(t);
        }
        @Override public Dimension getPreferredSize() {
            return new Dimension(25 + chars*tf.getFontMetrics(tf.getFont()).charWidth('0'), super.getPreferredSize().height);
        }
        @Override public Dimension getMinimumSize() {
            return new Dimension(25 + chars*tf.getFontMetrics(tf.getFont()).charWidth('0'), super.getMinimumSize().height);
        }
    }

    ImageLabel lonText = new ImageLabel("lon", tr("The geographic longitude at the mouse pointer."), 11);
    ImageLabel nameText = new ImageLabel("name", tr("The name of the object at the mouse pointer."), 20);
    JTextField helpText = new JTextField();
    ImageLabel latText = new ImageLabel("lat", tr("The geographic latitude at the mouse pointer."), 10);
    ImageLabel angleText = new ImageLabel("angle", tr("The angle between the previous and the current way segment."), 6);
    ImageLabel headingText = new ImageLabel("heading", tr("The (compass) heading of the line segment being drawn."), 6);
    ImageLabel distText = new ImageLabel("dist", tr("The length of the new way segment being drawn."), 8);

    /**
     * This is the thread that runs in the background and collects the information displayed.
     * It gets destroyed by MapFrame.java/destroy() when the MapFrame itself is destroyed.
     */
    public Thread thread;

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
         * The old modifiers that was pressed the last time this collector ran.
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
                if ((ms.modifiers & MouseEvent.CTRL_DOWN_MASK) != 0 || ms.mousePos == null) {
                    continue; // freeze display when holding down ctrl
                }

                if (mv.center == null) {
                    continue;
                }

                // This try/catch is a hack to stop the flooding bug reports about this.
                // The exception needed to handle with in the first place, means that this
                // access to the data need to be restarted, if the main thread modifies
                // the data.
                try {
                    OsmPrimitive osmNearest = null;
                    // Set the text label in the bottom status bar
                    osmNearest = mv.getNearest(ms.mousePos);
                    if (osmNearest != null) {
                        nameText.setText(osmNearest.getDisplayName(DefaultNameFormatter.getInstance()));
                    } else {
                        nameText.setText(tr("(no object)"));
                    }

                    // Popup Information
                    if ((ms.modifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0 ) {
                        Collection<OsmPrimitive> osms = mv.getAllNearest(ms.mousePos);

                        if (osms == null) {
                            continue;
                        }
                        if (osms != null && osms.equals(osmStatus) && ms.modifiers == oldModifiers) {
                            continue;
                        }

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
                            final StringBuilder text = new StringBuilder();
                            String name = osm.getDisplayName(DefaultNameFormatter.getInstance());
                            if (osm.id == 0 || osm.modified) {
                                name = "<i><b>"+ osm.getDisplayName(DefaultNameFormatter.getInstance())+"*</b></i>";
                            }
                            text.append(name);
                            if (osm.id != 0) {
                                text.append("<br>id="+osm.id);
                            }
                            for (Entry<String, String> e : osm.entrySet()) {
                                text.append("<br>"+e.getKey()+"="+e.getValue());
                            }
                            final JLabel l = new JLabel(
                                    "<html>"+text.toString()+"</html>",
                                    ImageProvider.get(OsmPrimitiveType.from(osm)),
                                    JLabel.HORIZONTAL
                            );
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
                                    Main.main.getCurrentDataSet().setSelected(osm);
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
     * @param mapFrame The MapFrame the status line is part of.
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
                // Do not update the view if ctrl is pressed.
                if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == 0) {
                    CoordinateFormat mCord = CoordinateFormat.getDefaultFormat();
                    LatLon p = mv.getLatLon(e.getX(),e.getY());
                    latText.setText(p.latToString(mCord));
                    lonText.setText(p.lonToString(mCord));
                }
            }
        });

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        add(latText, GBC.std());
        add(lonText, GBC.std().insets(3,0,0,0));
        add(headingText, GBC.std().insets(3,0,0,0));
        add(angleText, GBC.std().insets(3,0,0,0));
        add(distText, GBC.std().insets(3,0,0,0));

        helpText.setEditable(false);
        add(nameText, GBC.std().insets(3,0,0,0));
        add(helpText, GBC.eol().insets(3,0,0,0).fill(GBC.HORIZONTAL));

        // The background thread
        final Collector collector = new Collector(mapFrame);
        thread = new Thread(collector, "Map Status Collector");
        thread.setDaemon(true);
        thread.start();

        // Listen to keyboard/mouse events for pressing/releasing alt key and
        // inform the collector.
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener(){
                public void eventDispatched(AWTEvent event) {
                    if (event instanceof ComponentEvent &&
                            ((ComponentEvent)event).getComponent() == mapFrame.mapView) {
                        synchronized (collector) {
                            mouseState.modifiers = ((InputEvent)event).getModifiersEx();
                            if (event instanceof MouseEvent) {
                                mouseState.mousePos = ((MouseEvent)event).getPoint();
                            }
                            collector.notify();
                        }
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
                @Override public void keyPressed(KeyEvent e) {
                    synchronized (collector) {
                        mouseState.modifiers = e.getModifiersEx();
                        collector.notify();
                    }
                }

                @Override public void keyReleased(KeyEvent e) {
                    keyPressed(e);
                }
            });
        }
    }

    public String helpTopic() {
        return "Statusline";
    }

    @Override
    public void addMouseListener(MouseListener ml) {
        //super.addMouseListener(ml);
        lonText.addMouseListener(ml);
        latText.addMouseListener(ml);
    }

    public void setHelpText(String t) {
        helpText.setText(t);
        helpText.setToolTipText(t);
    }
    public void setAngle(double a) {
        angleText.setText(a < 0 ? "--" : Math.round(a*10)/10.0 + " °");
    }
    public void setHeading(double h) {
        headingText.setText(h < 0 ? "--" : Math.round(h*10)/10.0 + " °");
    }
    public void setDist(double dist) {
        String text = dist > 1000 ? (Math.round(dist/100)/10.0)+" km" : Math.round(dist*10)/10.0 +" m";
        distText.setText(dist < 0 ? "--" : text);
    }
}
