// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchMode;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.data.osm.search.SearchSetting;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSException;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSelector;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Search dialog to find primitives by a wide range of search criteria.
 * @since 14927 (extracted from {@code SearchAction})
 */
public class SearchDialog extends ExtendedDialog {

    private final SearchSetting searchSettings;

    protected final HistoryComboBox hcbSearchString = new HistoryComboBox();

    private JCheckBox addOnToolbar;
    private JCheckBox caseSensitive;
    private JCheckBox allElements;

    private JRadioButton standardSearch;
    private JRadioButton regexSearch;
    private JRadioButton mapCSSSearch;

    private JRadioButton replace;
    private JRadioButton add;
    private JRadioButton remove;
    private JRadioButton inSelection;

    /**
     * Constructs a new {@code SearchDialog}.
     * @param initialValues initial search settings
     * @param searchExpressionHistory list of all texts that were recently used in the search
     * @param expertMode expert mode
     */
    public SearchDialog(SearchSetting initialValues, List<String> searchExpressionHistory, boolean expertMode) {
        this(initialValues, searchExpressionHistory, new PanelOptions(expertMode, false), MainApplication.getMainFrame(),
                initialValues instanceof Filter ? tr("Filter") : tr("Search"),
                initialValues instanceof Filter ? tr("Submit filter") : tr("Search"),
                tr("Cancel"));
        setButtonIcons("dialogs/search", "cancel");
        configureContextsensitiveHelp("/Action/Search", true /* show help button */);
    }

    protected SearchDialog(SearchSetting initialValues, List<String> searchExpressionHistory, PanelOptions options,
                           Component mainFrame, String title, String... buttonTexts) {
        super(mainFrame, title, buttonTexts);
        this.searchSettings = new SearchSetting(initialValues);
        setContent(buildPanel(searchExpressionHistory, options));
    }

    /**
     * Determines which parts of the search dialog will be shown
     */
    protected static class PanelOptions {
        private final boolean expertMode;
        private final boolean overpassQuery;

        /**
         * Constructs new options which determine which parts of the search dialog will be shown
         * @param expertMode whether export mode is enabled
         * @param overpassQuery whether the panel shall be adapted for Overpass query
         */
        public PanelOptions(boolean expertMode, boolean overpassQuery) {
            this.expertMode = expertMode;
            this.overpassQuery = overpassQuery;
        }
    }

