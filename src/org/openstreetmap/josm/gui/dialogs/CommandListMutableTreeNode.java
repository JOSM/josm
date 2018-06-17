// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.openstreetmap.josm.command.PseudoCommand;

/**
 * MutableTreeNode implementation for Command list JTree
 */
public class CommandListMutableTreeNode extends DefaultMutableTreeNode {

    protected final transient PseudoCommand cmd;

    /**
     * Constructs a new {@code CommandListMutableTreeNode}.
     * @param cmd command
     */
    public CommandListMutableTreeNode(PseudoCommand cmd) {
        super(new JLabel(cmd.getDescriptionText(), cmd.getDescriptionIcon(), JLabel.HORIZONTAL));
        this.cmd = cmd;
    }

    /**
     * Returns the command.
     * @return the command
     */
    public PseudoCommand getCommand() {
        return cmd;
    }

    /**
     * Returns the index.
     * @return the index
     */
    public int getIndex() {
        return getParent().getIndex(this);
    }
}
