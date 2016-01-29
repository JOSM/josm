// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.plaf.basic.BasicArrowButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action for accessing recent relations.
 */
public class RecentRelationsAction implements ActionListener, CommandQueueListener, LayerChangeListener{

    private final SideButton editButton;
    private final BasicArrowButton arrow;
    private final Shortcut shortcut;

    /**
     * Constructs a new <code>RecentRelationsAction</code>.
     */
    public RecentRelationsAction(SideButton editButton) {
        this.editButton = editButton;
        arrow = editButton.createArrow(this);
        arrow.setToolTipText(tr("List of recent relations"));
        Main.main.undoRedo.addCommandQueueListener(this);
        MapView.addLayerChangeListener(this);
        enableArrow();
        shortcut = Shortcut.registerShortcut(
            "relationeditor:editrecentrelation",
            tr("Relation Editor: {0}", tr("Open recent relation")),
            KeyEvent.VK_ESCAPE,
            Shortcut.SHIFT
        );
        Main.registerActionShortcut(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EditRelationAction.launchEditor(getLastRelation());
            }
        }, shortcut);
    }

    public void enableArrow() {
        arrow.setEnabled(getLastRelation() != null);
    }

    public static Relation getLastRelation() {
        List<Relation> recentRelations = getRecentRelationsOnActiveLayer();
        if (recentRelations == null || recentRelations.isEmpty()) return null;
        for (Relation relation: recentRelations) {
            if (!isRelationListable(relation)) continue;
            return relation;
        }
        return null;
    }

    public static boolean isRelationListable(Relation relation) {
        return relation != null &&
            !relation.isDeleted() &&
            Main.main.getCurrentDataSet().containsRelation(relation);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        RecentRelationsPopupMenu.launch(editButton, shortcut.getKeyStroke());
    }

    @Override
    public void commandChanged(int queueSize, int redoSize) {
        enableArrow();
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        enableArrow();
    }

    @Override
    public void layerAdded(Layer newLayer) {
        enableArrow();
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        enableArrow();
    }

    public static List<Relation> getRecentRelationsOnActiveLayer() {
        if (Main.map == null || Main.map.mapView == null) return null;
        Layer activeLayer = Main.map.mapView.getActiveLayer();
        if (!(activeLayer instanceof OsmDataLayer)) {
            return null;
        } else {
            return ((OsmDataLayer) activeLayer).getRecentRelations();
        }
    }

    protected static class RecentRelationsPopupMenu extends JPopupMenu {
        public static void launch(Component parent, KeyStroke keystroke) {
            List<Relation> recentRelations = getRecentRelationsOnActiveLayer();
            JPopupMenu menu = new RecentRelationsPopupMenu(recentRelations, keystroke);
            Rectangle r = parent.getBounds();
            menu.show(parent, r.x, r.y + r.height);
        }

        /**
         * Constructs a new {@code SearchPopupMenu}.
         */
        public RecentRelationsPopupMenu(List<Relation> recentRelations, KeyStroke keystroke) {
            boolean first = true;
            for (Relation relation: recentRelations) {
                if (!isRelationListable(relation)) continue;
                JMenuItem menuItem = new RecentRelationsMenuItem(relation);
                if (first) {
                    menuItem.setAccelerator(keystroke);
                    first = false;
                }
                add(menuItem);
            }
        }
    }

    /**
     * A specialized {@link JMenuItem} for presenting one entry of the relation history
     */
    protected static class RecentRelationsMenuItem extends JMenuItem implements ActionListener {
        protected final transient Relation relation;

        public RecentRelationsMenuItem(Relation relation) {
            super(relation.getDisplayName(DefaultNameFormatter.getInstance()));
            this.relation = relation;
            addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            EditRelationAction.launchEditor(relation);
        }
    }
}
