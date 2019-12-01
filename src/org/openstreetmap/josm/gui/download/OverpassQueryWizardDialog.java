// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.download.overpass.OverpassWizardRegistration.OverpassWizardCallbacks;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.OverpassTurboQueryWizard;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.Utils;

/**
 * This dialog provides an easy and fast way to create an overpass query.
 * @since 12576
 * @since 12652: Moved here
 */
public final class OverpassQueryWizardDialog extends ExtendedDialog {

    private final HistoryComboBox queryWizard;
    private static final String HEADLINE_START = "<h3>";
    private static final String HEADLINE_END = "</h3>";
    private static final String TR_START = "<tr>";
    private static final String TR_END = "</tr>";
    private static final String TD_START = "<td>";
    private static final String TD_END = "</td>";
    private static final String SPAN_START = "<span>";
    private static final String SPAN_END = "</span>";
    private static final ListProperty OVERPASS_WIZARD_HISTORY =
            new ListProperty("download.overpass.wizard", new ArrayList<String>());
    private final transient OverpassTurboQueryWizard overpassQueryBuilder;

    // dialog buttons
    private static final int BUILD_QUERY = 0;
    private static final int BUILD_AN_EXECUTE_QUERY = 1;
    private static final int CANCEL = 2;

    private static final String DESCRIPTION_STYLE =
            "<style type=\"text/css\">\n"
            + "table { border-spacing: 0pt;}\n"
            + "h3 {text-align: center; padding: 8px;}\n"
            + "td {border: 1px solid #dddddd; text-align: left; padding: 8px;}\n"
            + "#desc {width: 350px;}"
            + "</style>\n";

    private final OverpassWizardCallbacks dsPanel;

    /**
     * Create a new {@link OverpassQueryWizardDialog}
     * @param callbacks The Overpass download source panel.
     */
    public OverpassQueryWizardDialog(OverpassWizardCallbacks callbacks) {
        super(callbacks.getParent(), tr("Overpass Turbo Query Wizard"),
                tr("Build query"), tr("Build query and execute"), tr("Cancel"));
        this.dsPanel = callbacks;

        this.queryWizard = new HistoryComboBox();
        this.overpassQueryBuilder = OverpassTurboQueryWizard.getInstance();

        JPanel panel = new JPanel(new GridBagLayout());

        JLabel searchLabel = new JLabel(tr("Search:"));
        JTextComponent descPane = buildDescriptionSection();
        JScrollPane scroll = GuiHelper.embedInVerticalScrollPane(descPane);
        scroll.getVerticalScrollBar().setUnitIncrement(10); // make scrolling smooth

        panel.add(searchLabel, GBC.std().insets(0, 0, 0, 20).anchor(GBC.SOUTHEAST));
        panel.add(queryWizard, GBC.eol().insets(0, 0, 0, 15).fill(GBC.HORIZONTAL).anchor(GBC.SOUTH));
        panel.add(scroll, GBC.eol().fill(GBC.BOTH).anchor(GBC.CENTER));

        List<String> items = new ArrayList<>(OVERPASS_WIZARD_HISTORY.get());
        if (!items.isEmpty()) {
            queryWizard.setText(items.get(0));
        }
        queryWizard.setPossibleItemsTopDown(items);

        setCancelButton(CANCEL + 1);
        setDefaultButton(BUILD_AN_EXECUTE_QUERY + 1);
        setContent(panel, false);
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
        queryWizard.addCurrentItemToHistory();
        OVERPASS_WIZARD_HISTORY.put(queryWizard.getHistory());
    }

