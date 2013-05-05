// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * PseudoCommand is a reduced form of a command. It can be presented in a tree view
 * as subcommand of real commands but it is just an empty shell and can not be
 * executed or undone.
 */
abstract public class PseudoCommand {
    /**
     * Provides a description text representing this command.
     */
    abstract public String getDescriptionText();

    /**
     * Provides a descriptive icon of this command.
     */
    public Icon getDescriptionIcon() {
        return null;
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
