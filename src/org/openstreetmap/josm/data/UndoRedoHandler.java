//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class UndoRedoHandler implements MapView.LayerChangeListener {

    /**
     * All commands that were made on the dataset. Don't write from outside!
     */
    public final LinkedList<Command> commands = new LinkedList<Command>();
    /**
     * The stack for redoing commands
     */
    public final LinkedList<Command> redoCommands = new LinkedList<Command>();

    private final LinkedList<CommandQueueListener> listenerCommands = new LinkedList<CommandQueueListener>();

    /**
     * Constructs a new {@code UndoRedoHandler}.
     */
    public UndoRedoHandler() {
        MapView.addLayerChangeListener(this);
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
        if (commands.size() > Main.pref.getInteger("undo.max", 1000)) {
            commands.removeFirst();
        }
        redoCommands.clear();
    }

    public void afterAdd() {
        fireCommandsChanged();

        // the command may have changed the selection so tell the listeners about the current situation
        Main.main.getCurrentDataSet().fireSelectionChanged();
    }

    /**
     * Executes the command and add it to the intern command queue.
     * @param c The command to execute. Must not be {@code null}.
     */
    synchronized public void add(final Command c) {
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
    synchronized public void undo(int num) {
        if (commands.isEmpty())
            return;
        Collection<? extends OsmPrimitive> oldSelection = Main.main.getCurrentDataSet().getSelected();
        Main.main.getCurrentDataSet().beginUpdate();
        try {
            for (int i=1; i<=num; ++i) {
                final Command c = commands.removeLast();
                c.undoCommand();
                redoCommands.addFirst(c);
                if (commands.isEmpty()) {
                    break;
                }
            }
        }
        finally {
            Main.main.getCurrentDataSet().endUpdate();
        }
        fireCommandsChanged();
        Collection<? extends OsmPrimitive> newSelection = Main.main.getCurrentDataSet().getSelected();
        if (!oldSelection.equals(newSelection)) {
            Main.main.getCurrentDataSet().fireSelectionChanged();
        }
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
        Collection<? extends OsmPrimitive> oldSelection = Main.main.getCurrentDataSet().getSelected();
        for (int i=0; i<num; ++i) {
            final Command c = redoCommands.removeFirst();
            c.executeCommand();
            commands.add(c);
            if (redoCommands.isEmpty()) {
                break;
            }
        }
        fireCommandsChanged();
        Collection<? extends OsmPrimitive> newSelection = Main.main.getCurrentDataSet().getSelected();
        if (!oldSelection.equals(newSelection)) {
            Main.main.getCurrentDataSet().fireSelectionChanged();
        }
    }

    public void fireCommandsChanged() {
        for (final CommandQueueListener l : listenerCommands) {
            l.commandChanged(commands.size(), redoCommands.size());
        }
    }

    public void clean() {
        redoCommands.clear();
        commands.clear();
        fireCommandsChanged();
    }

    public void clean(Layer layer) {
        if (layer == null)
            return;
        boolean changed = false;
        for (Iterator<Command> it = commands.iterator(); it.hasNext();) {
            if (it.next().invalidBecauselayerRemoved(layer)) {
                it.remove();
                changed = true;
            }
        }
        for (Iterator<Command> it = redoCommands.iterator(); it.hasNext();) {
            if (it.next().invalidBecauselayerRemoved(layer)) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            fireCommandsChanged();
        }
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        clean(oldLayer);
    }

    @Override
    public void layerAdded(Layer newLayer) {}
    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {}

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
