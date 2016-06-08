// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.plaf.basic.BasicArrowButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action for accessing recent relations.
 */
public class RecentRelationsAction implements ActionListener, CommandQueueListener, LayerChangeListener, ActiveLayerChangeListener {

    private final SideButton editButton;
    private final BasicArrowButton arrow;
    private final Shortcut shortcut;

    /**
     * Constructs a new <code>RecentRelationsAction</code>.
     * @param editButton edit button
     */
    public RecentRelationsAction(SideButton editButton) {
        this.editButton = editButton;
        arrow = editButton.createArrow(this);
        arrow.setToolTipText(tr("List of recent relations"));
        Main.main.undoRedo.addCommandQueueListener(this);
        Main.getLayerManager().addLayerChangeListener(this);
        Main.getLayerManager().addActiveLayerChangeListener(this);
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

    /**
     * Enables arrow button.
     */
    public void enableArrow() {
        arrow.setVisible(getLastRelation() != null);
    }

    /**
     * Returns the last relation.
     * @return the last relation
     */
    public static Relation getLastRelation() {
        List<Relation> recentRelations = getRecentRelationsOnActiveLayer();
        if (recentRelations == null || recentRelations.isEmpty())
            return null;
        for (Relation relation: recentRelations) {
            if (!isRelationListable(relation))
                continue;
            return relation;
        }
        return null;
    }

    /**
     * Determines if the given relation is listable in last relations.
     * @param relation relation
     * @return {@code true} if relation is non null, not deleted, and in current dataset
     */
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
    public void layerAdded(LayerAddEvent e) {
        enableArrow();
    }

    @Override
    public void layerRemoving(LayerRemoveEvent e) {
        enableArrow();
    }

    @Override
    public void layerOrderChanged(LayerOrderChangeEvent e) {
        enableArrow();
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        enableArrow();
    }

    /**
     * Returns the list of recent relations on active layer.
     * @return the list of recent relations on active layer
     */
    public static List<Relation> getRecentRelationsOnActiveLayer() {
        if (!Main.isDisplayingMapView())
            return Collections.emptyList();
        Layer activeLayer = Main.main.getActiveLayer();
        if (!(activeLayer instanceof OsmDataLayer)) {
            return Collections.emptyList();
        } else {
            return ((OsmDataLayer) activeLayer).getRecentRelations();
        }
    }

    protected static class RecentRelationsPopupMenu extends JPopupMenu {
        /**
         * Constructs a new {@code RecentRelationsPopupMenu}.
         * @param recentRelations list of recent relations
         * @param keystroke key stroke for the first menu item
         */
        public RecentRelationsPopupMenu(List<Relation> recentRelations, KeyStroke keystroke) {
            boolean first = true;
            for (Relation relation: recentRelations) {
                if (!isRelationListable(relation))
                    continue;
                JMenuItem menuItem = new RecentRelationsMenuItem(relation);
                if (first) {
                    menuItem.setAccelerator(keystroke);
                    first = false;
                }
                menuItem.setIcon(ImageProvider.getPadded(relation, ImageProvider.ImageSizes.MENU.getImageDimension()));
                add(menuItem);
            }
        }

        protected static void launch(Component parent, KeyStroke keystroke) {
            Rectangle r = parent.getBounds();
            new RecentRelationsPopupMenu(getRecentRelationsOnActiveLayer(), keystroke).show(parent, r.x, r.y + r.height);
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
