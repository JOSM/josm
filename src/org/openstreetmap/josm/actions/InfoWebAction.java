// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Display object information about OSM nodes, ways, or relations in web browser.
 * @since 4408
 */
public class InfoWebAction extends AbstractInfoAction {

    /**
     * Constructs a new {@code InfoWebAction}.
     */
    public InfoWebAction() {
        super(tr("Advanced info (web)"), "info",
                tr("Display object information about OSM nodes, ways, or relations in web browser."),
                Shortcut.registerShortcut("core:infoweb",
                        tr("Advanced info (web)"), KeyEvent.VK_I, Shortcut.CTRL_SHIFT),
                true, "action/infoweb", true);
        setHelpId(ht("/Action/InfoAboutElementsWeb"));
    }

    @Override
    protected String createInfoUrl(Object infoObject) {
        if (infoObject instanceof IPrimitive) {
            IPrimitive primitive = (IPrimitive) infoObject;
            return Config.getUrls().getBaseBrowseUrl() + '/' + OsmPrimitiveType.from(primitive).getAPIName() + '/' + primitive.getOsmId();
        } else if (infoObject instanceof Note) {
            Note note = (Note) infoObject;
            return Config.getUrls().getBaseBrowseUrl() + "/note/" + note.getId();
        } else {
            return null;
        }
    }

    @Override
    protected void updateEnabledState() {
        super.updateEnabledState();
        updateEnabledStateWithNotes();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        super.updateEnabledState(selection);
        updateEnabledStateWithNotes();
    }

    private void updateEnabledStateWithNotes() {
        // Allows enabling if a note is selected, even if no OSM object is selected
        if (!isEnabled() && MainApplication.isDisplayingMapView() && MainApplication.getMap().noteDialog.getSelectedNote() != null) {
            setEnabled(true);
        }
    }

    /**
     * Called when the note selection has changed.
     * TODO: make a proper listener mechanism to handle change of note selection
     * @since 8475
     */
    public final void noteSelectionChanged() {
        updateEnabledState();
    }
}
