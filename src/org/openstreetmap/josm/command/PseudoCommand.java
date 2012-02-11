// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import java.util.Collection;
import javax.swing.Icon;
import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * PseudoCommand is a reduced form of a command. It can be presented in a tree view
 * as subcommand of real commands but it is just an empty shell and can not be
 * executed or undone.
 */
abstract public class PseudoCommand {
    /**
     * Provide a description that can be presented in a list or tree view.
     * @deprecated This abstract method is deprecated.
     * On 2012-03-01, this abstract method will be removed.
     * At the same moment, the methods {@link #getDescrpitionText()} and {@link #getDescrpitionIcon()}
     * will be made abstract.
     * For a smooth transition, replace {@link #getDescription()} by implementations of
     * {@link #getDescrpitionText()} and {@link #getDescrpitionIcon()} as early as possible.
     * {@link #getDescription()} is no longer abstract and can therefore be removed.
     */
    @Deprecated
    public Object getDescription() {
        return null;
    }

    /**
     * Provides a description text representing this command.
     */
    public String getDescriptionText() {
        Object o = getDescription();
        if (o instanceof JLabel) {
            return ((JLabel) o).getText();
        } else {
            return o.toString();
        }
    }

    /**
     * Provides a descriptive icon of this command.
     */
    public Icon getDescriptionIcon() {
        Object o = getDescription();
        if (o instanceof JLabel) {
            return ((JLabel) o).getIcon();
        } else {
            return null;
        }
    }

    /**
     * Return the primitives that take part in this command.
     */
    abstract public Collection<? extends OsmPrimitive> getParticipatingPrimitives();

    /**
     * Returns the subcommands of this command.
     * Override for subclasses that have child commands.
     * @return the subcommands, null if there are no child commands
     */
    public Collection<PseudoCommand> getChildren() {
        return null;
    }
}
