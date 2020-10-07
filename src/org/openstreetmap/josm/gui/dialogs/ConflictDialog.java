// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import javax.swing.AbstractAction;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.actions.AbstractSelectAction;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.conflict.IConflictListener;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.PopupMenuHandler;
import org.openstreetmap.josm.gui.PrimitiveRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.conflict.pair.ConflictResolver;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This dialog displays the {@link ConflictCollection} of the active {@link OsmDataLayer} in a toggle
 * dialog on the right of the main frame.
 * @since 86
 */
public final class ConflictDialog extends ToggleDialog implements ActiveLayerChangeListener, IConflictListener, DataSelectionListener {

    private static final NamedColorProperty CONFLICT_COLOR = new NamedColorProperty(marktr("conflict"), Color.GRAY);
    private static final NamedColorProperty BACKGROUND_COLOR = new NamedColorProperty(marktr("background"), Color.BLACK);

    /** the collection of conflicts displayed by this conflict dialog */
    private transient ConflictCollection conflicts;

    /** the model for the list of conflicts */
    private transient ConflictListModel model;
    /** the list widget for the list of conflicts */
    private JList<OsmPrimitive> lstConflicts;

    private final JPopupMenu popupMenu = new JPopupMenu();
    private final transient PopupMenuHandler popupMenuHandler = new PopupMenuHandler(popupMenu);

    private final ResolveAction actResolve = new ResolveAction();
    private final SelectAction actSelect = new SelectAction();

    /**
     * Constructs a new {@code ConflictDialog}.
     */
    public ConflictDialog() {
        super(tr("Conflict"), "conflict", tr("Resolve conflicts"),
                Shortcut.registerShortcut("subwindow:conflict", tr("Toggle: {0}", tr("Conflict")),
                KeyEvent.VK_C, Shortcut.ALT_SHIFT), 100);

        build();
        refreshView();
    }

    /**
     * Replies the color used to paint conflicts.
     *
     * @return the color used to paint conflicts
     * @see #paintConflicts
     * @since 1221
     */
    public static Color getColor() {
        return CONFLICT_COLOR.get();
    }

    /**
     * builds the GUI
     */
    private void build() {
        synchronized (this) {
            model = new ConflictListModel();

            lstConflicts = new JList<>(model);
            lstConflicts.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            lstConflicts.setCellRenderer(new PrimitiveRenderer());
            lstConflicts.addMouseListener(new MouseEventHandler());
        }
        addListSelectionListener(e -> MainApplication.getMap().mapView.repaint());

        SideButton btnResolve = new SideButton(actResolve);
        addListSelectionListener(actResolve);

        SideButton btnSelect = new SideButton(actSelect);
        addListSelectionListener(actSelect);

        createLayout(lstConflicts, true, Arrays.asList(btnResolve, btnSelect));

        popupMenuHandler.addAction(MainApplication.getMenu().autoScaleActions.get(AutoScaleAction.AutoScaleMode.CONFLICT));

        ResolveToMyVersionAction resolveToMyVersionAction = new ResolveToMyVersionAction();
        ResolveToTheirVersionAction resolveToTheirVersionAction = new ResolveToTheirVersionAction();
        addListSelectionListener(resolveToMyVersionAction);
        addListSelectionListener(resolveToTheirVersionAction);
        JMenuItem btnResolveMy = popupMenuHandler.addAction(resolveToMyVersionAction);
        JMenuItem btnResolveTheir = popupMenuHandler.addAction(resolveToTheirVersionAction);

        popupMenuHandler.addListener(new ResolveButtonsPopupMenuListener(btnResolveTheir, btnResolveMy));
    }

    @Override
    public void showNotify() {
        MainApplication.getLayerManager().addAndFireActiveLayerChangeListener(this);
    }

    @Override
    public void hideNotify() {
        MainApplication.getLayerManager().removeActiveLayerChangeListener(this);
        removeDataLayerListeners(MainApplication.getLayerManager().getEditLayer());
    }

    /**
     * Add a list selection listener to the conflicts list.
     * @param listener the ListSelectionListener
     * @since 5958
     */
    public synchronized void addListSelectionListener(ListSelectionListener listener) {
        lstConflicts.getSelectionModel().addListSelectionListener(listener);
    }

