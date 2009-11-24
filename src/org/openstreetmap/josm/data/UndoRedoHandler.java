//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;

public class UndoRedoHandler implements LayerChangeListener {

    /**
     * All commands that were made on the dataset. Don't write from outside!
     */
    public final LinkedList<Command> commands = new LinkedList<Command>();
    /**
     * The stack for redoing commands
     */
    private final Stack<Command> redoCommands = new Stack<Command>();

    public final LinkedList<CommandQueueListener> listenerCommands = new LinkedList<CommandQueueListener>();

    public UndoRedoHandler() {
        Layer.listeners.add(this);
    }

    /**
     * Execute the command and add it to the intern command queue.
     */
    public void addNoRedraw(final Command c) {
        c.executeCommand();
        commands.add(c);
        redoCommands.clear();
    }

    public void afterAdd() {
        if (Main.map != null && Main.map.mapView.getActiveLayer() instanceof OsmDataLayer) {
            OsmDataLayer data = (OsmDataLayer)Main.map.mapView.getActiveLayer();
            data.fireDataChange();
        }
        fireCommandsChanged();

        // the command may have changed the selection so tell the listeners about the current situation
        Main.main.getCurrentDataSet().fireSelectionChanged();
    }

    /**
     * Execute the command and add it to the intern command queue.
     */
    public void add(final Command c) {
        addNoRedraw(c);
        afterAdd();
    }

    /**
     * Undoes the last added command.
     */
    public void undo() {
        if (commands.isEmpty())
            return;
        final Command c = commands.removeLast();
        c.undoCommand();
        redoCommands.push(c);
        if (Main.map != null && Main.map.mapView.getActiveLayer() instanceof OsmDataLayer) {
            OsmDataLayer data = (OsmDataLayer)Main.map.mapView.getActiveLayer();
            data.fireDataChange();
        }
        fireCommandsChanged();
        Main.main.getCurrentDataSet().setSelected();
    }

    /**
     * Redoes the last undoed command.
     * TODO: This has to be moved to a central place in order to support multiple layers.
     */
    public void redo() {
        if (redoCommands.isEmpty())
            return;
        final Command c = redoCommands.pop();
        c.executeCommand();
        commands.add(c);
        if (Main.map != null && Main.map.mapView.getActiveLayer() instanceof OsmDataLayer) {
            OsmDataLayer data = (OsmDataLayer)Main.map.mapView.getActiveLayer();
            data.fireDataChange();
        }
        fireCommandsChanged();
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

    public void layerRemoved(Layer oldLayer) {
        clean(oldLayer);
    }

    public void layerAdded(Layer newLayer) {}
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {}
}
