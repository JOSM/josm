// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * A command consisting of a sequenz of other commands. Executes the other commands
 * and undo them in reverse order.
 * @author imi
 */
public class SequenceCommand extends Command {

	/**
	 * The command sequenz to be executed.
	 */
	private Command[] sequence;
	private final String name;

	/**
	 * Create the command by specifying the list of commands to execute.
	 * @param sequenz The sequenz that should be executed.
	 */
	public SequenceCommand(String name, Collection<Command> sequenz) {
		this.name = name;
		this.sequence = new Command[sequenz.size()];
		this.sequence = sequenz.toArray(this.sequence);
	}

	/**
	 * Convinient constructor, if the commands are known at compile time.
	 */
	public SequenceCommand(String name, Command... sequenz) {
		this(name, Arrays.asList(sequenz));
	}
	
	@Override public void executeCommand() {
		for (Command c : sequence)
			c.executeCommand();
	}

	@Override public void undoCommand() {
		for (int i = sequence.length-1; i >= 0; --i)
			sequence[i].undoCommand();
	}

	@Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		for (Command c : sequence)
			c.fillModifiedData(modified, deleted, added);
	}

	@Override public MutableTreeNode description() {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(tr("Sequence")+": "+name);
		for (Command c : sequence)
			root.add(c.description());
	    return root;
    }
}
