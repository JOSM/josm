// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Optional;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.downloadtasks.DownloadNotesTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Action to use the Notes search API to download all notes matching a given search term.
 * @since 8071
 */
public class SearchNotesDownloadAction extends JosmAction {

    private static final String HISTORY_KEY = "osm.notes.searchHistory";

    /** Constructs a new note search action */
    public SearchNotesDownloadAction() {
        super(tr("Search Notes..."), "note_search", tr("Download notes from the note search API"), null, false, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        HistoryComboBox searchTermBox = new HistoryComboBox();
        searchTermBox.setPossibleItemsTopDown(Config.getPref().getList(HISTORY_KEY, Collections.emptyList()));

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        contentPanel.add(new JLabel(tr("Search the OSM API for notes containing words:")), gc);
        gc.gridy = 1;
        contentPanel.add(searchTermBox, gc);

        ExtendedDialog ed = new ExtendedDialog(MainApplication.getMainFrame(), tr("Search for notes"), tr("Search for notes"), tr("Cancel"))
            .setContent(contentPanel)
            .setButtonIcons("note_search", "cancel");
        ed.configureContextsensitiveHelp("/Action/SearchNotesDownload", true /* show help button */);
        if (ed.showDialog().getValue() != 1) {
            return;
        }

        String searchTerm = Optional.ofNullable(searchTermBox.getText()).orElse("").trim();
        if (searchTerm.isEmpty()) {
            new Notification(tr("You must enter a search term"))
                .setIcon(JOptionPane.WARNING_MESSAGE)
                .show();
            return;
        }

        searchTermBox.addCurrentItemToHistory();
        Config.getPref().putList(HISTORY_KEY, searchTermBox.getHistory());

        performSearch(searchTerm);
    }

    /**
     * Perform search.
     * @param searchTerm search term
     */
    public void performSearch(String searchTerm) {

        String trimmedSearchTerm = searchTerm.trim();

        try {
            final long id = Long.parseLong(trimmedSearchTerm);
            new DownloadNotesTask().download(id, null);
            return;
        } catch (NumberFormatException ignore) {
            Logging.trace(ignore);
        }

        int noteLimit = Config.getPref().getInt("osm.notes.downloadLimit", 1000);
        int closedLimit = Config.getPref().getInt("osm.notes.daysClosed", 7);

        StringBuilder sb = new StringBuilder(128);
        sb.append(OsmApi.getOsmApi().getBaseUrl())
            .append("notes/search?limit=")
            .append(noteLimit)
            .append("&closed=")
            .append(closedLimit)
            .append("&q=")
            .append(Utils.encodeUrl(trimmedSearchTerm));

        new DownloadNotesTask().loadUrl(new DownloadParams(), sb.toString(), null);
    }
}
