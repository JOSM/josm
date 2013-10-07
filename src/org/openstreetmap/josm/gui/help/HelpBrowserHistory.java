// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

public class HelpBrowserHistory extends Observable {
    private HelpBrowser browser;
    private List<String> history;
    private int historyPos = 0;

    public HelpBrowserHistory(HelpBrowser browser) {
        this.browser = browser;
        history = new ArrayList<String>();
    }

    public void clear() {
        history.clear();
        historyPos = 0;
        setChanged();
        notifyObservers();
    }

    public boolean canGoBack() {
        return historyPos > 0;
    }

    public boolean canGoForward() {
        return historyPos + 1 < history.size();
    }

    public void back() {
        historyPos--;
        if (historyPos < 0) return;
        String url = history.get(historyPos);
        browser.openUrl(url);
        setChanged();
        notifyObservers();
    }

    public void forward() {
        historyPos++;
        if (historyPos >= history.size()) return;
        String url = history.get(historyPos);
        browser.openUrl(url);
        setChanged();
        notifyObservers();
    }

    public void setCurrentUrl(String url) {
        boolean add=true;

        if (historyPos >= 0 && historyPos < history.size() && history.get(historyPos).equals(url)) {
            add = false;
        } else if (historyPos == history.size() -1) {
            // do nothing just append
        } else if (historyPos ==0 && history.size() > 0) {
            history = new ArrayList<String>(Collections.singletonList(history.get(0)));
        } else if (historyPos < history.size() -1 && historyPos > 0) {
            history = new ArrayList<String>(history.subList(0, historyPos));
        } else {
            history = new ArrayList<String>();
        }
        if(add)
        {
            history.add(url);
            historyPos = history.size()-1;
        }
        setChanged();
        notifyObservers();
    }
}
