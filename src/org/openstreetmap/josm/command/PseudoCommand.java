// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * PseudoCommand is a reduced form of a command. It can be presented in a tree view
 * as subcommand of real commands but it is just an empty shell and can not be
 * executed or undone.
 * @since  3262 (creation)
 * @since 10599 (functional interface)
 */
public interface PseudoCommand {

    /**
     * Provides a description text representing this command.
     * @return description text representing this command
     */
    String getDescriptionText();

    /**
     * Provides a descriptive icon of this command.
     * @return descriptive icon of this command
     */
    default Icon getDescriptionIcon() {
        return null;
    }

    /**
     * Return the primitives that take part in this command.
     * @return primitives that take part in this command
     */
    Collection<? extends OsmPrimitive> getParticipatingPrimitives();

    /**
     * Returns the subcommands of this command.
     * Override for subclasses that have child commands.
     * @return the subcommands, null if there are no child commands
     */
    default Collection<PseudoCommand> getChildren() {
        return null;
    }
}