    /**
     * Remove the given list selection listener from the conflicts list.
     * @param listener the ListSelectionListener
     * @since 5958
     */
    public synchronized void removeListSelectionListener(ListSelectionListener listener) {
        lstConflicts.getSelectionModel().removeListSelectionListener(listener);
    }

    /**
     * Replies the popup menu handler.
     * @return The popup menu handler
     * @since 5958
     */
    public PopupMenuHandler getPopupMenuHandler() {
        return popupMenuHandler;
    }

    /**
     * Launches a conflict resolution dialog for the first selected conflict
     */
    private void resolve() {
        synchronized (this) {
            if (conflicts == null || model.getSize() == 0)
                return;

            int index = lstConflicts.getSelectedIndex();
            if (index < 0) {
                index = 0;
            }

            Conflict<? extends OsmPrimitive> c = conflicts.get(index);
            ConflictResolutionDialog dialog = new ConflictResolutionDialog(MainApplication.getMainFrame());
            dialog.getConflictResolver().populate(c);
            dialog.showDialog();

            if (index < conflicts.size() - 1) {
                lstConflicts.setSelectedIndex(index);
            } else {
                lstConflicts.setSelectedIndex(index - 1);
            }
        }
        MainApplication.getMap().mapView.repaint();
    }

    /**
     * refreshes the view of this dialog
     */
    public void refreshView() {
        DataSet editDs = MainApplication.getLayerManager().getEditDataSet();
        synchronized (this) {
            conflicts = editDs == null ? new ConflictCollection() : editDs.getConflicts();
        }
        GuiHelper.runInEDT(() -> {
            model.fireContentChanged();
            updateTitle();
        });
    }

    private synchronized void updateTitle() {
        int conflictsCount = conflicts.size();
        if (conflictsCount > 0) {
            setTitle(trn("Conflict: {0} unresolved", "Conflicts: {0} unresolved", conflictsCount, conflictsCount) +
                    " ("+tr("Rel.:{0} / Ways:{1} / Nodes:{2}",
                            conflicts.getRelationConflicts().size(),
                            conflicts.getWayConflicts().size(),
                            conflicts.getNodeConflicts().size())+')');
        } else {
            setTitle(tr("Conflict"));
        }
    }

    /**
     * Paints all conflicts that can be expressed on the main window.
     *
     * @param g The {@code Graphics} used to paint
     * @param nc The {@code NavigatableComponent} used to get screen coordinates of nodes
     * @since 86
     */
    public void paintConflicts(final Graphics g, final NavigatableComponent nc) {
        Color preferencesColor = getColor();
        if (preferencesColor.equals(BACKGROUND_COLOR.get()))
            return;
        g.setColor(preferencesColor);
        OsmPrimitiveVisitor conflictPainter = new ConflictPainter(nc, g);
        synchronized (this) {
            for (OsmPrimitive o : lstConflicts.getSelectedValuesList()) {
                if (conflicts == null || !conflicts.hasConflictForMy(o)) {
                    continue;
                }
                conflicts.getConflictForMy(o).getTheir().accept(conflictPainter);
            }
        }
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        removeDataLayerListeners(e.getPreviousDataLayer());
        addDataLayerListeners(e.getSource().getActiveDataLayer());
        refreshView();
    }

    private void addDataLayerListeners(OsmDataLayer newLayer) {
        if (newLayer != null) {
            newLayer.getConflicts().addConflictListener(this);
            newLayer.data.addSelectionListener(this);
        }
    }

    private void removeDataLayerListeners(OsmDataLayer oldLayer) {
        if (oldLayer != null) {
            oldLayer.getConflicts().removeConflictListener(this);
            oldLayer.data.removeSelectionListener(this);
        }
    }

    /**
     * replies the conflict collection currently held by this dialog; may be null
     *
     * @return the conflict collection currently held by this dialog; may be null
     */
    public synchronized ConflictCollection getConflicts() {
        return conflicts;
    }

    /**
     * returns the first selected item of the conflicts list
     *
     * @return Conflict
     */
    public synchronized Conflict<? extends OsmPrimitive> getSelectedConflict() {
        if (conflicts == null || model.getSize() == 0)
            return null;

        int index = lstConflicts.getSelectedIndex();

        return index >= 0 && index < conflicts.size() ? conflicts.get(index) : null;
    }