    private JPanel buildPanel(List<String> searchExpressionHistory, PanelOptions options) {

        // prepare the combo box with the search expressions
        JLabel label = new JLabel(searchSettings instanceof Filter ? tr("Filter string:") : tr("Search string:"));

        String tooltip = tr("Enter the search expression");
        hcbSearchString.setText(searchSettings.text);
        hcbSearchString.setToolTipText(tooltip);

        hcbSearchString.setPossibleItemsTopDown(searchExpressionHistory);
        hcbSearchString.setPreferredSize(new Dimension(40, hcbSearchString.getPreferredSize().height));
        label.setLabelFor(hcbSearchString);

        replace = new JRadioButton(tr("select"), searchSettings.mode == SearchMode.replace);
        add = new JRadioButton(tr("add to selection"), searchSettings.mode == SearchMode.add);
        remove = new JRadioButton(tr("remove from selection"), searchSettings.mode == SearchMode.remove);
        inSelection = new JRadioButton(tr("find in selection"), searchSettings.mode == SearchMode.in_selection);
        ButtonGroup bg = new ButtonGroup();
        bg.add(replace);
        bg.add(add);
        bg.add(remove);
        bg.add(inSelection);

        caseSensitive = new JCheckBox(tr("case sensitive"), searchSettings.caseSensitive);
        allElements = new JCheckBox(tr("all objects"), searchSettings.allElements);
        allElements.setToolTipText(tr("Also include incomplete and deleted objects in search."));
        addOnToolbar = new JCheckBox(tr("add toolbar button"), false);
        addOnToolbar.setToolTipText(tr("Add a button with this search expression to the toolbar."));

        standardSearch = new JRadioButton(tr("standard"), !searchSettings.regexSearch && !searchSettings.mapCSSSearch);
        regexSearch = new JRadioButton(tr("regular expression"), searchSettings.regexSearch);
        mapCSSSearch = new JRadioButton(tr("MapCSS selector"), searchSettings.mapCSSSearch);

        ButtonGroup bg2 = new ButtonGroup();
        bg2.add(standardSearch);
        bg2.add(regexSearch);
        bg2.add(mapCSSSearch);

        JPanel selectionSettings = new JPanel(new GridBagLayout());
        selectionSettings.setBorder(BorderFactory.createTitledBorder(tr("Results")));
        selectionSettings.add(replace, GBC.eol().anchor(GBC.WEST).fill(GBC.HORIZONTAL));
        selectionSettings.add(add, GBC.eol());
        selectionSettings.add(remove, GBC.eol());
        selectionSettings.add(inSelection, GBC.eop());

        JPanel additionalSettings = new JPanel(new GridBagLayout());
        additionalSettings.setBorder(BorderFactory.createTitledBorder(tr("Options")));
        additionalSettings.add(caseSensitive, GBC.eol().anchor(GBC.WEST).fill(GBC.HORIZONTAL));

        JPanel left = new JPanel(new GridBagLayout());

        left.add(selectionSettings, GBC.eol().fill(GBC.BOTH));
        left.add(additionalSettings, GBC.eol().fill(GBC.BOTH));

        if (options.expertMode) {
            additionalSettings.add(allElements, GBC.eol());
            additionalSettings.add(addOnToolbar, GBC.eop());

            JPanel searchOptions = new JPanel(new GridBagLayout());
            searchOptions.setBorder(BorderFactory.createTitledBorder(tr("Search syntax")));
            searchOptions.add(standardSearch, GBC.eol().anchor(GBC.WEST).fill(GBC.HORIZONTAL));
            searchOptions.add(regexSearch, GBC.eol());
            searchOptions.add(mapCSSSearch, GBC.eol());

            left.add(searchOptions, GBC.eol().fill(GBC.BOTH));
        }

        JPanel right = buildHintsSection(hcbSearchString, options);
        JPanel top = new JPanel(new GridBagLayout());
        top.add(label, GBC.std().insets(0, 0, 5, 0));
        top.add(hcbSearchString, GBC.eol().fill(GBC.HORIZONTAL));

        JTextComponent editorComponent = hcbSearchString.getEditorComponent();
        Document document = editorComponent.getDocument();

        /*
         * Setup the logic to validate the contents of the search text field which is executed
         * every time the content of the field has changed. If the query is incorrect, then
         * the text field is colored red.
         */
        AbstractTextComponentValidator validator = new AbstractTextComponentValidator(editorComponent) {

            @Override
            public void validate() {
                if (!isValid()) {
                    feedbackInvalid(tr("Invalid search expression"));
                } else {
                    feedbackValid(tooltip);
                }
            }

            @Override
            public boolean isValid() {
                try {
                    SearchSetting ss = new SearchSetting();
                    ss.text = hcbSearchString.getText();
                    ss.caseSensitive = caseSensitive.isSelected();
                    ss.regexSearch = regexSearch.isSelected();
                    ss.mapCSSSearch = mapCSSSearch.isSelected();
                    SearchCompiler.compile(ss);
                    return true;
                } catch (SearchParseError | MapCSSException e) {
                    Logging.trace(e);
                    return false;
                }
            }
        };
        document.addDocumentListener(validator);
        ItemListener validateActionListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                validator.validate();
            }
        };
        standardSearch.addItemListener(validateActionListener);
        regexSearch.addItemListener(validateActionListener);
        mapCSSSearch.addItemListener(validateActionListener);

        /*
         * Setup the logic to append preset queries to the search text field according to
         * selected preset by the user. Every query is of the form ' group/sub-group/.../presetName'
         * if the corresponding group of the preset exists, otherwise it is simply ' presetName'.
         */
        TaggingPresetSelector selector = new TaggingPresetSelector(false, false);
        selector.setBorder(BorderFactory.createTitledBorder(tr("Search by preset")));
        selector.setDblClickListener(ev -> setPresetDblClickListener(selector, editorComponent));

        JPanel p = new JPanel(new GridBagLayout());
        p.add(top, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 0));
        if (!options.overpassQuery) {
            p.add(left, GBC.std().anchor(GBC.NORTH).insets(5, 10, 10, 0).fill(GBC.VERTICAL));
        }
        p.add(right, GBC.std().fill(GBC.BOTH).insets(0, 10, 0, 0));
        if (!options.overpassQuery) {
            p.add(selector, GBC.eol().fill(GBC.BOTH).insets(0, 10, 0, 0));
        }

        return p;
    }

    @Override
    protected void buttonAction(int buttonIndex, ActionEvent evt) {
        if (buttonIndex == 0) {
            try {
                SearchSetting ss = new SearchSetting();
                ss.text = hcbSearchString.getText();
                ss.caseSensitive = caseSensitive.isSelected();
                ss.regexSearch = regexSearch.isSelected();
                ss.mapCSSSearch = mapCSSSearch.isSelected();
                SearchCompiler.compile(ss);
                super.buttonAction(buttonIndex, evt);
            } catch (SearchParseError | MapCSSException e) {
                Logging.warn(e);
                String message = Utils.escapeReservedCharactersHTML(e.getMessage()
                        .replace("<html>", "")
                        .replace("</html>", ""));
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        "<html>" + tr("Search expression is not valid: \n\n {0}", message).replace("\n", "<br>") + "</html>",
                        tr("Invalid search expression"),
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            super.buttonAction(buttonIndex, evt);
        }
    }

    /**
     * Returns the search settings chosen by user.
     * @return the search settings chosen by user
     */
    public SearchSetting getSearchSettings() {
        searchSettings.text = hcbSearchString.getText();
        searchSettings.caseSensitive = caseSensitive.isSelected();
        searchSettings.allElements = allElements.isSelected();
        searchSettings.regexSearch = regexSearch.isSelected();
        searchSettings.mapCSSSearch = mapCSSSearch.isSelected();

        if (inSelection.isSelected()) {
            searchSettings.mode = SearchMode.in_selection;
        } else if (replace.isSelected()) {
            searchSettings.mode = SearchMode.replace;
        } else if (add.isSelected()) {
            searchSettings.mode = SearchMode.add;
        } else {
            searchSettings.mode = SearchMode.remove;
        }
        return searchSettings;
    }

    /**
     * Determines if the "add toolbar button" checkbox is selected.
     * @return {@code true} if the "add toolbar button" checkbox is selected
     */
    public boolean isAddOnToolbar() {
        return addOnToolbar.isSelected();
    }

    private static JPanel buildHintsSection(HistoryComboBox hcbSearchString, PanelOptions options) {
        JPanel hintPanel = new JPanel(new GridBagLayout());
        hintPanel.setBorder(BorderFactory.createTitledBorder(tr("Hints")));

        hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("basics"))
                .addKeyword(tr("Baker Street"), null, tr("''Baker'' and ''Street'' in any key"))
                .addKeyword(tr("\"Baker Street\""), "\"\"", tr("''Baker Street'' in any key"))
                .addKeyword("<i>key</i>:<i>valuefragment</i>", null,
                        tr("''valuefragment'' anywhere in ''key''"),
                        trc("search string example", "name:str matches name=Bakerstreet"))
                .addKeyword("-<i>key</i>:<i>valuefragment</i>", null, tr("''valuefragment'' nowhere in ''key''")),
                GBC.eol());
        hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addKeyword("<i>key:</i>", null, tr("matches if ''key'' exists"))
                .addKeyword("<i>key</i>=<i>value</i>", null, tr("''key'' with exactly ''value''"))
                .addKeyword("<i>key</i>~<i>regexp</i>", null, tr("value of ''key'' matching the regular expression ''regexp''"))
                .addKeyword("<i>key</i>=*", null, tr("''key'' with any value"))
                .addKeyword("<i>key</i>=", null, tr("''key'' with empty value"))
                .addKeyword("*=<i>value</i>", null, tr("''value'' in any key"))
                .addKeyword("<i>key</i>><i>value</i>", null, tr("matches if ''key'' is greater than ''value'' (analogously, less than)"))
                .addKeyword("\"key\"=\"value\"", "\"\"=\"\"",
                        tr("to quote operators.<br>Within quoted strings the <b>\"</b> and <b>\\</b> characters need to be escaped " +
                                "by a preceding <b>\\</b> (e.g. <b>\\\"</b> and <b>\\\\</b>)."),
                        trc("search string example", "name=\"Baker Street\""),
                        "\"addr:street\""),
                GBC.eol().anchor(GBC.CENTER));
        hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("combinators"))
                .addKeyword("<i>expr</i> <i>expr</i>", null,
                        tr("logical and (both expressions have to be satisfied)"),
                        trc("search string example", "Baker Street"))
                .addKeyword("<i>expr</i> | <i>expr</i>", "| ", tr("logical or (at least one expression has to be satisfied)"))
                .addKeyword("<i>expr</i> OR <i>expr</i>", "OR ", tr("logical or (at least one expression has to be satisfied)"))
                .addKeyword("-<i>expr</i>", null, tr("logical not"))
                .addKeyword("(<i>expr</i>)", "()", tr("use parenthesis to group expressions")),
                GBC.eol());

        SearchKeywordRow objectHints = new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("objects"))
                .addKeyword("type:node", "type:node ", tr("all nodes"))
                .addKeyword("type:way", "type:way ", tr("all ways"))
                .addKeyword("type:relation", "type:relation ", tr("all relations"));
        if (options.expertMode) {
            objectHints
                .addKeyword("closed", "closed ", tr("all closed ways"))
                .addKeyword("untagged", "untagged ", tr("object without useful tags"));
        }
        hintPanel.add(objectHints, GBC.eol());

        if (options.expertMode) {
            hintPanel.add(new SearchKeywordRow(hcbSearchString)
                    .addKeyword("preset:\"Annotation/Address\"", "preset:\"Annotation/Address\"",
                            tr("all objects that use the address preset"))
                    .addKeyword("preset:\"Geography/Nature/*\"", "preset:\"Geography/Nature/*\"",
                            tr("all objects that use any preset under the Geography/Nature group")),
                    GBC.eol().anchor(GBC.CENTER));
            hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("metadata"))
                .addKeyword("user:", "user:", tr("objects changed by author"),
                        trc("search string example", "user:<i>OSM username</i> (objects with the author <i>OSM username</i>)"),
                        trc("search string example", "user:anonymous (objects without an assigned author)"))
                .addKeyword("id:", "id:", tr("objects with given ID"),
                        trc("search string example", "id:0 (new objects)"))
                .addKeyword("version:", "version:", tr("objects with given version"),
                        trc("search string example", "version:0 (objects without an assigned version)"))
                .addKeyword("changeset:", "changeset:", tr("objects with given changeset ID"),
                        trc("search string example", "changeset:0 (objects without an assigned changeset)"))
                .addKeyword("timestamp:", "timestamp:", tr("objects with last modification timestamp within range"), "timestamp:2012/",
                        "timestamp:2008/2011-02-04T12"),
                GBC.eol());
            hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("properties"))
                .addKeyword("nodes:<i>20-</i>", "nodes:", tr("ways with at least 20 nodes, or relations containing at least 20 nodes"))
                .addKeyword("ways:<i>3-</i>", "ways:", tr("nodes with at least 3 referring ways, or relations containing at least 3 ways"))
                .addKeyword("tags:<i>5-10</i>", "tags:", tr("objects having 5 to 10 tags"))
                .addKeyword("role:", "role:", tr("objects with given role in a relation"))
                .addKeyword("areasize:<i>-100</i>", "areasize:", tr("closed ways with an area of 100 m\u00b2"))
                .addKeyword("waylength:<i>200-</i>", "waylength:", tr("ways with a length of 200 m or more")),
                GBC.eol());
            hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("state"))
                .addKeyword("modified", "modified ", tr("all modified objects"))
                .addKeyword("new", "new ", tr("all new objects"))
                .addKeyword("selected", "selected ", tr("all selected objects"))
                .addKeyword("incomplete", "incomplete ", tr("all incomplete objects"))
                .addKeyword("deleted", "deleted ", tr("all deleted objects (checkbox <b>{0}</b> must be enabled)", tr("all objects"))),
                GBC.eol());
            hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("related objects"))
                .addKeyword("child <i>expr</i>", "child ", tr("all children of objects matching the expression"), "child building")
                .addKeyword("parent <i>expr</i>", "parent ", tr("all parents of objects matching the expression"), "parent bus_stop")
                .addKeyword("hasRole:<i>stop</i>", "hasRole:", tr("relation containing a member of role <i>stop</i>"))
                .addKeyword("role:<i>stop</i>", "role:", tr("objects being part of a relation as role <i>stop</i>"))
                .addKeyword("nth:<i>7</i>", "nth:",
                        tr("n-th member of relation and/or n-th node of way"), "nth:5 (child type:relation)", "nth:-1")
                .addKeyword("nth%:<i>7</i>", "nth%:",
                        tr("every n-th member of relation and/or every n-th node of way"), "nth%:100 (child waterway)"),
                GBC.eol());
            hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("view"))
                .addKeyword("inview", "inview ", tr("objects in current view"))
                .addKeyword("allinview", "allinview ", tr("objects (and all its way nodes / relation members) in current view"))
                .addKeyword("indownloadedarea", "indownloadedarea ", tr("objects in downloaded area"))
                .addKeyword("allindownloadedarea", "allindownloadedarea ",
                        tr("objects (and all its way nodes / relation members) in downloaded area")),
                GBC.eol());
        }
        if (options.overpassQuery) {
            hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("location"))
                .addKeyword("<i>key=value in <u>location</u></i>", null,
                        tr("{0} all objects having {1} as attribute are downloaded.", "<i>tourism=hotel in Berlin</i> -", "'tourism=hotel'"))
                .addKeyword("<i>key=value around <u>location</u></i>", null,
                        tr("{0} all object with the corresponding key/value pair located around Berlin. Note, the default value for radius " +
                                "is set to 1000m, but it can be changed in the generated query.", "<i>tourism=hotel around Berlin</i> -"))
                .addKeyword("<i>key=value in bbox</i>", null,
                        tr("{0} all objects within the current selection that have {1} as attribute.", "<i>tourism=hotel in bbox</i> -",
                                "'tourism=hotel'")),
                GBC.eol());
        }

        return hintPanel;
    }

    /**
     *
     * @param selector Selector component that the user interacts with
     * @param searchEditor Editor for search queries
     */
    private static void setPresetDblClickListener(TaggingPresetSelector selector, JTextComponent searchEditor) {
        TaggingPreset selectedPreset = selector.getSelectedPresetAndUpdateClassification();

        if (selectedPreset == null) {
            return;
        }

        // Make sure that the focus is transferred to the search text field from the selector component
        searchEditor.requestFocusInWindow();

        // In order to make interaction with the search dialog simpler, we make sure that
        // if autocompletion triggers and the text field is not in focus, the correct area is selected.
        // We first request focus and then execute the selection logic.
        // invokeLater allows us to defer the selection until waiting for focus.
        SwingUtilities.invokeLater(() -> {
            int textOffset = searchEditor.getCaretPosition();
            String presetSearchQuery = " preset:" +
                    "\"" + selectedPreset.getRawName() + "\"";
            try {
                searchEditor.getDocument().insertString(textOffset, presetSearchQuery, null);
            } catch (BadLocationException e1) {
                throw new JosmRuntimeException(e1.getMessage(), e1);
            }
        });
    }

    private static class SearchKeywordRow extends JPanel {

        private final HistoryComboBox hcb;

        SearchKeywordRow(HistoryComboBox hcb) {
            super(new FlowLayout(FlowLayout.LEFT));
            this.hcb = hcb;
        }

        /**
         * Adds the title (prefix) label at the beginning of the row. Should be called only once.
         * @param title English title
         * @return {@code this} for easy chaining
         */
        public SearchKeywordRow addTitle(String title) {
            add(new JLabel(tr("{0}: ", title)));
            return this;
        }

        /**
         * Adds an example keyword label at the end of the row. Can be called several times.
         * @param displayText displayed HTML text
         * @param insertText optional: if set, makes the label clickable, and {@code insertText} will be inserted in search string
         * @param description optional: HTML text to be displayed in the tooltip
         * @param examples optional: examples joined as HTML list in the tooltip
         * @return {@code this} for easy chaining
         */
        public SearchKeywordRow addKeyword(String displayText, final String insertText, String description, String... examples) {
            JLabel label = new JLabel("<html>"
                    + "<style>td{border:1px solid gray; font-weight:normal;}</style>"
                    + "<table><tr><td>" + displayText + "</td></tr></table></html>");
            add(label);
            if (description != null || examples.length > 0) {
                label.setToolTipText("<html>"
                        + description
                        + (examples.length > 0 ? Utils.joinAsHtmlUnorderedList(Arrays.asList(examples)) : "")
                        + "</html>");
            }
            if (insertText != null) {
                label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                label.addMouseListener(new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        JTextComponent tf = hcb.getEditorComponent();

                        // Make sure that the focus is transferred to the search text field from the selector component
                        if (!tf.hasFocus()) {
                            tf.requestFocusInWindow();
                        }

                        // In order to make interaction with the search dialog simpler, we make sure that
                        // if autocompletion triggers and the text field is not in focus, the correct area is selected.
                        // We first request focus and then execute the selection logic.
                        // invokeLater allows us to defer the selection until waiting for focus.
                        SwingUtilities.invokeLater(() -> {
                            try {
                                tf.getDocument().insertString(tf.getCaretPosition(), ' ' + insertText, null);
                            } catch (BadLocationException ex) {
                                throw new JosmRuntimeException(ex.getMessage(), ex);
                            }
                        });
                    }
                });
            }
            return this;
        }
    }
}
