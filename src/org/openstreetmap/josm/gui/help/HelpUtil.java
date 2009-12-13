// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.help;

import java.awt.Component;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.HelpAction;
import org.openstreetmap.josm.tools.LanguageInfo;

public class HelpUtil {

    /**
     * Replies the base wiki URL.
     *
     * @return the base wiki URL
     */
    static public String getWikiBaseUrl() {
        return Main.pref.get("help.baseurl", "http://josm.openstreetmap.de");
    }

    /**
     * Replies the base wiki URL for help pages
     *
     * @return the base wiki URL for help pages
     */
    static public String getWikiBaseHelpUrl() {
        return getWikiBaseUrl() + "/wiki";
    }

    /**
     * Replies the URL on the wiki for an absolute help topic. The URL is encoded in UTF-8.
     *
     * @param absoluteHelpTopic the absolute help topic
     * @return the url
     * @see #buildAbsoluteHelpTopic(String)
     * @see #buildAbsoluteHelpTopic(String, Locale)
     */
    static public String getHelpTopicUrl(String absoluteHelpTopic) {
        String ret = getWikiBaseHelpUrl();
        ret = ret.replaceAll("\\/+$", "");
        absoluteHelpTopic  =absoluteHelpTopic.replace(" ", "%20");
        absoluteHelpTopic = absoluteHelpTopic.replaceAll("^\\/+", "/");
        return ret + absoluteHelpTopic;
    }

    /**
     * Replies the URL to the edit page for the absolute help topic.
     *
     * @param absoluteHelpTopic the absolute help topic
     * @return the URL to the edit page
     */
    static public String getHelpTopicEditUrl(String absoluteHelpTopic) {
        String topicUrl = getHelpTopicUrl(absoluteHelpTopic);
        topicUrl = topicUrl.replaceAll("#[^#]*$", ""); // remove optional fragment
        return topicUrl + "?action=edit";
    }

    /**
     * Extracts the relative help topic from an URL. Replies null, if
     * no relative help topic is found.
     *
     * @param url the url
     * @return the relative help topic in the URL, i.e. "/Action/New"
     */
    static public String extractRelativeHelpTopic(String url) {
        String topic = extractAbsoluteHelpTopic(url);
        if (topic == null) return null;
        String pattern = "/[A-Z][a-z]:" + getHelpTopicPrefix(Locale.ENGLISH).replaceAll("^\\/+", "");
        if (url.matches(pattern))
            return topic.substring(pattern.length());
        return null;
    }

    /**
     * Extracts the absolute help topic from an URL. Replies null, if
     * no absolute help topic is found.
     *
     * @param url the url
     * @return the absolute help topic in the URL, i.e. "/De:Help/Action/New"
     */
    static public String extractAbsoluteHelpTopic(String url) {
        if (!url.startsWith(getWikiBaseHelpUrl())) return null;
        url = url.substring(getWikiBaseHelpUrl().length());
        String prefix = getHelpTopicPrefix(Locale.ENGLISH);
        if (url.startsWith(prefix))
            return url;

        String pattern = "/[A-Z][a-z]:" + prefix.replaceAll("^\\/+", "");
        if (url.matches(pattern))
            return url;

        return null;
    }

    /**
     * Replies the help topic prefix for the current locale. Examples:
     * <ul>
     *   <li>/Help if the current locale is a locale with language "en"</li>
     *   <li>/De:Help if the current locale is a locale with language "de"</li>
     * </ul>
     *
     * @return the help topic prefix
     * @see #getHelpTopicPrefix(Locale)
     */
    static public String getHelpTopicPrefix() {
        return getHelpTopicPrefix(Locale.getDefault());
    }

    /**
     * Replies the help topic prefix for the given locale. Examples:
     * <ul>
     *   <li>/Help if the  locale is a locale with language "en"</li>
     *   <li>/De:Help if the  locale is a locale with language "de"</li>
     * </ul>
     *
     * @param locale the locale. {@see Locale#ENGLISH} assumed, if null.
     * @return the help topic prefix
     * @see #getHelpTopicPrefix(Locale)
     */
    static public String getHelpTopicPrefix(Locale locale) {
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        String ret = Main.pref.get("help.pathhelp", "/Help");
        ret = ret.replaceAll("^\\/+", ""); // remove leading /
        ret = "/" + LanguageInfo.getWikiLanguagePrefix(locale) + ret;
        return ret;
    }

