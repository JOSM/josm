// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.gui.util.ChangeNotifier;

/**
 * Help browser history.
 * @since 2274
 */
public class HelpBrowserHistory extends ChangeNotifier {
    private final IHelpBrowser browser;
    private List<String> history;
    private int historyPos;

    /**
     * Constructs a new {@code HelpBrowserHistory}.
     * @param browser help browser
     */
    public HelpBrowserHistory(IHelpBrowser browser) {
        this.browser = browser;
        history = new ArrayList<>();
    }

    /**
     * Clears the history.
     */
    public void clear() {
        history.clear();
        historyPos = 0;
        fireStateChanged();
    }

    /**
     * Determines if the help browser can go back.
     * @return {@code true} if a previous history position exists
     */
    public boolean canGoBack() {
        return historyPos > 0;
    }

    /**
     * Determines if the help browser can go forward.
     * @return {@code true} if a following history position exists
     */
    public boolean canGoForward() {
        return historyPos + 1 < history.size();
    }

    /**
     * Go back.
     */
    public void back() {
        historyPos--;
        if (historyPos < 0)
            return;
        String url = history.get(historyPos);
        browser.openUrl(url);
        fireStateChanged();
    }

    /**
     * Go forward.
     */
    public void forward() {
        historyPos++;
        if (historyPos >= history.size())
            return;
        String url = history.get(historyPos);
        browser.openUrl(url);
        fireStateChanged();
    }

    /**
     * Remembers the new current URL.
     * @param url the new current URL
     */
    public void setCurrentUrl(String url) {
        if (url != null) {
            boolean add = true;

            if (historyPos >= 0 && historyPos < history.size() && history.get(historyPos).equals(url)) {
                add = false;
            } else if (historyPos == history.size() -1) {
                // do nothing just append
            } else if (historyPos == 0 && !history.isEmpty()) {
                history = new ArrayList<>(Collections.singletonList(history.get(0)));
            } else if (historyPos < history.size() -1 && historyPos > 0) {
                history = new ArrayList<>(history.subList(0, historyPos));
            } else {
                history = new ArrayList<>();
            }
            if (add) {
                history.add(url);
                historyPos = history.size()-1;
            }
            fireStateChanged();
        }
    }
}
