// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.tools.Shortcut;

public class CommandStackDialog extends ToggleDialog implements CommandQueueListener {

    private DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    private JTree tree = new JTree(treeModel);

    public CommandStackDialog(final MapFrame mapFrame) {
        super(tr("Command Stack"), "commandstack", tr("Open a list of all commands (undo buffer)."),
                Shortcut.registerShortcut("subwindow:commandstack", tr("Toggle: {0}", tr("Command Stack")), KeyEvent.VK_O, Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 100, true);
        Main.main.undoRedo.listenerCommands.add(this);

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.expandRow(0);
        tree.setCellRenderer(new DefaultTreeCellRenderer(){
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
        });
        tree.setVisibleRowCount(8);
        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    @Override public void setVisible(boolean v) {
        if (v) {
            buildList();
        } else if (tree != null) {
            treeModel.setRoot(new DefaultMutableTreeNode());
        }
        super.setVisible(v);
    }

    private void buildList() {
        if(Main.main.undoRedo.commands.size() != 0) {
            setTitle(tr("Command Stack: {0}", Main.main.undoRedo.commands.size()));
        } else {
            setTitle(tr("Command Stack"));
        }
        if (Main.map == null || Main.map.mapView == null || Main.map.mapView.getEditLayer() == null)
            return;
        Collection<Command> commands = Main.main.undoRedo.commands;
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        for (Command c : commands) {
            root.add(c.description());
        }
        treeModel.setRoot(root);
        tree.scrollRowToVisible(treeModel.getChildCount(root)-1);
    }

    public void commandChanged(int queueSize, int redoSize) {
        if (!isVisible())
            return;
        treeModel.setRoot(new DefaultMutableTreeNode());
        buildList();
    }
}
