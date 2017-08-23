// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Dialog displaying list of all executed commands (undo/redo buffer).
 * @since 94
 */
public class CommandStackDialog extends ToggleDialog implements CommandQueueListener {

    private final DefaultTreeModel undoTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    private final DefaultTreeModel redoTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());

    private final JTree undoTree = new JTree(undoTreeModel);
    private final JTree redoTree = new JTree(redoTreeModel);

    private final transient UndoRedoSelectionListener undoSelectionListener;
    private final transient UndoRedoSelectionListener redoSelectionListener;

    private final JScrollPane scrollPane;
    private final JSeparator separator = new JSeparator();
    // only visible, if separator is the top most component
    private final Component spacer = Box.createRigidArea(new Dimension(0, 3));

    // last operation is remembered to select the next undo/redo entry in the list
    // after undo/redo command
    private UndoRedoType lastOperation = UndoRedoType.UNDO;

    // Actions for context menu and Enter key
    private final SelectAction selectAction = new SelectAction();
    private final SelectAndZoomAction selectAndZoomAction = new SelectAndZoomAction();

    /**
     * Constructs a new {@code CommandStackDialog}.
     */
    public CommandStackDialog() {
        super(tr("Command Stack"), "commandstack", tr("Open a list of all commands (undo buffer)."),
                Shortcut.registerShortcut("subwindow:commandstack", tr("Toggle: {0}",
                tr("Command Stack")), KeyEvent.VK_O, Shortcut.ALT_SHIFT), 100);
        undoTree.addMouseListener(new MouseEventHandler());
        undoTree.setRootVisible(false);
        undoTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        undoTree.setShowsRootHandles(true);
        undoTree.expandRow(0);
        undoTree.setCellRenderer(new CommandCellRenderer());
        undoSelectionListener = new UndoRedoSelectionListener(undoTree);
        undoTree.getSelectionModel().addTreeSelectionListener(undoSelectionListener);
        InputMapUtils.unassignCtrlShiftUpDown(undoTree, JComponent.WHEN_FOCUSED);

        redoTree.addMouseListener(new MouseEventHandler());
        redoTree.setRootVisible(false);
        redoTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        redoTree.setShowsRootHandles(true);
        redoTree.expandRow(0);
        redoTree.setCellRenderer(new CommandCellRenderer());
        redoSelectionListener = new UndoRedoSelectionListener(redoTree);
        redoTree.getSelectionModel().addTreeSelectionListener(redoSelectionListener);
        InputMapUtils.unassignCtrlShiftUpDown(redoTree, JComponent.WHEN_FOCUSED);

        JPanel treesPanel = new JPanel(new GridBagLayout());

        treesPanel.add(spacer, GBC.eol());
        spacer.setVisible(false);
        treesPanel.add(undoTree, GBC.eol().fill(GBC.HORIZONTAL));
        separator.setVisible(false);
        treesPanel.add(separator, GBC.eol().fill(GBC.HORIZONTAL));
        treesPanel.add(redoTree, GBC.eol().fill(GBC.HORIZONTAL));
        treesPanel.add(Box.createRigidArea(new Dimension(0, 0)), GBC.std().weight(0, 1));
        treesPanel.setBackground(redoTree.getBackground());

        wireUpdateEnabledStateUpdater(selectAction, undoTree);
        wireUpdateEnabledStateUpdater(selectAction, redoTree);

        UndoRedoAction undoAction = new UndoRedoAction(UndoRedoType.UNDO);
        wireUpdateEnabledStateUpdater(undoAction, undoTree);

        UndoRedoAction redoAction = new UndoRedoAction(UndoRedoType.REDO);
        wireUpdateEnabledStateUpdater(redoAction, redoTree);

        scrollPane = (JScrollPane) createLayout(treesPanel, true, Arrays.asList(
            new SideButton(selectAction),
            new SideButton(undoAction),
            new SideButton(redoAction)
        ));

        InputMapUtils.addEnterAction(undoTree, selectAndZoomAction);
        InputMapUtils.addEnterAction(redoTree, selectAndZoomAction);
    }

    private static class CommandCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,
                boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode v = (DefaultMutableTreeNode) value;
            if (v.getUserObject() instanceof JLabel) {
                JLabel l = (JLabel) v.getUserObject();
                setIcon(l.getIcon());
                setText(l.getText());
            }
            return this;
        }
    }

    private void updateTitle() {
        int undo = undoTreeModel.getChildCount(undoTreeModel.getRoot());
        int redo = redoTreeModel.getChildCount(redoTreeModel.getRoot());
        if (undo > 0 || redo > 0) {
            setTitle(tr("Command Stack: Undo: {0} / Redo: {1}", undo, redo));
        } else {
            setTitle(tr("Command Stack"));
        }
    }

    /**
     * Selection listener for undo and redo area.
     * If one is clicked, takes away the selection from the other, so
     * it behaves as if it was one component.
     */
    private class UndoRedoSelectionListener implements TreeSelectionListener {
        private final JTree source;

        UndoRedoSelectionListener(JTree source) {
            this.source = source;
        }

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if (source == undoTree) {
                redoTree.getSelectionModel().removeTreeSelectionListener(redoSelectionListener);
                redoTree.clearSelection();
                redoTree.getSelectionModel().addTreeSelectionListener(redoSelectionListener);
            }
            if (source == redoTree) {
                undoTree.getSelectionModel().removeTreeSelectionListener(undoSelectionListener);
                undoTree.clearSelection();
                undoTree.getSelectionModel().addTreeSelectionListener(undoSelectionListener);
            }
        }
    }

    /**
     * Wires updater for enabled state to the events. Also updates dialog title if needed.
     * @param updater updater
     * @param tree tree on which wire updater
     */
    protected void wireUpdateEnabledStateUpdater(final IEnabledStateUpdating updater, JTree tree) {
        addShowNotifyListener(updater);

        tree.addTreeSelectionListener(e -> updater.updateEnabledState());

        tree.getModel().addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                updater.updateEnabledState();
                updateTitle();
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                updater.updateEnabledState();
                updateTitle();
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                updater.updateEnabledState();
                updateTitle();
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                updater.updateEnabledState();
                updateTitle();
            }
        });
    }

    @Override
    public void showNotify() {
        buildTrees();
        for (IEnabledStateUpdating listener : showNotifyListener) {
            listener.updateEnabledState();
        }
        Main.main.undoRedo.addCommandQueueListener(this);
    }

    /**
     * Simple listener setup to update the button enabled state when the side dialog shows.
     */
    private final transient Set<IEnabledStateUpdating> showNotifyListener = new LinkedHashSet<>();

    private void addShowNotifyListener(IEnabledStateUpdating listener) {
        showNotifyListener.add(listener);
    }

    @Override
    public void hideNotify() {
        undoTreeModel.setRoot(new DefaultMutableTreeNode());
        redoTreeModel.setRoot(new DefaultMutableTreeNode());
        Main.main.undoRedo.removeCommandQueueListener(this);
    }

    /**
     * Build the trees of undo and redo commands (initially or when
     * they have changed).
     */
    private void buildTrees() {
        setTitle(tr("Command Stack"));
        if (Main.getLayerManager().getEditLayer() == null)
            return;

        List<Command> undoCommands = Main.main.undoRedo.commands;
        DefaultMutableTreeNode undoRoot = new DefaultMutableTreeNode();
        for (int i = 0; i < undoCommands.size(); ++i) {
            undoRoot.add(getNodeForCommand(undoCommands.get(i), i));
        }
        undoTreeModel.setRoot(undoRoot);

        List<Command> redoCommands = Main.main.undoRedo.redoCommands;
        DefaultMutableTreeNode redoRoot = new DefaultMutableTreeNode();
        for (int i = 0; i < redoCommands.size(); ++i) {
            redoRoot.add(getNodeForCommand(redoCommands.get(i), i));
        }
        redoTreeModel.setRoot(redoRoot);
        if (redoTreeModel.getChildCount(redoRoot) > 0) {
            redoTree.scrollRowToVisible(0);
            scrollPane.getHorizontalScrollBar().setValue(0);
        }

        separator.setVisible(!undoCommands.isEmpty() || !redoCommands.isEmpty());
        spacer.setVisible(undoCommands.isEmpty() && !redoCommands.isEmpty());

        // if one tree is empty, move selection to the other
        switch (lastOperation) {
        case UNDO:
            if (undoCommands.isEmpty()) {
                lastOperation = UndoRedoType.REDO;
            }
            break;
        case REDO:
            if (redoCommands.isEmpty()) {
                lastOperation = UndoRedoType.UNDO;
            }
            break;
        }

        // select the next command to undo/redo
        switch (lastOperation) {
        case UNDO:
            undoTree.setSelectionRow(undoTree.getRowCount()-1);
            break;
        case REDO:
            redoTree.setSelectionRow(0);
            break;
        }

        undoTree.scrollRowToVisible(undoTreeModel.getChildCount(undoRoot)-1);
        scrollPane.getHorizontalScrollBar().setValue(0);
    }

    /**
     * Wraps a command in a CommandListMutableTreeNode.
     * Recursively adds child commands.
     * @param c the command
     * @param idx index
     * @return the resulting node
     */
    protected CommandListMutableTreeNode getNodeForCommand(PseudoCommand c, int idx) {
        CommandListMutableTreeNode node = new CommandListMutableTreeNode(c, idx);
        if (c.getChildren() != null) {
            List<PseudoCommand> children = new ArrayList<>(c.getChildren());
            for (int i = 0; i < children.size(); ++i) {
                node.add(getNodeForCommand(children.get(i), i));
            }
        }
        return node;
    }

    /**
     * Return primitives that are affected by some command
     * @param path GUI elements
     * @return collection of affected primitives, onluy usable ones
     */
    protected static Collection<? extends OsmPrimitive> getAffectedPrimitives(TreePath path) {
        PseudoCommand c = ((CommandListMutableTreeNode) path.getLastPathComponent()).getCommand();
        final OsmDataLayer currentLayer = Main.getLayerManager().getEditLayer();
        return new SubclassFilteredCollection<>(
                c.getParticipatingPrimitives(),
                o -> {
                    OsmPrimitive p = currentLayer.data.getPrimitiveById(o);
                    return p != null && p.isUsable();
                }
        );
    }

    @Override
    public void commandChanged(int queueSize, int redoSize) {
        if (!isVisible())
            return;
        buildTrees();
    }

    /**
     * Action that selects the objects that take part in a command.
     */
    public class SelectAction extends AbstractAction implements IEnabledStateUpdating {

        /**
         * Constructs a new {@code SelectAction}.
         */
        public SelectAction() {
            putValue(NAME, tr("Select"));
            putValue(SHORT_DESCRIPTION, tr("Selects the objects that take part in this command (unless currently deleted)"));
            new ImageProvider("dialogs", "select").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TreePath path;
            if (!undoTree.isSelectionEmpty()) {
                path = undoTree.getSelectionPath();
            } else if (!redoTree.isSelectionEmpty()) {
                path = redoTree.getSelectionPath();
            } else
                throw new IllegalStateException();

            DataSet dataSet = Main.getLayerManager().getEditDataSet();
            if (dataSet == null) return;
            dataSet.setSelected(getAffectedPrimitives(path));
        }

        @Override
        public void updateEnabledState() {
            setEnabled(!undoTree.isSelectionEmpty() || !redoTree.isSelectionEmpty());
        }
    }

    /**
     * Action that selects the objects that take part in a command, then zoom to them.
     */
    public class SelectAndZoomAction extends SelectAction {
        /**
         * Constructs a new {@code SelectAndZoomAction}.
         */
        public SelectAndZoomAction() {
            putValue(NAME, tr("Select and zoom"));
            putValue(SHORT_DESCRIPTION,
                    tr("Selects the objects that take part in this command (unless currently deleted), then and zooms to it"));
            new ImageProvider("dialogs/autoscale", "selection").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            AutoScaleAction.autoScale("selection");
        }
    }

    /**
     * undo / redo switch to reduce duplicate code
     */
    protected enum UndoRedoType {
        UNDO,
        REDO
    }

    /**
     * Action to undo or redo all commands up to (and including) the seleced item.
     */
    protected class UndoRedoAction extends AbstractAction implements IEnabledStateUpdating {
        private final UndoRedoType type;
        private final JTree tree;

        /**
         * constructor
         * @param type decide whether it is an undo action or a redo action
         */
        public UndoRedoAction(UndoRedoType type) {
            this.type = type;
            if (UndoRedoType.UNDO == type) {
                tree = undoTree;
                putValue(NAME, tr("Undo"));
                putValue(SHORT_DESCRIPTION, tr("Undo the selected and all later commands"));
                new ImageProvider("undo").getResource().attachImageIcon(this, true);
            } else {
                tree = redoTree;
                putValue(NAME, tr("Redo"));
                putValue(SHORT_DESCRIPTION, tr("Redo the selected and all earlier commands"));
                new ImageProvider("redo").getResource().attachImageIcon(this, true);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            lastOperation = type;
            TreePath path = tree.getSelectionPath();

            // we can only undo top level commands
            if (path.getPathCount() != 2)
                throw new IllegalStateException();

            int idx = ((CommandListMutableTreeNode) path.getLastPathComponent()).getIndex();

            // calculate the number of commands to undo/redo; then do it
            switch (type) {
            case UNDO:
                int numUndo = ((DefaultMutableTreeNode) undoTreeModel.getRoot()).getChildCount() - idx;
                Main.main.undoRedo.undo(numUndo);
                break;
            case REDO:
                int numRedo = idx+1;
                Main.main.undoRedo.redo(numRedo);
                break;
            }
            MainApplication.getMap().repaint();
        }

        @Override
        public void updateEnabledState() {
            // do not allow execution if nothing is selected or a sub command was selected
            setEnabled(!tree.isSelectionEmpty() && tree.getSelectionPath().getPathCount() == 2);
        }
    }

    class MouseEventHandler extends PopupMenuLauncher {

        MouseEventHandler() {
            super(new CommandStackPopup());
        }

        @Override
        public void mouseClicked(MouseEvent evt) {
            if (isDoubleClick(evt)) {
                selectAndZoomAction.actionPerformed(null);
            }
        }
    }

    private class CommandStackPopup extends JPopupMenu {
        CommandStackPopup() {
            add(selectAction);
            add(selectAndZoomAction);
        }
    }
}
