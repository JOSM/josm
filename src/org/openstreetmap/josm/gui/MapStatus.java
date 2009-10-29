// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.SystemColor;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.help.Helpful;
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
         * the mouse position of the previous iteration. This is used to show
         * the popup until the cursor is moved.
         */
        private Point oldMousePos;
        /**
         * Contains the labels that are currently shown in the information
         * popup
         */
        private List<JLabel> popupLabels = null;
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

                // Do nothing, if required data is missing
                if(ms.mousePos == null || mv.center == null) {
                    continue;
                }

                // Freeze display when holding down CTRL
                if ((ms.modifiers & MouseEvent.CTRL_DOWN_MASK) != 0) {
                    // update the information popup's labels though, because
                    // the selection might have changed from the outside
                    popupUpdateLabels();
                    continue;
                }

                // This try/catch is a hack to stop the flooding bug reports about this.
                // The exception needed to handle with in the first place, means that this
                // access to the data need to be restarted, if the main thread modifies
                // the data.
                try {
                    // Set the text label in the bottom status bar
                    statusBarElementUpdate(ms);

                    // The popup != null check is required because a left-click
                    // produces several events as well, which would make this
                    // variable true. Of course we only want the popup to show
                    // if the middle mouse button has been pressed in the first
                    // place
                    boolean isAtOldPosition = (oldMousePos != null
                            && oldMousePos.equals(ms.mousePos)
                            && popup != null);
                    boolean middleMouseDown = (ms.modifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0;


                    // Popup Information
                    // display them if the middle mouse button is pressed and
                    // keep them until the mouse is moved
                    if (middleMouseDown || isAtOldPosition)
                    {
                        Collection<OsmPrimitive> osms = mv.getAllNearest(ms.mousePos);

                        if (osms == null) {
                            continue;
                        }

                        final JPanel c = new JPanel(new GridBagLayout());
                        final JLabel lbl = new JLabel(
                                "<html>"+tr("Middle click again to cycle through.<br>"+
                                "Hold CTRL to select directly from this list with the mouse.<hr>")+"</html>",
                                null,
                                JLabel.HORIZONTAL
                        );
                        lbl.setHorizontalAlignment(JLabel.LEFT);
                        c.add(lbl, GBC.eol().insets(2, 0, 2, 0));

                        // Only cycle if the mouse has not been moved and the
                        // middle mouse button has been pressed at least twice
                        // (the reason for this is the popup != null check for
                        // isAtOldPosition, see above. This is a nice side
                        // effect though, because it does not change selection
                        // of the first middle click)
                        if(isAtOldPosition && middleMouseDown) {
                            // Hand down mouse modifiers so the SHIFT mod can be
                            // handled correctly (see funcion)
                            popupCycleSelection(osms, ms.modifiers);
                        }

                        // These labels may need to be updated from the outside
                        // so collect them
                        List<JLabel> lbls = new ArrayList<JLabel>();
                        for (final OsmPrimitive osm : osms) {
                            JLabel l = popupBuildPrimitiveLabels(osm);
                            lbls.add(l);
                            c.add(l, GBC.eol().fill(GBC.HORIZONTAL).insets(2, 0, 2, 2));
                        }

                        popupShowPopup(popupCreatePopup(c, ms), lbls);
                    } else {
                        popupHidePopup();
                    }

                    oldMousePos = ms.mousePos;
                } catch (ConcurrentModificationException x) {
                    //x.printStackTrace();
                } catch (NullPointerException x) {
                    //x.printStackTrace();
                }
            }
        }

        /**
         * Creates a popup for the given content next to the cursor. Tries to
         * keep the popup on screen and shows a vertical scrollbar, if the
         * screen is too small.
         * @param content
         * @param ms
         * @return popup
         */
        private final Popup popupCreatePopup(Component content, MouseState ms) {
            Point p = mv.getLocationOnScreen();
            Dimension scrn = Toolkit.getDefaultToolkit().getScreenSize();

            // Create a JScrollPane around the content, in case there's not
            // enough space
            JScrollPane sp = new JScrollPane(content);
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setBorder(BorderFactory.createRaisedBevelBorder());
            // Implement max-size content-independent
            Dimension prefsize = sp.getPreferredSize();
            int w = Math.min(prefsize.width, Math.min(800, (scrn.width/2) - 16));
            int h = Math.min(prefsize.height, scrn.height - 10);
            sp.setPreferredSize(new Dimension(w, h));

            int xPos = p.x + ms.mousePos.x + 16;
            // Display the popup to the left of the cursor if it would be cut
            // off on its right, but only if more space is available
            if(xPos + w > scrn.width && xPos > scrn.width/2) {
                xPos = p.x + ms.mousePos.x - 4 - w;
            }
            int yPos = p.y + ms.mousePos.y + 16;
            // Move the popup up if it would be cut off at its bottom but do not
            // move it off screen on the top
            if(yPos + h > scrn.height - 5) {
                yPos = Math.max(5, scrn.height - h - 5);
            }

            PopupFactory pf = PopupFactory.getSharedInstance();
            return pf.getPopup(mv, sp, xPos, yPos);
        }

        /**
         * Calls this to update the element that is shown in the statusbar
         * @param ms
         */
        private final void statusBarElementUpdate(MouseState ms) {
            final OsmPrimitive osmNearest = mv.getNearest(ms.mousePos);
            if (osmNearest != null) {
                nameText.setText(osmNearest.getDisplayName(DefaultNameFormatter.getInstance()));
            } else {
                nameText.setText(tr("(no object)"));
            }
        }

        /**
         * Call this with a set of primitives to cycle through them. Method
         * will automatically select the next item and update the map
         * @param osms
         * @param mouse modifiers
         */
        private final void popupCycleSelection(Collection<OsmPrimitive> osms, int mods) {
            DataSet ds = Main.main.getCurrentDataSet();
            // Find some items that are required for cycling through
            OsmPrimitive firstItem = null;
            OsmPrimitive firstSelected = null;
            OsmPrimitive nextSelected = null;
            for (final OsmPrimitive osm : osms) {
                if(firstItem == null) {
                    firstItem = osm;
                }
                if(firstSelected != null && nextSelected == null) {
                    nextSelected = osm;
                }
                if(firstSelected == null && ds.isSelected(osm)) {
                    firstSelected = osm;
                }
            }

            // Clear previous selection if SHIFT (add to selection) is not
            // pressed. Cannot use "setSelected()" because it will cause a
            // fireSelectionChanged event which is unnecessary at this point.
            if((mods & MouseEvent.SHIFT_DOWN_MASK) == 0) {
                ds.clearSelection();
            }

            // This will cycle through the available items.
            if(firstSelected == null) {
                ds.addSelected(firstItem);
            } else {
                ds.clearSelection(firstSelected);
                if(nextSelected != null) {
                    ds.addSelected(nextSelected);
                }
            }
            ds.fireSelectionChanged();
        }

        /**
         * Tries to hide the given popup
         * @param popup
         */
        private final void popupHidePopup() {
            popupLabels = null;
            if(popup == null)
                return;
            final Popup staticPopup = popup;
            popup = null;
            EventQueue.invokeLater(new Runnable(){
                public void run() { staticPopup.hide(); }});
        }

        /**
         * Tries to show the given popup, can be hidden using popupHideOldPopup
         * If an old popup exists, it will be automatically hidden
         * @param popup
         */
        private final void popupShowPopup(Popup newPopup, List<JLabel> lbls) {
            final Popup staticPopup = newPopup;
            if(this.popup != null) {
                // If an old popup exists, remove it when the new popup has been
                // drawn to keep flickering to a minimum
                final Popup staticOldPopup = this.popup;
                EventQueue.invokeLater(new Runnable(){
                    public void run() {
                        staticPopup.show();
                        staticOldPopup.hide();
                    }
                });
            } else {
                // There is no old popup
                EventQueue.invokeLater(new Runnable(){
                    public void run() { staticPopup.show(); }});
            }
            this.popupLabels = lbls;
            this.popup = newPopup;
        }

        /**
         * This method should be called if the selection may have changed from
         * outside of this class. This is the case when CTRL is pressed and the
         * user clicks on the map instead of the popup.
         */
        private final void popupUpdateLabels() {
            if(this.popup == null || this.popupLabels == null)
                return;
            for(JLabel l : this.popupLabels) {
                l.validate();
            }
        }

        /**
         * Sets the colors for the given label depending on the selected status of
         * the given OsmPrimitive
         *
         * @param lbl The label to color
         * @param osm The primitive to derive the colors from
         */
        private final void popupSetLabelColors(JLabel lbl, OsmPrimitive osm) {
            DataSet ds = Main.main.getCurrentDataSet();
            if(ds.isSelected(osm)) {
                lbl.setBackground(SystemColor.textHighlight);
                lbl.setForeground(SystemColor.textHighlightText);
            } else {
                lbl.setBackground(SystemColor.control);
                lbl.setForeground(SystemColor.controlText);
            }
        }

        /**
         * Builds the labels with all necessary listeners for the info popup for the
         * given OsmPrimitive
         * @param osm  The primitive to create the label for
         * @return
         */
        private final JLabel popupBuildPrimitiveLabels(final OsmPrimitive osm) {
            final StringBuilder text = new StringBuilder();
            String name = osm.getDisplayName(DefaultNameFormatter.getInstance());
            if (osm.isNew() || osm.isModified()) {
                name = "<i><b>"+ name + "*</b></i>";
            }
            text.append(name);

            if (!osm.isNew()) {
                text.append(" [id="+osm.getId()+"]");
            }

            if(osm.getUser() != null) {
                text.append(" [" + tr("User:") + " " + osm.getUser().getName() + "]");
            }

            for (Entry<String, String> e1 : osm.entrySet()) {
                text.append("<br>" + e1.getKey() + "=" + e1.getValue());
            }

            final JLabel l = new JLabel(
                    "<html>" +text.toString() + "</html>",
                    ImageProvider.get(OsmPrimitiveType.from(osm)),
                    JLabel.HORIZONTAL
            ) {
                // This is necessary so the label updates its colors when the
                // selection is changed from the outside
                @Override public void validate() {
                    super.validate();
                    popupSetLabelColors(this, osm);
                }
            };
            l.setOpaque(true);
            popupSetLabelColors(l, osm);
            l.setFont(l.getFont().deriveFont(Font.PLAIN));
            l.setVerticalTextPosition(JLabel.TOP);
            l.setHorizontalAlignment(JLabel.LEFT);
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new MouseAdapter(){
                @Override public void mouseEntered(MouseEvent e) {
                    l.setBackground(SystemColor.info);
                    l.setForeground(SystemColor.infoText);
                }
                @Override public void mouseExited(MouseEvent e) {
                    popupSetLabelColors(l, osm);
                }
                @Override public void mouseClicked(MouseEvent e) {
                    DataSet ds = Main.main.getCurrentDataSet();
                    // Let the user toggle the selection
                    ds.toggleSelected(osm);
                    l.validate();
                }
            });
            // Sometimes the mouseEntered event is not catched, thus the label
            // will not be highlighted, making it confusing. The MotionListener
            // can correct this defect.
            l.addMouseMotionListener(new MouseMotionListener() {
                public void mouseMoved(MouseEvent e) {
                    l.setBackground(SystemColor.info);
                    l.setForeground(SystemColor.infoText);
                }
                public void mouseDragged(MouseEvent e) {
                    l.setBackground(SystemColor.info);
                    l.setForeground(SystemColor.infoText);
                }
            });
            return l;
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
