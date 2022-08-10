// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.actions.SplitWayAction.SplitWayActionConcrete;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.CachingProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MenuScroller;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.util.HighlightHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Map mode for splitting ways.
 * 
 * @since xxx
 */
public class SplitMode extends MapMode {

    /** Prioritized selected ways over others when splitting */
    static final CachingProperty<Boolean> PREFER_SELECTED_WAYS
            = new BooleanProperty("split-mode.prefer-selected-ways", true).cached();

    /** Don't consider disabled ways */
    static final CachingProperty<Boolean> IGNORE_DISABLED_WAYS
            = new BooleanProperty("split-mode.ignore-disabled-ways", true).cached();

    /** Helper to keep track of highlighted primitives */
    HighlightHelper highlight = new HighlightHelper();

    /**
     * Construct a new SplitMode object
     */
    public SplitMode() {
        super(tr("Split mode"), "splitway", tr("Split ways"),
        Shortcut.registerShortcut("mapmode:split", tr("Mode: {0}", tr("Split mode")), KeyEvent.VK_T, Shortcut.DIRECT),
        ImageProvider.getCursor("crosshair", null));
    }
    
    @Override
    public void enterMode() {
        super.enterMode();
        MapView mv = MainApplication.getMap().mapView;
        mv.addMouseListener(this);
        mv.addMouseMotionListener(this);
    }

    @Override
    public void exitMode() {
        super.exitMode();
        MapView mv = MainApplication.getMap().mapView;
        mv.removeMouseMotionListener(this);
        mv.removeMouseListener(this);
        removeHighlighting();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);

        MapView mv = MainApplication.getMap().mapView;
        int mouseDownButton = e.getButton();
        Point mousePos = e.getPoint();

        // return early
        if (!mv.isActiveLayerVisible() || Boolean.FALSE.equals(this.getValue("active")) || mouseDownButton != MouseEvent.BUTTON1)
            return;

        // update which modifiers are pressed (shift, alt, ctrl)
        updateKeyModifiers(e);

        DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null)
            return;

        final List<Way> selectedWays = new ArrayList<>(ds.getSelectedWays());
        Optional<OsmPrimitive> primitiveAtPoint = getPrimitiveAtPoint(e.getPoint());
        if (!primitiveAtPoint.isPresent())
            return;
            
        final OsmPrimitive nearestPrimitive = primitiveAtPoint.get();
        
        if (nearestPrimitive instanceof Node) {
            // Split way at node
            Node n = (Node) nearestPrimitive;

            List<Way> applicableWays = getApplicableWays(n, selectedWays);

            if (applicableWays.isEmpty()) {
                new Notification(
                        tr("The selected node is not in the middle of any non-closed way."))
                        .setIcon(JOptionPane.WARNING_MESSAGE)
                        .show();
                return;
            }

            if (applicableWays.size() > 1) {
                createPopup(n, applicableWays).show(mv, mousePos.x, mousePos.y);
                return;
            } else {
                final Way splitWay = applicableWays.get(0);
                SplitWayAction.doSplitWayShowSegmentSelection(splitWay, Collections.singletonList(n), null);
                if (updateUserFeedback(e)) {
                    MainApplication.getMap().mapView.repaint();
                }
            }
        } else if (nearestPrimitive instanceof Way) {
            // TODO: Implement way splitting when a point on a way segment (not a node) is selected
            // Insert node into way and split

            new Notification(
                    tr("Splitting in the middle of way segments is not yet implemented."))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .show();
            return;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (updateUserFeedback(e)) {
            MainApplication.getMap().mapView.repaint();
        }
    }

    private Optional<OsmPrimitive> getPrimitiveAtPoint(Point p) {
        MapView mv = MainApplication.getMap().mapView;
        return Optional.ofNullable(mv.getNearestNodeOrWay(p, mv.isSelectablePredicate, true));
    }

    /**
     * Get a list of potential ways to be split for a given node
     * @param n The node at which ways should be split
     * @param preferredWays List of ways that should be prioritized over others. 
     * If one or more potential preferred ways are found, other ways are disregarded.
     * @return List of potential ways to be split
     */
    private List<Way> getApplicableWays(Node n, Collection<Way> preferredWays) {
        final List<Way> parentWays = n.getParentWays();
        List<Way> applicableWays = parentWays.stream()
            .filter(w -> w.isDrawable() &&
                         !(w.isDisabled() && IGNORE_DISABLED_WAYS.get()) &&
                         !w.isClosed() &&
                         w.isInnerNode(n))
            .collect(Collectors.toList());

        if (PREFER_SELECTED_WAYS.get() && preferredWays != null) {
            List<Way> preferredApplicableWays = applicableWays.stream()
                .filter(w -> preferredWays.contains(w)).collect(Collectors.toList());

            if (!preferredApplicableWays.isEmpty()) {
                applicableWays = preferredApplicableWays;
            }
        }

        return applicableWays;
    }

    /**
     * Create a new split way selection popup
     * @param n Node at which ways should be split
     * @param applicableWays Potential split ways to select from
     * @return A new popup object
     */
    private JPopupMenu createPopup(Node n, Collection<Way> applicableWays) {
        JPopupMenu pm = new JPopupMenu("<html>" + tr("Select way to split.<br>" +
            "Hold CTRL for multiple selection.") + "</html>");
            
        Border titleUnderline = BorderFactory.createMatteBorder(1, 0, 0, 0, pm.getForeground());
        TitledBorder labelBorder = BorderFactory.createTitledBorder(titleUnderline, pm.getLabel(),
            TitledBorder.CENTER, TitledBorder.ABOVE_TOP, pm.getFont(), pm.getForeground());
        pm.setBorder(BorderFactory.createCompoundBorder(pm.getBorder(), labelBorder));

        for (final Way w : applicableWays) {
            JMenuItem mi = new JMenuItem(new SplitWayActionConcrete(w, Collections.singletonList(n), null));

            mi.setText("<html>" + createLabelText(w) + "</html>");

            addHoverHighlightListener(mi, Arrays.asList(n, w));

            mi.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (highlight.highlightOnly(Arrays.asList(n, w))) {
                        MainApplication.getMap().mapView.repaint();
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (removeHighlighting()) {
                        MainApplication.getMap().mapView.repaint();
                    }
                }
            });

            mi.addActionListener(actionEvent -> {
                removeHighlighting();
                // Prevent popup menu from closing when ctrl is pressed while selecting a way to split
                updateKeyModifiers(actionEvent);
                if (platformMenuShortcutKeyMask) {
                    JMenuItem source = (JMenuItem) actionEvent.getSource();
                    JPopupMenu popup = (JPopupMenu) source.getParent();
                    popup.remove(source);

                    // Close popup menu anyway when there are no more options left
                    if (popup.getSubElements().length > 0) {
                        popup.setVisible(true);                                    
                    }
                }
            });

            pm.add(mi);
        }
        
        MenuScroller.setScrollerFor(pm);
        return pm;
    }

    /**
     * Determine objects to highlight and update highlight
     * @param e {@link MouseEvent} that triggered the update
     * @return true if repaint is required
     */
    private boolean updateUserFeedback(MouseEvent e) {
        List<OsmPrimitive> toHighlight = new ArrayList<>(2);

        Optional<OsmPrimitive> pHovered = getPrimitiveAtPoint(e.getPoint());
        DataSet ds = getLayerManager().getEditDataSet();

        if (pHovered.filter(p -> p instanceof Node).isPresent()) {
            Node nHovered = (Node) pHovered.get();
            final List<Way> selectedWays = ds != null ? new ArrayList<>(ds.getSelectedWays()) : null;
            List<Way> applicableWays = getApplicableWays(nHovered, selectedWays);
            if (!applicableWays.isEmpty()) {
                pHovered.ifPresent(toHighlight::add);
            }
            if (applicableWays.size() == 1) {
                toHighlight.add(applicableWays.get(0));
            }
        }

        return highlight.highlightOnly(toHighlight);
    }

    /**
     * Removes all existing highlights.
     * @return true if a repaint is required
     */
    private boolean removeHighlighting() {
        boolean anyHighlighted = highlight.anyHighlighted();
        highlight.clear();
        return anyHighlighted;
    }

    /**
     * Add a mouse listener to the component {@code c} which highlights {@code prims} 
     * when the mouse pointer is hovering over the component
     * @param c The component to add the hover mouse listener to
     * @param prims The primitives to highlight when the component is hovered
     */
    private void addHoverHighlightListener(Component c, Collection<OsmPrimitive> prims) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (highlight.highlightOnly(prims)) {
                    MainApplication.getMap().mapView.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (removeHighlighting()) {
                    MainApplication.getMap().mapView.repaint();
                }
            }
        });
    }

    /**
     * Create the text for a {@link OsmPrimitive} label, including its keys
     * @param primitive The {@link OsmPrimitive} to describe
     * @return Text describing the {@link OsmPrimitive}
     */
    private static String createLabelText(OsmPrimitive primitive) {
        return createLabelText(primitive, true);
    }

    /**
     * Create the text for a {@link OsmPrimitive} label
     * @param primitive The {@link OsmPrimitive} to describe
     * @param includeKeys Include keys in description
     * @return Text describing the {@link OsmPrimitive}
     */
    private static String createLabelText(OsmPrimitive primitive, boolean includeKeys) {
        final StringBuilder text = new StringBuilder(32);
        String name = Utils.escapeReservedCharactersHTML(primitive.getDisplayName(DefaultNameFormatter.getInstance()));
        if (primitive.isNewOrUndeleted() || primitive.isModified()) {
            name = "<i><b>"+ name + "*</b></i>";
        }
        text.append(name);

        if (!primitive.isNew()) {
            text.append(" [id=").append(primitive.getId()).append(']');
        }

        if (primitive.getUser() != null) {
            text.append(" [").append(tr("User:")).append(' ')
                .append(Utils.escapeReservedCharactersHTML(primitive.getUser().getName())).append(']');
        }

        if (includeKeys) {
            primitive.visitKeys((p, key, value) -> text.append("<br>").append(key).append('=').append(value));
        }

        return text.toString();
    }
}
