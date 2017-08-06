// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.search;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

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

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ActionParameter;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.ParameterizedAction;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSException;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences.ActionParser;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSelector;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * The search action allows the user to search the data layer using a complex search string.
 *
 * @see SearchCompiler
 */
public class SearchAction extends JosmAction implements ParameterizedAction {

    /**
     * The default size of the search history
     */
    public static final int DEFAULT_SEARCH_HISTORY_SIZE = 15;
    /**
     * Maximum number of characters before the search expression is shortened for display purposes.
     */
    public static final int MAX_LENGTH_SEARCH_EXPRESSION_DISPLAY = 100;

    private static final String SEARCH_EXPRESSION = "searchExpression";

    /**
     * Search mode.
     */
    public enum SearchMode {
        /** replace selection */
        replace('R'),
        /** add to selection */
        add('A'),
        /** remove from selection */
        remove('D'),
        /** find in selection */
        in_selection('S');

        private final char code;

        SearchMode(char code) {
            this.code = code;
        }

        /**
         * Returns the unique character code of this mode.
         * @return the unique character code of this mode
         */
        public char getCode() {
            return code;
        }

        /**
         * Returns the search mode matching the given character code.
         * @param code character code
         * @return search mode matching the given character code
         */
        public static SearchMode fromCode(char code) {
            for (SearchMode mode: values()) {
                if (mode.getCode() == code)
                    return mode;
            }
            return null;
        }
    }

    private static final LinkedList<SearchSetting> searchHistory = new LinkedList<>();
    static {
        for (String s: Main.pref.getCollection("search.history", Collections.<String>emptyList())) {
            SearchSetting ss = SearchSetting.readFromString(s);
            if (ss != null) {
                searchHistory.add(ss);
            }
        }
    }

    /**
     * Gets the search history
     * @return The last searched terms. Do not modify it.
     */
    public static Collection<SearchSetting> getSearchHistory() {
        return searchHistory;
    }

    /**
     * Saves a search to the search history.
     * @param s The search to save
     */
    public static void saveToHistory(SearchSetting s) {
        if (searchHistory.isEmpty() || !s.equals(searchHistory.getFirst())) {
            searchHistory.addFirst(new SearchSetting(s));
        } else if (searchHistory.contains(s)) {
            // move existing entry to front, fixes #8032 - search history loses entries when re-using queries
            searchHistory.remove(s);
            searchHistory.addFirst(new SearchSetting(s));
        }
        int maxsize = Main.pref.getInteger("search.history-size", DEFAULT_SEARCH_HISTORY_SIZE);
        while (searchHistory.size() > maxsize) {
            searchHistory.removeLast();
        }
        Set<String> savedHistory = new LinkedHashSet<>(searchHistory.size());
        for (SearchSetting item: searchHistory) {
            savedHistory.add(item.writeToString());
        }
        Main.pref.putCollection("search.history", savedHistory);
    }

    /**
     * Gets a list of all texts that were recently used in the search
     * @return The list of search texts.
     */
    public static List<String> getSearchExpressionHistory() {
        List<String> ret = new ArrayList<>(getSearchHistory().size());
        for (SearchSetting ss: getSearchHistory()) {
            ret.add(ss.text);
        }
        return ret;
    }

    private static volatile SearchSetting lastSearch;

