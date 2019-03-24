// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.search;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.ActionParameter;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.ParameterizedAction;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.search.PushbackTokenizer;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.SimpleMatchFactory;
import org.openstreetmap.josm.data.osm.search.SearchMode;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.data.osm.search.SearchSetting;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.SearchDialog;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences.ActionParser;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * The search action allows the user to search the data layer using a complex search string.
 *
 * @see SearchCompiler
 * @see SearchDialog
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

    private static final LinkedList<SearchSetting> searchHistory = new LinkedList<>();
    static {
        SearchCompiler.addMatchFactory(new SimpleMatchFactory() {
            @Override
            public Collection<String> getKeywords() {
                return Arrays.asList("inview", "allinview");
            }

            @Override
            public Match get(String keyword, boolean caseSensitive, boolean regexSearch, PushbackTokenizer tokenizer) throws SearchParseError {
                switch(keyword) {
                case "inview":
                    return new InView(false);
                case "allinview":
                    return new InView(true);
                default:
                    throw new IllegalStateException("Not expecting keyword " + keyword);
                }
            }
        });

        for (String s: Config.getPref().getList("search.history", Collections.<String>emptyList())) {
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
        int maxsize = Config.getPref().getInt("search.history-size", DEFAULT_SEARCH_HISTORY_SIZE);
        while (searchHistory.size() > maxsize) {
            searchHistory.removeLast();
        }
        Set<String> savedHistory = new LinkedHashSet<>(searchHistory.size());
        for (SearchSetting item: searchHistory) {
            savedHistory.add(item.writeToString());
        }
        Config.getPref().putList("search.history", new ArrayList<>(savedHistory));
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
        super(tr("Search..."), "dialogs/search", tr("Search for objects"),
                Shortcut.registerShortcut("system:find", tr("Search..."), KeyEvent.VK_F, Shortcut.CTRL), true);
        setHelpId(ht("/Action/Search"));
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

        SearchDialog dialog = new SearchDialog(
                initialValues, getSearchExpressionHistory(), ExpertToggleAction.isExpert());

        if (dialog.showDialog().getValue() != 1) return null;

        // User pressed OK - let's perform the search
        SearchSetting searchSettings = dialog.getSearchSettings();

        if (dialog.isAddOnToolbar()) {
            ToolbarPreferences.ActionDefinition aDef =
                    new ToolbarPreferences.ActionDefinition(MainApplication.getMenu().search);
            aDef.getParameters().put(SEARCH_EXPRESSION, searchSettings);
            // Display search expression as tooltip instead of generic one
            aDef.setName(Utils.shortenString(searchSettings.text, MAX_LENGTH_SEARCH_EXPRESSION_DISPLAY));
            // parametrized action definition is now composed
            ActionParser actionParser = new ToolbarPreferences.ActionParser(null);
            String res = actionParser.saveAction(aDef);

            // add custom search button to toolbar preferences
            MainApplication.getToolbar().addCustomButton(res, -1, false);
        }

        return searchSettings;
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
     * @since 13950 (signature)
     */
    public static Collection<IPrimitive> searchAndReturn(String search, SearchMode mode) {
        final SearchSetting searchSetting = new SearchSetting();
        searchSetting.text = search;
        searchSetting.mode = mode;
        CapturingSearchReceiver receiver = new CapturingSearchReceiver();
        SearchTask.newSearchTask(searchSetting, receiver).run();
        return receiver.result;
    }

    /**
     * Interfaces implementing this may receive the result of the current search.
     * @author Michael Zangl
     * @since 10457
     * @since 10600 (functional interface)
     * @since 13950 (signature)
     */
    @FunctionalInterface
    interface SearchReceiver {
        /**
         * Receive the search result
         * @param ds The data set searched on.
         * @param result The result collection, including the initial collection.
         * @param foundMatches The number of matches added to the result.
         * @param setting The setting used.
         * @param parent parent component
         */
        void receiveSearchResult(OsmData<?, ?, ?, ?> ds, Collection<IPrimitive> result,
                int foundMatches, SearchSetting setting, Component parent);
    }

    /**
     * Select the search result and display a status text for it.
     */
    private static class SelectSearchReceiver implements SearchReceiver {

        @Override
        public void receiveSearchResult(OsmData<?, ?, ?, ?> ds, Collection<IPrimitive> result,
                int foundMatches, SearchSetting setting, Component parent) {
            ds.setSelected(result);
            MapFrame map = MainApplication.getMap();
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
                if (map != null) {
                    map.statusLine.setHelpText(msg);
                }
                if (!GraphicsEnvironment.isHeadless()) {
                    new Notification(msg).show();
                }
            } else {
                map.statusLine.setHelpText(tr("Found {0} matches", foundMatches));
            }
        }
    }

    /**
     * This class stores the result of the search in a local variable.
     * @author Michael Zangl
     */
    private static final class CapturingSearchReceiver implements SearchReceiver {
        private Collection<IPrimitive> result;

        @Override
        public void receiveSearchResult(OsmData<?, ?, ?, ?> ds, Collection<IPrimitive> result, int foundMatches,
                SearchSetting setting, Component parent) {
                    this.result = result;
        }
    }

    static final class SearchTask extends PleaseWaitRunnable {
        private final OsmData<?, ?, ?, ?> ds;
        private final SearchSetting setting;
        private final Collection<IPrimitive> selection;
        private final Predicate<IPrimitive> predicate;
        private boolean canceled;
        private int foundMatches;
        private final SearchReceiver resultReceiver;

        private SearchTask(OsmData<?, ?, ?, ?> ds, SearchSetting setting, Collection<IPrimitive> selection,
                Predicate<IPrimitive> predicate, SearchReceiver resultReceiver) {
            super(tr("Searching"));
            this.ds = ds;
            this.setting = setting;
            this.selection = selection;
            this.predicate = predicate;
            this.resultReceiver = resultReceiver;
        }

        static SearchTask newSearchTask(SearchSetting setting, SearchReceiver resultReceiver) {
            final OsmData<?, ?, ?, ?> ds = MainApplication.getLayerManager().getActiveData();
            if (ds == null) {
                throw new IllegalStateException("No active dataset");
            }
            return newSearchTask(setting, ds, resultReceiver);
        }

        /**
         * Create a new search task for the given search setting.
         * @param setting The setting to use
         * @param ds The data set to search on
         * @param resultReceiver will receive the search result
         * @return A new search task.
         */
        private static SearchTask newSearchTask(SearchSetting setting, final OsmData<?, ?, ?, ?> ds, SearchReceiver resultReceiver) {
            final Collection<IPrimitive> selection = new HashSet<>(ds.getAllSelected());
            return new SearchTask(ds, setting, selection, IPrimitive::isSelected, resultReceiver);
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

                Collection<? extends IPrimitive> all;
                if (setting.allElements) {
                    all = ds.allPrimitives();
                } else {
                    all = ds.getPrimitives(p -> p.isSelectable()); // Do not use method reference before Java 11!
                }
                final ProgressMonitor subMonitor = getProgressMonitor().createSubTaskMonitor(all.size(), false);
                subMonitor.beginTask(trn("Searching in {0} object", "Searching in {0} objects", all.size(), all.size()));

                for (IPrimitive osm : all) {
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
            } catch (SearchParseError e) {
                Logging.debug(e);
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
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
            resultReceiver.receiveSearchResult(ds, selection, foundMatches, setting, getProgressMonitor().getWindowParent());
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
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getActiveData() != null);
    }

    @Override
    public List<ActionParameter<?>> getActionParameters() {
        return Collections.<ActionParameter<?>>singletonList(new SearchSettingsActionParameter(SEARCH_EXPRESSION));
    }
}
