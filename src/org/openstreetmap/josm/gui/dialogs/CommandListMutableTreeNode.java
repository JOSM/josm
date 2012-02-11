// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.openstreetmap.josm.command.PseudoCommand;

/**
 * MutableTreeNode implementation for Command list JTree
 */
public class CommandListMutableTreeNode extends DefaultMutableTreeNode {

    protected PseudoCommand cmd;
    protected int idx;

    public CommandListMutableTreeNode(PseudoCommand cmd, int idx) {
        super(new JLabel(cmd.getDescriptionText(), cmd.getDescriptionIcon(), JLabel.HORIZONTAL));
        this.cmd = cmd;
        this.idx = idx;
    }

    public PseudoCommand getCommand() {
        return cmd;
    }

    public int getIndex() {
        return idx;
    }
}
