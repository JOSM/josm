// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * User action to copy the URL of one or several object(s) to the clipboard.
 * @since 17767
 */
public class CopyUrlAction extends JosmAction {

    /**
     * Constructs a new {@code CopyCoordinatesAction}.
     */
    public CopyUrlAction() {
        super(tr("Copy server URLs"), "copy",
                tr("Copy server URLs of selected objects to clipboard."),
                Shortcut.registerShortcut("copy:urls", tr("Edit: {0}", tr("Copy server URLs")),
                        KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
                false, "copy/urls", true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        final String base = Config.getUrls().getBaseBrowseUrl() + '/';
        String string = getSelected().stream()
                .filter(p -> !p.isNew())
                .map(p -> base + OsmPrimitiveType.from(p).getAPIName() + '/' + p.getOsmId())
                .collect(Collectors.joining("\n"));
        Note note = getNote();
        if (note != null && note.getId() > 0) {
            string = string + "\n" + base + "/note/" + note.getId();
        }
        ClipboardUtils.copyString(string);
    }

    @Override
    protected void updateEnabledState() {
        Note note = getNote();
        setEnabled((note != null && note.getId() > 0) || !getSelected().stream().allMatch(OsmPrimitive::isNew));
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledState();
    }

    private static Note getNote() {
        return MainApplication.isDisplayingMapView() ? MainApplication.getMap().noteDialog.getSelectedNote() : null;
    }

    private Collection<OsmPrimitive> getSelected() {
        DataSet ds = getLayerManager().getActiveDataSet();
        if (ds == null) {
            return Collections.emptyList();
        } else {
            return ds.getAllSelected();
        }
    }
}
