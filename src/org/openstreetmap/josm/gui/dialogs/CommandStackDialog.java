// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Box;
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
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.data.osm.DatasetCollection;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Shortcut;

public class CommandStackDialog extends ToggleDialog implements CommandQueueListener {

    private DefaultTreeModel undoTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    private DefaultTreeModel redoTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());

    private JTree undoTree = new JTree(undoTreeModel);
    private JTree redoTree = new JTree(redoTreeModel);

    private UndoRedoSelectionListener undoSelectionListener;
    private UndoRedoSelectionListener redoSelectionListener;

    private JScrollPane scrollPane;
    private JSeparator separator = new JSeparator();
    // only visible, if separator is the top most component
    private Component spacer = Box.createRigidArea(new Dimension(0, 3));

    // last operation is remembered to select the next undo/redo entry in the list
    // after undo/redo command
    private UndoRedoType lastOperation = UndoRedoType.UNDO;

    public CommandStackDialog(final MapFrame mapFrame) {
        super(tr("Command Stack"), "commandstack", tr("Open a list of all commands (undo buffer)."),
                Shortcut.registerShortcut("subwindow:commandstack", tr("Toggle: {0}", tr("Command Stack")), KeyEvent.VK_O, Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 100, true);
        Main.main.undoRedo.listenerCommands.add(this);

        undoTree.addMouseListener(new PopupMenuHandler());
        undoTree.setRootVisible(false);
        undoTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        undoTree.setShowsRootHandles(true);
        undoTree.expandRow(0);
        undoTree.setCellRenderer(new CommandCellRenderer());
        undoSelectionListener = new UndoRedoSelectionListener(undoTree);
        undoTree.getSelectionModel().addTreeSelectionListener(undoSelectionListener);

        redoTree.addMouseListener(new PopupMenuHandler());
        redoTree.setRootVisible(false);
        redoTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        redoTree.setShowsRootHandles(true);
        redoTree.expandRow(0);
        redoTree.setCellRenderer(new CommandCellRenderer());
        redoSelectionListener = new UndoRedoSelectionListener(redoTree);
        redoTree.getSelectionModel().addTreeSelectionListener(redoSelectionListener);

        JPanel treesPanel = new JPanel(new GridBagLayout());

        treesPanel.add(spacer, GBC.eol());
        spacer.setVisible(false);
        treesPanel.add(undoTree, GBC.eol().fill(GBC.HORIZONTAL));
        separator.setVisible(false);
        treesPanel.add(separator, GBC.eol().fill(GBC.HORIZONTAL));
        treesPanel.add(redoTree, GBC.eol().fill(GBC.HORIZONTAL));
        treesPanel.add(Box.createRigidArea(new Dimension(0, 0)), GBC.std().weight(0, 1));
        treesPanel.setBackground(redoTree.getBackground());

        scrollPane = new JScrollPane(treesPanel);
        add(scrollPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private static class CommandCellRenderer extends DefaultTreeCellRenderer {
        @Override public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode v = (DefaultMutableTreeNode)value;
            if (v.getUserObject() instanceof JLabel) {
                JLabel l = (JLabel)v.getUserObject();
                setIcon(l.getIcon());
                setText(l.getText());
            }
            return this;
        }
    }

    /**
     * Selection listener for undo and redo area.
     * If one is clicked, takes away the selection from the other, so
     * it behaves as if it was one component.
     */
    private class UndoRedoSelectionListener implements TreeSelectionListener {
        private JTree source;

        public UndoRedoSelectionListener(JTree source) {
            this.source = source;
        }

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

    protected JPanel createButtonPanel() {
        JPanel buttonPanel = getButtonPanel(3);

        SelectAction selectAction = new SelectAction();
        wireUpdateEnabledStateUpdater(selectAction, undoTree);
        wireUpdateEnabledStateUpdater(selectAction, redoTree);
        buttonPanel.add(new SideButton(selectAction));

        UndoRedoAction undoAction = new UndoRedoAction(UndoRedoType.UNDO);
        wireUpdateEnabledStateUpdater(undoAction, undoTree);
        buttonPanel.add(new SideButton(undoAction));

        UndoRedoAction redoAction = new UndoRedoAction(UndoRedoType.REDO);
        wireUpdateEnabledStateUpdater(redoAction, redoTree);
        buttonPanel.add(new SideButton(redoAction));

        return buttonPanel;
    }

    /**
     * Interface to provide a callback for enabled state update.
     */
    protected interface IEnabledStateUpdating {
        void updateEnabledState();
    }

    /**
     * Wires updater for enabled state to the events.
     */
    protected void wireUpdateEnabledStateUpdater(final IEnabledStateUpdating updater, JTree tree) {
        addShowNotifyListener(updater);

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                updater.updateEnabledState();
            }
        });

        tree.getModel().addTreeModelListener(new TreeModelListener() {
            public void treeNodesChanged(TreeModelEvent e) {
                updater.updateEnabledState();
            }

            public void treeNodesInserted(TreeModelEvent e) {
                updater.updateEnabledState();
            }

            public void treeNodesRemoved(TreeModelEvent e) {
                updater.updateEnabledState();
            }

            public void treeStructureChanged(TreeModelEvent e) {
                updater.updateEnabledState();
            }
        });
    }

    @Override
    public void showNotify() {
        buildTrees();
        for (IEnabledStateUpdating listener : showNotifyListener) {
            listener.updateEnabledState();
        }
    }

    /**
     * Simple listener setup to update the button enabled state when the side dialog shows.
     */
    Set<IEnabledStateUpdating> showNotifyListener = new LinkedHashSet<IEnabledStateUpdating>();

    private void addShowNotifyListener(IEnabledStateUpdating listener) {
        showNotifyListener.add(listener);
    }

    @Override
    public void hideNotify() {
        undoTreeModel.setRoot(new DefaultMutableTreeNode());
        redoTreeModel.setRoot(new DefaultMutableTreeNode());
    }

    /**
     * Build the trees of undo and redo commands (initially or when
     * they have changed).
     */
    private void buildTrees() {
        setTitle(tr("Command Stack"));
        if (Main.map == null || Main.map.mapView == null || Main.map.mapView.getEditLayer() == null)
            return;

        List<Command> undoCommands = Main.main.undoRedo.commands;
        DefaultMutableTreeNode undoRoot = new DefaultMutableTreeNode();
        for (int i=0; i<undoCommands.size(); ++i) {
            undoRoot.add(getNodeForCommand(undoCommands.get(i), i));
        }
        undoTreeModel.setRoot(undoRoot);
        undoTree.scrollRowToVisible(undoTreeModel.getChildCount(undoRoot)-1);
        scrollPane.getHorizontalScrollBar().setValue(0);

        List<Command> redoCommands = Main.main.undoRedo.redoCommands;
        DefaultMutableTreeNode redoRoot = new DefaultMutableTreeNode();
        for (int i=0; i<redoCommands.size(); ++i) {
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
    }

    /**
     * Wraps a command in a CommandListMutableTreeNode.
     * Recursively adds child commands.
     */
    protected CommandListMutableTreeNode getNodeForCommand(PseudoCommand c, int idx) {
        CommandListMutableTreeNode node = new CommandListMutableTreeNode(c, idx);
        if (c.getChildren() != null) {
            List<PseudoCommand> children = new ArrayList<PseudoCommand>(c.getChildren());
            for (int i=0; i<children.size(); ++i) {
                node.add(getNodeForCommand(children.get(i), i));
            }
        }
        return node;
    }

    public void commandChanged(int queueSize, int redoSize) {
        if (!isVisible())
            return;
        buildTrees();
    }

    public class SelectAction extends AbstractAction implements IEnabledStateUpdating {

        public SelectAction() {
            super();
            putValue(NAME,tr("Select"));
            putValue(SHORT_DESCRIPTION, tr("Selects the objects that take part in this command (unless currently deleted)"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs","select"));

        }

        public void actionPerformed(ActionEvent e) {
            TreePath path;
            undoTree.getSelectionPath();
            if (!undoTree.isSelectionEmpty()) {
                path = undoTree.getSelectionPath();
            } else if (!redoTree.isSelectionEmpty()) {
                path = redoTree.getSelectionPath();
            } else
                throw new IllegalStateException();

            if (Main.map == null || Main.map.mapView == null || Main.map.mapView.getEditLayer() == null) return;
            PseudoCommand c = ((CommandListMutableTreeNode) path.getLastPathComponent()).getCommand();

            final OsmDataLayer currentLayer = Main.map.mapView.getEditLayer();

            DatasetCollection<OsmPrimitive> prims = new DatasetCollection<OsmPrimitive>(
                    c.getParticipatingPrimitives(),
                    new Predicate<OsmPrimitive>(){
                        public boolean evaluate(OsmPrimitive o) {
                            OsmPrimitive p = currentLayer.data.getPrimitiveById(o);
                            return p != null && p.isUsable();
                        }
                    }
            );
            Main.map.mapView.getEditLayer().data.setSelected(prims);
        }

        public void updateEnabledState() {
            setEnabled(!undoTree.isSelectionEmpty() || !redoTree.isSelectionEmpty());
        }

    }

    /**
     * undo / redo switch to reduce duplicate code
     */
    protected enum UndoRedoType {UNDO, REDO};

    /**
     * Action to undo or redo all commands up to (and including) the seleced item.
     */
    protected class UndoRedoAction extends AbstractAction implements IEnabledStateUpdating {
        private UndoRedoType type;
        private JTree tree;

        /**
         * constructor
         * @param type decide whether it is an undo action or a redo action
         */
        public UndoRedoAction(UndoRedoType type) {
            super();
            this.type = type;
            switch (type) {
                case UNDO:
                    tree = undoTree;
                    putValue(NAME,tr("Undo"));
                    putValue(SHORT_DESCRIPTION, tr("Undo the selected and all later commands"));
                    putValue(SMALL_ICON, ImageProvider.get("undo"));
                    break;
                case REDO:
                    tree = redoTree;
                    putValue(NAME,tr("Redo"));
                    putValue(SHORT_DESCRIPTION, tr("Redo the selected and all earlier commands"));
                    putValue(SMALL_ICON, ImageProvider.get("redo"));
                    break;
            }
        }

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
            Main.map.repaint();
        }

        public void updateEnabledState() {
            // do not allow execution if nothing is selected or a sub command was selected
            setEnabled(!tree.isSelectionEmpty() && tree.getSelectionPath().getPathCount()==2);
        }
    }

    class PopupMenuHandler extends PopupMenuLauncher {
        @Override
        public void launch(MouseEvent evt) {
            Point p = evt.getPoint();
            JTree tree = (JTree) evt.getSource();
            int row = tree.getRowForLocation(p.x, p.y);
            if (row != -1) {
                TreePath path = tree.getPathForLocation(p.x, p.y);
                // right click on unselected element -> select it first
                if (!tree.isPathSelected(path)) {
                    tree.setSelectionPath(path);
                }
                TreePath[] selPaths = tree.getSelectionPaths();

                CommandStackPopup menu = new CommandStackPopup(selPaths);
                menu.show(tree, p.x, p.y-3);
            }
        }
    }

    private class CommandStackPopup extends JPopupMenu {
        private TreePath[] sel;
        public CommandStackPopup(TreePath[] sel){
            this.sel = sel;
            add(new SelectAction());
        }
    }
}
