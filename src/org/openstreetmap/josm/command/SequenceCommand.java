// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.ReportedException;

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
    protected final boolean continueOnError;

    /**
     * Create the command by specifying the list of commands to execute.
     * @param ds The target data set. Must not be {@code null}
     * @param name The description text
     * @param sequenz The sequence that should be executed
     * @param continueOnError Determines if the sequence execution should continue after one of its commands fails
     * @since 12726
     */
    public SequenceCommand(DataSet ds, String name, Collection<Command> sequenz, boolean continueOnError) {
        super(ds);
        this.name = name;
        this.sequence = sequenz.toArray(new Command[0]);
        this.continueOnError = continueOnError;
    }

    /**
     * Create the command by specifying the list of commands to execute.
     * @param name The description text
     * @param sequenz The sequence that should be executed. Must not be null or empty
     * @param continueOnError Determines if the sequence execution should continue after one of its commands fails
     * @since 11874
     */
    public SequenceCommand(String name, Collection<Command> sequenz, boolean continueOnError) {
        this(sequenz.iterator().next().getAffectedDataSet(), name, sequenz, continueOnError);
    }

    /**
     * Create the command by specifying the list of commands to execute.
     * @param name The description text
     * @param sequenz The sequence that should be executed.
     */
    public SequenceCommand(String name, Collection<Command> sequenz) {
        this(name, sequenz, false);
    }

    /**
     * Convenient constructor, if the commands are known at compile time.
     * @param name The description text
     * @param sequenz The sequence that should be executed.
     */
    public SequenceCommand(String name, Command... sequenz) {
        this(name, Arrays.asList(sequenz));
    }

    /**
     * Convenient constructor, if the commands are known at compile time.
     * @param name The description text to be used for the sequence command, if one is created.
     * @param sequenz The sequence that should be executed.
     * @return Either a SequenceCommand, or the only command in the potential sequence
     * @since 16573
     */
    public static Command wrapIfNeeded(String name, Command... sequenz) {
        if (sequenz.length == 1) {
            return sequenz[0];
        }
        return new SequenceCommand(name, sequenz);
    }

    /**
     * Convenient constructor, if the commands are known at compile time.
     * @param name The description text to be used for the sequence command, if one is created.
     * @param sequenz The sequence that should be executed.
     * @return Either a SequenceCommand, or the only command in the potential sequence
     * @since 16573
     */
    public static Command wrapIfNeeded(String name, Collection<Command> sequenz) {
        if (sequenz.size() == 1) {
            return sequenz.iterator().next();
        }
        return new SequenceCommand(name, sequenz);
    }

    @Override public boolean executeCommand() {
        for (int i = 0; i < sequence.length; i++) {
            boolean result;
            try {
                result = sequence[i].executeCommand();
            } catch (AssertionError | Exception e) {
                throw createReportedException(e, i);
            }
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
        for (int i = start; i >= 0; --i) {
            try {
                sequence[i].undoCommand();
            } catch (AssertionError | Exception e) {
                throw createReportedException(e, i);
            }
        }
    }

    private ReportedException createReportedException(Throwable e, int i) {
        ReportedException exception = new ReportedException(e);
        exception.startSection("sequence_information");
        exception.put("sequence_name", getDescriptionText());
        exception.put("sequence_command", sequence[i].getDescriptionText());
        exception.put("sequence_index", i);
        exception.put("sequence_commands", Stream.of(sequence)
                .map(o -> o.getClass().getCanonicalName())
                .map(String::valueOf)
                .collect(Collectors.joining(";", "[", "]")));
        exception.put("sequence_commands_descriptions", Stream.of(sequence)
                .map(Command::getDescriptionText)
                .collect(Collectors.joining(";", "[", "]")));
        return exception;
    }

    @Override
    public void undoCommand() {
        // We probably aborted this halfway though the execution sequence because of a sub-command error.
        // We already undid the sub-commands.
        if (!sequenceComplete)
            return;
        undoCommands(sequence.length-1);
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        for (Command c : sequence) {
            c.fillModifiedData(modified, deleted, added);
        }
    }

    @Override
    public String getDescriptionText() {
        return tr("Sequence: {0}", name);
    }

    /**
     * Returns the command name used in description text.
     * @return the command name
     * @since 14283
     */
    public final String getName() {
        return name;
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
        Collection<OsmPrimitive> prims = new HashSet<>();
        for (Command c : sequence) {
            prims.addAll(c.getParticipatingPrimitives());
        }
        return prims;
    }

    protected final void setSequence(Command... sequence) {
        this.sequence = Utils.copyArray(sequence);
    }

    protected final void setSequenceComplete(boolean sequenceComplete) {
        this.sequenceComplete = sequenceComplete;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), Arrays.hashCode(sequence), sequenceComplete, name, continueOnError);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        SequenceCommand that = (SequenceCommand) obj;
        return sequenceComplete == that.sequenceComplete &&
                continueOnError == that.continueOnError &&
                Arrays.equals(sequence, that.sequence) &&
                Objects.equals(name, that.name);
    }
}
