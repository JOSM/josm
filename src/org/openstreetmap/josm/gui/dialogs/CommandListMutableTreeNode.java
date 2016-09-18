// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.openstreetmap.josm.command.PseudoCommand;

/**
 * MutableTreeNode implementation for Command list JTree
 */
public class CommandListMutableTreeNode extends DefaultMutableTreeNode {

    protected final PseudoCommand cmd;
    protected final int idx;

    /**
     * Constructs a new {@code CommandListMutableTreeNode}.
     * @param cmd command
     * @param idx index
     */
    public CommandListMutableTreeNode(PseudoCommand cmd, int idx) {
        super(new JLabel(cmd.getDescriptionText(), cmd.getDescriptionIcon(), JLabel.HORIZONTAL));
        this.cmd = cmd;
        this.idx = idx;
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
        return idx;
    }
}
