// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.JLabel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A command consisting of a sequence of other commands. Executes the other commands
 * and undo them in reverse order.
 * @author imi
 */
public class SequenceCommand extends Command {

    /**
     * The command sequenz to be executed.
     */
    private Command[] sequence;
    private boolean sequence_complete;
    private final String name;
    public boolean continueOnError = false;

    /**
     * Create the command by specifying the list of commands to execute.
     * @param sequenz The sequence that should be executed.
     */
    public SequenceCommand(String name, Collection<Command> sequenz) {
        super();
        this.name = name;
        this.sequence = new Command[sequenz.size()];
        this.sequence = sequenz.toArray(this.sequence);
    }

    /**
     * Convenient constructor, if the commands are known at compile time.
     */
    public SequenceCommand(String name, Command... sequenz) {
        this(name, Arrays.asList(sequenz));
    }

    @Override public boolean executeCommand() {
        for (int i=0; i < sequence.length; i++) {
            Command c = sequence[i];
            boolean result = c.executeCommand();
            if (!result) {
                Main.debug("SequenceCommand, executing command[" + i + "] " +  c + " result: " + result);
            }
            if (!result && !continueOnError) {
                this.undoCommands(i-1);
                return false;
            }
        }
        sequence_complete = true;
        return true;
    }

    public Command getLastCommand() {
        if(sequence.length == 0)
            return null;
        return sequence[sequence.length-1];
    }
    private void undoCommands(int start) {
        // We probably aborted this halfway though the
        // execution sequence because of a sub-command
        // error.  We already undid the sub-commands.
        if (!sequence_complete)
            return;
        for (int i = start; i >= 0; --i) {
            sequence[i].undoCommand();
        }
    }

    @Override public void undoCommand() {
        this.undoCommands(sequence.length-1);
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        for (Command c : sequence) {
            c.fillModifiedData(modified, deleted, added);
        }
    }

    @Override public JLabel getDescription() {
        return new JLabel(tr("Sequence")+": "+name, ImageProvider.get("data", "sequence"), JLabel.HORIZONTAL);
    }

    @Override
    public Collection<PseudoCommand> getChildren() {
        return (List) Arrays.asList(sequence);
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        Collection<OsmPrimitive> prims = new HashSet<OsmPrimitive>();
        for (Command c : sequence) {
            prims.addAll(c.getParticipatingPrimitives());
        }
        return prims;
    }
}