    /**
     * Replies the absolute, localized help topic for the given topic.
     *
     * Example: for a topic "/Dialog/RelationEditor" and the locale "de", this method
     * replies "/De:Help/Dialog/RelationEditor"
     *
     * @param topic the relative help topic. Home help topic assumed, if null.
     * @param locale the locale. {@see Locale#ENGLISH} assumed, if null.
     * @return the absolute, localized help topic
     */
    static public String buildAbsoluteHelpTopic(String topic, Locale locale) {
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        if (topic == null || topic.trim().length() == 0 || topic.trim().equals("/"))
            return getHelpTopicPrefix(locale);
        String ret = getHelpTopicPrefix(locale);
        if (topic.startsWith("/")) {
            ret += topic;
        } else {
            ret += "/" + topic;
        }
        ret = ret.replaceAll("\\/+", "\\/"); // just in case, collapse sequences of //
        return ret;
    }

    /**
     * Replies the absolute, localized help topic for the given topic and the
     * current locale.
     *
     * @param topic the relative help topic. Home help topic assumed, if null.
     * @return the absolute, localized help topic
     * @see Locale#getDefault()
     * @see #buildAbsoluteHelpTopic(String, Locale)
     */
    static public String buildAbsoluteHelpTopic(String topic) {
        return buildAbsoluteHelpTopic(topic, Locale.getDefault());
    }

    /**
     * Replies the context specific help topic configured for <code>context</code>.
     *
     * @return the help topic. null, if no context specific help topic is found
     */
    static public String getContextSpecificHelpTopic(Object context) {
        if (context == null)
            return null;
        if (context instanceof Helpful)
            return ((Helpful)context).helpTopic();
        if (context instanceof JMenu) {
            JMenu b = (JMenu)context;
            if (b.getClientProperty("help") != null)
                return (String)b.getClientProperty("help");
            return null;
        }
        if (context instanceof AbstractButton) {
            AbstractButton b = (AbstractButton)context;
            if (b.getClientProperty("help") != null)
                return (String)b.getClientProperty("help");
            return getContextSpecificHelpTopic(b.getAction());
        }
        if (context instanceof Action)
            return (String)((Action)context).getValue("help");
        if (context instanceof JComponent && ((JComponent)context).getClientProperty("help") != null)
            return (String)((JComponent)context).getClientProperty("help");
        if (context instanceof Component)
            return getContextSpecificHelpTopic(((Component)context).getParent());
        return null;
    }

    /**
     * Replies the global help action, if available. Otherwise, creates an instance
     * of {@see HelpAction}.
     *
     * @return
     */
    static private Action getHelpAction() {
        try {
            return Main.main.menu.help;
        } catch(NullPointerException e) {
            return new HelpAction();
        }
    }

    /**
     * Makes a component aware of context sensitive help.
     *
     * A relative help topic doesn't start with /Help and doesn't include a locale
     * code. Example: /Dialog/RelationEditor is a relative help topic, /De:Help/Dialog/RelationEditor
     * is not.
     *
     * @param component the component  the component
     * @param topic the help topic. Set to the default help topic if null.
     */
    static public void setHelpContext(JComponent component, String relativeHelpTopic) {
        if (relativeHelpTopic == null) {
            relativeHelpTopic = "/";
        }
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F1"), "help");
        component.getActionMap().put("help", getHelpAction());
        component.putClientProperty("help", relativeHelpTopic);
    }

    /**
     * This is a simple marker method for help topic literals. If you declare a help
     * topic literal in the source you should enclose it in ht(...).
     *
     *  <strong>Example</strong>
     *  <pre>
     *     String helpTopic = ht("/Dialog/RelationEditor");
     *  or
     *     putValue("help", ht("/Dialog/RelationEditor"));
     *  </pre>
     *
     *
     * @param helpTopic
     */
    static public String ht(String helpTopic) {
        // this is just a marker method
        return helpTopic;
    }
}
