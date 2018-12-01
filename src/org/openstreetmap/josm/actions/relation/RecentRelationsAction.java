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

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.UndoRedoHandler.CommandQueueListener;
import org.openstreetmap.josm.data.osm.DefaultNameFormatter;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action for accessing recent relations.
 * @since 9668
 */
public class RecentRelationsAction extends JosmAction implements CommandQueueListener {

    private final SideButton editButton;
    private final BasicArrowButton arrow;
    private final Shortcut shortcut;
    private final LaunchEditorAction launchAction;

    /**
     * Constructs a new <code>RecentRelationsAction</code>.
     * @param editButton edit button
     */
    public RecentRelationsAction(SideButton editButton) {
        super(RecentRelationsAction.class.getName(), null, null, null, false, true);
        this.editButton = editButton;
        arrow = editButton.createArrow(this);
        arrow.setToolTipText(tr("List of recent relations"));
        UndoRedoHandler.getInstance().addCommandQueueListener(this);
        enableArrow();
        shortcut = Shortcut.registerShortcut("relationeditor:editrecentrelation",
            tr("Relation Editor: {0}", tr("Open recent relation")), KeyEvent.VK_ESCAPE, Shortcut.SHIFT);
        launchAction = new LaunchEditorAction();
        MainApplication.registerActionShortcut(launchAction, shortcut);
    }

    /**
     * Enables arrow button.
     */
    public void enableArrow() {
        if (arrow != null) {
            arrow.setVisible(getLastRelation() != null);
        }
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
            MainApplication.getLayerManager().getEditDataSet().containsRelation(relation);
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
    protected void updateEnabledState() {
        enableArrow();
    }

    @Override
    public void destroy() {
        MainApplication.unregisterActionShortcut(launchAction, shortcut);
        UndoRedoHandler.getInstance().removeCommandQueueListener(this);
        super.destroy();
    }

    /**
     * Returns the list of recent relations on active layer.
     * @return the list of recent relations on active layer
     */
    public static List<Relation> getRecentRelationsOnActiveLayer() {
        if (!MainApplication.isDisplayingMapView())
            return Collections.emptyList();
        Layer activeLayer = MainApplication.getLayerManager().getActiveLayer();
        if (!(activeLayer instanceof OsmDataLayer)) {
            return Collections.emptyList();
        } else {
            return ((OsmDataLayer) activeLayer).getRecentRelations();
        }
    }

    static class LaunchEditorAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            EditRelationAction.launchEditor(getLastRelation());
        }
    }

    static class RecentRelationsPopupMenu extends JPopupMenu {
        /**
         * Constructs a new {@code RecentRelationsPopupMenu}.
         * @param recentRelations list of recent relations
         * @param keystroke key stroke for the first menu item
         */
        RecentRelationsPopupMenu(List<Relation> recentRelations, KeyStroke keystroke) {
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

        static void launch(Component parent, KeyStroke keystroke) {
            if (parent.isShowing()) {
                Rectangle r = parent.getBounds();
                new RecentRelationsPopupMenu(getRecentRelationsOnActiveLayer(), keystroke).show(parent, r.x, r.y + r.height);
            }
        }
    }

    /**
     * A specialized {@link JMenuItem} for presenting one entry of the relation history
     */
    static class RecentRelationsMenuItem extends JMenuItem implements ActionListener {
        private final transient Relation relation;

        RecentRelationsMenuItem(Relation relation) {
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
