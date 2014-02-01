// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
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
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.gui.NavigatableComponent.SoMChangeListener;
import org.openstreetmap.josm.gui.help.Helpful;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor.ProgressMonitorDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.ImageLabel;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.Destroyable;
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
public class MapStatus extends JPanel implements Helpful, Destroyable, PreferenceChangedListener {

    /**
     * Property for map status background color.
     * @since 6789
     */
    public static final ColorProperty PROP_BACKGROUND_COLOR = new ColorProperty(
            marktr("Status bar background"), Color.decode("#b8cfe5"));

    /**
     * Property for map status background color (active state).
     * @since 6789
     */
    public static final ColorProperty PROP_ACTIVE_BACKGROUND_COLOR = new ColorProperty(
            marktr("Status bar background: active"), Color.decode("#aaff5e"));

    /**
     * Property for map status foreground color.
     * @since 6789
     */
    public static final ColorProperty PROP_FOREGROUND_COLOR = new ColorProperty(
            marktr("Status bar foreground"), Color.black);

    /**
     * Property for map status foreground color (active state).
     * @since 6789
     */
    public static final ColorProperty PROP_ACTIVE_FOREGROUND_COLOR = new ColorProperty(
            marktr("Status bar foreground: active"), Color.black);

    /**
     * The MapView this status belongs to.
     */
    final MapView mv;
    final Collector collector;

    public class BackgroundProgressMonitor implements ProgressMonitorDialog {

        private String title;
        private String customText;

        private void updateText() {
            if (customText != null && !customText.isEmpty()) {
                progressBar.setToolTipText(tr("{0} ({1})", title, customText));
            } else {
                progressBar.setToolTipText(title);
            }
        }

        @Override
        public void setVisible(boolean visible) {
            progressBar.setVisible(visible);
        }

        @Override
        public void updateProgress(int progress) {
            progressBar.setValue(progress);
            progressBar.repaint();
            MapStatus.this.doLayout();
        }

        @Override
        public void setCustomText(String text) {
            this.customText = text;
            updateText();
        }

        @Override
        public void setCurrentAction(String text) {
            this.title = text;
            updateText();
        }

        @Override
        public void setIndeterminate(boolean newValue) {
            UIManager.put("ProgressBar.cycleTime", UIManager.getInt("ProgressBar.repaintInterval") * 100);
            progressBar.setIndeterminate(newValue);
        }

        @Override
        public void appendLogMessage(String message) {
            if (message != null && !message.isEmpty()) {
                Main.info("appendLogMessage not implemented for background tasks. Message was: " + message);
            }
        }

    }

    final ImageLabel latText = new ImageLabel("lat", tr("The geographic latitude at the mouse pointer."), 11, PROP_BACKGROUND_COLOR.get());
    final ImageLabel lonText = new ImageLabel("lon", tr("The geographic longitude at the mouse pointer."), 11, PROP_BACKGROUND_COLOR.get());
    final ImageLabel headingText = new ImageLabel("heading", tr("The (compass) heading of the line segment being drawn."), 6, PROP_BACKGROUND_COLOR.get());
    final ImageLabel angleText = new ImageLabel("angle", tr("The angle between the previous and the current way segment."), 6, PROP_BACKGROUND_COLOR.get());
    final ImageLabel distText = new ImageLabel("dist", tr("The length of the new way segment being drawn."), 10, PROP_BACKGROUND_COLOR.get());
    final ImageLabel nameText = new ImageLabel("name", tr("The name of the object at the mouse pointer."), 20, PROP_BACKGROUND_COLOR.get());
    final JosmTextField helpText = new JosmTextField();
    final JProgressBar progressBar = new JProgressBar();
    public final BackgroundProgressMonitor progressMonitor = new BackgroundProgressMonitor();

    private final SoMChangeListener somListener;

    // Distance value displayed in distText, stored if refresh needed after a change of system of measurement
    private double distValue;

    // Determines if angle panel is enabled or not
    private boolean angleEnabled = false;

    /**
     * This is the thread that runs in the background and collects the information displayed.
     * It gets destroyed by destroy() when the MapFrame itself is destroyed.
     */
    private Thread thread;

    private final List<StatusTextHistory> statusText = new ArrayList<StatusTextHistory>();

    private static class StatusTextHistory {
        final Object id;
        final String text;