    private synchronized boolean isConflictSelected() {
        final ListSelectionModel selModel = lstConflicts.getSelectionModel();
        return selModel.getMinSelectionIndex() >= 0 && selModel.getMaxSelectionIndex() >= selModel.getMinSelectionIndex();
    }

    @Override
    public void onConflictsAdded(ConflictCollection conflicts) {
        refreshView();
    }

    @Override
    public void onConflictsRemoved(ConflictCollection conflicts) {
        Logging.debug("1 conflict has been resolved.");
        refreshView();
    }

    @Override
    public synchronized void selectionChanged(SelectionChangeEvent event) {
        lstConflicts.setValueIsAdjusting(true);
        lstConflicts.clearSelection();
        for (OsmPrimitive osm : event.getSelection()) {
            if (conflicts != null && conflicts.hasConflictForMy(osm)) {
                int pos = model.indexOf(osm);
                if (pos >= 0) {
                    lstConflicts.addSelectionInterval(pos, pos);
                }
            }
        }
        lstConflicts.setValueIsAdjusting(false);
    }

    @Override
    public String helpTopic() {
        return ht("/Dialog/ConflictList");
    }

    static final class ResolveButtonsPopupMenuListener implements PopupMenuListener {
        private final JMenuItem btnResolveTheir;
        private final JMenuItem btnResolveMy;

        ResolveButtonsPopupMenuListener(JMenuItem btnResolveTheir, JMenuItem btnResolveMy) {
            this.btnResolveTheir = btnResolveTheir;
            this.btnResolveMy = btnResolveMy;
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            btnResolveMy.setVisible(ExpertToggleAction.isExpert());
            btnResolveTheir.setVisible(ExpertToggleAction.isExpert());
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            // Do nothing
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            // Do nothing
        }
    }

    class MouseEventHandler extends PopupMenuLauncher {
        /**
         * Constructs a new {@code MouseEventHandler}.
         */
        MouseEventHandler() {
            super(popupMenu);
        }

        @Override public void mouseClicked(MouseEvent e) {
            if (isDoubleClick(e)) {
                resolve();
            }
        }
    }

    /**
     * The {@link ListModel} for conflicts
     *
     */
    class ConflictListModel implements ListModel<OsmPrimitive> {

        private final CopyOnWriteArrayList<ListDataListener> listeners;

        /**
         * Constructs a new {@code ConflictListModel}.
         */
        ConflictListModel() {
            listeners = new CopyOnWriteArrayList<>();
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            if (l != null) {
                listeners.addIfAbsent(l);
            }
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }

        protected void fireContentChanged() {
            ListDataEvent evt = new ListDataEvent(
                    this,
                    ListDataEvent.CONTENTS_CHANGED,
                    0,
                    getSize()
            );
            for (ListDataListener listener : listeners) {
                listener.contentsChanged(evt);
            }
        }

        @Override
        public synchronized OsmPrimitive getElementAt(int index) {
            if (index < 0 || index >= getSize())
                return null;
            return conflicts.get(index).getMy();
        }

        @Override
        public synchronized int getSize() {
            return conflicts != null ? conflicts.size() : 0;
        }

        public synchronized int indexOf(OsmPrimitive my) {
            if (conflicts != null) {
                return IntStream.range(0, conflicts.size())
                        .filter(i -> conflicts.get(i).isMatchingMy(my))
                        .findFirst().orElse(-1);
            }
            return -1;
        }

        public synchronized OsmPrimitive get(int idx) {
            return conflicts != null ? conflicts.get(idx).getMy() : null;
        }
    }

