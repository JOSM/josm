// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.datatransfer.OsmTransferHandler;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action, to paste all tags from one primitive to another.
 *
 * It will take the primitive from the copy-paste buffer an apply all its tags
 * to the selected primitive(s).
 *
 * @author David Earl
 */
public final class PasteTagsAction extends JosmAction {

    private static final String HELP = ht("/Action/PasteTags");
    private final OsmTransferHandler transferHandler = new OsmTransferHandler();

    /**
     * Constructs a new {@code PasteTagsAction}.
     */
    public PasteTagsAction() {
        super(tr("Paste Tags"), "pastetags",
                tr("Apply tags of contents of paste buffer to all selected items."),
                Shortcut.registerShortcut("system:pastestyle", tr("Edit: {0}", tr("Paste Tags")),
                KeyEvent.VK_V, Shortcut.CTRL_SHIFT), true);
        putValue("help", HELP);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> selection = getLayerManager().getEditDataSet().getSelected();

        if (selection.isEmpty())
            return;

        transferHandler.pasteTags(selection);
    }

    @Override
    protected void updateEnabledState() {
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null) {
            setEnabled(false);
            return;
        }
        // buffer listening slows down the program and is not very good for arbitrary text in buffer
        setEnabled(!ds.selectionEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
