// License: GPL. For details, see LICENSE file.
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Copy OSM primitives to clipboard in order to paste them, or their tags, somewhere else.
 * @since 404
 */
public final class CopyAction extends JosmAction {

    // regular expression that matches text clipboard contents after copying
    public static final String CLIPBOARD_REGEXP = "((node|way|relation)\\s\\d+,)*(node|way|relation)\\s\\d+";

    /**
     * Constructs a new {@code CopyAction}.
     */
    public CopyAction() {
        super(tr("Copy"), "copy",
                tr("Copy selected objects to paste buffer."),
                Shortcut.registerShortcut("system:copy", tr("Edit: {0}", tr("Copy")), KeyEvent.VK_C, Shortcut.CTRL), true);
        putValue("help", ht("/Action/Copy"));
        // CUA shortcut for copy (https://en.wikipedia.org/wiki/IBM_Common_User_Access#Description)
        Main.registerActionShortcut(this,
                Shortcut.registerShortcut("system:copy:cua", tr("Edit: {0}", tr("Copy")), KeyEvent.VK_INSERT, Shortcut.CTRL));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(isEmptySelection()) return;
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();

        copy(getEditLayer(), selection);
    }

    /**
     * Copies the given primitive ids to the clipboard. The output by this function
     * looks similar to: node 1089302677,node 1089303458,way 93793372
     * @param source The OSM data layer source
     * @param primitives The OSM primitives to copy
     */
    public static void copy(OsmDataLayer source, Collection<OsmPrimitive> primitives) {
        /* copy ids to the clipboard */
        String ids = getCopyString(primitives);
        Utils.copyToClipboard(ids);

        Main.pasteBuffer.makeCopy(primitives);
        Main.pasteSource = source;
    }

    public static String getCopyString(Collection<? extends OsmPrimitive> primitives) {
        StringBuilder idsBuilder = new StringBuilder();
        for (OsmPrimitive p : primitives) {
            idsBuilder.append(OsmPrimitiveType.from(p).getAPIName()).append(' ').append(p.getId()).append(',');
        }
        return idsBuilder.substring(0, idsBuilder.length() - 1);
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    private boolean isEmptySelection() {
        Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select something to copy."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return true;
        }
        return false;
    }
}
