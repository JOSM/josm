// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.search;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences.ActionParser;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ActionParameter;
import org.openstreetmap.josm.actions.ActionParameter.SearchSettingsActionParameter;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.ParameterizedAction;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Property;
import org.openstreetmap.josm.tools.Shortcut;

public class SearchAction extends JosmAction implements ParameterizedAction {

    public static final int DEFAULT_SEARCH_HISTORY_SIZE = 15;

    private static final String SEARCH_EXPRESSION = "searchExpression";

    public static enum SearchMode {
        replace('R'), add('A'), remove('D'), in_selection('S');

        private final char code;

        SearchMode(char code) {
            this.code = code;
        }

        public char getCode() {
            return code;
        }

        public static SearchMode fromCode(char code) {
            for (SearchMode mode: values()) {
                if (mode.getCode() == code)
                    return mode;
            }
            return null;
        }
    }

    private static LinkedList<SearchSetting> searchHistory = null;

    public static Collection<SearchSetting> getSearchHistory() {
        if (searchHistory == null) {
            searchHistory = new LinkedList<SearchSetting>();
            for (String s: Main.pref.getCollection("search.history", Collections.<String>emptyList())) {
                SearchSetting ss = SearchSetting.readFromString(s);
                if (ss != null) {
                    searchHistory.add(ss);
                }
            }
        }

        return searchHistory;
    }

    public static void saveToHistory(SearchSetting s) {
        if(searchHistory.isEmpty() || !s.equals(searchHistory.getFirst())) {
            searchHistory.addFirst(new SearchSetting(s));
        }
        int maxsize = Main.pref.getInteger("search.history-size", DEFAULT_SEARCH_HISTORY_SIZE);
        while (searchHistory.size() > maxsize) {
            searchHistory.removeLast();
        }
        List<String> savedHistory = new ArrayList<String>();
        for (SearchSetting item: searchHistory) {
            savedHistory.add(item.writeToString());
        }
        Main.pref.putCollection("search.history", savedHistory);
    }

    public static List<String> getSearchExpressionHistory() {
        ArrayList<String> ret = new ArrayList<String>(getSearchHistory().size());
        for (SearchSetting ss: getSearchHistory()) {
            ret.add(ss.text);
        }
        return ret;
    }

    private static SearchSetting lastSearch = null;

    public SearchAction() {
        super(tr("Search..."), "dialogs/search", tr("Search for objects."),
                Shortcut.registerShortcut("system:find", tr("Search..."), KeyEvent.VK_F, Shortcut.GROUP_HOTKEY), true);
        putValue("help", ht("/Action/Search"));
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        search();
    }

    public void actionPerformed(ActionEvent e, Map<String, Object> parameters) {
        if (parameters.get(SEARCH_EXPRESSION) == null) {
            actionPerformed(e);
        } else {
            searchWithoutHistory((SearchSetting) parameters.get(SEARCH_EXPRESSION));
        }
    }

    private static class DescriptionTextBuilder {

        StringBuilder s = new StringBuilder(4096);

        public StringBuilder append(String string) {
            return s.append(string);
        }

        StringBuilder appendItem(String item) {
            return append("<li>").append(item).append("</li>\n");
        }

        StringBuilder appendItemHeader(String itemHeader) {
            return append("<li class=\"header\">").append(itemHeader).append("</li>\n");
        }

        @Override
        public String toString() {
            return s.toString();
        }
    }

