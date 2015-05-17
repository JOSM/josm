// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadNotesTask;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.Utils;

/**
 * Action to use the Notes search API to download all notes matching a given search term.
 * @since 8071
 */
public class SearchNotesDownloadAction extends JosmAction {

    private static final String HISTORY_KEY = "osm.notes.searchHistory";

    /** Constructs a new note search action */
    public SearchNotesDownloadAction() {
        super(tr("Search Notes..."), "note_search", tr("Download notes from the note search API"), null, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        HistoryComboBox searchTermBox = new HistoryComboBox();
        List<String> searchHistory = new LinkedList<>(Main.pref.getCollection(HISTORY_KEY, new LinkedList<String>()));
        Collections.reverse(searchHistory);
        searchTermBox.setPossibleItems(searchHistory);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        contentPanel.add(new JLabel(tr("Search the OSM API for notes containing words:")), gc);
        gc.gridy = 1;
        contentPanel.add(searchTermBox, gc);

        ExtendedDialog ed = new ExtendedDialog(Main.parent, tr("Search for notes"),
                new String[] {tr("Search for notes"), tr("Cancel")});
        ed.setContent(contentPanel);
        ed.setButtonIcons(new String[] {"note_search", "cancel"});
        ed.showDialog();
        if (ed.getValue() != 1) {
            return;
        }

        String searchTerm = searchTermBox.getText();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            Notification notification = new Notification(tr("You must enter a search term"));
            notification.setIcon(JOptionPane.WARNING_MESSAGE);
            notification.show();
            return;
        }

        searchTermBox.addCurrentItemToHistory();
        Main.pref.putCollection(HISTORY_KEY, searchTermBox.getHistory());

        performSearch(searchTerm);
    }

    public void performSearch(String searchTerm) {

        searchTerm = searchTerm.trim();

        try {
            final long id = Long.parseLong(searchTerm);
            new DownloadNotesTask().download(false, id, null);
            return;
        } catch (NumberFormatException ignore) {
        }

        int noteLimit = Main.pref.getInteger("osm.notes.downloadLimit", 1000);
        int closedLimit = Main.pref.getInteger("osm.notes.daysCloased", 7);

        StringBuilder sb = new StringBuilder();
        sb.append(OsmApi.getOsmApi().getBaseUrl())
            .append("notes/search?limit=")
            .append(noteLimit)
            .append("&closed=")
            .append(closedLimit)
            .append("&q=")
            .append(Utils.encodeUrl(searchTerm));

        new DownloadNotesTask().loadUrl(false, sb.toString(), null);
    }
}
