// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.util.Iterator;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This is the global undo/redo handler for all {@link DataSet}s.
 * <p>
 * If you want to change a data set, you can use {@link #add(Command)} to execute a command on it and make that command undoable.
 */
public class UndoRedoHandler {

    /**
     * All commands that were made on the dataset. Don't write from outside!
     *
     * @see #getLastCommand()
     */
    public final LinkedList<Command> commands = new LinkedList<>();
    /**
     * The stack for redoing commands
     */
    public final LinkedList<Command> redoCommands = new LinkedList<>();

    private final LinkedList<CommandQueueListener> listenerCommands = new LinkedList<>();

    /**
     * Constructs a new {@code UndoRedoHandler}.
     */
    public UndoRedoHandler() {
        // Do nothing
    }

    /**
     * A listener that gets notified of command queue (undo/redo) size changes.
     * @since 12718 (moved from {@code OsmDataLayer}
     */
    @FunctionalInterface
    public interface CommandQueueListener {
        /**
         * Notifies the listener about the new queue size
         * @param queueSize Undo stack size
         * @param redoSize Redo stack size
         */
        void commandChanged(int queueSize, int redoSize);
    }

    /**
     * Gets the last command that was executed on the command stack.
     * @return That command or <code>null</code> if there is no such command.
     * @since #12316
     */
    public Command getLastCommand() {
        return commands.peekLast();
    }

    /**
     * Executes the command and add it to the intern command queue.
     * @param c The command to execute. Must not be {@code null}.
     */
    public void addNoRedraw(final Command c) {
        CheckParameterUtil.ensureParameterNotNull(c, "c");
        c.executeCommand();
        commands.add(c);
        // Limit the number of commands in the undo list.
        // Currently you have to undo the commands one by one. If
        // this changes, a higher default value may be reasonable.
        if (commands.size() > Config.getPref().getInt("undo.max", 1000)) {
            commands.removeFirst();
        }
        redoCommands.clear();
    }

    /**
     * Fires a commands change event after adding a command.
     */
    public void afterAdd() {
        fireCommandsChanged();
    }

    /**
     * Executes the command and add it to the intern command queue.
     * @param c The command to execute. Must not be {@code null}.
     */
    public synchronized void add(final Command c) {
        addNoRedraw(c);
        afterAdd();
    }

    /**
     * Undoes the last added command.
     */
    public void undo() {
        undo(1);
    }

    /**
     * Undoes multiple commands.
     * @param num The number of commands to undo
     */
    public synchronized void undo(int num) {
        if (commands.isEmpty())
            return;
        DataSet ds = Main.main.getEditDataSet();
        if (ds != null) {
            ds.beginUpdate();
        }
        try {
            for (int i = 1; i <= num; ++i) {
                final Command c = commands.removeLast();
                c.undoCommand();
                redoCommands.addFirst(c);
                if (commands.isEmpty()) {
                    break;
                }
            }
        } finally {
            if (ds != null) {
                ds.endUpdate();
            }
        }
        fireCommandsChanged();
    }

    /**
     * Redoes the last undoed command.
     */
    public void redo() {
        redo(1);
    }

    /**
     * Redoes multiple commands.
     * @param num The number of commands to redo
     */
    public void redo(int num) {
        if (redoCommands.isEmpty())
            return;
        for (int i = 0; i < num; ++i) {
            final Command c = redoCommands.removeFirst();
            c.executeCommand();
            commands.add(c);
            if (redoCommands.isEmpty()) {
                break;
            }
        }
        fireCommandsChanged();
    }

    /**
     * Fires a command change to all listeners.
     */
    private void fireCommandsChanged() {
        for (final CommandQueueListener l : listenerCommands) {
            l.commandChanged(commands.size(), redoCommands.size());
        }
    }

    /**
     * Resets the undo/redo list.
     */
    public void clean() {
        redoCommands.clear();
        commands.clear();
        fireCommandsChanged();
    }

    /**
     * Resets all commands that affect the given dataset.
     * @param dataSet The data set that was affected.
     * @since 12718
     */
    public void clean(DataSet dataSet) {
        if (dataSet == null)
            return;
        boolean changed = false;
        for (Iterator<Command> it = commands.iterator(); it.hasNext();) {
            if (it.next().getAffectedDataSet() == dataSet) {
                it.remove();
                changed = true;
            }
        }
        for (Iterator<Command> it = redoCommands.iterator(); it.hasNext();) {
            if (it.next().getAffectedDataSet() == dataSet) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            fireCommandsChanged();
        }
    }

    /**
     * Removes a command queue listener.
     * @param l The command queue listener to remove
     */
    public void removeCommandQueueListener(CommandQueueListener l) {
        listenerCommands.remove(l);
    }

    /**
     * Adds a command queue listener.
     * @param l The commands queue listener to add
     * @return {@code true} if the listener has been added, {@code false} otherwise
     */
    public boolean addCommandQueueListener(CommandQueueListener l) {
        return listenerCommands.add(l);
    }
}
