// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Label that contains a clickable link.
 * @author Imi
 */
public class UrlLabel extends JEditorPane implements HyperlinkListener {

    private final String url;

    public UrlLabel(String url) {
        this (url, url);
    }

    public UrlLabel(String url, String description) {
        this.url = url;
        setContentType("text/html");
        setText("<html><a href=\""+url+"\">"+description+"</a></html>");
        setToolTipText(url);
        setEditable(false);
        setOpaque(false);
        addHyperlinkListener(this);
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            OpenBrowser.displayUrl(url);
        }
    }
}
