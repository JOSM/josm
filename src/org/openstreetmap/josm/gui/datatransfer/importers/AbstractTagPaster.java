// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.datatransfer.importers;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.TransferHandler.TransferSupport;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.I18n;

/**
 * This transfer support allows us to transfer tags to the selected primitives
 * @author Michael Zangl
 * @since 10604
 */
public abstract class AbstractTagPaster extends AbstractOsmDataPaster {

    AbstractTagPaster(DataFlavor df) {
        super(df);
    }

    @Override
    public boolean importData(TransferSupport support, OsmDataLayer layer, EastNorth pasteAt)
            throws UnsupportedFlavorException, IOException {
        Collection<OsmPrimitive> selection = layer.data.getSelected();
        if (selection.isEmpty()) {
            return false;
        }

        return importTagsOn(support, selection);
    }

    @Override
    public boolean importTagsOn(TransferSupport support, Collection<? extends OsmPrimitive> selection)
            throws UnsupportedFlavorException, IOException {
        ChangePropertyCommand command = new ChangePropertyCommand(OsmDataManager.getInstance().getEditDataSet(), selection, getTags(support));
        commitCommands(selection, Collections.singletonList(command));
        return true;
    }

    /**
     * Create and execute SequenceCommand with descriptive title
     * @param selection selected primitives
     * @param commands the commands to perform in a sequential command
     * @since 10737
     */
    protected static void commitCommands(Collection<? extends OsmPrimitive> selection, List<Command> commands) {
        if (!commands.isEmpty()) {
            String title1 = trn("Pasting {0} tag", "Pasting {0} tags", commands.size(), commands.size());
            String title2 = trn("to {0} object", "to {0} objects", selection.size(), selection.size());
            @I18n.QuirkyPluralString
            final String title = title1 + ' ' + title2;
            UndoRedoHandler.getInstance().add(new SequenceCommand(title, commands));
        }
    }

    /**
     * Gets the tags that should be pasted.
     * @param support The TransferSupport to get the tags from.
     * @return The tags
     * @throws UnsupportedFlavorException if the requested data flavor is not supported
     * @throws IOException if an I/O error occurs
     */
    protected abstract Map<String, String> getTags(TransferSupport support) throws UnsupportedFlavorException, IOException;
}
