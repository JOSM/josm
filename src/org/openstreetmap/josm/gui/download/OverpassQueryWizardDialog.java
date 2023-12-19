// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.data.osm.search.SearchSetting;
import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.gui.dialogs.SearchDialog;
import org.openstreetmap.josm.gui.download.overpass.OverpassWizardRegistration.OverpassWizardCallbacks;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompComboBoxModel;
import org.openstreetmap.josm.gui.widgets.JosmComboBoxModel;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.SearchCompilerQueryWizard;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.Utils;

/**
 * This dialog provides an easy and fast way to create an overpass query.
 * @since 12576
 * @since 12652: Moved here
 */
public final class OverpassQueryWizardDialog extends SearchDialog {

    private static final ListProperty OVERPASS_WIZARD_HISTORY =
            new ListProperty("download.overpass.wizard", new ArrayList<>());
    private final OverpassWizardCallbacks callbacks;

    // dialog buttons
    private static final int BUILD_QUERY = 0;
    private static final int BUILD_AN_EXECUTE_QUERY = 1;
    private static final int CANCEL = 2;

    private final AutoCompComboBoxModel<SearchSetting> model;

    /** preferences reader/writer with automatic transmogrification to and from String */
    private final JosmComboBoxModel<SearchSetting>.Preferences prefs;

    /**
     * Create a new {@link OverpassQueryWizardDialog}
     * @param callbacks The Overpass download source panel.
     */
    public OverpassQueryWizardDialog(OverpassWizardCallbacks callbacks) {
        super(new SearchSetting(), new AutoCompComboBoxModel<>(), new PanelOptions(false, true), callbacks.getParent(),
                tr("Overpass Query Wizard"),
                tr("Build query"), tr("Build query and execute"), tr("Cancel"));
        this.callbacks = callbacks;
        model = hcbSearchString.getModel();
        setButtonIcons("dialogs/magic-wand", "download-overpass", "cancel");
        setCancelButton(CANCEL + 1);
        setDefaultButton(BUILD_AN_EXECUTE_QUERY + 1);
        prefs = model.prefs(SearchSetting::fromString, SearchSetting::toString);
        prefs.load(OVERPASS_WIZARD_HISTORY);
    }

    @Override
    public void buttonAction(int buttonIndex, ActionEvent evt) {
        switch (buttonIndex) {
            case BUILD_QUERY:
                if (this.buildQueryAction()) {
                    this.saveHistory();
                    super.buttonAction(BUILD_QUERY, evt);
                }
                break;
            case BUILD_AN_EXECUTE_QUERY:
                if (this.buildQueryAction()) {
                    this.saveHistory();
                    super.buttonAction(BUILD_AN_EXECUTE_QUERY, evt);

                    DownloadDialog.getInstance().startDownload();
                }
                break;
            default:
                super.buttonAction(buttonIndex, evt);
        }
    }

    /**
     * Saves the latest, successfully parsed search term.
     */
    private void saveHistory() {
        Optional.ofNullable(SearchSetting.fromString(hcbSearchString.getText()))
            .ifPresent(model::addTopElement);
        prefs.save(OVERPASS_WIZARD_HISTORY);
    }

    /**
     * Tries to process a search term using {@link SearchCompilerQueryWizard}. If the term cannot
     * be parsed, the the corresponding dialog is shown.
     * @param searchTerm The search term to parse.
     * @return {@link Optional#empty()} if an exception was thrown when parsing, meaning
     * that the term cannot be processed, or non-empty {@link Optional} containing the result
     * of parsing.
     */
    private Optional<String> tryParseSearchTerm(String searchTerm) {
        try {
            return Optional.of(SearchCompilerQueryWizard.constructQuery(searchTerm));
        } catch (UncheckedParseException | IllegalStateException ex) {
            Logging.error(ex);
            JOptionPane.showMessageDialog(
                    callbacks.getParent(),
                    "<html>" +
                     tr("The Overpass wizard could not parse the following query:") +
                     Utils.joinAsHtmlUnorderedList(Collections.singleton(Utils.escapeReservedCharactersHTML(searchTerm))) +
                     "</html>",
                    tr("Parse error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return Optional.empty();
        }
    }

    /**
     * Builds an Overpass query out from {@link SearchSetting} contents.
     * @return {@code true} if the query successfully built, {@code false} otherwise.
     */
    private boolean buildQueryAction() {
        Optional<String> q = tryParseSearchTerm(getSearchSettings().text);
        q.ifPresent(callbacks::submitWizardResult);
        return q.isPresent();
    }
}
