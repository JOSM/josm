// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

/**
 * Help browser super interface.
 * @since 9644
 */
interface IHelpBrowser {

    /**
     * Replies the current URL.
     * @return the current URL
     */
    String getUrl();

    /**
     * Sets the current URL.
     * @param url the current URL
     * @since 14807
     */
    void setUrl(String url);

    /**
     * Replies the browser history.
     * @return the browser history
     */
    HelpBrowserHistory getHistory();

    /**
     * Loads and displays the help information for a help topic given
     * by a relative help topic name, i.e. "/Action/New".
     *
     * @param relativeHelpTopic the relative help topic
     */
    void openHelpTopic(String relativeHelpTopic);

    /**
     * Opens an URL and displays the content.
     *
     * If the URL is the locator of an absolute help topic, help content is loaded from
     * the JOSM wiki. Otherwise, the help browser loads the page from the given URL.
     *
     * @param url the url
     */
    void openUrl(String url);
}