        public StatusTextHistory(Object id, String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof StatusTextHistory && ((StatusTextHistory)obj).id == id;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(id);
        }
    }

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
        @Override
        public void run() {
            registerListeners();
            try {
                for (;;) {

                    final MouseState ms = new MouseState();
                    synchronized (this) {
                        // TODO Would be better if the timeout wasn't necessary
                        try {
                            wait(1000);
                        } catch (InterruptedException e) {
                            // Occurs frequently during JOSM shutdown, log set to debug only
                            Main.debug("InterruptedException in "+MapStatus.class.getSimpleName());
                        }
                        ms.modifiers = mouseState.modifiers;
                        ms.mousePos = mouseState.mousePos;
                    }
                    if (parent != Main.map)
                        return; // exit, if new parent.

                    // Do nothing, if required data is missing
                    if(ms.mousePos == null || mv.center == null) {
                        continue;
                    }

                    try {
                        EventQueue.invokeAndWait(new Runnable() {

                            @Override
                            public void run() {
                                // Freeze display when holding down CTRL
                                if ((ms.modifiers & MouseEvent.CTRL_DOWN_MASK) != 0) {
                                    // update the information popup's labels though, because
                                    // the selection might have changed from the outside
                                    popupUpdateLabels();
                                    return;
                                }

                                // This try/catch is a hack to stop the flooding bug reports about this.
                                // The exception needed to handle with in the first place, means that this
                                // access to the data need to be restarted, if the main thread modifies
                                // the data.
                                DataSet ds = null;
                                // The popup != null check is required because a left-click
                                // produces several events as well, which would make this
                                // variable true. Of course we only want the popup to show
                                // if the middle mouse button has been pressed in the first
                                // place
                                boolean mouseNotMoved = oldMousePos != null
                                        && oldMousePos.equals(ms.mousePos);
                                boolean isAtOldPosition = mouseNotMoved && popup != null;
                                boolean middleMouseDown = (ms.modifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0;
                                try {
                                    ds = mv.getCurrentDataSet();
                                    if (ds != null) {
                                        // This is not perfect, if current dataset was changed during execution, the lock would be useless
                                        if(isAtOldPosition && middleMouseDown) {
                                            // Write lock is necessary when selecting in popupCycleSelection
                                            // locks can not be upgraded -> if do read lock here and write lock later (in OsmPrimitive.updateFlags)
                                            // then always occurs deadlock (#5814)
                                            ds.beginUpdate();
                                        } else {
                                            ds.getReadLock().lock();
                                        }
                                    }

                                    // Set the text label in the bottom status bar
                                    // "if mouse moved only" was added to stop heap growing
                                    if (!mouseNotMoved) {
                                        statusBarElementUpdate(ms);
                                    }

                                    // Popup Information
                                    // display them if the middle mouse button is pressed and
                                    // keep them until the mouse is moved
                                    if (middleMouseDown || isAtOldPosition) {
                                        Collection<OsmPrimitive> osms = mv.getAllNearest(ms.mousePos, OsmPrimitive.isUsablePredicate);

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
                                        List<JLabel> lbls = new ArrayList<JLabel>(osms.size());
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
                                    Main.warn(x);
                                } finally {
                                    if (ds != null) {
                                        if(isAtOldPosition && middleMouseDown) {
                                            ds.endUpdate();
                                        } else {
                                            ds.getReadLock().unlock();
                                        }
                                    }
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        // Occurs frequently during JOSM shutdown, log set to debug only
                        Main.debug("InterruptedException in "+MapStatus.class.getSimpleName());
                    } catch (InvocationTargetException e) {
                        Main.warn(e);
                    }
                }
            } finally {
                unregisterListeners();
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
        private Popup popupCreatePopup(Component content, MouseState ms) {
            Point p = mv.getLocationOnScreen();
            Dimension scrn = Toolkit.getDefaultToolkit().getScreenSize();

            // Create a JScrollPane around the content, in case there's not enough space
            JScrollPane sp = GuiHelper.embedInVerticalScrollPane(content);
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
        private void statusBarElementUpdate(MouseState ms) {
            final OsmPrimitive osmNearest = mv.getNearestNodeOrWay(ms.mousePos, OsmPrimitive.isUsablePredicate, false);
            if (osmNearest != null) {
                nameText.setText(osmNearest.getDisplayName(DefaultNameFormatter.getInstance()));
            } else {
                nameText.setText(tr("(no object)"));
            }
        }

        /**
         * Call this with a set of primitives to cycle through them. Method
         * will automatically select the next item and update the map
         * @param osms primitives to cycle through
         * @param mods modifiers (i.e. control keys)
         */
        private void popupCycleSelection(Collection<OsmPrimitive> osms, int mods) {
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
        }

        /**
         * Tries to hide the given popup
         */
        private void popupHidePopup() {
            popupLabels = null;
            if(popup == null)
                return;
            final Popup staticPopup = popup;
            popup = null;
            EventQueue.invokeLater(new Runnable(){
               @Override
               public void run() {
                    staticPopup.hide();
                }});
        }

        /**
         * Tries to show the given popup, can be hidden using {@link #popupHidePopup}
         * If an old popup exists, it will be automatically hidden
         * @param newPopup popup to show
         * @param lbls lables to show (see {@link #popupLabels})
         */
        private void popupShowPopup(Popup newPopup, List<JLabel> lbls) {
            final Popup staticPopup = newPopup;
            if(this.popup != null) {
                // If an old popup exists, remove it when the new popup has been
                // drawn to keep flickering to a minimum
                final Popup staticOldPopup = this.popup;
                EventQueue.invokeLater(new Runnable(){
                    @Override public void run() {
                        staticPopup.show();
                        staticOldPopup.hide();
                    }
                });
            } else {
                // There is no old popup
                EventQueue.invokeLater(new Runnable(){
                     @Override public void run() { staticPopup.show(); }});
            }
            this.popupLabels = lbls;
            this.popup = newPopup;
        }

        /**
         * This method should be called if the selection may have changed from
         * outside of this class. This is the case when CTRL is pressed and the
         * user clicks on the map instead of the popup.
         */
        private void popupUpdateLabels() {
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
        private void popupSetLabelColors(JLabel lbl, OsmPrimitive osm) {
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
         * @return labels for info popup
         */
        private JLabel popupBuildPrimitiveLabels(final OsmPrimitive osm) {
            final StringBuilder text = new StringBuilder();
            String name = osm.getDisplayName(DefaultNameFormatter.getInstance());
            if (osm.isNewOrUndeleted() || osm.isModified()) {
                name = "<i><b>"+ name + "*</b></i>";
            }
            text.append(name);

            boolean idShown = Main.pref.getBoolean("osm-primitives.showid");
            // fix #7557 - do not show ID twice

            if (!osm.isNew() && !idShown) {
                text.append(" [id="+osm.getId()+"]");
            }

            if(osm.getUser() != null) {
                text.append(" [" + tr("User:") + " " + osm.getUser().getName() + "]");
            }

            for (String key : osm.keySet()) {
                text.append("<br>" + key + "=" + osm.get(key));
            }

            final JLabel l = new JLabel(
                    "<html>" +text.toString() + "</html>",
                    ImageProvider.get(osm.getDisplayType()),
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
                 @Override public void mouseMoved(MouseEvent e) {
                    l.setBackground(SystemColor.info);
                    l.setForeground(SystemColor.infoText);
                }
                 @Override public void mouseDragged(MouseEvent e) {
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
    static class MouseState {
        Point mousePos;
        int modifiers;
    }
    /**
     * The last sent mouse movement event.
     */
    MouseState mouseState = new MouseState();

    private AWTEventListener awtListener = new AWTEventListener() {
         @Override
         public void eventDispatched(AWTEvent event) {
            if (event instanceof InputEvent &&
                    ((InputEvent)event).getComponent() == mv) {
                synchronized (collector) {
                    mouseState.modifiers = ((InputEvent)event).getModifiersEx();
                    if (event instanceof MouseEvent) {
                        mouseState.mousePos = ((MouseEvent)event).getPoint();
                    }
                    collector.notify();
                }
            }
        }
    };

    private MouseMotionListener mouseMotionListener = new MouseMotionListener() {
        @Override
        public void mouseMoved(MouseEvent e) {
            synchronized (collector) {
                mouseState.modifiers = e.getModifiersEx();
                mouseState.mousePos = e.getPoint();
                collector.notify();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            mouseMoved(e);
        }
    };

    private KeyAdapter keyAdapter = new KeyAdapter() {
        @Override public void keyPressed(KeyEvent e) {
            synchronized (collector) {
                mouseState.modifiers = e.getModifiersEx();
                collector.notify();
            }
        }

        @Override public void keyReleased(KeyEvent e) {
            keyPressed(e);
        }
    };

    private void registerListeners() {
        // Listen to keyboard/mouse events for pressing/releasing alt key and
        // inform the collector.
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(awtListener,
                    AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        } catch (SecurityException ex) {
            mv.addMouseMotionListener(mouseMotionListener);
            mv.addKeyListener(keyAdapter);
        }
    }

    private void unregisterListeners() {
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener);
        } catch (SecurityException e) {
            // Don't care, awtListener probably wasn't registered anyway
        }
        mv.removeMouseMotionListener(mouseMotionListener);
        mv.removeKeyListener(keyAdapter);
    }


    /**
     * Construct a new MapStatus and attach it to the map view.
     * @param mapFrame The MapFrame the status line is part of.
     */
    public MapStatus(final MapFrame mapFrame) {
        this.mv = mapFrame.mapView;
        this.collector = new Collector(mapFrame);

        // Context menu of status bar
        setComponentPopupMenu(new JPopupMenu() {
            JCheckBoxMenuItem doNotHide = new JCheckBoxMenuItem(new AbstractAction(tr("Do not hide status bar")) {
                @Override public void actionPerformed(ActionEvent e) {
                    boolean sel = ((JCheckBoxMenuItem) e.getSource()).getState();
                    Main.pref.put("statusbar.always-visible", sel);
                }
            });
            JMenuItem jumpButton;
            {
                jumpButton = add(Main.main.menu.jumpToAct);
                addPopupMenuListener(new PopupMenuListener() {
                    @Override
                    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                        Component invoker = ((JPopupMenu)e.getSource()).getInvoker();
                        jumpButton.setVisible(invoker == latText || invoker == lonText);
                        doNotHide.setSelected(Main.pref.getBoolean("statusbar.always-visible", true));
                    }
                    @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
                    @Override public void popupMenuCanceled(PopupMenuEvent e) {}
                });
                add(doNotHide);
            }
        });

        // also show Jump To dialog on mouse click (except context menu)
        MouseListener jumpToOnLeftClick = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON3) {
                    Main.main.menu.jumpToAct.showJumpToDialog();
                }
            }
        };

        // Listen for mouse movements and set the position text field
        mv.addMouseMotionListener(new MouseMotionListener(){
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }
            @Override
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

        latText.setInheritsPopupMenu(true);
        lonText.setInheritsPopupMenu(true);
        headingText.setInheritsPopupMenu(true);
        distText.setInheritsPopupMenu(true);
        nameText.setInheritsPopupMenu(true);

        add(latText, GBC.std());
        add(lonText, GBC.std().insets(3,0,0,0));
        add(headingText, GBC.std().insets(3,0,0,0));
        add(angleText, GBC.std().insets(3,0,0,0));
        add(distText, GBC.std().insets(3,0,0,0));

        distText.addMouseListener(new MouseAdapter() {
            private final List<String> soms = new ArrayList<String>(new TreeSet<String>(NavigatableComponent.SYSTEMS_OF_MEASUREMENT.keySet()));

            @Override
            public void mouseClicked(MouseEvent e) {
                String som = ProjectionPreference.PROP_SYSTEM_OF_MEASUREMENT.get();
                String newsom = soms.get((soms.indexOf(som)+1)%soms.size());
                NavigatableComponent.setSystemOfMeasurement(newsom);
            }
        });

        NavigatableComponent.addSoMChangeListener(somListener = new SoMChangeListener() {
            @Override
            public void systemOfMeasurementChanged(String oldSoM, String newSoM) {
                setDist(distValue);
            }
        });

        latText.addMouseListener(jumpToOnLeftClick);
        lonText.addMouseListener(jumpToOnLeftClick);

        helpText.setEditable(false);
        add(nameText, GBC.std().insets(3,0,0,0));
        add(helpText, GBC.std().insets(3,0,0,0).fill(GBC.HORIZONTAL));

        progressBar.setMaximum(PleaseWaitProgressMonitor.PROGRESS_BAR_MAX);
        progressBar.setVisible(false);
        GBC gbc = GBC.eol();
        gbc.ipadx = 100;
        add(progressBar,gbc);
        progressBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                PleaseWaitProgressMonitor monitor = Main.currentProgressMonitor;
                if (monitor != null) {
                    monitor.showForegroundDialog();
                }
            }
        });

        Main.pref.addPreferenceChangeListener(this);

        // The background thread
        thread = new Thread(collector, "Map Status Collector");
        thread.setDaemon(true);
        thread.start();
    }

    public JPanel getAnglePanel() {
        return angleText;
    }

    @Override
    public String helpTopic() {
        return ht("/Statusline");
    }

    @Override
    public synchronized void addMouseListener(MouseListener ml) {
        //super.addMouseListener(ml);
        lonText.addMouseListener(ml);
        latText.addMouseListener(ml);
    }

    public void setHelpText(String t) {
        setHelpText(null, t);
    }

    public void setHelpText(Object id, final String text)  {

        StatusTextHistory entry = new StatusTextHistory(id, text);

        statusText.remove(entry);
        statusText.add(entry);

        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                helpText.setText(text);
                helpText.setToolTipText(text);
            }
        });
    }

    public void resetHelpText(Object id) {
        if (statusText.isEmpty())
            return;

        StatusTextHistory entry = new StatusTextHistory(id, null);
        if (statusText.get(statusText.size() - 1).equals(entry)) {
            if (statusText.size() == 1) {
                setHelpText("");
            } else {
                StatusTextHistory history = statusText.get(statusText.size() - 2);
                setHelpText(history.id, history.text);
            }
        }
        statusText.remove(entry);
    }

    public void setAngle(double a) {
        angleText.setText(a < 0 ? "--" : Math.round(a*10)/10.0 + " \u00B0");
    }

    public void setHeading(double h) {
        headingText.setText(h < 0 ? "--" : Math.round(h*10)/10.0 + " \u00B0");
    }

    /**
     * Sets the distance text to the given value
     * @param dist The distance value to display, in meters
     */
    public void setDist(double dist) {
        distValue = dist;
        distText.setText(dist < 0 ? "--" : NavigatableComponent.getDistText(dist));
    }

    /**
     * Sets the distance text to the total sum of given ways length
     * @param ways The ways to consider for the total distance
     * @since 5991
     */
    public void setDist(Collection<Way> ways) {
        double dist = -1;
        // Compute total length of selected way(s) until an arbitrary limit set to 250 ways
        // in order to prevent performance issue if a large number of ways are selected (old behaviour kept in that case, see #8403)
        int maxWays = Math.max(1, Main.pref.getInteger("selection.max-ways-for-statusline", 250));
        if (!ways.isEmpty() && ways.size() <= maxWays) {
            dist = 0.0;
            for (Way w : ways) {
                dist += w.getLength();
            }
        }
        setDist(dist);
    }

    /**
     * Activates the angle panel.
     * @param activeFlag {@code true} to activate it, {@code false} to deactivate it
     */
    public void activateAnglePanel(boolean activeFlag) {
        angleEnabled = activeFlag;
        refreshAnglePanel();
    }

    private void refreshAnglePanel() {
        angleText.setBackground(angleEnabled ? PROP_ACTIVE_BACKGROUND_COLOR.get() : PROP_BACKGROUND_COLOR.get());
        angleText.setForeground(angleEnabled ? PROP_ACTIVE_FOREGROUND_COLOR.get() : PROP_FOREGROUND_COLOR.get());
    }

    @Override
    public void destroy() {
        NavigatableComponent.removeSoMChangeListener(somListener);
        Main.pref.removePreferenceChangeListener(this);

        // MapFrame gets destroyed when the last layer is removed, but the status line background
        // thread that collects the information doesn't get destroyed automatically.
        if (thread != null) {
            try {
                thread.interrupt();
            } catch (Exception e) {
                Main.error(e);
            }
        }
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        String key = e.getKey();
        if (key.startsWith("color.")) {
            key = key.substring("color.".length());
            if (PROP_BACKGROUND_COLOR.getKey().equals(key) || PROP_FOREGROUND_COLOR.getKey().equals(key)) {
                for (ImageLabel il : new ImageLabel[]{latText, lonText, headingText, distText, nameText}) {
                    il.setBackground(PROP_BACKGROUND_COLOR.get());
                    il.setForeground(PROP_FOREGROUND_COLOR.get());
                }
                refreshAnglePanel();
            } else if (PROP_ACTIVE_BACKGROUND_COLOR.getKey().equals(key) || PROP_ACTIVE_FOREGROUND_COLOR.getKey().equals(key)) {
                refreshAnglePanel();
            }
        }
    }

    /**
     * Loads all colors from preferences.
     * @since 6789
     */
    public static void getColors() {
        PROP_BACKGROUND_COLOR.get();
        PROP_FOREGROUND_COLOR.get();
        PROP_ACTIVE_BACKGROUND_COLOR.get();
        PROP_ACTIVE_FOREGROUND_COLOR.get();
    }
}