    public static SearchSetting showSearchDialog(SearchSetting initialValues) {
        if (initialValues == null) {
            initialValues = new SearchSetting();
        }
        // -- prepare the combo box with the search expressions
        //
        JLabel label = new JLabel( initialValues instanceof Filter ? tr("Filter string:") : tr("Search string:"));
        final HistoryComboBox hcbSearchString = new HistoryComboBox();
        hcbSearchString.setText(initialValues.text);
        hcbSearchString.getEditor().selectAll();
        hcbSearchString.getEditor().getEditorComponent().requestFocusInWindow();
        hcbSearchString.setToolTipText(tr("Enter the search expression"));
        // we have to reverse the history, because ComboBoxHistory will reverse it again
        // in addElement()
        //
        List<String> searchExpressionHistory = getSearchExpressionHistory();
        Collections.reverse(searchExpressionHistory);
        hcbSearchString.setPossibleItems(searchExpressionHistory);
        hcbSearchString.setPreferredSize(new Dimension(40, hcbSearchString.getPreferredSize().height));

        JRadioButton replace = new JRadioButton(tr("replace selection"), initialValues.mode == SearchMode.replace);
        JRadioButton add = new JRadioButton(tr("add to selection"), initialValues.mode == SearchMode.add);
        JRadioButton remove = new JRadioButton(tr("remove from selection"), initialValues.mode == SearchMode.remove);
        JRadioButton in_selection = new JRadioButton(tr("find in selection"), initialValues.mode == SearchMode.in_selection);
        ButtonGroup bg = new ButtonGroup();
        bg.add(replace);
        bg.add(add);
        bg.add(remove);
        bg.add(in_selection);

        final JCheckBox caseSensitive = new JCheckBox(tr("case sensitive"), initialValues.caseSensitive);
        JCheckBox allElements = new JCheckBox(tr("all objects"), initialValues.allElements);
        allElements.setToolTipText(tr("Also include incomplete and deleted objects in search."));
        final JCheckBox regexSearch   = new JCheckBox(tr("regular expression"), initialValues.regexSearch);
        final JCheckBox addOnToolbar  = new JCheckBox(tr("Add toolbar button"), false); 

        JPanel top = new JPanel(new GridBagLayout());
        top.add(label, GBC.std().insets(0, 0, 5, 0));
        top.add(hcbSearchString, GBC.eol().fill(GBC.HORIZONTAL));
        JPanel left = new JPanel(new GridBagLayout());
        left.add(replace, GBC.eol());
        left.add(add, GBC.eol());
        left.add(remove, GBC.eol());
        left.add(in_selection, GBC.eop());
        left.add(caseSensitive, GBC.eol());
        if(Main.pref.getBoolean("expert", false))
        {
            left.add(allElements, GBC.eol());
            left.add(regexSearch, GBC.eol());
            left.add(addOnToolbar, GBC.eol()); 
        }

        JPanel right = new JPanel();
        DescriptionTextBuilder descriptionText = new DescriptionTextBuilder();
        descriptionText.append("<html><style>li.header{font-size:110%; list-style-type:none; margin-top:5px;}</style><ul>");
        descriptionText.appendItem(tr("<b>Baker Street</b> - ''Baker'' and ''Street'' in any key"));
        descriptionText.appendItem(tr("<b>\"Baker Street\"</b> - ''Baker Street'' in any key"));
        descriptionText.appendItem(tr("<b>key:Bak</b> - ''Bak'' anywhere in the key ''key''"));
        descriptionText.appendItem(tr("<b>-key:Bak</b> - ''Bak'' nowhere in the key ''key''"));
        descriptionText.appendItem(tr("<b>key=value</b> - key ''key'' with value exactly ''value''"));
        descriptionText.appendItem(tr("<b>key=*</b> - key ''key'' with any value. Try also <b>*=value</b>, <b>key=</b>, <b>*=*</b>, <b>*=</b>"));
        descriptionText.appendItem(tr("<b>key:</b> - key ''key'' set to any value"));
        descriptionText.appendItem(tr("<b>key?</b> - key ''key'' with the value ''yes'', ''true'', ''1'' or ''on''"));
        if(Main.pref.getBoolean("expert", false))
        {
            descriptionText.appendItemHeader(tr("Special targets"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>type:</b>... - objects with corresponding type (<b>node</b>, <b>way</b>, <b>relation</b>)"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>user:</b>... - objects changed by user"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>user:anonymous</b> - objects changed by anonymous users"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>id:</b>... - objects with given ID (0 for new objects)"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>version:</b>... - objects with given version (0 objects without an assigned version)"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>changeset:</b>... - objects with given changeset ID (0 objects without an assigned changeset)"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>nodes:</b>... - objects with given number of nodes (<b>nodes:</b>count, <b>nodes:</b>min-max, <b>nodes:</b>min- or <b>nodes:</b>-max)"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>tags:</b>... - objects with given number of tags (<b>tags:</b>count, <b>tags:</b>min-max, <b>tags:</b>min- or <b>tags:</b>-max)"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>role:</b>... - objects with given role in a relation"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>timestamp:</b>... - objects with this timestamp (2009-11-12T14:51:09Z, 2009-11-12 or T14:51 ...)"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>areasize:</b>... - closed ways with given area in m\u00b2 (<b>areasize:</b>min-max or <b>areasize:</b>max)"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>modified</b> - all changed objects"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>selected</b> - all selected objects"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>incomplete</b> - all incomplete objects"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>untagged</b> - all untagged objects"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>closed</b> - all closed ways (a node is not considered closed)"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>child <i>expr</i></b> - all children of objects matching the expression"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>parent <i>expr</i></b> - all parents of objects matching the expression"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>(all)indownloadedarea</b> - objects (and all its way nodes / relation members) in downloaded area"));
            /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("<b>(all)inview</b> - objects (and all its way nodes / relation members) in current view"));
        }
        /* I18n: don't translate the bold text keyword */ descriptionText.appendItem(tr("Use <b>|</b> or <b>OR</b> to combine with logical or"));
        descriptionText.appendItem(tr("Use <b>\"</b> to quote operators (e.g. if key contains <b>:</b>)")
                + "<br/>"
                + tr("Within quoted strings the <b>\"</b> and <b>\\</b> characters need to be escaped by a preceding <b>\\</b> (e.g. <b>\\\"</b> and <b>\\\\</b>)."));
        descriptionText.appendItem(tr("Use <b>(</b> and <b>)</b> to group expressions"));
        descriptionText.append("</ul></html>");
        JLabel description = new JLabel(descriptionText.toString());
        description.setFont(description.getFont().deriveFont(Font.PLAIN));
        right.add(description);

        final JPanel p = new JPanel(new GridBagLayout());
        p.add(top, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 0));
        p.add(left, GBC.std().anchor(GBC.NORTH).insets(5, 10, 10, 0));
        p.add(right, GBC.eol());
        ExtendedDialog dialog = new ExtendedDialog(
                Main.parent,
                initialValues instanceof Filter ? tr("Filter") : tr("Search"),
                        new String[] {
                    initialValues instanceof Filter ? tr("Submit filter") : tr("Start Search"),
                            tr("Cancel")}
        ) {
            @Override
            protected void buttonAction(int buttonIndex, ActionEvent evt) {
                if (buttonIndex == 0) {
                    try {
                        SearchCompiler.compile(hcbSearchString.getText(), caseSensitive.isSelected(), regexSearch.isSelected());
                        super.buttonAction(buttonIndex, evt);
                    } catch (ParseError e) {
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
        dialog.setButtonIcons(new String[] {"dialogs/search.png", "cancel.png"});
        dialog.configureContextsensitiveHelp("/Action/Search", true /* show help button */);
        dialog.setContent(p);
        dialog.showDialog();
        int result = dialog.getValue();

        if(result != 1) return null;

        // User pressed OK - let's perform the search
        SearchMode mode = replace.isSelected() ? SearchAction.SearchMode.replace
                : (add.isSelected() ? SearchAction.SearchMode.add
                        : (remove.isSelected() ? SearchAction.SearchMode.remove : SearchAction.SearchMode.in_selection));
        initialValues.text = hcbSearchString.getText();
        initialValues.mode = mode;
        initialValues.caseSensitive = caseSensitive.isSelected();
        initialValues.allElements = allElements.isSelected();
        initialValues.regexSearch = regexSearch.isSelected();
        
        if (addOnToolbar.isSelected()) {
            ToolbarPreferences.ActionDefinition aDef = 
                    new ToolbarPreferences.ActionDefinition(Main.main.menu.search);
            aDef.getParameters().put("searchExpression", initialValues);
            // parametrized action definition is now composed
            ActionParser actionParser = new ToolbarPreferences.ActionParser(null);
            String res = actionParser.saveAction(aDef);
            
            Collection<String> t = new LinkedList<String>(Main.pref.getCollection("toolbar"));
            if (t!=null) {
                // add custom search button to toolbar preferences
                if (!t.contains(res)) t.add(res);
                Main.pref.putCollection("toolbar", t);
                Main.toolbar.refreshToolbarControl();
            }
        }
        return initialValues;
    }

    /**
     * Launches the dialog for specifying search criteria and runs
     * a search
     */
    public static void search() {
        SearchSetting se = showSearchDialog(lastSearch);
        if(se != null) {
            searchWithHistory(se);
        }
    }

    /**
     * Adds the search specified by the settings in <code>s</code> to the
     * search history and performs the search.
     *
     * @param s
     */
    public static void searchWithHistory(SearchSetting s) {
        saveToHistory(s);
        lastSearch = new SearchSetting(s);
        search(s);
    }

    public static void searchWithoutHistory(SearchSetting s) {
        lastSearch = new SearchSetting(s);
        search(s);
    }

    public interface Function{
        public Boolean isSomething(OsmPrimitive o);
    }

    public static int getSelection(SearchSetting s, Collection<OsmPrimitive> sel, Function f) {
        int foundMatches = 0;
        try {
            String searchText = s.text;
            SearchCompiler.Match matcher = SearchCompiler.compile(searchText, s.caseSensitive, s.regexSearch);

            if (s.mode == SearchMode.replace) {
                sel.clear();
            }

            Collection<OsmPrimitive> all;
            if(s.allElements) {
                all = Main.main.getCurrentDataSet().allPrimitives();
            } else {
                all = Main.main.getCurrentDataSet().allNonDeletedCompletePrimitives();
            }
            for (OsmPrimitive osm : all) {
                if (s.mode == SearchMode.replace) {
                    if (matcher.match(osm)) {
                        sel.add(osm);
                        ++foundMatches;
                    }
                } else if (s.mode == SearchMode.add && !f.isSomething(osm) && matcher.match(osm)) {
                    sel.add(osm);
                    ++foundMatches;
                } else if (s.mode == SearchMode.remove && f.isSomething(osm) && matcher.match(osm)) {
                    sel.remove(osm);
                    ++foundMatches;
                } else if (s.mode == SearchMode.in_selection &&  f.isSomething(osm)&& !matcher.match(osm)) {
                    sel.remove(osm);
                    ++foundMatches;
                }
            }
        } catch (SearchCompiler.ParseError e) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    e.getMessage(),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE

            );
        }
        return foundMatches;
    }

    /**
     * Version of getSelection that is customized for filter, but should
     * also work in other context.
     *
     * @param s the search settings
     * @param all the collection of all the primitives that should be considered
     * @param p the property that should be set/unset if something is found
     */
    public static void getSelection(SearchSetting s, Collection<OsmPrimitive> all, Property<OsmPrimitive, Boolean> p) {
        try {
            String searchText = s.text;
            if (s instanceof Filter && ((Filter)s).inverted) {
                searchText = String.format("-(%s)", searchText);
            }
            SearchCompiler.Match matcher = SearchCompiler.compile(searchText, s.caseSensitive, s.regexSearch);

            for (OsmPrimitive osm : all) {
                if (s.mode == SearchMode.replace) {
                    if (matcher.match(osm)) {
                        p.set(osm, true);
                    } else {
                        p.set(osm, false);
                    }
                } else if (s.mode == SearchMode.add && !p.get(osm) && matcher.match(osm)) {
                    p.set(osm, true);
                } else if (s.mode == SearchMode.remove && p.get(osm) && matcher.match(osm)) {
                    p.set(osm, false);
                } else if (s.mode == SearchMode.in_selection && p.get(osm) && !matcher.match(osm)) {
                    p.set(osm, false);
                }
            }
        } catch (SearchCompiler.ParseError e) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    e.getMessage(),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE

            );
        }
    }