    class ResolveAction extends AbstractAction implements ListSelectionListener {
        ResolveAction() {
            putValue(NAME, tr("Resolve"));
            putValue(SHORT_DESCRIPTION, tr("Open a merge dialog of all selected items in the list above."));
            new ImageProvider("dialogs", "conflict").getResource().attachImageIcon(this, true);
            putValue("help", ht("/Dialog/ConflictList#ResolveAction"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            resolve();
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(isConflictSelected());
        }
    }

    final class SelectAction extends AbstractSelectAction implements ListSelectionListener {
        private SelectAction() {
            putValue("help", ht("/Dialog/ConflictList#SelectAction"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Collection<OsmPrimitive> sel = new LinkedList<>();
            synchronized (this) {
                sel.addAll(lstConflicts.getSelectedValuesList());
            }
            DataSet ds = MainApplication.getLayerManager().getEditDataSet();
            if (ds != null) { // Can't see how it is possible but it happened in #7942
                ds.setSelected(sel);
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(isConflictSelected());
        }
    }

    abstract class ResolveToAction extends ResolveAction {
        private final String name;
        private final MergeDecisionType type;

        ResolveToAction(String name, String description, MergeDecisionType type) {
            this.name = name;
            this.type = type;
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, description);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final ConflictResolver resolver = new ConflictResolver();
            final List<Command> commands = new ArrayList<>();
            synchronized (this) {
                for (OsmPrimitive osmPrimitive : lstConflicts.getSelectedValuesList()) {
                    Conflict<? extends OsmPrimitive> c = conflicts.getConflictForMy(osmPrimitive);
                    if (c != null) {
                        resolver.populate(c);
                        resolver.decideRemaining(type);
                        commands.add(resolver.buildResolveCommand());
                    }
                }
            }
            UndoRedoHandler.getInstance().add(new SequenceCommand(name, commands));
            refreshView();
        }
    }

    class ResolveToMyVersionAction extends ResolveToAction {
        ResolveToMyVersionAction() {
            super(tr("Resolve to my versions"), tr("Resolves all unresolved conflicts to ''my'' version"),
                    MergeDecisionType.KEEP_MINE);
        }
    }

    class ResolveToTheirVersionAction extends ResolveToAction {
        ResolveToTheirVersionAction() {
            super(tr("Resolve to their versions"), tr("Resolves all unresolved conflicts to ''their'' version"),
                    MergeDecisionType.KEEP_THEIR);
        }
    }

    /**
     * Paints conflicts.
     */
    public static class ConflictPainter implements OsmPrimitiveVisitor {
        // Manage a stack of visited relations to avoid infinite recursion with cyclic relations (fix #7938)
        private final Set<Relation> visited = new HashSet<>();
        private final NavigatableComponent nc;
        private final Graphics g;

        ConflictPainter(NavigatableComponent nc, Graphics g) {
            this.nc = nc;
            this.g = g;
        }

        @Override
        public void visit(Node n) {
            Point p = nc.getPoint(n);
            g.drawRect(p.x-1, p.y-1, 2, 2);
        }

        private void visit(Node n1, Node n2) {
            Point p1 = nc.getPoint(n1);
            Point p2 = nc.getPoint(n2);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        @Override
        public void visit(Way w) {
            Node lastN = null;
            for (Node n : w.getNodes()) {
                if (lastN == null) {
                    lastN = n;
                    continue;
                }
                visit(lastN, n);
                lastN = n;
            }
        }

        @Override
        public void visit(Relation e) {
            if (!visited.contains(e)) {
                visited.add(e);
                try {
                    for (RelationMember em : e.getMembers()) {
                        em.getMember().accept(this);
                    }
                } finally {
                    visited.remove(e);
                }
            }
        }
    }

    /**
     * Warns the user about the number of detected conflicts
     *
     * @param numNewConflicts the number of detected conflicts
     * @since 5775
     */
    public void warnNumNewConflicts(int numNewConflicts) {
        if (numNewConflicts == 0)
            return;

        String msg1 = trn(
                "There was {0} conflict detected.",
                "There were {0} conflicts detected.",
                numNewConflicts,
                numNewConflicts
        );

        final StringBuilder sb = new StringBuilder();
        sb.append("<html>").append(msg1).append("</html>");
        if (numNewConflicts > 0) {
            final ButtonSpec[] options = {
                    new ButtonSpec(
                            tr("OK"),
                            new ImageProvider("ok"),
                            tr("Click to close this dialog and continue editing"),
                            null /* no specific help */
                    )
            };
            GuiHelper.runInEDT(() -> {
                HelpAwareOptionPane.showOptionDialog(
                        MainApplication.getMainFrame(),
                        sb.toString(),
                        tr("Conflicts detected"),
                        JOptionPane.WARNING_MESSAGE,
                        null, /* no icon */
                        options,
                        options[0],
                        ht("/Concepts/Conflict#WarningAboutDetectedConflicts")
                );
                unfurlDialog();
                MainApplication.getMap().repaint();
            });
        }
    }
}
