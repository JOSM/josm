// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A command consisting of a sequence of other commands. Executes the other commands
 * and undo them in reverse order.
 * @author imi
 * @since 31
 */
public class SequenceCommand extends Command {

    /** The command sequence to be executed. */
    private Command[] sequence;
    private boolean sequenceComplete;
    private final String name;
    /** Determines if the sequence execution should continue after one of its commands fails. */
    public boolean continueOnError = false;

    /**
     * Create the command by specifying the list of commands to execute.
     * @param name The description text
     * @param sequenz The sequence that should be executed.
     */
    public SequenceCommand(String name, Collection<Command> sequenz) {
        super();
        this.name = name;
        this.sequence = sequenz.toArray(new Command[sequenz.size()]);
    }

    /**
     * Convenient constructor, if the commands are known at compile time.
     * @param name The description text
     * @param sequenz The sequence that should be executed.
     */
    public SequenceCommand(String name, Command... sequenz) {
        this(name, Arrays.asList(sequenz));
    }

    @Override public boolean executeCommand() {
        for (int i=0; i < sequence.length; i++) {
            boolean result = sequence[i].executeCommand();
            if (!result && !continueOnError) {
                undoCommands(i-1);
                return false;
            }
        }
        sequenceComplete = true;
        return true;
    }

    /**
     * Returns the last command.
     * @return The last command, or {@code null} if the sequence is empty.
     */
    public Command getLastCommand() {
        if (sequence.length == 0)
            return null;
        return sequence[sequence.length-1];
    }
    
    protected final void undoCommands(int start) {
        // We probably aborted this halfway though the
        // execution sequence because of a sub-command
        // error.  We already undid the sub-commands.
        if (!sequenceComplete)
            return;
        for (int i = start; i >= 0; --i) {
            sequence[i].undoCommand();
        }
    }

    @Override public void undoCommand() {
        undoCommands(sequence.length-1);
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        for (Command c : sequence) {
            c.fillModifiedData(modified, deleted, added);
        }
    }

    @Override
    public String getDescriptionText() {
        return tr("Sequence: {0}", name);
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "sequence");
    }

    @Override
    public Collection<PseudoCommand> getChildren() {
        return Arrays.<PseudoCommand>asList(sequence);
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        Collection<OsmPrimitive> prims = new HashSet<OsmPrimitive>();
        for (Command c : sequence) {
            prims.addAll(c.getParticipatingPrimitives());
        }
        return prims;
    }
    
    protected final void setSequence(Command[] sequence) {
        this.sequence = Arrays.copyOf(sequence, sequence.length);
    }
    
    protected final void setSequenceComplete(boolean sequenceComplete) {
        this.sequenceComplete = sequenceComplete;
    }
}