    public static void search(String search, SearchMode mode) {
        search(new SearchSetting(search, mode, false, false, false));
    }

    public static void search(SearchSetting s) {
        // FIXME: This is confusing. The GUI says nothing about loading primitives from an URL. We'd like to *search*
        // for URLs in the current data set.
        // Disabling until a better solution is in place
        //
        //        if (search.startsWith("http://") || search.startsWith("ftp://") || search.startsWith("https://")
        //                || search.startsWith("file:/")) {
        //            SelectionWebsiteLoader loader = new SelectionWebsiteLoader(search, mode);
        //            if (loader.url != null && loader.url.getHost() != null) {
        //                Main.worker.execute(loader);
        //                return;
        //            }
        //        }

        final DataSet ds = Main.main.getCurrentDataSet();
        Collection<OsmPrimitive> sel = new HashSet<OsmPrimitive>(ds.getSelected());
        int foundMatches = getSelection(s, sel, new Function(){
            public Boolean isSomething(OsmPrimitive o){
                return ds.isSelected(o);
            }
        });
        ds.setSelected(sel);
        if (foundMatches == 0) {
            String msg = null;
            if (s.mode == SearchMode.replace) {
                msg = tr("No match found for ''{0}''", s.text);
            } else if (s.mode == SearchMode.add) {
                msg = tr("Nothing added to selection by searching for ''{0}''", s.text);
            } else if (s.mode == SearchMode.remove) {
                msg = tr("Nothing removed from selection by searching for ''{0}''", s.text);
            } else if (s.mode == SearchMode.in_selection) {
                msg = tr("Nothing found in selection by searching for ''{0}''", s.text);
            }
            Main.map.statusLine.setHelpText(msg);
            JOptionPane.showMessageDialog(
                    Main.parent,
                    msg,
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
        } else {
            Main.map.statusLine.setHelpText(tr("Found {0} matches", foundMatches));
        }
    }

    public static class SearchSetting {
        public String text;
        public SearchMode mode;
        public boolean caseSensitive;
        public boolean regexSearch;
        public boolean allElements;

        public SearchSetting() {
            this("", SearchMode.replace, false /* case insensitive */,
                    false /* no regexp */, false /* only useful primitives */);
        }

        public SearchSetting(String text, SearchMode mode, boolean caseSensitive,
                boolean regexSearch, boolean allElements) {
            this.caseSensitive = caseSensitive;
            this.regexSearch = regexSearch;
            this.allElements = allElements;
            this.mode = mode;
            this.text = text;
        }

        public SearchSetting(SearchSetting original) {
            this(original.text, original.mode, original.caseSensitive,
                    original.regexSearch, original.allElements);
        }

        @Override
        public String toString() {
            String cs = caseSensitive ?
                    /*case sensitive*/  trc("search", "CS") :
                        /*case insensitive*/  trc("search", "CI");
                    String rx = regexSearch ? (", " +
                            /*regex search*/ trc("search", "RX")) : "";
                    String all = allElements ? (", " +
                            /*all elements*/ trc("search", "A")) : "";
                    return "\"" + text + "\" (" + cs + rx + all + ", " + mode + ")";
        }

        @Override
        public boolean equals(Object other) {
            if(!(other instanceof SearchSetting))
                return false;
            SearchSetting o = (SearchSetting) other;
            return (o.caseSensitive == this.caseSensitive
                    && o.regexSearch == this.regexSearch
                    && o.allElements == this.allElements
                    && o.mode.equals(this.mode)
                    && o.text.equals(this.text));
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }

        public static SearchSetting readFromString(String s) {
            if (s.length() == 0)
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
                } else if (s.charAt(index) == ' ') {
                    break;
                } else {
                    System.out.println("Unknown char in SearchSettings: " + s);
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

        public String writeToString() {
            if (text == null || text.length() == 0)
                return "";

            StringBuilder result = new StringBuilder();
            result.append(mode.getCode());
            if (caseSensitive) {
                result.append('C');
            }
            if (regexSearch) {
                result.append('R');
            }
            if (allElements) {
                result.append('A');
            }
            result.append(' ');
            result.append(text);
            return result.toString();
        }
    }

    /**
     * Refreshes the enabled state
     *
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }

    public List<ActionParameter<?>> getActionParameters() {
        return Collections.<ActionParameter<?>>singletonList(new SearchSettingsActionParameter(SEARCH_EXPRESSION));
    }

    public static String escapeStringForSearch(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