    /**
     * Tries to process a search term using {@link OverpassTurboQueryWizard}. If the term cannot
     * be parsed, the the corresponding dialog is shown.
     * @param searchTerm The search term to parse.
     * @return {@link Optional#empty()} if an exception was thrown when parsing, meaning
     * that the term cannot be processed, or non-empty {@link Optional} containing the result
     * of parsing.
     */
    private Optional<String> tryParseSearchTerm(String searchTerm) {
        try {
            return Optional.of(overpassQueryBuilder.constructQuery(searchTerm));
        } catch (UncheckedParseException | IllegalStateException ex) {
            Logging.error(ex);
            JOptionPane.showMessageDialog(
                    dsPanel.getParent(),
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
     * Builds an Overpass query out from {@link OverpassQueryWizardDialog#queryWizard} contents.
     * @return {@code true} if the query successfully built, {@code false} otherwise.
     */
    private boolean buildQueryAction() {
        final String wizardSearchTerm = this.queryWizard.getText();

        Optional<String> q = this.tryParseSearchTerm(wizardSearchTerm);
        q.ifPresent(dsPanel::submitWizardResult);
        return q.isPresent();
    }

    private static JTextComponent buildDescriptionSection() {
        JEditorPane descriptionSection = new JEditorPane("text/html", getDescriptionContent());
        descriptionSection.setEditable(false);
        descriptionSection.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                OpenBrowser.displayUrl(e.getURL().toString());
            }
        });

        return descriptionSection;
    }

    private static String getDescriptionContent() {
        return new StringBuilder("<html>")
                .append(DESCRIPTION_STYLE)
                .append("<body>")
                .append(HEADLINE_START)
                .append(tr("Query Wizard"))
                .append(HEADLINE_END)
                .append("<p>")
                .append(tr("Allows you to interact with <i>Overpass API</i> by writing declarative, human-readable terms."))
                .append(tr("The <i>Query Wizard</i> tool will transform those to a valid overpass query."))
                .append(tr("For more detailed description see "))
                .append(tr("<a href=\"{0}\">OSM Wiki</a>.", Config.getUrls().getOSMWebsite() + "/wiki/Overpass_turbo/Wizard"))
                .append("</p>")
                .append(HEADLINE_START).append(tr("Hints")).append(HEADLINE_END)
                .append("<table>").append(TR_START).append(TD_START)
                .append(Utils.joinAsHtmlUnorderedList(Arrays.asList("<i>type:node</i>", "<i>type:relation</i>", "<i>type:way</i>")))
                .append(TD_END).append(TD_START)
                .append(SPAN_START).append(tr("Download objects of a certain type.")).append(SPAN_END)
                .append(TD_END).append(TR_END)
                .append(TR_START).append(TD_START)
                .append(Utils.joinAsHtmlUnorderedList(
                        Arrays.asList("<i>key=value in <u>location</u></i>",
                                "<i>key=value around <u>location</u></i>",
                                "<i>key=value in bbox</i>")))
                .append(TD_END).append(TD_START)
                .append(tr("Download object by specifying a specific location. For example,"))
                .append(Utils.joinAsHtmlUnorderedList(Arrays.asList(
                        tr("{0} all objects having {1} as attribute are downloaded.", "<i>tourism=hotel in Berlin</i> -", "'tourism=hotel'"),
                        tr("{0} all object with the corresponding key/value pair located around Berlin. Note, the default value for radius "+
                                "is set to 1000m, but it can be changed in the generated query.", "<i>tourism=hotel around Berlin</i> -"),
                        tr("{0} all objects within the current selection that have {1} as attribute.", "<i>tourism=hotel in bbox</i> -",
                                "'tourism=hotel'"))))
                .append(SPAN_START)
                .append(tr("Instead of <i>location</i> any valid place name can be used like address, city, etc."))
                .append(SPAN_END)
                .append(TD_END).append(TR_END)
                .append(TR_START).append(TD_START)
                .append(Utils.joinAsHtmlUnorderedList(Arrays.asList("<i>key=value</i>", "<i>key=*</i>", "<i>key~regex</i>",
                        "<i>key!=value</i>", "<i>key!~regex</i>", "<i>key=\"combined value\"</i>")))
                .append(TD_END).append(TD_START)
                .append(tr("<span>Download objects that have some concrete key/value pair, only the key with any contents for the value, " +
                        "the value matching some regular expression. \"Not equal\" operators are supported as well.</span>"))
                .append(TD_END).append(TR_END)
                .append(TR_START).append(TD_START)
                .append(Utils.joinAsHtmlUnorderedList(Arrays.asList(
                        tr("<i>expression1 {0} expression2</i>", "or"),
                        tr("<i>expression1 {0} expression2</i>", "and"))))
                .append(TD_END).append(TD_START)
                .append(SPAN_START)
                .append(tr("Basic logical operators can be used to create more sophisticated queries. Instead of \"or\" - \"|\", \"||\" " +
                        "can be used, and instead of \"and\" - \"&\", \"&&\"."))
                .append(SPAN_END)
                .append(TD_END).append(TR_END)
                .append(TR_START).append(TD_START)
                .append(Utils.joinAsHtmlUnorderedList(Arrays.asList(
                        tr("<i>ref ~ \"[0-9]+\"</i>"), tr("<i>name ~ /postnord/i in sweden</i>"))))
                .append(TD_END).append(TD_START)
                .append(SPAN_START)
                .append(tr("Regular expressions can be provided either as plain strings or with the regex notation. " +
                        "The modifier \"i\" makes the match case-insensitive"))
                .append(SPAN_END)
                .append(TD_END).append(TR_END)
                .append("</table>")
                .append("</body>")
                .append("</html>")
                .toString();
    }
}