    /**
     * Constructs a new {@code SearchAction}.
     */
    public SearchAction() {
        super(tr("Search..."), "dialogs/search", tr("Search for objects."),
                Shortcut.registerShortcut("system:find", tr("Search..."), KeyEvent.VK_F, Shortcut.CTRL), true);
        putValue("help", ht("/Action/Search"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        search();
    }

    @Override
    public void actionPerformed(ActionEvent e, Map<String, Object> parameters) {
        if (parameters.get(SEARCH_EXPRESSION) == null) {
            actionPerformed(e);
        } else {
            searchWithoutHistory((SearchSetting) parameters.get(SEARCH_EXPRESSION));
        }
    }

    private static class SearchKeywordRow extends JPanel {

        private final HistoryComboBox hcb;

        SearchKeywordRow(HistoryComboBox hcb) {
            super(new FlowLayout(FlowLayout.LEFT));
            this.hcb = hcb;
        }

        public SearchKeywordRow addTitle(String title) {
            add(new JLabel(tr("{0}: ", title)));
            return this;
        }

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

                        /*
                         * Make sure that the focus is transferred to the search text field
                         * from the selector component.
                         */
                        if (!tf.hasFocus()) {
                            tf.requestFocusInWindow();
                        }

                        /*
                         * In order to make interaction with the search dialog simpler,
                         * we make sure that if autocompletion triggers and the text field is
                         * not in focus, the correct area is selected. We first request focus
                         * and then execute the selection logic. invokeLater allows us to
                         * defer the selection until waiting for focus.
                         */
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

    /**
     * Builds and shows the search dialog.
     * @param initialValues A set of initial values needed in order to initialize the search dialog.
     *                      If is {@code null}, then default settings are used.
     * @return Returns {@link SearchAction} object containing parameters of the search.
     */
    public static SearchSetting showSearchDialog(SearchSetting initialValues) {
        if (initialValues == null) {
            initialValues = new SearchSetting();
        }

        // prepare the combo box with the search expressions
        JLabel label = new JLabel(initialValues instanceof Filter ? tr("Filter string:") : tr("Search string:"));
        HistoryComboBox hcbSearchString = new HistoryComboBox();
        String tooltip = tr("Enter the search expression");
        hcbSearchString.setText(initialValues.text);
        hcbSearchString.setToolTipText(tooltip);

        // we have to reverse the history, because ComboBoxHistory will reverse it again in addElement()
        List<String> searchExpressionHistory = getSearchExpressionHistory();
        Collections.reverse(searchExpressionHistory);
        hcbSearchString.setPossibleItems(searchExpressionHistory);
        hcbSearchString.setPreferredSize(new Dimension(40, hcbSearchString.getPreferredSize().height));
        label.setLabelFor(hcbSearchString);

        JRadioButton replace = new JRadioButton(tr("replace selection"), initialValues.mode == SearchMode.replace);
        JRadioButton add = new JRadioButton(tr("add to selection"), initialValues.mode == SearchMode.add);
        JRadioButton remove = new JRadioButton(tr("remove from selection"), initialValues.mode == SearchMode.remove);
        JRadioButton inSelection = new JRadioButton(tr("find in selection"), initialValues.mode == SearchMode.in_selection);
        ButtonGroup bg = new ButtonGroup();
        bg.add(replace);
        bg.add(add);
        bg.add(remove);
        bg.add(inSelection);

        JCheckBox caseSensitive = new JCheckBox(tr("case sensitive"), initialValues.caseSensitive);
        JCheckBox allElements = new JCheckBox(tr("all objects"), initialValues.allElements);
        allElements.setToolTipText(tr("Also include incomplete and deleted objects in search."));
        JCheckBox addOnToolbar = new JCheckBox(tr("add toolbar button"), false);

        JRadioButton standardSearch = new JRadioButton(tr("standard"), !initialValues.regexSearch && !initialValues.mapCSSSearch);
        JRadioButton regexSearch = new JRadioButton(tr("regular expression"), initialValues.regexSearch);
        JRadioButton mapCSSSearch = new JRadioButton(tr("MapCSS selector"), initialValues.mapCSSSearch);
        ButtonGroup bg2 = new ButtonGroup();
        bg2.add(standardSearch);
        bg2.add(regexSearch);
        bg2.add(mapCSSSearch);

        JPanel selectionSettings = new JPanel(new GridBagLayout());
        selectionSettings.setBorder(BorderFactory.createTitledBorder(tr("Selection settings")));
        selectionSettings.add(replace, GBC.eol().anchor(GBC.WEST).fill(GBC.HORIZONTAL));
        selectionSettings.add(add, GBC.eol());
        selectionSettings.add(remove, GBC.eol());
        selectionSettings.add(inSelection, GBC.eop());

        JPanel additionalSettings = new JPanel(new GridBagLayout());
        additionalSettings.setBorder(BorderFactory.createTitledBorder(tr("Additional settings")));
        additionalSettings.add(caseSensitive, GBC.eol().anchor(GBC.WEST).fill(GBC.HORIZONTAL));

        JPanel left = new JPanel(new GridBagLayout());

        left.add(selectionSettings, GBC.eol().fill(GBC.BOTH));
        left.add(additionalSettings, GBC.eol().fill(GBC.BOTH));

        if (ExpertToggleAction.isExpert()) {
            additionalSettings.add(allElements, GBC.eol());
            additionalSettings.add(addOnToolbar, GBC.eop());

            JPanel searchOptions = new JPanel(new GridBagLayout());
            searchOptions.setBorder(BorderFactory.createTitledBorder(tr("Search syntax")));
            searchOptions.add(standardSearch, GBC.eol().anchor(GBC.WEST).fill(GBC.HORIZONTAL));
            searchOptions.add(regexSearch, GBC.eol());
            searchOptions.add(mapCSSSearch, GBC.eol());

            left.add(searchOptions, GBC.eol().fill(GBC.BOTH));
        }

        JPanel right = SearchAction.buildHintsSection(hcbSearchString);
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
        document.addDocumentListener(new AbstractTextComponentValidator(editorComponent) {

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
                } catch (ParseError | MapCSSException e) {
                    return false;
                }
            }
        });

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
        p.add(left, GBC.std().anchor(GBC.NORTH).insets(5, 10, 10, 0).fill(GBC.VERTICAL));
        p.add(right, GBC.std().fill(GBC.BOTH).insets(0, 10, 0, 0));
        p.add(selector, GBC.eol().fill(GBC.BOTH).insets(0, 10, 0, 0));

        ExtendedDialog dialog = new ExtendedDialog(
                Main.parent,
                initialValues instanceof Filter ? tr("Filter") : tr("Search"),
                initialValues instanceof Filter ? tr("Submit filter") : tr("Start Search"),
                tr("Cancel")
        ) {
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
                    } catch (ParseError e) {
                        Main.debug(e);
                        JOptionPane.showMessageDialog(
                                Main.parent,
                                tr("Search expression is not valid: \n\n {0}", e.getMessage()),
                                tr("Invalid search expression"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    super.buttonAction(buttonIndex, evt);
                }
            }
        };
        dialog.setButtonIcons("dialogs/search", "cancel");
        dialog.configureContextsensitiveHelp("/Action/Search", true /* show help button */);
        dialog.setContent(p);

        if (dialog.showDialog().getValue() != 1) return null;

        // User pressed OK - let's perform the search
        initialValues.text = hcbSearchString.getText();
        initialValues.caseSensitive = caseSensitive.isSelected();
        initialValues.allElements = allElements.isSelected();
        initialValues.regexSearch = regexSearch.isSelected();
        initialValues.mapCSSSearch = mapCSSSearch.isSelected();

        if (inSelection.isSelected()) {
            initialValues.mode = SearchAction.SearchMode.in_selection;
        } else if (replace.isSelected()) {
            initialValues.mode = SearchAction.SearchMode.replace;
        } else if (add.isSelected()) {
            initialValues.mode = SearchAction.SearchMode.add;
        } else {
            initialValues.mode = SearchAction.SearchMode.remove;
        }

        if (addOnToolbar.isSelected()) {
            ToolbarPreferences.ActionDefinition aDef =
                    new ToolbarPreferences.ActionDefinition(Main.main.menu.search);
            aDef.getParameters().put(SEARCH_EXPRESSION, initialValues);
            // Display search expression as tooltip instead of generic one
            aDef.setName(Utils.shortenString(initialValues.text, MAX_LENGTH_SEARCH_EXPRESSION_DISPLAY));
            // parametrized action definition is now composed
            ActionParser actionParser = new ToolbarPreferences.ActionParser(null);
            String res = actionParser.saveAction(aDef);

            // add custom search button to toolbar preferences
            Main.toolbar.addCustomButton(res, -1, false);
        }

        return initialValues;
    }

    private static JPanel buildHintsSection(HistoryComboBox hcbSearchString) {
        JPanel hintPanel = new JPanel(new GridBagLayout());
        hintPanel.setBorder(BorderFactory.createTitledBorder(tr("Search hints")));

        hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("basics"))
                .addKeyword(tr("Baker Street"), null, tr("''Baker'' and ''Street'' in any key"))
                .addKeyword(tr("\"Baker Street\""), "\"\"", tr("''Baker Street'' in any key"))
                .addKeyword("<i>key</i>:<i>valuefragment</i>", null,
                        tr("''valuefragment'' anywhere in ''key''"), "name:str matches name=Bakerstreet")
                .addKeyword("-<i>key</i>:<i>valuefragment</i>", null, tr("''valuefragment'' nowhere in ''key''")),
                GBC.eol());
        hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addKeyword("<i>key</i>=<i>value</i>", null, tr("''key'' with exactly ''value''"))
                .addKeyword("<i>key</i>=*", null, tr("''key'' with any value"))
                .addKeyword("*=<i>value</i>", null, tr("''value'' in any key"))
                .addKeyword("<i>key</i>=", null, tr("matches if ''key'' exists"))
                .addKeyword("<i>key</i>><i>value</i>", null, tr("matches if ''key'' is greater than ''value'' (analogously, less than)"))
                .addKeyword("\"key\"=\"value\"", "\"\"=\"\"",
                        tr("to quote operators.<br>Within quoted strings the <b>\"</b> and <b>\\</b> characters need to be escaped " +
                                "by a preceding <b>\\</b> (e.g. <b>\\\"</b> and <b>\\\\</b>)."),
                        "\"addr:street\""),
                GBC.eol().anchor(GBC.CENTER));
        hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("combinators"))
                .addKeyword("<i>expr</i> <i>expr</i>", null, tr("logical and (both expressions have to be satisfied)"))
                .addKeyword("<i>expr</i> | <i>expr</i>", "| ", tr("logical or (at least one expression has to be satisfied)"))
                .addKeyword("<i>expr</i> OR <i>expr</i>", "OR ", tr("logical or (at least one expression has to be satisfied)"))
                .addKeyword("-<i>expr</i>", null, tr("logical not"))
                .addKeyword("(<i>expr</i>)", "()", tr("use parenthesis to group expressions")),
                GBC.eol());

        if (ExpertToggleAction.isExpert()) {
            hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("objects"))
                .addKeyword("type:node", "type:node ", tr("all nodes"))
                .addKeyword("type:way", "type:way ", tr("all ways"))
                .addKeyword("type:relation", "type:relation ", tr("all relations"))
                .addKeyword("closed", "closed ", tr("all closed ways"))
                .addKeyword("untagged", "untagged ", tr("object without useful tags")),
                GBC.eol());
            hintPanel.add(new SearchKeywordRow(hcbSearchString)
                    .addKeyword("preset:\"Annotation/Address\"", "preset:\"Annotation/Address\"",
                            tr("all objects that use the address preset"))
                    .addKeyword("preset:\"Geography/Nature/*\"", "preset:\"Geography/Nature/*\"",
                            tr("all objects that use any preset under the Geography/Nature group")),
                    GBC.eol().anchor(GBC.CENTER));
            hintPanel.add(new SearchKeywordRow(hcbSearchString)
                .addTitle(tr("metadata"))
                .addKeyword("user:", "user:", tr("objects changed by user", "user:anonymous"))
                .addKeyword("id:", "id:", tr("objects with given ID"), "id:0 (new objects)")
                .addKeyword("version:", "version:", tr("objects with given version"), "version:0 (objects without an assigned version)")
                .addKeyword("changeset:", "changeset:", tr("objects with given changeset ID"),
                        "changeset:0 (objects without an assigned changeset)")
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

        return hintPanel;
    }

    /**
     * Launches the dialog for specifying search criteria and runs a search
     */
    public static void search() {
        SearchSetting se = showSearchDialog(lastSearch);
        if (se != null) {
            searchWithHistory(se);
        }
    }

    /**
     * Adds the search specified by the settings in <code>s</code> to the
     * search history and performs the search.
     *
     * @param s search settings
     */
    public static void searchWithHistory(SearchSetting s) {
        saveToHistory(s);
        lastSearch = new SearchSetting(s);
        search(s);
    }

    /**
     * Performs the search specified by the settings in <code>s</code> without saving it to search history.
     *
     * @param s search settings
     */
    public static void searchWithoutHistory(SearchSetting s) {
        lastSearch = new SearchSetting(s);
        search(s);
    }

    /**
     * Performs the search specified by the search string {@code search} and the search mode {@code mode}.
     *
     * @param search the search string to use
     * @param mode the search mode to use
     */
    public static void search(String search, SearchMode mode) {
        final SearchSetting searchSetting = new SearchSetting();
        searchSetting.text = search;
        searchSetting.mode = mode;
        search(searchSetting);
    }

    static void search(SearchSetting s) {
        SearchTask.newSearchTask(s, new SelectSearchReceiver()).run();
    }

    /**
     * Performs the search specified by the search string {@code search} and the search mode {@code mode} and returns the result of the search.
     *
     * @param search the search string to use
     * @param mode the search mode to use
     * @return The result of the search.
     * @since 10457
     */
    public static Collection<OsmPrimitive> searchAndReturn(String search, SearchMode mode) {
        final SearchSetting searchSetting = new SearchSetting();
        searchSetting.text = search;
        searchSetting.mode = mode;
        CapturingSearchReceiver receiver = new CapturingSearchReceiver();
        SearchTask.newSearchTask(searchSetting, receiver).run();
        return receiver.result;
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

        /*
         * Make sure that the focus is transferred to the search text field
         * from the selector component.
         */
        searchEditor.requestFocusInWindow();

        /*
         * In order to make interaction with the search dialog simpler,
         * we make sure that if autocompletion triggers and the text field is
         * not in focus, the correct area is selected. We first request focus
         * and then execute the selection logic. invokeLater allows us to
         * defer the selection until waiting for focus.
         */
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

    /**
     * Interfaces implementing this may receive the result of the current search.
     * @author Michael Zangl
     * @since 10457
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    interface SearchReceiver {
        /**
         * Receive the search result
         * @param ds The data set searched on.
         * @param result The result collection, including the initial collection.
         * @param foundMatches The number of matches added to the result.
         * @param setting The setting used.
         */
        void receiveSearchResult(DataSet ds, Collection<OsmPrimitive> result, int foundMatches, SearchSetting setting);
    }

    /**
     * Select the search result and display a status text for it.
     */
    private static class SelectSearchReceiver implements SearchReceiver {

        @Override
        public void receiveSearchResult(DataSet ds, Collection<OsmPrimitive> result, int foundMatches, SearchSetting setting) {
            ds.setSelected(result);
            if (foundMatches == 0) {
                final String msg;
                final String text = Utils.shortenString(setting.text, MAX_LENGTH_SEARCH_EXPRESSION_DISPLAY);
                if (setting.mode == SearchMode.replace) {
                    msg = tr("No match found for ''{0}''", text);
                } else if (setting.mode == SearchMode.add) {
                    msg = tr("Nothing added to selection by searching for ''{0}''", text);
                } else if (setting.mode == SearchMode.remove) {
                    msg = tr("Nothing removed from selection by searching for ''{0}''", text);
                } else if (setting.mode == SearchMode.in_selection) {
                    msg = tr("Nothing found in selection by searching for ''{0}''", text);
                } else {
                    msg = null;
                }
                if (Main.map != null) {
                    Main.map.statusLine.setHelpText(msg);
                }
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            msg,
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            } else {
                Main.map.statusLine.setHelpText(tr("Found {0} matches", foundMatches));
            }
        }
    }

    /**
     * This class stores the result of the search in a local variable.
     * @author Michael Zangl
     */
    private static final class CapturingSearchReceiver implements SearchReceiver {
        private Collection<OsmPrimitive> result;

        @Override
        public void receiveSearchResult(DataSet ds, Collection<OsmPrimitive> result, int foundMatches,
                SearchSetting setting) {
                    this.result = result;
        }
    }

    static final class SearchTask extends PleaseWaitRunnable {
        private final DataSet ds;
        private final SearchSetting setting;
        private final Collection<OsmPrimitive> selection;
        private final Predicate<OsmPrimitive> predicate;
        private boolean canceled;
        private int foundMatches;
        private final SearchReceiver resultReceiver;

        private SearchTask(DataSet ds, SearchSetting setting, Collection<OsmPrimitive> selection, Predicate<OsmPrimitive> predicate,
                SearchReceiver resultReceiver) {
            super(tr("Searching"));
            this.ds = ds;
            this.setting = setting;
            this.selection = selection;
            this.predicate = predicate;
            this.resultReceiver = resultReceiver;
        }

        static SearchTask newSearchTask(SearchSetting setting, SearchReceiver resultReceiver) {
            final DataSet ds = Main.getLayerManager().getEditDataSet();
            return newSearchTask(setting, ds, resultReceiver);
        }

        /**
         * Create a new search task for the given search setting.
         * @param setting The setting to use
         * @param ds The data set to search on
         * @param resultReceiver will receive the search result
         * @return A new search task.
         */
        private static SearchTask newSearchTask(SearchSetting setting, final DataSet ds, SearchReceiver resultReceiver) {
            final Collection<OsmPrimitive> selection = new HashSet<>(ds.getAllSelected());
            return new SearchTask(ds, setting, selection, ds::isSelected, resultReceiver);
        }

        @Override
        protected void cancel() {
            this.canceled = true;
        }

        @Override
        protected void realRun() {
            try {
                foundMatches = 0;
                SearchCompiler.Match matcher = SearchCompiler.compile(setting);

                if (setting.mode == SearchMode.replace) {
                    selection.clear();
                } else if (setting.mode == SearchMode.in_selection) {
                    foundMatches = selection.size();
                }

                Collection<OsmPrimitive> all;
                if (setting.allElements) {
                    all = ds.allPrimitives();
                } else {
                    all = ds.getPrimitives(OsmPrimitive::isSelectable);
                }
                final ProgressMonitor subMonitor = getProgressMonitor().createSubTaskMonitor(all.size(), false);
                subMonitor.beginTask(trn("Searching in {0} object", "Searching in {0} objects", all.size(), all.size()));

                for (OsmPrimitive osm : all) {
                    if (canceled) {
                        return;
                    }
                    if (setting.mode == SearchMode.replace) {
                        if (matcher.match(osm)) {
                            selection.add(osm);
                            ++foundMatches;
                        }
                    } else if (setting.mode == SearchMode.add && !predicate.test(osm) && matcher.match(osm)) {
                        selection.add(osm);
                        ++foundMatches;
                    } else if (setting.mode == SearchMode.remove && predicate.test(osm) && matcher.match(osm)) {
                        selection.remove(osm);
                        ++foundMatches;
                    } else if (setting.mode == SearchMode.in_selection && predicate.test(osm) && !matcher.match(osm)) {
                        selection.remove(osm);
                        --foundMatches;
                    }
                    subMonitor.worked(1);
                }
                subMonitor.finishTask();
            } catch (ParseError e) {
                Main.debug(e);
                JOptionPane.showMessageDialog(
                        Main.parent,
                        e.getMessage(),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }

        @Override
        protected void finish() {
            if (canceled) {
                return;
            }
            resultReceiver.receiveSearchResult(ds, selection, foundMatches, setting);
        }
    }

    /**
     * This class defines a set of parameters that is used to
     * perform search within the search dialog.
     */
    public static class SearchSetting {
        public String text;
        public SearchMode mode;
        public boolean caseSensitive;
        public boolean regexSearch;
        public boolean mapCSSSearch;
        public boolean allElements;

        /**
         * Constructs a new {@code SearchSetting}.
         */
        public SearchSetting() {
            text = "";
            mode = SearchMode.replace;
        }

        /**
         * Constructs a new {@code SearchSetting} from an existing one.
         * @param original original search settings
         */
        public SearchSetting(SearchSetting original) {
            text = original.text;
            mode = original.mode;
            caseSensitive = original.caseSensitive;
            regexSearch = original.regexSearch;
            mapCSSSearch = original.mapCSSSearch;
            allElements = original.allElements;
        }

        @Override
        public String toString() {
            String cs = caseSensitive ?
                    /*case sensitive*/  trc("search", "CS") :
                        /*case insensitive*/  trc("search", "CI");
            String rx = regexSearch ? ", " +
                            /*regex search*/ trc("search", "RX") : "";
            String css = mapCSSSearch ? ", " +
                            /*MapCSS search*/ trc("search", "CSS") : "";
            String all = allElements ? ", " +
                            /*all elements*/ trc("search", "A") : "";
            return '"' + text + "\" (" + cs + rx + css + all + ", " + mode + ')';
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            SearchSetting that = (SearchSetting) other;
            return caseSensitive == that.caseSensitive &&
                    regexSearch == that.regexSearch &&
                    mapCSSSearch == that.mapCSSSearch &&
                    allElements == that.allElements &&
                    mode == that.mode &&
                    Objects.equals(text, that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, mode, caseSensitive, regexSearch, mapCSSSearch, allElements);
        }

        /**
         * <p>Transforms a string following a certain format, namely "[R | A | D | S][C?,R?,A?,M?] [a-zA-Z]"
         * where the first part defines the mode of the search, see {@link SearchMode}, the second defines
         * a set of attributes within the {@code SearchSetting} class and the second is the search query.
         * <p>
         * Attributes are as follows:
         * <ul>
         *     <li>C - if search is case sensitive
         *     <li>R - if the regex syntax is used
         *     <li>A - if all objects are considered
         *     <li>M - if the mapCSS syntax is used
         * </ul>
         * <p>For example, "RC type:node" is a valid string representation of an object that replaces the
         * current selection, is case sensitive and searches for all objects of type node.
         * @param s A string representation of a {@code SearchSetting} object
         *          from which the object must be built.
         * @return A {@code SearchSetting} defined by the input string.
         */
        public static SearchSetting readFromString(String s) {
            if (s.isEmpty())
                return null;

            SearchSetting result = new SearchSetting();

            int index = 1;

            result.mode = SearchMode.fromCode(s.charAt(0));
            if (result.mode == null) {
                result.mode = SearchMode.replace;
                index = 0;
            }

            while (index < s.length()) {
                if (s.charAt(index) == 'C') {
                    result.caseSensitive = true;
                } else if (s.charAt(index) == 'R') {
                    result.regexSearch = true;
                } else if (s.charAt(index) == 'A') {
                    result.allElements = true;
                } else if (s.charAt(index) == 'M') {
                    result.mapCSSSearch = true;
                } else if (s.charAt(index) == ' ') {
                    break;
                } else {
                    Main.warn("Unknown char in SearchSettings: " + s);
                    break;
                }
                index++;
            }

            if (index < s.length() && s.charAt(index) == ' ') {
                index++;
            }

            result.text = s.substring(index);

            return result;
        }

        /**
         * Builds a string representation of the {@code SearchSetting} object,
         * see {@link #readFromString(String)} for more details.
         * @return A string representation of the {@code SearchSetting} object.
         */
        public String writeToString() {
            if (text == null || text.isEmpty())
                return "";

            StringBuilder result = new StringBuilder();
            result.append(mode.getCode());
            if (caseSensitive) {
                result.append('C');
            }
            if (regexSearch) {
                result.append('R');
            }
            if (mapCSSSearch) {
                result.append('M');
            }
            if (allElements) {
                result.append('A');
            }
            result.append(' ')
                  .append(text);
            return result.toString();
        }
    }

    /**
     * {@link ActionParameter} implementation with {@link SearchSetting} as value type.
     * @since 12547 (moved from {@link ActionParameter})
     */
    public static class SearchSettingsActionParameter extends ActionParameter<SearchSetting> {

        /**
         * Constructs a new {@code SearchSettingsActionParameter}.
         * @param name parameter name (the key)
         */
        public SearchSettingsActionParameter(String name) {
            super(name);
        }

        @Override
        public Class<SearchSetting> getType() {
            return SearchSetting.class;
        }

        @Override
        public SearchSetting readFromString(String s) {
            return SearchSetting.readFromString(s);
        }

        @Override
        public String writeToString(SearchSetting value) {
            if (value == null)
                return "";
            return value.writeToString();
        }
    }

    /**
     * Refreshes the enabled state
     *
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditLayer() != null);
    }

    @Override
    public List<ActionParameter<?>> getActionParameters() {
        return Collections.<ActionParameter<?>>singletonList(new SearchSettingsActionParameter(SEARCH_EXPRESSION));
    }
}
